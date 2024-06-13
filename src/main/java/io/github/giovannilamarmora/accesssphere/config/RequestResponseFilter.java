package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.web.WebManager;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(value = 10)
public class RequestResponseFilter implements WebFilter {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value(value = "${filter.requestResponse.shouldNotFilter}")
  private List<String> shouldNotFilter;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (WebManager.shouldNotFilter(exchange.getRequest(), shouldNotFilter))
      return chain.filter(exchange);
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    if (shouldFilter(exchange.getRequest())) {
      return Mono.defer(
          () -> {
            LOG.info(
                "[REQUEST] Received Request: {} {} from {}",
                request.getMethod(),
                request.getURI(),
                request.getHeaders().get("Referer"));
            return chain
                .filter(exchange)
                .then(
                    Mono.fromRunnable(
                        () -> {
                          LOG.info(
                              "[RESPONSE] Sent Response: {} with status {} to {}",
                              request.getURI(),
                              response.getStatusCode(),
                              request.getHeaders().get("Referer"));
                        }));
          });
    } else {
      return chain.filter(exchange);
    }
  }

  protected boolean shouldFilter(ServerHttpRequest req) {
    String method = req.getMethod().name();
    return !HttpMethod.OPTIONS.name().equals(method);
  }
}
