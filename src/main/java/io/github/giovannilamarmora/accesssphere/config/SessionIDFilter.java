package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionHandler;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
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
  private static final String SESSION_COOKIE_NAME = "Session-ID";

  @Value(value = "${filter.session-id.shouldNotFilter}")
  private List<String> shouldNotFilter;

  @Value(value = "${filter.session-id.generateSessionURI}")
  private List<String> generateSessionURI;

  private final SessionID sessionID;
  private final AccessTokenData accessTokenData;

  @Autowired private AccessTokenService accessTokenService;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (WebManager.shouldNotFilter(exchange.getRequest(), shouldNotFilter))
      return chain.filter(exchange);

    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    HttpCookie sessionCookie = request.getCookies().getFirst(SESSION_COOKIE_NAME);
    String session_id = null;
    if (isGenerateSessionURI(request)) {
      session_id = SessionID.builder().generate();
      LOG.info("Generating new session ID: {}", session_id);
      response.addCookie(CookieManager.setCookie(SESSION_COOKIE_NAME, session_id));
      addSessionInContext(session_id);
      return chain.filter(exchange);
    } else if (ObjectUtils.isEmpty(sessionCookie)
        || ObjectUtils.isEmpty(sessionCookie.getValue())) {
      LOG.error("Session ID not found, needs login");
      return ExceptionHandler.handleFilterException(
          new UserException(ExceptionMap.ERR_OAUTH_401, "Invalid Session ID Provided!"), exchange);
    }

    session_id = sessionCookie.getValue();

    String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (ObjectUtils.isEmpty(bearer)) {
      LOG.error("Missing Authorization Header");
      return ExceptionHandler.handleFilterException(
          new OAuthException(ExceptionMap.ERR_OAUTH_401, "Missing Authorization Header"), exchange);
    }

    AccessTokenData accessTokenDB = accessTokenService.getByAccessTokenOrIdToken(bearer);
    if (ObjectUtils.isEmpty(accessTokenDB.getSessionId())
        || !accessTokenDB.getSessionId().equalsIgnoreCase(session_id)) {
      LOG.error(
          "Invalid session_id, should be {} instead to {}",
          accessTokenDB.getSessionId(),
          session_id);
      return ExceptionHandler.handleFilterException(
          new OAuthException(ExceptionMap.ERR_OAUTH_403, "Invalid Session ID!"), exchange);
    }
    addSessionInContext(session_id);
    BeanUtils.copyProperties(accessTokenDB, accessTokenData);
    return chain.filter(exchange);
  }

  private boolean isGenerateSessionURI(ServerHttpRequest req) {
    String path = req.getPath().value();
    return isTokenEndpointWithPasswordGrant(req, path) || isConfiguredGenerateSessionURI(path);
  }

  private boolean isTokenEndpointWithPasswordGrant(ServerHttpRequest req, String path) {
    if (path.equalsIgnoreCase("/v1/oAuth/2.0/token")) {
      String param = req.getQueryParams().getFirst("grant_type");
      return !ObjectUtils.isEmpty(param) && param.equalsIgnoreCase(GrantType.PASSWORD.type());
    }
    return false;
  }

  private boolean isConfiguredGenerateSessionURI(String path) {
    return generateSessionURI.stream()
        .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path));
  }

  private void addSessionInContext(String session_id) {
    SessionID sessionID1 = new SessionID(session_id);
    BeanUtils.copyProperties(sessionID1, sessionID);
  }
}
