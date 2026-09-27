package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
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
@Logged
@Service
public class OAuth2ClientService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private PermissionRoleManager permissionRoleManager;
  @Autowired private AdminUserService techUserService;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;

  /**
   * Recupera la lista dei Client registrati applicando i vincoli di perimetro (Data Scoping)
   * dell'operatore chiamante.
   *
   * @return {@link Mono} con la lista di {@link OAuth2Client} autorizzati per l'operatore.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<OAuth2Client>> getOAuth2Clients() {
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
            });
  }

  /**
   * Recupera i dettagli di un singolo client tramite ID verificando che sia nel perimetro
   * autorizzato dell'operatore chiamante e che possieda il permesso {@code clients:read}.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente il {@link OAuth2Client} richiesto.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuth2Client> getOAuth2ClientByClientID(String clientID) {
    SystemPermission readClientPermission = SystemPermission.CLIENTS_READ;

    permissionRoleManager.checkPermissionAndReturnUserRoles(readClientPermission);

    LOG.info("🌐 Retrieving client details for client ID: {}", clientID);

    return oAuth2ClientDomainService.getOAuth2ClientByClientID(clientID);
  }

  /**
   * Registra un nuovo client OAuth2 nel sistema verificando le autorizzazioni dell'operatore.
   *
   * @param oAuth2Client DTO con la configurazione del nuovo client.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuth2Client> createOAuth2Client(OAuth2Client oAuth2Client) {
    SystemPermission writeClientPermission = SystemPermission.CLIENTS_WRITE;
    if (!permissionRoleManager.hasAdminPermission(writeClientPermission)) {
      LOG.error(
          "⛔ Access denied: missing permission [{}] on client [{}] for identifier [{}]",
          writeClientPermission,
          oAuth2Authorization.getClientId(),
          oAuth2Authorization.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
    LOG.info("🌐 Registering new OAuth 2.0 client: {}", oAuth2Client.getClientId());

    if (!permissionRoleManager.isSuperAdmin(oAuth2Authorization)) {
      if (oAuth2Client.getClientId() != null
          && oAuth2Client.getClientId().equalsIgnoreCase(techUserService.getAdminClientId())) {
        throw new OAuthException(
            ExceptionMap.ERR_TECH_403, "Non-technical users cannot register technical clients");
      }
      permissionRoleManager.assertPermission(
          oAuth2Authorization, oAuth2Client.getClientId(), "clients:write");
    }

    oAuth2ClientDomainService.addClientToDatabase(oAuth2Client);

    // Auto-associazione del creatore non-tech come amministratore del nuovo client
    if (!permissionRoleManager.isSuperAdmin(oAuth2Authorization)
        && oAuth2Authorization.getIdentifier() != null
        && oAuth2Client.getClientId() != null) {
      userClientRoleDAO.save(
          new UserClientRoleEntity(
              oAuth2Authorization.getIdentifier(),
              oAuth2Client.getClientId().trim().toUpperCase(),
              "ROLE_ADMIN"));
    }
    return Mono.just(oAuth2Client);
  }

  /**
   * Aggiorna le configurazioni di un client garantendo che appartenga al perimetro dell'operatore e
   * che questi possieda il permesso {@code clients:write}.
   *
   * @param oAuth2Client DTO con i parametri aggiornati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void updateClientInDatabase(OAuth2Client oAuth2Client) {
    permissionRoleManager.assertHasConsoleAccess(oAuth2Authorization);

    if (!permissionRoleManager.isSuperAdmin(oAuth2Authorization)) {
      if (oAuth2Client.getClientId() != null
          && oAuth2Client.getClientId().equalsIgnoreCase(techUserService.getAdminClientId())) {
        throw new OAuthException(
            ExceptionMap.ERR_TECH_403, "Non-technical users cannot modify the technical client");
      }
      permissionRoleManager.assertPermission(
          oAuth2Authorization, oAuth2Client.getClientId(), "clients:write");
    }

    oAuth2ClientDomainService.updateClientInDatabase(oAuth2Client);
  }

  /**
   * Elimina un client dal database verificando che l'operatore possieda il permesso {@code
   * clients:delete}. Il client tecnico di sistema {@code ACCESS-SPHERE-AUTH} non puo mai essere
   * eliminato.
   *
   * @param oAuth2Client DTO del client da cancellare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteClientFromDatabase(OAuth2Client oAuth2Client) {
    permissionRoleManager.assertHasConsoleAccess(oAuth2Authorization);

    if (oAuth2Client.getClientId() != null
        && oAuth2Client.getClientId().equalsIgnoreCase(techUserService.getAdminClientId())) {
      throw new OAuthException(
          ExceptionMap.ERR_TECH_403,
          "The technical client cannot be deleted under any circumstance");
    }

    if (!permissionRoleManager.isSuperAdmin(oAuth2Authorization)) {
      permissionRoleManager.assertPermission(
          oAuth2Authorization, oAuth2Client.getClientId(), "clients:delete");
    }

    oAuth2ClientDomainService.deleteClientFromDatabase(oAuth2Client);
  }
}
