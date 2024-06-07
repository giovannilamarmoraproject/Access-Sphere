package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionHandler;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.utils.web.CookieManager;
import io.github.giovannilamarmora.utils.web.WebManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
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
public class SessionIDFilter implements WebFilter {

  private static final Logger LOG = LoggerFactory.getLogger(SessionIDFilter.class);
  private static final String SESSION_COOKIE_NAME = "Session-ID";

  @Value(value = "${filter.session-id.shouldNotFilter}")
  private List<String> shouldNotFilter;

  @Value(value = "${filter.session-id.generateSessionURI}")
  private List<String> generateSessionURI;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (WebManager.shouldNotFilter(exchange.getRequest(), shouldNotFilter))
      return chain.filter(exchange);

    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    HttpCookie sessionCookie = request.getCookies().getFirst(SESSION_COOKIE_NAME);

    if (isGenerateSessionURI(request)) {
      String newSessionID = SessionID.builder().generate();
      LOG.info("Generating new session ID: {}", newSessionID);
      response.addCookie(CookieManager.setCookie(SESSION_COOKIE_NAME, newSessionID));
    } else if (ObjectUtils.isEmpty(sessionCookie)
        || ObjectUtils.isEmpty(sessionCookie.getValue())) {
      LOG.error("Session ID not found, needs login");
      return ExceptionHandler.handleFilterException(
          new UserException(ExceptionMap.ERR_OAUTH_401, "Invalid Session ID Provided!"), exchange);
    }

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
}
