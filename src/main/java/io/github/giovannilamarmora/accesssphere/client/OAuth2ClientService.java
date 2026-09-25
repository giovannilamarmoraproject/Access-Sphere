package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

/**
 * Service per la gestione dei Client OAuth2. Fornisce funzionalita di base (lettura, inserimento,
 * aggiornamento, eliminazione) basate esclusivamente sul database locale.
 */
@Logged
@Service
public class OAuth2ClientService {
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private IOAuth2ClientDAO ioAuth2ClientDAO;
  @Autowired private TechUserService techUserService;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;

  /** Recupera la lista di tutti i Client restituiti come oggetto Response reattivo. */
  public Mono<ResponseEntity<Response>> getClients() {
    Response response =
        new Response(HttpStatus.OK.value(), "Client credential list", TraceUtils.getSpanID(), null);

    return getClientCredentials()
        .map(
            client -> {
              response.setData(client);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Recupera la lista di tutti i ClientCredential dal database. Applica validazione sui permessi
   * del tech client.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<OAuth2Client>> getClientCredentials() {
    LOG.info("Retrieving all registered clients from Database");
    List<OAuth2ClientEntity> oAuth2ClientEntity = ioAuth2ClientDAO.findAll();
    if (ObjectUtils.isEmpty(oAuth2ClientEntity)) {
      LOG.error("No clients found in Database");
      return Mono.error(new OAuthException(ExceptionMap.ERR_OAUTH_404, "Clients not found!"));
    }
    List<RoleDefinitionEntity> allRoles = roleDefinitionDAO.findAll();
    Map<String, List<AppRole>> rolesByClient =
        allRoles != null
            ? allRoles.stream()
                .filter(r -> r.getClientId() != null)
                .collect(
                    Collectors.groupingBy(
                        r -> r.getClientId().toUpperCase(),
                        Collectors.mapping(
                            r ->
                                new AppRole(
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

    TechUserValidator.validateTechClient(
        dtos, oAuth2Authorization, techUserService.getTechClientId());
    return Mono.just(dtos);
  }

  /**
   * Recupera le credenziali di un Client dato il suo clientID.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente il {@link OAuth2Client}.
   * @throws OAuthException se il client non viene trovato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuth2Client> getClientCredentialByClientID(String clientID) {
    LOG.info("Searching for credentials for client ID: {}", clientID);
    OAuth2ClientEntity oAuth2ClientEntity = ioAuth2ClientDAO.findByClientId(clientID);
    if (ObjectUtils.isEmpty(oAuth2ClientEntity)) {
      LOG.error("Client with ID: {} not found in Database", clientID);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }
    OAuth2Client dto = OAuth2ClientMapper.toDTO(oAuth2ClientEntity);
    populateAppRoles(dto);
    return Mono.just(dto);
  }

  /**
   * Recupera le credenziali di un Client dato il suo clientID restituendo una Response HTTP 200.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link Response}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> getClientById(String clientID) {
    return getClientCredentialByClientID(clientID)
        .map(
            client -> {
              Response response =
                  new Response(
                      HttpStatus.OK.value(), "Client details", TraceUtils.getSpanID(), client);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Inserisce un nuovo ClientCredential a database e persiste le definizioni ruoli nella tabella
   * relazionale.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void addClientToDatabase(OAuth2Client oAuth2Client) {
    LOG.info("Inserting new client into Database: {}", oAuth2Client.getClientId());
    OAuthValidator.validateClientRoles(oAuth2Client);
    OAuth2ClientEntity clientEntity = OAuth2ClientMapper.toEntity(oAuth2Client);
    ioAuth2ClientDAO.save(clientEntity);

    // Persiste le definizioni ruoli su ROLE_DEFINITIONS
    if (oAuth2Client.getAppRoles() != null) {
      for (AppRole appRole : oAuth2Client.getAppRoles()) {
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
    LOG.debug("Client {} successfully inserted", oAuth2Client.getClientId());
  }

  /**
   * Aggiorna un ClientCredential esistente a database.
   *
   * @throws IllegalArgumentException se il client non esiste.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void updateClientInDatabase(OAuth2Client oAuth2Client) {
    LOG.info("Updating client in Database: {}", oAuth2Client.getClientId());
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
        for (AppRole appRole : oAuth2Client.getAppRoles()) {
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
      LOG.debug("Client {} successfully updated", oAuth2Client.getClientId());
    } else {
      LOG.error("Cannot update: Client {} not found", oAuth2Client.getClientId());
      throw new IllegalArgumentException("Client not found: " + oAuth2Client.getClientId());
    }
  }

  /**
   * Rimuove un ClientCredential dal database ripulendo definizioni ruoli e assegnazioni utenti
   * associate.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteClientFromDatabase(OAuth2Client oAuth2Client) {
    LOG.info("Removing client from Database: {}", oAuth2Client.getClientId());
    OAuth2ClientEntity existingClientOptional =
        ioAuth2ClientDAO.findByClientId(oAuth2Client.getClientId());
    if (!ObjectUtils.isEmpty(existingClientOptional)) {
      // Pulizia relazionale a cascata per ruoli e associazioni del client
      roleDefinitionDAO.deleteByClientId(oAuth2Client.getClientId());
      userClientRoleDAO.deleteByClientId(oAuth2Client.getClientId());

      ioAuth2ClientDAO.delete(existingClientOptional);
      LOG.debug("Client {} successfully removed", oAuth2Client.getClientId());
    } else {
      LOG.warn("No action taken: Client {} not found for removal", oAuth2Client.getClientId());
    }
  }

  /**
   * Helper per popolare l'elenco dei ruoli applicativi sul DTO ClientCredential leggendo
   * direttamente dalla tabella relazionale ROLE_DEFINITIONS.
   */
  private void populateAppRoles(OAuth2Client dto) {
    if (dto == null || dto.getClientId() == null) return;
    List<RoleDefinitionEntity> roles = roleDefinitionDAO.findByClientId(dto.getClientId());
    if (roles != null) {
      dto.setAppRoles(
          roles.stream()
              .map(
                  r ->
                      new AppRole(r.getRole(), r.getType(), r.getDescription(), r.getPermissions()))
              .toList());
    }
  }
}
