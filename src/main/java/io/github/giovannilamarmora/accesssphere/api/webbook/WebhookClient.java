package io.github.giovannilamarmora.accesssphere.api.webbook;

import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookBody;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.webClient.UtilsUriBuilder;
import io.github.giovannilamarmora.utils.webClient.WebClientRest;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

@Component
@Logged
public class WebhookClient {

  public final Logger LOG = LoggerFilter.getLogger(this.getClass());
  public final WebClientRest webClientRest = new WebClientRest();

  @Autowired public WebClient.Builder builder;

  @PostConstruct
  void init() {
    webClientRest.init(builder);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<Object>> execute(String url, String bearer, WebhookBody body) {

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    Function<UriBuilder, URI> finalUrl = UtilsUriBuilder.buildUri(url, null);

    return webClientRest.perform(HttpMethod.POST, finalUrl, body, headers, Object.class);
  }
}
