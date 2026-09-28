package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientDeleteResponse;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientListResponse;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientResponse;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per l'amministrazione dei Client OAuth 2.0.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link OAuth2ClientController}.
 *   <li>Delega le verifiche di autorizzazione, scoping multi-tenant e logica applicativa a {@link
 *       OAuth2ClientService}.
 *   <li>Ha la responsabilita esclusiva di costruire le risposte HTTP {@link ResponseEntity}
 *       incapsulando i dati nei wrapper fortemente tipizzati {@link OAuth2ClientResponse}, {@link
 *       OAuth2ClientListResponse} e {@link OAuth2ClientDeleteResponse} corredati di codice di stato
 *       HTTP e distributed tracing (Span ID).
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.CLIENTS, description = OpenAPI.Description.CLIENTS)
public class OAuth2ClientControllerImpl implements OAuth2ClientController {

  private static final Logger LOG = LoggerFilter.getLogger(OAuth2ClientControllerImpl.class);

  @Autowired private OAuth2ClientService oAuth2ClientService;

  /**
   * Recupera tutti i client OAuth2 autorizzati per l'operatore chiamante.
   *
   * @param bearer Token Bearer di autenticazione.
   * @param request Richiesta HTTP server.
   * @return Mono con ResponseEntity contenente {@link OAuth2ClientListResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<OAuth2ClientListResponse>> getOAuth2Clients(
      String bearer, ServerHttpRequest request) {
    return oAuth2ClientService
        .getOAuth2Clients()
        .map(
            clients -> {
              String message =
                  "Successfully retrieved %s OAuth2 client(s).".formatted(clients.size());

              OAuth2ClientListResponse response =
                  new OAuth2ClientListResponse(
                      HttpStatus.OK.value(), message, TraceUtils.getSpanID(), clients);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Recupera i dettagli di un singolo client identificato dal suo Client ID.
   *
   * @param bearer Token Bearer di autenticazione.
   * @param clientId Identificativo univoco del client.
   * @param request Richiesta HTTP server.
   * @return Mono con ResponseEntity contenente {@link OAuth2ClientResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<OAuth2ClientResponse>> getOAuth2ClientByClientID(
      String bearer, String clientId, ServerHttpRequest request) {
    return oAuth2ClientService
        .getOAuth2ClientByClientID(clientId)
        .map(
            client -> {
              OAuth2ClientResponse response =
                  new OAuth2ClientResponse(
                      HttpStatus.OK.value(),
                      "Successfully retrieved %s OAuth2 client.".formatted(clientId),
                      TraceUtils.getSpanID(),
                      client);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Registra un nuovo client OAuth2 nel sistema.
   *
   * @param bearer Token Bearer di autenticazione.
   * @param oAuth2Client DTO con la configurazione del nuovo client.
   * @param request Richiesta HTTP server.
   * @return Mono con ResponseEntity con stato 201 CREATED e {@link OAuth2ClientResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<OAuth2ClientResponse>> createOAuth2Client(
      String bearer, OAuth2Client oAuth2Client, ServerHttpRequest request) {
    return oAuth2ClientService
        .saveOAuth2Client(oAuth2Client)
        .map(
            client -> {
              OAuth2ClientResponse response =
                  new OAuth2ClientResponse(
                      HttpStatus.CREATED.value(),
                      "Client %s created".formatted(oAuth2Client.getClientId()),
                      TraceUtils.getSpanID(),
                      client);
              return ResponseEntity.status(HttpStatus.CREATED).body(response);
            });
  }

  /**
   * Aggiorna le configurazioni di un client specificato da path variable.
   *
   * @param clientId Identificativo univoco del client.
   * @param bearer Token Bearer di autenticazione.
   * @param oAuth2Client DTO con le configurazioni aggiornate.
   * @param request Richiesta HTTP server.
   * @return Mono con ResponseEntity contenente {@link OAuth2ClientResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<OAuth2ClientResponse>> updateOAuth2ClientByClientId(
      String clientId, String bearer, OAuth2Client oAuth2Client, ServerHttpRequest request) {
    return oAuth2ClientService
        .updateOAuth2ClientByClientId(oAuth2Client, clientId)
        .map(
            client -> {
              OAuth2ClientResponse response =
                  new OAuth2ClientResponse(
                      HttpStatus.OK.value(),
                      "Client %s updated".formatted(oAuth2Client.getClientId()),
                      TraceUtils.getSpanID(),
                      client);
              return ResponseEntity.status(HttpStatus.OK).body(response);
            });
  }

  /**
   * Elimina un client OAuth2 dal database IAM.
   *
   * @param bearer Token Bearer di autenticazione.
   * @param clientId Identificativo univoco del client da eliminare.
   * @param request Richiesta HTTP server.
   * @return Mono con ResponseEntity contenente {@link OAuth2ClientDeleteResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<OAuth2ClientDeleteResponse>> deleteClient(
      String bearer, String clientId, ServerHttpRequest request) {
    oAuth2ClientService.deleteOAuth2ClientByClientId(clientId);
    OAuth2ClientDeleteResponse response =
        new OAuth2ClientDeleteResponse(
            HttpStatus.ACCEPTED.value(),
            "Successfully deleted Client %s".formatted(clientId),
            TraceUtils.getSpanID(),
            null);
    return Mono.just(ResponseEntity.accepted().body(response));
  }
}
