package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.ClientMapper;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserConfig;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionDTO;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Servizio di dominio per l'amministrazione dei ruoli e permessi utente (RBAC).
 *
 * <p>Interconnessioni logiche:
 * <ul>
 *   <li>Persiste le relazioni utente-client-ruolo su tabella relazionale {@code USER_CLIENT_ROLE} via {@link IUserClientRoleDAO}.
 *   <li>Mantiene la retrocompatibilita al 100% sincronizzando la colonna denormalizzata legacy {@code USERS.ROLES} via {@link UserService}.
 *   <li>Risolve la validita dei ruoli dai client OAuth2 registrati tramite {@link ClientService} e {@link IClientDAO}.
 *   <li>Gestisce il catalogo centralizzato dei ruoli e permessi su {@code /v1/roles} con CRUD atomico e revoca a cascata.
 *   <li>Consente l'esecuzione delle operazioni esclusivamente a operatori autenticati con ruolo tecnico ({@link TechUserService}).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RolesService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private ClientService clientService;
  @Autowired private UserService userService;
  @Autowired private TechUserService techUserService;
  @Autowired private TechUserConfig techUserConfig;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IClientDAO clientDAO;
  @Autowired private IUserDAO userDAO;

  /**
   * Modifica i ruoli assegnati a un determinato utente, persistendo le associazioni nella tabella relazionale
   * {@code USER_CLIENT_ROLE} e mantenendo allineata per retrocompatibilita la lista flat legacy in {@code USERS.ROLES}.
   * Supporta sia il formato moderno basato su {@code clientRoles} (client-scoped con permessi aggregati), sia
   * il formato legacy basato su array flat di stringhe {@code roles}.
   * L'operazione e consentita esclusivamente a un utente tecnico autorizzato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param roles Oggetto contenente la lista dei ruoli da assegnare (clientRoles e/o roles legacy).
   * @return Mono contenente la risposta HTTP con l'utente aggiornato e arricchito.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changeRoles(String identifier, UserRoles roles) {
    LOG.info("👥 [ROLES] Changing user roles process started, identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can change the user roles {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String techClientId = techUserConfig.getTechClientId();
    Mono<List<ClientCredential>> getClients = clientService.getClientCredentials();

    return getClients
        .flatMap(
            clientCredentials -> {
              // 1. Raccoglie tutti i ruoli ammessi dai client registrati (nome ruolo e tipologia applicativa)
              Set<String> allowedRoles = new HashSet<>();
              if (clientCredentials != null) {
                clientCredentials.forEach(
                    c -> {
                      if (c.getAppRoles() != null) {
                        c.getAppRoles()
                            .forEach(
                                r -> {
                                  if (r != null) {
                                    if (r.getRole() != null)
                                      allowedRoles.add(r.getRole().toUpperCase());
                                    if (r.getType() != null)
                                      allowedRoles.add(r.getType().toUpperCase());
                                  }
                                });
                      }
                    });
              }

              // 2. Raccoglie i ruoli canonici/di sistema definiti a livello di piattaforma (UserRole e AppRoleType)
              Arrays.stream(UserRole.values())
                  .forEach(
                      ur -> {
                        allowedRoles.add(ur.name().toUpperCase());
                        allowedRoles.add("ROLE_" + ur.name().toUpperCase());
                      });
              Arrays.stream(AppRoleType.values())
                  .forEach(art -> allowedRoles.add(art.getValue().toUpperCase()));
              allowedRoles.add("ROLE_TECH");

              List<UserClientRoleEntity> entitiesToSave = new ArrayList<>();
              List<String> distinctRoleNames = new ArrayList<>();

              // CASO A: Ricevuto formato moderno 'clientRoles'
              if (roles != null && roles.getClientRoles() != null && !roles.getClientRoles().isEmpty()) {
                LOG.info("👥 [ROLES] Processing modern clientRoles payload with {} entries", roles.getClientRoles().size());
                for (UserClientRoleDTO cr : roles.getClientRoles()) {
                  if (cr == null || cr.getRole() == null || cr.getRole().isBlank()) continue;
                  String targetClientId = (cr.getClientId() != null && !cr.getClientId().isBlank())
                      ? cr.getClientId().trim()
                      : techClientId;
                  String roleName = cr.getRole().trim().toUpperCase();

                  boolean valid = allowedRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(roleName));
                  if (!valid) {
                    LOG.warn("⚠️ [ROLES] Discarding unrecognized client-scoped role [{}] for client [{}]", roleName, targetClientId);
                    continue;
                  }

                  boolean alreadyAdded = entitiesToSave.stream()
                      .anyMatch(e -> e.getClientId().equalsIgnoreCase(targetClientId) && e.getRole().equalsIgnoreCase(roleName));
                  if (!alreadyAdded) {
                    entitiesToSave.add(new UserClientRoleEntity(identifier, targetClientId, roleName));
                  }
                  if (!distinctRoleNames.contains(roleName)) {
                    distinctRoleNames.add(roleName);
                  }
                }
              }
              // CASO B: Ricevuto formato legacy 'roles' (array flat di stringhe)
              else if (roles != null && roles.getRoles() != null && !roles.getRoles().isEmpty()) {
                LOG.info("👥 [ROLES] Processing legacy roles payload with {} entries", roles.getRoles().size());
                List<String> validLegacyRoles = roles.getRoles().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(r -> !r.isBlank())
                    .filter(r -> {
                      boolean valid = allowedRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(r));
                      if (!valid) {
                        LOG.warn("⚠️ [ROLES] Discarding unrecognized role [{}] for user {}", r, identifier);
                      }
                      return valid;
                    })
                    .map(String::toUpperCase)
                    .distinct()
                    .toList();

                for (String r : validLegacyRoles) {
                  // Determina a quale client appartiene il ruolo
                  String targetClientId = techClientId;
                  if (clientCredentials != null) {
                    for (ClientCredential cc : clientCredentials) {
                      if (cc.getAppRoles() != null) {
                        boolean match = cc.getAppRoles().stream().anyMatch(ar -> ar != null && ar.matches(r));
                        if (match) {
                          targetClientId = cc.getClientId();
                          break;
                        }
                      }
                    }
                  }

                  entitiesToSave.add(new UserClientRoleEntity(identifier, targetClientId, r));
                  distinctRoleNames.add(r);
                }
              }

              // 3. Sostituzione atomica nella tabella relazionale USER_CLIENT_ROLE
              userClientRoleDAO.deleteByUserIdentifier(identifier);
              if (!entitiesToSave.isEmpty()) {
                userClientRoleDAO.saveAll(entitiesToSave);
                LOG.info("💾 [ROLES] Persisted {} entries in USER_CLIENT_ROLE for user {}", entitiesToSave.size(), identifier);
              }

              // 4. Sincronizzazione per retrocompatibilita su USERS.ROLES
              return userService
                  .getUserByIdentifier(identifier)
                  .flatMap(
                      user -> {
                        user.setRoles(distinctRoleNames);
                        return userService
                            .updateUser(identifier, user)
                            .map(
                                updatedUser -> {
                                  Response response =
                                      new Response(
                                          HttpStatus.OK.value(),
                                          "User roles for "
                                              + updatedUser.getUsername()
                                              + " successfully changed!",
                                          TraceUtils.getSpanID(),
                                          updatedUser);
                                  return ResponseEntity.ok(response);
                                });
                      });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "✅ [ROLES] Changing user roles process ended, identifier: {}", identifier));
  }

  /**
   * Assegna un singolo ruolo contestualizzato a uno specifico Client OAuth2 per l'utente indicato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param clientId Identificativo univoco del client OAuth2 target.
   * @param role Nome del ruolo da assegnare.
   * @return Mono contenente la risposta HTTP con l'utente aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> assignClientRole(
      String identifier, String clientId, String role) {
    LOG.info("👥 [ROLES] Assigning role [{}] for client [{}] to user [{}]", role, clientId, identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can assign client roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedRole = role.trim().toUpperCase();
    String normalizedClientId = clientId.trim();

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            user -> {
              boolean alreadyExists =
                  userClientRoleDAO.existsByUserIdentifierAndClientIdAndRole(
                      identifier, normalizedClientId, normalizedRole);
              if (!alreadyExists) {
                userClientRoleDAO.save(
                    new UserClientRoleEntity(identifier, normalizedClientId, normalizedRole));
                LOG.info(
                    "💾 [ROLES] Successfully created USER_CLIENT_ROLE mapping ({}, {}, {})",
                    identifier,
                    normalizedClientId,
                    normalizedRole);
              }

              // Sincronizza campo flat legacy
              List<String> currentRoles = new ArrayList<>(user.getRoles() != null ? user.getRoles() : List.of());
              if (!currentRoles.contains(normalizedRole)) {
                currentRoles.add(normalizedRole);
                user.setRoles(currentRoles);
              }

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Role " + normalizedRole + " successfully assigned for client " + normalizedClientId,
                                TraceUtils.getSpanID(),
                                updatedUser);
                        return ResponseEntity.ok(response);
                      });
            });
  }

  /**
   * Revoca un singolo ruolo contestualizzato a uno specifico Client OAuth2 per l'utente indicato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param clientId Identificativo univoco del client OAuth2 target.
   * @param role Nome del ruolo da revocare.
   * @return Mono contenente la risposta HTTP con l'utente aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> revokeClientRole(
      String identifier, String clientId, String role) {
    LOG.info("👥 [ROLES] Revoking role [{}] for client [{}] from user [{}]", role, clientId, identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can revoke client roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedRole = role.trim().toUpperCase();
    String normalizedClientId = clientId.trim();

    userClientRoleDAO.deleteByUserIdentifierAndClientIdAndRole(
        identifier, normalizedClientId, normalizedRole);
    LOG.info(
        "🗑️ [ROLES] Deleted USER_CLIENT_ROLE mapping ({}, {}, {})",
        identifier,
        normalizedClientId,
        normalizedRole);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            user -> {
              // Verifica se il ruolo rimosso e ancora assegnato a un altro client per questo utente
              List<UserClientRoleEntity> remainingUserRoles =
                  userClientRoleDAO.findByUserIdentifier(identifier);
              boolean roleStillPresentInOtherClient =
                  remainingUserRoles.stream()
                      .anyMatch(r -> r.getRole().equalsIgnoreCase(normalizedRole));

              if (!roleStillPresentInOtherClient && user.getRoles() != null) {
                List<String> updatedLegacyRoles =
                    user.getRoles().stream()
                        .filter(r -> !r.equalsIgnoreCase(normalizedRole))
                        .toList();
                user.setRoles(updatedLegacyRoles);
              }

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Role " + normalizedRole + " successfully revoked for client " + normalizedClientId,
                                TraceUtils.getSpanID(),
                                updatedUser);
                        return ResponseEntity.ok(response);
                      });
            });
  }

  /**
   * Restituisce il catalogo completo dei ruoli applicativi definiti nei vari Client OAuth2,
   * arricchiti con il conteggio e l'elenco sintetico degli utenti a cui ciascun ruolo è assegnato.
   * Se viene specificato clientId, il catalogo viene filtrato unicamente per quel client.
   *
   * @param clientId Filtro opzionale per identificativo Client OAuth2.
   * @return Mono contenente la lista dei ruoli definiti con dati di assegnazione utente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> getRolesCatalog(String clientId) {
    LOG.info("📋 [ROLES] Getting roles catalog, clientId filter: [{}]", clientId);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can view the roles catalog");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    List<ClientCredentialEntity> clientEntities;
    if (StringUtils.hasText(clientId)) {
      ClientCredentialEntity singleClient = clientDAO.findByClientId(clientId.trim());
      clientEntities = singleClient != null ? List.of(singleClient) : List.of();
    } else {
      clientEntities = clientDAO.findAll();
    }

    List<RoleDefinitionDTO> catalog = new ArrayList<>();
    for (ClientCredentialEntity clientEntity : clientEntities) {
      if (clientEntity.getAppRoles() == null) continue;
      List<AppRole> roles = ClientMapper.parseAppRoles(clientEntity.getAppRoles());
      if (roles == null) continue;

      for (AppRole role : roles) {
        if (role == null || role.getRole() == null) continue;
        String roleName = role.getRole().toUpperCase();
        String cId = clientEntity.getClientId();

        List<UserClientRoleEntity> assignments =
            userClientRoleDAO.findByClientIdAndRole(cId, roleName);
        List<String> userIdentifiers =
            assignments.stream().map(UserClientRoleEntity::getUserIdentifier).distinct().toList();

        List<UserSummaryDTO> assignedUsers = new ArrayList<>();
        if (!userIdentifiers.isEmpty()) {
          List<UserEntity> users = userDAO.findAllByIdentifierIn(userIdentifiers);
          if (users != null) {
            assignedUsers =
                users.stream()
                    .map(
                        u ->
                            new UserSummaryDTO(
                                u.getIdentifier(),
                                u.getUsername(),
                                u.getName(),
                                u.getSurname(),
                                u.getEmail()))
                    .toList();
          }
        }

        RoleDefinitionDTO def =
            new RoleDefinitionDTO(
                cId,
                roleName,
                role.getType(),
                role.getDescription(),
                role.getPermissions(),
                assignedUsers.size(),
                assignedUsers);
        catalog.add(def);
      }
    }

    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Roles catalog successfully retrieved",
            TraceUtils.getSpanID(),
            catalog);
    return Mono.just(ResponseEntity.ok(response));
  }

  /**
   * Restituisce la lista degli utenti a cui è attualmente assegnato uno specifico ruolo per un client.
   * Utilizzato in particolare dalla console prima della cancellazione per mostrare l'elenco nominativo
   * degli utenti impattati nel popup di conferma SweetAlert2.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome identificativo del ruolo.
   * @return Mono con la lista degli utenti assegnati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> getUsersWithRole(String clientId, String role) {
    LOG.info("👥 [ROLES] Getting users with role [{}] for client [{}]", role, clientId);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can view assigned users for a role");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedClientId = clientId.trim();
    String normalizedRole = role.trim().toUpperCase();

    List<UserClientRoleEntity> assignments =
        userClientRoleDAO.findByClientIdAndRole(normalizedClientId, normalizedRole);
    List<String> userIdentifiers =
        assignments.stream().map(UserClientRoleEntity::getUserIdentifier).distinct().toList();

    List<UserSummaryDTO> assignedUsers = new ArrayList<>();
    if (!userIdentifiers.isEmpty()) {
      List<UserEntity> users = userDAO.findAllByIdentifierIn(userIdentifiers);
      if (users != null) {
        assignedUsers =
            users.stream()
                .map(
                    u ->
                        new UserSummaryDTO(
                            u.getIdentifier(),
                            u.getUsername(),
                            u.getName(),
                            u.getSurname(),
                            u.getEmail()))
                .toList();
      }
    }

    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Assigned users successfully retrieved",
            TraceUtils.getSpanID(),
            assignedUsers);
    return Mono.just(ResponseEntity.ok(response));
  }

  /**
   * Crea un nuovo ruolo applicativo per il Client specificato, registrandone nome, tipologia,
   * descrizione e permessi granulari nella configurazione del client.
   *
   * @param dto DTO contenente i parametri di definizione del nuovo ruolo.
   * @return Mono con il ruolo creato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> createRole(RoleDefinitionDTO dto) {
    LOG.info("✨ [ROLES] Creating new role [{}] for client [{}]", dto.getRole(), dto.getClientId());
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can create roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String clientId = dto.getClientId().trim();
    String roleName = dto.getRole().trim().toUpperCase();

    ClientCredentialEntity clientEntity = clientDAO.findByClientId(clientId);
    if (clientEntity == null) {
      LOG.error("❌ Client [{}] not found", clientId);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, "Client not found: " + clientId);
    }

    List<AppRole> currentRoles = ClientMapper.parseAppRoles(clientEntity.getAppRoles());
    currentRoles = currentRoles != null ? new ArrayList<>(currentRoles) : new ArrayList<>();

    boolean exists =
        currentRoles.stream()
            .anyMatch(r -> r.getRole() != null && r.getRole().equalsIgnoreCase(roleName));
    if (exists) {
      LOG.error("❌ Role [{}] already exists for client [{}]", roleName, clientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, "Role " + roleName + " already exists for client " + clientId);
    }

    String rawType = dto.getType();
    String type =
        StringUtils.hasText(rawType)
            ? ("DEFAULT".equalsIgnoreCase(rawType.trim()) ? AppRoleType.USER.getValue() : rawType.trim().toUpperCase())
            : AppRoleType.USER.getValue();
    AppRole newRole = new AppRole();
    newRole.setRole(roleName);
    newRole.setType(type);
    newRole.setDescription(dto.getDescription());
    newRole.setPermissions(dto.getPermissions() != null ? dto.getPermissions() : List.of());

    currentRoles.add(newRole);
    clientEntity.setAppRoles(
        Mapper.writeObjectToString(ClientMapper.normalizeAppRoles(currentRoles)));
    clientDAO.save(clientEntity);

    RoleDefinitionDTO result =
        new RoleDefinitionDTO(
            clientId, roleName, type, dto.getDescription(), newRole.getPermissions(), 0, List.of());
    Response response =
        new Response(
            HttpStatus.CREATED.value(),
            "Role " + roleName + " successfully created for client " + clientId,
            TraceUtils.getSpanID(),
            result);
    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  /**
   * Aggiorna metadati, tipologia e permessi di un ruolo esistente su uno specifico Client OAuth2.
   * Se il nome del ruolo viene modificato, aggiorna a cascata le associazioni corrispondenti in USER_CLIENT_ROLE.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome attuale del ruolo da aggiornare.
   * @param dto DTO con i nuovi attributi del ruolo.
   * @return Mono con il ruolo aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> updateRole(
      String clientId, String role, RoleDefinitionDTO dto) {
    LOG.info("🔄 [ROLES] Updating role [{}] for client [{}]", role, clientId);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can update roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedClientId = clientId.trim();
    String currentRoleName = role.trim().toUpperCase();
    String newRoleName =
        StringUtils.hasText(dto.getRole()) ? dto.getRole().trim().toUpperCase() : currentRoleName;

    ClientCredentialEntity clientEntity = clientDAO.findByClientId(normalizedClientId);
    if (clientEntity == null) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, "Client not found: " + normalizedClientId);
    }

    List<AppRole> currentRoles = ClientMapper.parseAppRoles(clientEntity.getAppRoles());
    if (currentRoles == null || currentRoles.isEmpty()) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + currentRoleName + " not found for client " + normalizedClientId);
    }
    currentRoles = new ArrayList<>(currentRoles);

    AppRole targetAppRole =
        currentRoles.stream()
            .filter(r -> r.getRole() != null && r.getRole().equalsIgnoreCase(currentRoleName))
            .findFirst()
            .orElse(null);

    if (targetAppRole == null) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + currentRoleName + " not found for client " + normalizedClientId);
    }

    // Se il nome è variato, verifica che non entri in collisione con un altro ruolo esistente del client
    if (!currentRoleName.equalsIgnoreCase(newRoleName)) {
      boolean duplicate =
          currentRoles.stream()
              .anyMatch(
                  r ->
                      r != targetAppRole
                          && r.getRole() != null
                          && r.getRole().equalsIgnoreCase(newRoleName));
      if (duplicate) {
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_400,
            "Role " + newRoleName + " already exists for client " + normalizedClientId);
      }
      targetAppRole.setRole(newRoleName);

      // Aggiorna a cascata le associazioni in USER_CLIENT_ROLE
      List<UserClientRoleEntity> existingMappings =
          userClientRoleDAO.findByClientIdAndRole(normalizedClientId, currentRoleName);
      for (UserClientRoleEntity mapping : existingMappings) {
        mapping.setRole(newRoleName);
        userClientRoleDAO.save(mapping);
      }
    }

    if (StringUtils.hasText(dto.getType())) {
      String rawType = dto.getType().trim().toUpperCase();
      targetAppRole.setType("DEFAULT".equalsIgnoreCase(rawType) ? AppRoleType.USER.getValue() : rawType);
    }
    targetAppRole.setDescription(dto.getDescription());
    if (dto.getPermissions() != null) {
      targetAppRole.setPermissions(dto.getPermissions());
    }

    clientEntity.setAppRoles(
        Mapper.writeObjectToString(ClientMapper.normalizeAppRoles(currentRoles)));
    clientDAO.save(clientEntity);

    // Recupera conteggio e lista utenti aggiornata
    List<UserClientRoleEntity> assignments =
        userClientRoleDAO.findByClientIdAndRole(normalizedClientId, newRoleName);
    List<String> userIdentifiers =
        assignments.stream().map(UserClientRoleEntity::getUserIdentifier).distinct().toList();
    List<UserSummaryDTO> assignedUsers = new ArrayList<>();
    if (!userIdentifiers.isEmpty()) {
      List<UserEntity> users = userDAO.findAllByIdentifierIn(userIdentifiers);
      if (users != null) {
        assignedUsers =
            users.stream()
                .map(
                    u ->
                        new UserSummaryDTO(
                            u.getIdentifier(),
                            u.getUsername(),
                            u.getName(),
                            u.getSurname(),
                            u.getEmail()))
                .toList();
      }
    }

    RoleDefinitionDTO result =
        new RoleDefinitionDTO(
            normalizedClientId,
            newRoleName,
            targetAppRole.getType(),
            targetAppRole.getDescription(),
            targetAppRole.getPermissions(),
            assignedUsers.size(),
            assignedUsers);
    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Role " + newRoleName + " updated successfully",
            TraceUtils.getSpanID(),
            result);
    return Mono.just(ResponseEntity.ok(response));
  }

  /**
   * Elimina un ruolo applicativo dal Client e revoca a cascata tutte le assegnazioni utente
   * corrispondenti dalla tabella USER_CLIENT_ROLE, sincronizzando la colonna legacy USERS.ROLES
   * per gli utenti impattati.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome del ruolo da eliminare.
   * @return Mono con risposta di conferma e conteggio utenti impattati.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> deleteRole(String clientId, String role) {
    LOG.info("🗑️ [ROLES] Deleting role [{}] from client [{}]", role, clientId);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can delete roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedClientId = clientId.trim();
    String normalizedRole = role.trim().toUpperCase();

    ClientCredentialEntity clientEntity = clientDAO.findByClientId(normalizedClientId);
    if (clientEntity == null) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, "Client not found: " + normalizedClientId);
    }

    List<AppRole> currentRoles = ClientMapper.parseAppRoles(clientEntity.getAppRoles());
    if (currentRoles == null || currentRoles.isEmpty()) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + normalizedRole + " not found for client " + normalizedClientId);
    }
    currentRoles = new ArrayList<>(currentRoles);

    if (currentRoles.size() <= 1) {
      LOG.error("❌ Cannot delete the only remaining role for client [{}]", normalizedClientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          "Cannot delete role " + normalizedRole + ". Every OAuth2 client must have at least one role configured.");
    }

    boolean removed =
        currentRoles.removeIf(
            r -> r.getRole() != null && r.getRole().equalsIgnoreCase(normalizedRole));
    if (!removed) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + normalizedRole + " not found for client " + normalizedClientId);
    }

    clientEntity.setAppRoles(
        Mapper.writeObjectToString(ClientMapper.normalizeAppRoles(currentRoles)));
    clientDAO.save(clientEntity);

    // 1. Identifica tutti gli utenti che avevano questo ruolo per questo client
    List<UserClientRoleEntity> impactedAssignments =
        userClientRoleDAO.findByClientIdAndRole(normalizedClientId, normalizedRole);
    List<String> impactedUserIdentifiers =
        impactedAssignments.stream()
            .map(UserClientRoleEntity::getUserIdentifier)
            .distinct()
            .toList();

    // 2. Cancella a cascata tutte le associazioni relazionali per questo (clientId, role)
    userClientRoleDAO.deleteByClientIdAndRole(normalizedClientId, normalizedRole);
    LOG.info(
        "🗑️ [ROLES] Deleted {} assignments from USER_CLIENT_ROLE for role [{}] on client [{}]",
        impactedAssignments.size(),
        normalizedRole,
        normalizedClientId);

    // 3. Sincronizza USERS.ROLES per ciascun utente impattato
    for (String uId : impactedUserIdentifiers) {
      UserEntity user = userDAO.findUserEntityByIdentifier(uId);
      if (user != null) {
        List<UserClientRoleEntity> remainingUserRoles =
            userClientRoleDAO.findByUserIdentifier(uId);
        boolean stillHasRole =
            remainingUserRoles.stream().anyMatch(r -> r.getRole().equalsIgnoreCase(normalizedRole));

        if (!stillHasRole && user.getRoles() != null) {
          List<String> legacyList = new ArrayList<>(Arrays.asList(user.getRoles().split(",")));
          legacyList.removeIf(r -> r.trim().equalsIgnoreCase(normalizedRole));
          user.setRoles(String.join(",", legacyList));
          userDAO.save(user);
        }
      }
    }

    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Role "
                + normalizedRole
                + " successfully deleted from client "
                + normalizedClientId
                + " and revoked from "
                + impactedUserIdentifiers.size()
                + " users",
            TraceUtils.getSpanID(),
            null);
    return Mono.just(ResponseEntity.ok(response));
  }
}

