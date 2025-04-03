package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionHandler;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMessage;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.utilities.*;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.MDCUtils;
import io.github.giovannilamarmora.utils.web.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

  @Value("${cookie-domain}")
  private String cookieDomain;

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

    String session_id = RequestManager.getCookieOrHeaderData(Cookie.COOKIE_SESSION_ID, request);

    if (isGenerateSessionURI(request) || isAuthorizeCheckWithBearerNull(request)) {
      session_id = SessionID.builder().generate();
      LOG.info("Generating new session ID: {}", session_id);
      setSessionIDInResponse(session_id, response, request);
      addSessionInContext(session_id);
      return chain.filter(exchange);
    } else if (ObjectUtils.isEmpty(session_id)) {
      LOG.error(
          "Session ID not found for path [{}], with hostname {}, needs login",
          request.getPath().value(),
          request.getHeaders().get("Referer"));
      return ExceptionHandler.handleFilterException(
          new UserException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMessage.NO_SESSION_ID.getMessage()),
          exchange);
    }

    if (isBearerNotRequiredEndpoint(request) || isAuthorizeCheckWithBearer(request)) {
      setSessionIDInResponse(session_id, response, request);
      addSessionInContext(session_id);
      return chain.filter(exchange);
    }

    String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (ObjectUtils.isEmpty(bearer)) {
      LOG.error(ExceptionMessage.AUTHORIZATION_HEADER.getMessage());
      return ExceptionHandler.handleFilterException(
          new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMessage.AUTHORIZATION_HEADER.getMessage()),
          exchange);
    }

    AccessTokenData accessTokenDB = new AccessTokenData();
    try {
      LOG.info("Checking Bearer token for request {}", request.getPath());
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
          ExceptionMessage.SESSION_SHOULD_BE.getMessage(),
          ObjectUtils.isEmpty(accessTokenDB) ? null : accessTokenDB.getSessionId(),
          session_id);
      return ExceptionHandler.handleFilterException(
          new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMessage.INVALID_SESSION_ID.getMessage()),
          exchange);
    }
    addSessionInContext(session_id);
    setSessionIDInResponse(session_id, response, request);
    BeanUtils.copyProperties(accessTokenDB, accessTokenData);
    return chain.filter(exchange);
  }

  public Mono<Void> logoutFilter(ServerWebExchange exchange, WebFilterChain chain) {
    LOG.info("Logout Filter detect");
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    String session_id = RequestManager.getCookieOrHeaderData(Cookie.COOKIE_SESSION_ID, request);

    if (ObjectUtils.isEmpty(session_id)) {
      LOG.warn("Session ID not found, already logged out");
    }

    String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (ObjectUtils.isEmpty(bearer)) {
      LOG.error(ExceptionMessage.AUTHORIZATION_HEADER.getMessage());
      return ExceptionHandler.handleFilterException(
          new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMessage.AUTHORIZATION_HEADER.getMessage()),
          exchange);
    }

    AccessTokenData accessTokenDB = new AccessTokenData();
    try {
      accessTokenDB = accessTokenService.getByAccessTokenOrIdToken(bearer);
      if (ObjectUtils.isEmpty(accessTokenDB)
          || ObjectUtils.isEmpty(accessTokenDB.getSessionId())
          || !accessTokenDB.getSessionId().equalsIgnoreCase(session_id)) {
        LOG.error(
            ExceptionMessage.SESSION_SHOULD_BE.getMessage(),
            ObjectUtils.isEmpty(accessTokenDB) ? null : accessTokenDB.getSessionId(),
            session_id);
        return ExceptionHandler.handleFilterException(
            new OAuthException(
                ExceptionMap.ERR_OAUTH_401, ExceptionMessage.INVALID_SESSION_ID.getMessage()),
            exchange);
      }
      addSessionInContext(session_id);
      setSessionIDInResponse(session_id, response, request);
    } catch (TokenException e) {
      LOG.warn("Access Token not found, already logged out");
    }
    BeanUtils.copyProperties(accessTokenDB, accessTokenData);
    return chain.filter(exchange);
  }

  private boolean isGenerateSessionURI(ServerHttpRequest req) {
    String path = req.getPath().value();
    return isTokenEndpointWithPasswordGrant(req, path) || isConfiguredGenerateSessionURI(req, path);
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
    String codeParam = req.getQueryParams().getFirst("code");
    String refreshTokenParam = req.getQueryParams().getFirst("refresh_token");

    // Se è l'endpoint token e ha il parametro code (authorization code grant)
    boolean isTokenPathWithCode =
        PatternMatchUtils.simpleMatch("*/v1/oAuth/2.0/token", path) && codeParam != null;

    // Se è l'endpoint token e ha il parametro refresh_token (refresh token grant)
    boolean isTokenPathWithRefresh =
        PatternMatchUtils.simpleMatch("*/v1/oAuth/2.0/token", path) && refreshTokenParam != null;

    return isTokenPathWithCode
        || isTokenPathWithRefresh
        || bearerNotFilterURI.stream()
            .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path));
  }

  private boolean isConfiguredGenerateSessionURI(ServerHttpRequest req, String path) {
    return generateSessionURI.stream()
            .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path))
        && !req.getHeaders().containsKey("Authorization");
  }

  private boolean isAuthorizeCheckWithBearerNull(ServerHttpRequest request) {
    String auth = request.getHeaders().getFirst("Authorization");

    boolean isNull = ObjectUtils.isEmpty(auth) || auth.equalsIgnoreCase("null");
    return (request.getPath().value().equalsIgnoreCase("/v1/oAuth/2.0/authorize")
        && request.getHeaders().containsKey("Authorization")
        && isNull);
  }

  private boolean isAuthorizeCheckWithBearer(ServerHttpRequest request) {
    return (request.getPath().value().equalsIgnoreCase("/v1/oAuth/2.0/authorize")
        && request.getHeaders().containsKey("Authorization"));
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

  private void setSessionIDInResponse(
      String sessionId, ServerHttpResponse response, ServerHttpRequest request) {
    // CookieManager.setCookieInResponse(Cookie.COOKIE_SESSION_ID, sessionId, cookieDomain,
    // response);
    // HeaderManager.addHeaderInResponse(ExposedHeaders.SESSION_ID, sessionId, response);
    // ResponseManager.setCookieAndHeaderData(ExposedHeaders.SESSION_ID, sessionId, response,
    // request);
    setCookieAndHeaderData(ExposedHeaders.SESSION_ID, sessionId, response, request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static void setCookieAndHeaderData(
      String headerName,
      String headerValue,
      ServerHttpResponse response,
      ServerHttpRequest request) {

    // Set the specified header in the response
    HeaderManager.addOrSetHeaderInResponse(headerName, headerValue, response);

    // Set the corresponding cookie with domain handling based on Referer, Origin, or Host
    CookieManager.setCookieInResponse(headerName, headerValue, response, request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static void setCookieInResponse(
      String cookieName,
      String cookieValue,
      ServerHttpResponse response,
      ServerHttpRequest request) {

    String redirectUri = request.getQueryParams().getFirst("redirect_uri");
    // Extract domain from Referer or Origin (keeping URL handling as before)
    String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);
    String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
    String host = request.getHeaders().getFirst(HttpHeaders.HOST);
    String url =
        (redirectUri != null)
            ? redirectUri
            : (origin != null && !origin.equals("*") ? origin : (host != null ? host : referer));
    String domain = WebManager.extractDomain(url);

    // Create the cookie with security attributes
    ResponseCookie.ResponseCookieBuilder cookieBuilder =
        ResponseCookie.from(cookieName, cookieValue)
            .maxAge(360000)
            .sameSite("None")
            .secure(true)
            .httpOnly(true)
            .path("/");

    // Set domain if available
    if (domain != null) {
      cookieBuilder.domain(domain);
    }

    // Add cookie to response
    response.getHeaders().add(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());

    LOG.debug("Set Cookie {}, with value {}, successfully in Response", cookieName, cookieValue);
  }
}
