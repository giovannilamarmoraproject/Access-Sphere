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
        .getClients()
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
  public Mono<ResponseEntity<OAuth2ClientResponse>> getClientById(
      String bearer, String clientId, ServerHttpRequest request) {
    LOG.info("🌐 [CLIENT CONTROLLER] Retrieving client details for client ID: {}", clientId);
    return oAuth2ClientService
        .getClientById(clientId)
        .map(
            client -> {
              OAuth2ClientResponse response =
                  new OAuth2ClientResponse(
                      HttpStatus.OK.value(), "Client details", TraceUtils.getSpanID(), client);
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
  public Mono<ResponseEntity<OAuth2ClientResponse>> createClient(
      String bearer, OAuth2Client oAuth2Client, ServerHttpRequest request) {
    LOG.info(
        "🌐 [CLIENT CONTROLLER] Registering new OAuth 2.0 client: {}", oAuth2Client.getClientId());
    oAuth2ClientService.addClientToDatabase(oAuth2Client);
    OAuth2ClientResponse response =
        new OAuth2ClientResponse(
            HttpStatus.CREATED.value(), "Client created", TraceUtils.getSpanID(), oAuth2Client);
    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(response));
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
  public Mono<ResponseEntity<OAuth2ClientResponse>> updateClientById(
      String clientId, String bearer, OAuth2Client oAuth2Client, ServerHttpRequest request) {
    LOG.info("🌐 [CLIENT CONTROLLER] Updating OAuth 2.0 client by path ID: {}", clientId);
    if (oAuth2Client.getClientId() == null) {
      oAuth2Client.setClientId(clientId);
    }
    oAuth2ClientService.updateClientInDatabase(oAuth2Client);
    OAuth2ClientResponse response =
        new OAuth2ClientResponse(
            HttpStatus.OK.value(), "Client updated", TraceUtils.getSpanID(), oAuth2Client);
    return Mono.just(ResponseEntity.ok(response));
  }

  /**
   * Aggiorna le configurazioni di un client specificato nel body della richiesta.
   *
   * @param bearer Token Bearer di autenticazione.
   * @param oAuth2Client DTO con le configurazioni aggiornate.
   * @param request Richiesta HTTP server.
   * @return Mono con ResponseEntity contenente {@link OAuth2ClientResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<OAuth2ClientResponse>> updateClient(
      String bearer, OAuth2Client oAuth2Client, ServerHttpRequest request) {
    LOG.info("🌐 [CLIENT CONTROLLER] Updating OAuth 2.0 client: {}", oAuth2Client.getClientId());
    oAuth2ClientService.updateClientInDatabase(oAuth2Client);
    OAuth2ClientResponse response =
        new OAuth2ClientResponse(
            HttpStatus.OK.value(), "Client updated", TraceUtils.getSpanID(), oAuth2Client);
    return Mono.just(ResponseEntity.ok(response));
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
    LOG.info("🌐 [CLIENT CONTROLLER] Deleting OAuth 2.0 client: {}", clientId);
    OAuth2Client cred = new OAuth2Client();
    cred.setClientId(clientId);
    oAuth2ClientService.deleteClientFromDatabase(cred);
    OAuth2ClientDeleteResponse response =
        new OAuth2ClientDeleteResponse(
            HttpStatus.OK.value(), "Client deleted", TraceUtils.getSpanID(), null);
    return Mono.just(ResponseEntity.ok(response));
  }
}
