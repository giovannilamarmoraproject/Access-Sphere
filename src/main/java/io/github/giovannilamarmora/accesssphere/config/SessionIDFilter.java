package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionHandler;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.utils.logger.MDCUtils;
import io.github.giovannilamarmora.utils.web.CookieManager;
import io.github.giovannilamarmora.utils.web.WebManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(value = 2)
@RequiredArgsConstructor
public class SessionIDFilter implements WebFilter {

  private static final Logger LOG = LoggerFactory.getLogger(SessionIDFilter.class);

  @Value(value = "${filter.session-id.shouldNotFilter}")
  private List<String> shouldNotFilter;

  @Value(value = "${filter.session-id.generateSessionURI}")
  private List<String> generateSessionURI;

  @Value(value = "${filter.session-id.logoutURI}")
  private String logoutURI;

  @Value(value = "${filter.session-id.bearerNotFilter}")
  private List<String> bearerNotFilterURI;

  private final SessionID sessionID;
  @Autowired private AccessTokenData accessTokenData;

  @Autowired private AccessTokenService accessTokenService;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (WebManager.shouldNotFilter(exchange.getRequest(), shouldNotFilter))
      return chain.filter(exchange);

    if (isLogout(exchange.getRequest())) return logoutFilter(exchange, chain);

    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    HttpCookie sessionCookie = request.getCookies().getFirst(SessionID.SESSION_COOKIE_NAME);
    String session_id = null;
    if (isGenerateSessionURI(request)) {
      session_id = SessionID.builder().generate();
      LOG.info("Generating new session ID: {}", session_id);
      response.addCookie(CookieManager.setCookie(SessionID.SESSION_COOKIE_NAME, session_id));
      addSessionInContext(session_id);
      return chain.filter(exchange);
    } else if (ObjectUtils.isEmpty(sessionCookie)
        || ObjectUtils.isEmpty(sessionCookie.getValue())) {
      LOG.error("Session ID not found for path {}, needs login", request.getPath().value());
      return ExceptionHandler.handleFilterException(
          new UserException(ExceptionMap.ERR_OAUTH_401, "No Session ID Provided!"), exchange);
    }

    session_id = sessionCookie.getValue();

    if (isBearerNotRequiredEndpoint(request)) {
      addSessionInContext(session_id);
      return chain.filter(exchange);
    }

    String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (ObjectUtils.isEmpty(bearer)) {
      LOG.error("Missing Authorization Header");
      return ExceptionHandler.handleFilterException(
          new OAuthException(ExceptionMap.ERR_OAUTH_401, "Missing Authorization Header"), exchange);
    }

    AccessTokenData accessTokenDB = new AccessTokenData();
    try {
      accessTokenDB = accessTokenService.getByAccessTokenOrIdToken(bearer);
    } catch (TokenException e) {
      LOG.error("An error happen during validation of token, message is {}", e.getMessage());
      return ExceptionHandler.handleFilterException(
          new OAuthException(e.getExceptionCode(), e.getMessage()), exchange);
    }

    if (ObjectUtils.isEmpty(accessTokenDB)
        || ObjectUtils.isEmpty(accessTokenDB.getSessionId())
        || !accessTokenDB.getSessionId().equalsIgnoreCase(session_id)) {
      LOG.error(
          "Invalid session_id, should be {} instead to {}",
          ObjectUtils.isEmpty(accessTokenDB) ? null : accessTokenDB.getSessionId(),
          session_id);
      return ExceptionHandler.handleFilterException(
          new OAuthException(ExceptionMap.ERR_OAUTH_403, "Invalid Session ID Provided!"), exchange);
    }
    addSessionInContext(session_id);
    BeanUtils.copyProperties(accessTokenDB, accessTokenData);
    return chain.filter(exchange);
  }

  public Mono<Void> logoutFilter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    HttpCookie sessionCookie = request.getCookies().getFirst(SessionID.SESSION_COOKIE_NAME);
    String session_id = null;
    if (ObjectUtils.isEmpty(sessionCookie) || ObjectUtils.isEmpty(sessionCookie.getValue())) {
      LOG.warn("Session ID not found, already logged out");
    } else session_id = sessionCookie.getValue();

    String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (ObjectUtils.isEmpty(bearer)) {
      LOG.error("Missing Authorization Header");
      return ExceptionHandler.handleFilterException(
          new OAuthException(ExceptionMap.ERR_OAUTH_401, "Missing Authorization Header"), exchange);
    }

    AccessTokenData accessTokenDB = new AccessTokenData();
    try {
      accessTokenDB = accessTokenService.getByAccessTokenOrIdToken(bearer);
      if (ObjectUtils.isEmpty(accessTokenDB)
          || ObjectUtils.isEmpty(accessTokenDB.getSessionId())
          || !accessTokenDB.getSessionId().equalsIgnoreCase(session_id)) {
        LOG.error(
            "Invalid session_id, should be {} instead to {}",
            ObjectUtils.isEmpty(accessTokenDB) ? null : accessTokenDB.getSessionId(),
            session_id);
        return ExceptionHandler.handleFilterException(
            new OAuthException(ExceptionMap.ERR_OAUTH_403, "Invalid Session ID Provided!"),
            exchange);
      }
      addSessionInContext(session_id);
    } catch (TokenException e) {
      LOG.warn("Access Token not found, already logged out");
    }
    BeanUtils.copyProperties(accessTokenDB, accessTokenData);
    return chain.filter(exchange);
  }

  private boolean isGenerateSessionURI(ServerHttpRequest req) {
    String path = req.getPath().value();
    return isTokenEndpointWithPasswordGrant(req, path) || isConfiguredGenerateSessionURI(path);
  }

  private boolean isTokenEndpointWithPasswordGrant(ServerHttpRequest req, String path) {
    if (PatternMatchUtils.simpleMatch("*/v1/oAuth/2.0/token", path)) {
      String param = req.getQueryParams().getFirst("grant_type");
      return !ObjectUtils.isEmpty(param) && param.equalsIgnoreCase(GrantType.PASSWORD.type());
    }
    return false;
  }

  private boolean isBearerNotRequiredEndpoint(ServerHttpRequest req) {
    String path = req.getPath().value();
    return bearerNotFilterURI.stream()
        .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path));
  }

  private boolean isConfiguredGenerateSessionURI(String path) {
    return generateSessionURI.stream()
        .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path));
  }

  private void addSessionInContext(String session_id) {
    SessionID sessionID1 = new SessionID(session_id);
    BeanUtils.copyProperties(sessionID1, sessionID);
    MDCUtils.setDataIntoMDC("Session-ID", session_id);
  }

  private boolean isLogout(ServerHttpRequest request) {
    String path = request.getPath().value();
    return PatternMatchUtils.simpleMatch(logoutURI, path);
  }
}
