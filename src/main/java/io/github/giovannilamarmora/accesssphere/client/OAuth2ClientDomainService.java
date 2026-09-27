package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.database.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.database.OAuth2ClientRepository;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.RoleDefinitionRepository;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
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
 *       OAuthService}, {@code MFAAuthenticationService}, task di init) senza alcun rischio di
 *       blocco o alterazione dei dati.
 *   <li>Incapsula l'accesso a {@link IOAuth2ClientDAO}, {@link IRoleDefinitionDAO} e {@link
 *       IUserClientRoleDAO}.
 * </ul>
 */
@Service
public class OAuth2ClientDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String OAUTH2_DOMAIN_LOG = "Client Domain";

  @Autowired private IOAuth2ClientDAO ioAuth2ClientDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private OAuth2ClientRepository oAuth2ClientRepository;
  @Autowired private RoleDefinitionRepository roleDefinitionRepository;

  /**
   * Recupera la lista completa di tutti i Client registrati a database con i rispettivi ruoli
   * applicativi arricchiti.
   *
   * @return {@link Mono} con la lista di {@link OAuth2Client}.
   */
  public Mono<List<OAuth2Client>> getOAuth2Clients() {
    LOG.info("📋 {} Retrieving all registered clients from Database", OAUTH2_DOMAIN_LOG);
    Optional<List<OAuth2Client>> optionalOAuth2Clients = oAuth2ClientRepository.findAll();
    if (optionalOAuth2Clients.isEmpty() || optionalOAuth2Clients.get().isEmpty()) {
      LOG.error("❌ {} No clients found in Database", OAUTH2_DOMAIN_LOG);
      return Mono.error(
          new OAuthException(ExceptionMap.ERR_OAUTH_404, "OAuth2 Clients not found!"));
    }

    Optional<List<RoleDefinition>> optionalRoleDefinitions = roleDefinitionRepository.findAll();

    Map<String, List<RoleDefinition>> rolesByClient =
        optionalRoleDefinitions.isPresent() && !optionalRoleDefinitions.get().isEmpty()
            ? optionalRoleDefinitions.get().stream()
                .filter(
                    roleDefinition -> !ObjectToolkit.isNullOrEmpty(roleDefinition.getClientId()))
                .collect(
                    Collectors.groupingBy(
                        roleDefinition -> roleDefinition.getClientId().toUpperCase(),
                        Collectors.mapping(
                            roleDefinition -> {
                              RoleDefinition role = new RoleDefinition();
                              BeanUtils.copyProperties(roleDefinition, role);
                              return role;
                            },
                            Collectors.toList())))
            : Map.of();

    List<OAuth2Client> oAuth2Clients = optionalOAuth2Clients.get();

    oAuth2Clients.forEach(
        client ->
            client.setAppRoles(
                !ObjectToolkit.isNullOrEmpty(client.getClientId())
                    ? rolesByClient.getOrDefault(client.getClientId().toUpperCase(), List.of())
                    : List.of()));

    LOG.info("📋 {} Found {} clients into the Database", OAUTH2_DOMAIN_LOG, oAuth2Clients.size());
    return Mono.just(oAuth2Clients);
  }

  /**
   * Recupera le credenziali e le definizioni ruoli di un Client dato il suo clientID.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente il {@link OAuth2Client}.
   * @throws OAuthException se il client non esiste a database.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuth2Client> getOAuth2ClientByClientID(String clientID) {
    LOG.info("🔍 {} Searching for credentials for client ID: {}", OAUTH2_DOMAIN_LOG, clientID);
    Optional<OAuth2Client> optionalOAuth2Client = oAuth2ClientRepository.findByClientId(clientID);
    if (optionalOAuth2Client.isEmpty() || ObjectToolkit.isNullOrEmpty(optionalOAuth2Client.get())) {
      LOG.error("❌ {} Client with ID: {} not found in Database", OAUTH2_DOMAIN_LOG, clientID);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }

    Optional<List<RoleDefinition>> optionalRoleDefinitions =
        roleDefinitionRepository.findByClientId(clientID);

    OAuth2Client oAuth2Client = optionalOAuth2Client.get();
    if (optionalRoleDefinitions.isPresent() && !optionalRoleDefinitions.get().isEmpty()) {
      List<RoleDefinition> roleDefinitions = optionalRoleDefinitions.get();
      oAuth2Client.setAppRoles(
          roleDefinitions.stream()
              .map(
                  roleDefinition -> {
                    RoleDefinition role = new RoleDefinition();
                    BeanUtils.copyProperties(roleDefinition, role);
                    return role;
                  })
              .toList());
    }
    LOG.info("📋 {} Return client {} into the Database", OAUTH2_DOMAIN_LOG, clientID);
    return Mono.just(oAuth2Client);
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
    LOG.info(
        "💾 [CLIENT DOMAIN] Inserting new client into Database: {}", oAuth2Client.getClientId());
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
      LOG.warn(
          "⚠️ [CLIENT DOMAIN] No action taken: Client {} not found for removal",
          oAuth2Client.getClientId());
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
                      new RoleDefinition(
                          r.getRole(), r.getType(), r.getDescription(), r.getPermissions()))
              .toList());
    }
  }
}
