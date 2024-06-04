package io.github.giovannilamarmora.accesssphere.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionHandler;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RateLimitingFilter implements WebFilter {

  private final Bucket bucket;

  @Value(value = "${filter.rate-limiting.shouldFilter}")
  private List<String> shouldFilter;

  public RateLimitingFilter() {
    Bandwidth limit =
        Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build();
    this.bucket = Bucket.builder().addLimit(limit).build();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Applica rate limiting solo agli endpoint desiderati
    if (shouldFilter(exchange.getRequest())) {
      if (bucket.tryConsume(1)) {
        return chain.filter(exchange);
      } else {
        return ExceptionHandler.handleFilterException(
            new UserException(ExceptionMap.ERR_USER_429, ExceptionMap.ERR_USER_429.getMessage()),
            exchange);
      }
    } else {
      return chain.filter(exchange);
    }
  }

  protected boolean shouldFilter(ServerHttpRequest req) {
    String path = req.getPath().value();
    String method = req.getMethod().name();
    if (HttpMethod.OPTIONS.name().equals(method)) {
      return true;
    }
    return shouldFilter.stream()
        .anyMatch(endpoint -> PatternMatchUtils.simpleMatch(endpoint, path));
  }
}
