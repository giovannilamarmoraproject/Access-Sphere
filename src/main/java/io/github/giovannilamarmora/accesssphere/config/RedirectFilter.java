package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.utilities.ExposedHeaders;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.web.HeaderManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RedirectFilter implements WebFilter {
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  private final SessionID sessionID;
  List<String> shouldFilterURL =
      List.of("/v1/oAuth/2.0/authorize", "/v1/oAuth/2.0/token", "/login");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    if (shouldFilter(request)) {
      HeaderManager.addOrSetHeaderInResponse(
          ExposedHeaders.SESSION_ID, sessionID.getSessionID(), exchange.getResponse());
      HeaderManager.addOrSetHeaderInResponse(
          ExposedHeaders.REDIRECT_URI,
          request.getQueryParams().getFirst("redirect_uri"),
          exchange.getResponse());
      HeaderManager.addOrSetHeaderInResponse(
          ExposedHeaders.REGISTRATION_TOKEN,
          request.getQueryParams().getFirst("registration_token"),
          exchange.getResponse());
      // LOG.info("Filtering Redirect for path {}", exchange.getRequest().getPath());
    }
    return chain.filter(exchange);
  }

  protected boolean shouldFilter(ServerHttpRequest req) {
    String path = req.getPath().value();
    String method = req.getMethod().name();
    if (HttpMethod.OPTIONS.name().equals(method)) {
      return false;
    }
    return shouldFilterURL.stream()
        .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path));
  }
}
