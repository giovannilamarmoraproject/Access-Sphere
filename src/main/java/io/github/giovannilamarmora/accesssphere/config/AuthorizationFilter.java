package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechnicalException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionHandler;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.utils.web.WebManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(value = 3)
@RequiredArgsConstructor
public class AuthorizationFilter implements WebFilter {

  private static final Logger LOG = LoggerFactory.getLogger(AuthorizationFilter.class);

  @Value(value = "${filter.authorization.shouldFilter}")
  private List<String> shouldFilter;

  @Autowired private TechUserService techUserService;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

    if (!WebManager.shouldFilter(exchange.getRequest(), shouldFilter))
      return chain.filter(exchange);

    LOG.info(
        "Filtering Authorization for {} for method {}",
        exchange.getRequest().getPath().value(),
        exchange.getRequest().getMethod());

    if (techUserService.isTechUser()) {
      LOG.error("The subject type {} cannot make this request", SubjectType.TECHNICAL);
      return ExceptionHandler.handleFilterException(
          new TechnicalException(ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage()),
          exchange);
    }
    return chain.filter(exchange);
  }
}
