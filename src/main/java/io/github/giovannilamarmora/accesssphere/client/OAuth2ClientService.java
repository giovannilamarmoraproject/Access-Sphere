package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientDataResponse;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientRequest;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Application Service per la gestione dei Client OAuth 2.0.
 *
 * <p>Responsabilita:
 *
 * <ul>
 *   <li>Funge da Gatekeeper di sicurezza e perimetro per gli endpoint REST consumati dalla UI o da
 *       chiamanti API.
 *   <li>Verifica l'accesso alla console e i permessi granulari (es. {@code clients:read}, {@code
 *       clients:write}, {@code clients:delete}) tramite {@link PermissionRoleManager}.
 *   <li>Applica lo Scoping Multi-Tenant: per gli operatori delegati restituisce e consente
 *       l'amministrazione unicamente dei client presenti nei loro {@code managedClients}.
 *   <li>Delega le operazioni effettive di persistenza al Domain Service sottostante {@link
 *       OAuth2ClientDomainService}.
 *   <li>Restituisce direttamente modelli DTO puri; la costruzione delle risposte HTTP {@link
 *       org.springframework.http.ResponseEntity} e delegata al Controller.
 * </ul>
 */
@Service
public class OAuth2ClientService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private PermissionRoleManager permissionRoleManager;
  @Autowired private OAuth2Authorization oAuth2Authorization;

  /**
   * Recupera la lista dei Client registrati applicando i vincoli di perimetro (Data Scoping)
   * dell'operatore chiamante.
   *
   * @return {@link Mono} con la lista di {@link OAuth2Client} autorizzati per l'operatore.
   */
  public Mono<List<OAuth2ClientDataResponse>> getOAuth2Clients() {
    SystemPermission readClientPermission = SystemPermission.CLIENTS_READ;
    List<RoleDefinition> hasCorrectRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(readClientPermission);

    LOG.info("🌐 Retrieving all authorized OAuth 2.0 clients");

    return oAuth2ClientDomainService
        .getOAuth2Clients()
        .map(
            allClients -> {
              if (permissionRoleManager.hasAdminPermission(readClientPermission)) {
                return allClients;
              }
              // Estrae i clientId per i quali l'utente possiede almeno un ruolo abilitato
              Set<String> authorizedClientIds =
                  hasCorrectRoles.stream()
                      .map(RoleDefinition::getClientId)
                      .filter(clientId -> !ObjectToolkit.isNullOrEmpty(clientId))
                      .map(String::toUpperCase)
                      .collect(Collectors.toSet());

              // Filtra la lista restituendo esclusivamente i client autorizzati
              return allClients.stream()
                  .filter(
                      client ->
                          client.getClientId() != null
                              && authorizedClientIds.contains(client.getClientId().toUpperCase()))
                  .toList();
            })
        .map(OAuth2ClientMapper::toResponseList);
  }

  /**
   * Recupera i dettagli di un singolo client tramite ID verificando che sia nel perimetro
   * autorizzato dell'operatore chiamante e che possieda il permesso {@code clients:read}.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente il {@link OAuth2Client} richiesto.
   */
  public Mono<OAuth2ClientDataResponse> getOAuth2ClientByClientID(String clientID) {
    SystemPermission readClientPermission = SystemPermission.CLIENTS_READ;

    permissionRoleManager.checkPermissionAndReturnUserRoles(readClientPermission);

    LOG.info("🌐 Retrieving client details for client ID: {}", clientID);

    return oAuth2ClientDomainService
        .getOAuth2ClientByClientID(clientID)
        .map(OAuth2ClientMapper::toResponse);
  }

  /**
   * Registra un nuovo client OAuth2 nel sistema verificando le autorizzazioni dell'operatore.
   *
   * @param oAuth2ClientRequest DTO con la configurazione del nuovo client.
   */
  public Mono<OAuth2ClientDataResponse> saveOAuth2Client(OAuth2ClientRequest oAuth2ClientRequest) {
    SystemPermission writeClientPermission = SystemPermission.CLIENTS_WRITE;
    if (!permissionRoleManager.hasAdminPermission(writeClientPermission)) {
      LOG.error(
          "⛔ Access denied: missing permission [{}] on create client [{}] for identifier [{}]",
          writeClientPermission,
          oAuth2Authorization.getClientId(),
          oAuth2Authorization.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
    LOG.info("🌐 Registering new OAuth 2.0 client: {}", oAuth2ClientRequest.getClientId());

    OAuth2Client oAuth2Client = OAuth2ClientMapper.toModel(oAuth2ClientRequest);

    return oAuth2ClientDomainService
        .saveOAuth2Client(oAuth2Client)
        .map(OAuth2ClientMapper::toResponse);
  }

  /**
   * Aggiorna le configurazioni di un client garantendo che appartenga al perimetro dell'operatore e
   * che questi possieda il permesso {@code clients:write}.
   *
   * @param oAuth2ClientRequest DTO con i parametri aggiornati.
   */
  public Mono<OAuth2ClientDataResponse> updateOAuth2ClientByClientId(
      OAuth2ClientRequest oAuth2ClientRequest, String clientId) {
    SystemPermission writeClientPermission = SystemPermission.CLIENTS_WRITE;
    if (!permissionRoleManager.hasAdminPermission(writeClientPermission)) {
      LOG.error(
          "⛔ Access denied: missing permission [{}] on update client [{}] for identifier [{}]",
          writeClientPermission,
          oAuth2Authorization.getClientId(),
          oAuth2Authorization.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    LOG.info("🌐 Updating OAuth 2.0 client by path ID: {}", clientId);

    OAuth2Client oAuth2Client = OAuth2ClientMapper.toModel(oAuth2ClientRequest);

    return oAuth2ClientDomainService
        .updateOAuth2ClientByClientId(oAuth2Client, clientId)
        .map(OAuth2ClientMapper::toResponse);
  }

  /**
   * Elimina un client dal database verificando che l'operatore possieda il permesso {@code
   * clients:delete}. Il client tecnico di sistema {@code ACCESS-SPHERE-AUTH} non puo mai essere
   * eliminato.
   *
   * @param clientId client da cancellare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteOAuth2ClientByClientId(String clientId) {
    SystemPermission writeClientPermission = SystemPermission.CLIENTS_DELETE;
    if (!permissionRoleManager.hasAdminPermission(writeClientPermission)) {
      LOG.error(
          "⛔ Access denied: missing permission [{}] on delete client [{}] for identifier [{}]",
          writeClientPermission,
          oAuth2Authorization.getClientId(),
          oAuth2Authorization.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    LOG.info("🔐 Check if is an admin client: {}", oAuth2Authorization.getClientId());
    if (permissionRoleManager.getAdminClientId().equalsIgnoreCase(clientId)) {
      LOG.error(
          "⛔ Access denied: cannot delete admin client [{}]", oAuth2Authorization.getClientId());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    LOG.info("🌐 Deleting OAuth 2.0 client: {}", clientId);

    oAuth2ClientDomainService.deleteOAuth2ClientByClientId(clientId);
  }
}
