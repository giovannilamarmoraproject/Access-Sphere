package io.github.giovannilamarmora.accesssphere.webhooks.client;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

/**
 * Client HTTP reattivo per l'invio delle notifiche POST verso gli endpoint Webhook registrati.
 */
@Component
@Logged
public class WebhookClient {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private final WebClientRest webClientRest = new WebClientRest();

  @Autowired private WebClient.Builder builder;

  @PostConstruct
  void init() {
    webClientRest.init(builder);
  }

  /**
   * Invia una notifica HTTP POST all'URL del Webhook con payload JSON e Bearer token.
   *
   * @param url URL di destinazione dell'endpoint webhook.
   * @param bearer Token di accesso Bearer da includere nell'header Authorization.
   * @param body Payload JSON contenente i dettagli dell'evento.
   * @return Mono contenente la risposta dell'endpoint remoto.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<Object>> execute(String url, String bearer, WebhookBody body) {
    LOG.info("📡 Sending webhook notification to URL: {}", url);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (bearer != null && !bearer.isBlank()) {
      headers.setBearerAuth(bearer.replaceFirst("^(?i)Bearer\\s+", ""));
    }

    Function<UriBuilder, URI> finalUrl = UtilsUriBuilder.buildUri(url, null);

    return webClientRest.perform(HttpMethod.POST, finalUrl, body, headers, Object.class);
  }
}
