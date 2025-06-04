package io.github.giovannilamarmora.accesssphere.webhooks;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.accesssphere.api.webbook.WebhookClient;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.model.TokenExchange;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookBody;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Logged
public class WebhookService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private ClientService clientService;
  @Autowired private WebhookClient webhookClient;
  @Autowired private OAuthService oAuthService;

  public Mono<Void> checkAndExecuteWebhook(
      WebhookAction action, String bearer, String identifier, ServerWebExchange exchange) {

    LOG.info("🚀 Webhooks process check started for action: {}", action);

    return clientService
        .getClientCredentials()
        .flatMapMany(Flux::fromIterable)
        .filter(clientCredential -> !ObjectToolkit.isNullOrEmpty(clientCredential.getWebhooks()))
        .flatMap(
            clientCredential -> {
              List<Webhook> matchingWebhooks =
                  clientCredential.getWebhooks().stream()
                      .filter(webhook -> webhook.getAction().equals(action))
                      .collect(Collectors.toList());

              if (matchingWebhooks.isEmpty()) {
                LOG.info(
                    "❌ Webhook {} not active for client: {}",
                    action,
                    clientCredential.getClientId());
                return Mono.empty();
              }

              TokenExchange tokenExchange =
                  new TokenExchange(
                      GrantType.TOKEN_EXCHANGE.type(),
                      bearer.contains("Bearer") ? bearer.split("Bearer ")[1] : bearer,
                      "urn:ietf:params:oauth:token-type:access_token",
                      clientCredential.getClientId(),
                      "openid");

              return oAuthService
                  .tokenExchange(bearer, tokenExchange, exchange)
                  .flatMapMany(
                      response -> {
                        if (ObjectToolkit.isNullOrEmpty(response.getBody())) {
                          LOG.error("Webhook response is null");
                          return Mono.empty();
                        }
                        OAuthTokenResponse oAuthTokenResponse =
                            Mapper.convertObject(
                                response.getBody().getData(), new TypeReference<>() {});
                        String exchangedToken = oAuthTokenResponse.getToken().getAccess_token();
                        return Flux.fromIterable(matchingWebhooks)
                            .flatMap(
                                webhook ->
                                    webhookClient
                                        .execute(
                                            webhook.getUrl(),
                                            exchangedToken, // Usa il nuovo token
                                            new WebhookBody(identifier, action))
                                        .doOnSubscribe(
                                            sub ->
                                                LOG.info(
                                                    "✅ Executing webhook {} for client: {}",
                                                    webhook.getUrl(),
                                                    clientCredential.getClientId()))
                                        .doOnError(
                                            error ->
                                                LOG.error(
                                                    "🔥 Error executing webhook {}: {}",
                                                    webhook.getUrl(),
                                                    error.getMessage()))
                                        .onErrorResume(e -> Mono.empty()));
                      });
            })
        .then();
  }
}
