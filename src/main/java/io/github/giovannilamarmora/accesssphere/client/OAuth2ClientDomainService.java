package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.database.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.database.OAuth2ClientRepository;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.RoleDefinitionRepository;
import io.github.giovannilamarmora.accesssphere.roles.database.UserClientRoleRepository;
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

  @Autowired private OAuth2ClientRepository oAuth2ClientRepository;
  @Autowired private RoleDefinitionRepository roleDefinitionRepository;
  @Autowired private UserClientRoleRepository userClientRoleRepository;

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
  public Mono<OAuth2Client> saveOAuth2Client(OAuth2Client oAuth2Client) {
    LOG.info(
        "💾 {} Inserting new client into Database: {}",
        OAUTH2_DOMAIN_LOG,
        oAuth2Client.getClientId());
    OAuth2ClientValidator.validateClientRoles(oAuth2Client);

    boolean alreadyExist = oAuth2ClientRepository.existsByClientId(oAuth2Client.getClientId());
    if (alreadyExist) {
      LOG.error(
          "❌ {} The client with ID {} already exists",
          OAUTH2_DOMAIN_LOG,
          oAuth2Client.getClientId());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }

    OAuth2Client clientSaved = saveOrUpdateOAuth2Client(oAuth2Client);
    LOG.info("✅ {} Client {} successfully inserted", OAUTH2_DOMAIN_LOG, oAuth2Client.getClientId());
    return Mono.just(clientSaved);
  }

  /**
   * Aggiorna un ClientCredential esistente a database e sincronizza le relative definizioni ruoli.
   *
   * @param oAuth2Client DTO con le configurazioni aggiornate.
   * @throws IllegalArgumentException se il client non esiste.
   */
  @Transactional
  public Mono<OAuth2Client> updateOAuth2ClientByClientId(
      OAuth2Client oAuth2Client, String clientId) {
    LOG.info("🔄 {} Updating client in Database: {}", OAUTH2_DOMAIN_LOG, clientId);
    OAuth2ClientValidator.validateClientRoles(oAuth2Client);

    Optional<OAuth2Client> existingClient = oAuth2ClientRepository.findByClientId(clientId);
    if (existingClient.isEmpty()) {
      LOG.error(
          "❌ {} The client with ID {} already does not exist",
          OAUTH2_DOMAIN_LOG,
          oAuth2Client.getClientId());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }

    oAuth2Client.setId(existingClient.get().getId());
    oAuth2Client.setClientId(clientId);

    OAuth2Client clientSaved = saveOrUpdateOAuth2Client(oAuth2Client);

    LOG.debug("✅ {} Client {} successfully updated", OAUTH2_DOMAIN_LOG, clientId);

    return Mono.just(clientSaved);
  }

  /**
   * Rimuove un ClientCredential dal database ripulendo a cascata definizioni ruoli e associazioni
   * utente collegate.
   *
   * @param clientId client da rimuovere.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteOAuth2ClientByClientId(String clientId) {
    LOG.info("🗑️ {} Removing client from Database: {}", OAUTH2_DOMAIN_LOG, clientId);

    boolean alreadyExist = oAuth2ClientRepository.existsByClientId(clientId);
    if (!alreadyExist) {
      LOG.error(
          "❌ {} The client with ID {} does not exist, it cannot be removed",
          OAUTH2_DOMAIN_LOG,
          clientId);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }

    roleDefinitionRepository.deleteByClientId(clientId);
    userClientRoleRepository.deleteByClientId(clientId);
    oAuth2ClientRepository.deleteByClientId(clientId);

    LOG.debug("✅ {} Client {} successfully removed", OAUTH2_DOMAIN_LOG, clientId);
  }

  private OAuth2Client saveOrUpdateOAuth2Client(OAuth2Client oAuth2Client) {
    OAuth2Client clientSaved = oAuth2ClientRepository.save(oAuth2Client);

    // Persiste le definizioni ruoli su ROLE_DEFINITIONS
    clientSaved.setAppRoles(
        oAuth2Client.getAppRoles().stream()
            .map(
                roleDefinition -> {
                  String roleName = roleDefinition.getRole().trim().toUpperCase();
                  Boolean roleExists =
                      roleDefinitionRepository.existsByClientIdAndRole(
                          oAuth2Client.getClientId(), roleName);
                  if (roleExists) {
                    LOG.warn(
                        "❌ The role {} for client ID: {} already exists",
                        roleName,
                        oAuth2Client.getClientId());
                    return roleDefinition;
                  }
                  return roleDefinitionRepository.save(roleDefinition);
                })
            .toList());
    return clientSaved;
  }
}
