package io.github.giovannilamarmora.accesssphere.webhooks;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientDomainService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.model.TokenExchange;
import io.github.giovannilamarmora.accesssphere.webhooks.client.WebhookClient;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookBody;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Servizio di orchestrazione per la gestione e l'invio asincrono dei Webhook di sistema.
 *
 * <p>Identifica i client OAuth2 con webhook associati all'azione, scambia il token Bearer tramite
 * Token Exchange per autenticare la chiamata esterna e notifica gli endpoint remoti in modo
 * resiliente.
 */
@Service
@Logged
public class WebhookService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private WebhookClient webhookClient;
  @Autowired @Lazy private OAuthService oAuthService;

  /**
   * Verifica ed esegue tutti i webhook configurati per l'azione specificata sui client OAuth2 censiti.
   *
   * @param action Azione/evento che innesca la notifica (es. {@link WebhookAction#DELETE_USER}).
   * @param bearer Token Bearer originale utilizzato per effettuare il Token Exchange.
   * @param identifier Identificatore univoco dell'entita' oggetto dell'evento (es. UUID utente).
   * @param exchange Scambio HTTP corrente per il contesto di autorizzazione.
   * @return Mono vuoto al completamento non bloccante di tutte le notifiche.
   */
  public Mono<Void> checkAndExecuteWebhook(
      WebhookAction action, String bearer, String identifier, ServerWebExchange exchange) {

    LOG.info("🚀 Webhook process initiated for action: {}, identifier: {}", action, identifier);

    if (ObjectToolkit.isNullOrEmpty(bearer) || exchange == null) {
      LOG.warn(
          "⚠️ Webhook execution skipped: bearer token or ServerWebExchange is null for action {}",
          action);
      return Mono.empty();
    }

    return oAuth2ClientDomainService
        .getOAuth2Clients()
        .flatMapMany(Flux::fromIterable)
        .filter(client -> !ObjectToolkit.isNullOrEmpty(client.getWebhooks()))
        .flatMap(client -> processClientWebhooks(client, action, bearer, identifier, exchange))
        .then()
        .doOnSuccess(v -> LOG.info("🏁 Webhook processing completed for action: {}", action));
  }

  /**
   * Elabora i webhook di un singolo client OAuth2, filtrando quelli corrispondenti all'azione.
   */
  private Mono<Void> processClientWebhooks(
      OAuth2Client client,
      WebhookAction action,
      String bearer,
      String identifier,
      ServerWebExchange exchange) {

    List<Webhook> matchingWebhooks =
        WebhookUtils.filterMatchingWebhooks(client.getWebhooks(), action);

    if (matchingWebhooks.isEmpty()) {
      LOG.debug("❌ Webhook {} not active for client: {}", action, client.getClientId());
      return Mono.empty();
    }

    LOG.info(
        "🎯 Found {} active webhook(s) for client: {} and action: {}",
        matchingWebhooks.size(),
        client.getClientId(),
        action);

    return exchangeTokenForClient(client.getClientId(), bearer, exchange)
        .flatMapMany(
            exchangedToken ->
                dispatchWebhooks(
                    matchingWebhooks, client.getClientId(), exchangedToken, identifier, action))
        .then();
  }

  /**
   * Esegue il Token Exchange per ottenere un access token valido per il client target.
   */
  private Mono<String> exchangeTokenForClient(
      String clientId, String bearer, ServerWebExchange exchange) {

    String cleanToken = WebhookUtils.cleanBearerToken(bearer);
    TokenExchange tokenExchange =
        new TokenExchange(
            GrantType.TOKEN_EXCHANGE.type(),
            cleanToken,
            "urn:ietf:params:oauth:token-type:access_token",
            clientId,
            "openid");

    return oAuthService
        .tokenExchange(bearer, tokenExchange, exchange)
        .flatMap(this::extractAccessToken)
        .doOnError(
            err ->
                LOG.error(
                    "🔥 Token exchange failed for client {}: {}", clientId, err.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  /**
   * Estrae la stringa dell'access token dalla risposta del token exchange in modo sicuro.
   */
  private Mono<String> extractAccessToken(ResponseEntity<Response> response) {
    if (ObjectToolkit.isNullOrEmpty(response.getBody())
        || ObjectToolkit.isNullOrEmpty(response.getBody().getData())) {
      LOG.error("❌ Token exchange response body or data is null");
      return Mono.empty();
    }
    try {
      OAuthTokenResponse oAuthTokenResponse =
          Mapper.convertObject(response.getBody().getData(), new TypeReference<>() {});
      if (oAuthTokenResponse != null
          && oAuthTokenResponse.getToken() != null
          && !ObjectToolkit.isNullOrEmpty(oAuthTokenResponse.getToken().getAccess_token())) {
        return Mono.just(oAuthTokenResponse.getToken().getAccess_token());
      }
    } catch (Exception e) {
      LOG.error(
          "❌ Failed to parse OAuthTokenResponse during webhook token exchange: {}",
          e.getMessage());
    }
    return Mono.empty();
  }

  /**
   * Invia le chiamate HTTP POST per la lista di webhook del client in parallelo non bloccante.
   */
  private Flux<ResponseEntity<Object>> dispatchWebhooks(
      List<Webhook> matchingWebhooks,
      String clientId,
      String exchangedToken,
      String identifier,
      WebhookAction action) {

    WebhookBody body = new WebhookBody(identifier, action);

    return Flux.fromIterable(matchingWebhooks)
        .flatMap(
            webhook ->
                webhookClient
                    .execute(webhook.getUrl(), exchangedToken, body)
                    .doOnSubscribe(
                        sub ->
                            LOG.info(
                                "✅ Executing webhook '{}' [{}] for client: {}",
                                webhook.getName(),
                                webhook.getUrl(),
                                clientId))
                    .doOnSuccess(
                        resp ->
                            LOG.info(
                                "🎉 Webhook '{}' successfully executed for client: {}",
                                webhook.getName(),
                                clientId))
                    .doOnError(
                        error ->
                            LOG.error(
                                "🔥 Error executing webhook '{}' [{}]: {}",
                                webhook.getName(),
                                webhook.getUrl(),
                                error.getMessage()))
                    .onErrorResume(e -> Mono.empty()));
  }
}
