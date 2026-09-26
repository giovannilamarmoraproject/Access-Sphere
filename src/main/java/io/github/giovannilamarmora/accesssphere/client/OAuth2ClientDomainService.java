package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

/**
 * Domain Service puro per l'aggregato OAuth2 Client.
 *
 * <p>Responsabilita:
 *
 * <ul>
 *   <li>Gestisce la pura logica di dominio, persistenza relazionale e integrita transazionale per i
 *       Client OAuth2.
 *   <li>Completamente disaccoppiato da controlli di sessione HTTP o vincoli di autorizzazione
 *       utente; puo essere invocato direttamente dagli engine interni di sistema (es. {@code
 *       OAuthService}, {@code MFAAuthenticationService}, task di init) senza alcun rischio di blocco
 *       o alterazione dei dati.
 *   <li>Incapsula l'accesso a {@link IOAuth2ClientDAO}, {@link IRoleDefinitionDAO} e {@link
 *       IUserClientRoleDAO}.
 * </ul>
 */
@Logged
@Service
public class OAuth2ClientDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IOAuth2ClientDAO ioAuth2ClientDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;

  /**
   * Recupera le credenziali e le definizioni ruoli di un Client dato il suo clientID.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente il {@link OAuth2Client}.
   * @throws OAuthException se il client non esiste a database.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuth2Client> getClientCredentialByClientID(String clientID) {
    LOG.info("🔍 [CLIENT DOMAIN] Searching for credentials for client ID: {}", clientID);
    OAuth2ClientEntity oAuth2ClientEntity = ioAuth2ClientDAO.findByClientId(clientID);
    if (ObjectUtils.isEmpty(oAuth2ClientEntity)) {
      LOG.error("❌ [CLIENT DOMAIN] Client with ID: {} not found in Database", clientID);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }
    OAuth2Client dto = OAuth2ClientMapper.toDTO(oAuth2ClientEntity);
    populateAppRoles(dto);
    return Mono.just(dto);
  }

  /**
   * Recupera la lista completa di tutti i Client registrati a database con i rispettivi ruoli
   * applicativi arricchiti.
   *
   * @return {@link Mono} con la lista di {@link OAuth2Client}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<OAuth2Client>> getClientCredentials() {
    LOG.info("📋 [CLIENT DOMAIN] Retrieving all registered clients from Database");
    List<OAuth2ClientEntity> oAuth2ClientEntity = ioAuth2ClientDAO.findAll();
    if (ObjectUtils.isEmpty(oAuth2ClientEntity)) {
      LOG.error("❌ [CLIENT DOMAIN] No clients found in Database");
      return Mono.error(new OAuthException(ExceptionMap.ERR_OAUTH_404, "Clients not found!"));
    }
    List<RoleDefinitionEntity> allRoles = roleDefinitionDAO.findAll();
    Map<String, List<RoleDefinition>> rolesByClient =
        allRoles != null
            ? allRoles.stream()
                .filter(r -> r.getClientId() != null)
                .collect(
                    Collectors.groupingBy(
                        r -> r.getClientId().toUpperCase(),
                        Collectors.mapping(
                            r ->
                                new RoleDefinition(
                                    r.getRole(),
                                    r.getType(),
                                    r.getDescription(),
                                    r.getPermissions()),
                            Collectors.toList())))
            : Map.of();

    List<OAuth2Client> dtos =
        oAuth2ClientEntity.stream()
            .map(OAuth2ClientMapper::toDTO)
            .peek(
                dto ->
                    dto.setAppRoles(
                        rolesByClient.getOrDefault(dto.getClientId().toUpperCase(), List.of())))
            .toList();

    return Mono.just(dtos);
  }

  /**
   * Inserisce un nuovo ClientCredential a database e persiste atomicamente le definizioni ruoli
   * nella tabella relazionale {@code ROLE_DEFINITIONS}.
   *
   * @param oAuth2Client DTO con la configurazione del nuovo client.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void addClientToDatabase(OAuth2Client oAuth2Client) {
    LOG.info("💾 [CLIENT DOMAIN] Inserting new client into Database: {}", oAuth2Client.getClientId());
    OAuthValidator.validateClientRoles(oAuth2Client);
    OAuth2ClientEntity clientEntity = OAuth2ClientMapper.toEntity(oAuth2Client);
    ioAuth2ClientDAO.save(clientEntity);

    // Persiste le definizioni ruoli su ROLE_DEFINITIONS
    if (oAuth2Client.getAppRoles() != null) {
      for (RoleDefinition appRole : oAuth2Client.getAppRoles()) {
        if (appRole != null && appRole.getRole() != null) {
          String rName = appRole.getRole().trim().toUpperCase();
          if (!roleDefinitionDAO.existsByClientIdAndRole(oAuth2Client.getClientId(), rName)) {
            roleDefinitionDAO.save(
                new RoleDefinitionEntity(
                    oAuth2Client.getClientId(),
                    rName,
                    appRole.getType(),
                    appRole.getDescription(),
                    appRole.getPermissions() != null ? appRole.getPermissions() : List.of("*")));
          }
        }
      }
    }
    LOG.debug("✅ [CLIENT DOMAIN] Client {} successfully inserted", oAuth2Client.getClientId());
  }

  /**
   * Aggiorna un ClientCredential esistente a database e sincronizza le relative definizioni ruoli.
   *
   * @param oAuth2Client DTO con le configurazioni aggiornate.
   * @throws IllegalArgumentException se il client non esiste.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void updateClientInDatabase(OAuth2Client oAuth2Client) {
    LOG.info("🔄 [CLIENT DOMAIN] Updating client in Database: {}", oAuth2Client.getClientId());
    if (oAuth2Client.getAppRoles() != null) {
      OAuthValidator.validateClientRoles(oAuth2Client);
    }
    OAuth2ClientEntity existingClientEntity =
        ioAuth2ClientDAO.findByClientId(oAuth2Client.getClientId());
    if (!ObjectUtils.isEmpty(existingClientEntity)) {
      OAuth2ClientMapper.updateEntity(existingClientEntity, oAuth2Client);
      ioAuth2ClientDAO.save(existingClientEntity);

      // Sincronizza i ruoli nella tabella relazionale ROLE_DEFINITIONS
      if (oAuth2Client.getAppRoles() != null) {
        for (RoleDefinition appRole : oAuth2Client.getAppRoles()) {
          if (appRole != null && appRole.getRole() != null) {
            String rName = appRole.getRole().trim().toUpperCase();
            Optional<RoleDefinitionEntity> existingRole =
                roleDefinitionDAO.findByClientIdAndRole(oAuth2Client.getClientId(), rName);
            if (existingRole.isPresent()) {
              RoleDefinitionEntity roleEntity = existingRole.get();
              roleEntity.setType(appRole.getType());
              roleEntity.setDescription(appRole.getDescription());
              roleEntity.setPermissions(
                  appRole.getPermissions() != null ? appRole.getPermissions() : List.of("*"));
              roleDefinitionDAO.save(roleEntity);
            } else {
              roleDefinitionDAO.save(
                  new RoleDefinitionEntity(
                      oAuth2Client.getClientId(),
                      rName,
                      appRole.getType(),
                      appRole.getDescription(),
                      appRole.getPermissions() != null ? appRole.getPermissions() : List.of("*")));
            }
          }
        }
      }
      LOG.debug("✅ [CLIENT DOMAIN] Client {} successfully updated", oAuth2Client.getClientId());
    } else {
      LOG.error("❌ [CLIENT DOMAIN] Cannot update: Client {} not found", oAuth2Client.getClientId());
      throw new IllegalArgumentException("Client not found: " + oAuth2Client.getClientId());
    }
  }

  /**
   * Rimuove un ClientCredential dal database ripulendo a cascata definizioni ruoli e associazioni
   * utente collegate.
   *
   * @param oAuth2Client DTO del client da rimuovere.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteClientFromDatabase(OAuth2Client oAuth2Client) {
    LOG.info("🗑️ [CLIENT DOMAIN] Removing client from Database: {}", oAuth2Client.getClientId());
    OAuth2ClientEntity existingClientOptional =
        ioAuth2ClientDAO.findByClientId(oAuth2Client.getClientId());
    if (!ObjectUtils.isEmpty(existingClientOptional)) {
      // Pulizia relazionale a cascata per ruoli e associazioni del client
      roleDefinitionDAO.deleteByClientId(oAuth2Client.getClientId());
      userClientRoleDAO.deleteByClientId(oAuth2Client.getClientId());

      ioAuth2ClientDAO.delete(existingClientOptional);
      LOG.debug("✅ [CLIENT DOMAIN] Client {} successfully removed", oAuth2Client.getClientId());
    } else {
      LOG.warn("⚠️ [CLIENT DOMAIN] No action taken: Client {} not found for removal", oAuth2Client.getClientId());
    }
  }

  /**
   * Popola l'elenco dei ruoli applicativi sul DTO leggendo direttamente dalla tabella relazionale
   * {@code ROLE_DEFINITIONS}.
   *
   * @param dto DTO del client da arricchire.
   */
  public void populateAppRoles(OAuth2Client dto) {
    if (dto == null || dto.getClientId() == null) return;
    List<RoleDefinitionEntity> roles = roleDefinitionDAO.findByClientId(dto.getClientId());
    if (roles != null) {
      dto.setAppRoles(
          roles.stream()
              .map(
                  r ->
                      new RoleDefinition(r.getRole(), r.getType(), r.getDescription(), r.getPermissions()))
              .toList());
    }
  }
}
