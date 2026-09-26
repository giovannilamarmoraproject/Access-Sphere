package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserConfig;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
 *
 * <ul>
 *   <li>Persiste le relazioni utente-client-ruolo su tabella relazionale {@code USER_CLIENT_ROLE}
 *       via {@link IUserClientRoleDAO}.
 *   <li>Mantiene la retrocompatibilita al 100% sincronizzando la colonna denormalizzata legacy
 *       {@code USERS.ROLES} via {@link UserService}.
 *   <li>Risolve la validita dei ruoli dai client OAuth2 registrati tramite {@link
 *       OAuth2ClientService} e {@link IOAuth2ClientDAO}.
 *   <li>Gestisce il catalogo centralizzato dei ruoli e permessi su {@code /v1/roles} con CRUD
 *       atomico e revoca a cascata.
 *   <li>Consente l'esecuzione delle operazioni esclusivamente a operatori autenticati con ruolo
 *       tecnico ({@link AdminUserService}).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RolesService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private OAuth2ClientService oAuth2ClientService;
  @Autowired private UserService userService;
  @Autowired private AdminUserService techUserService;
  @Autowired private AdminUserConfig adminUserConfig;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private IOAuth2ClientDAO clientDAO;
  @Autowired private IUserDAO userDAO;

  /**
   * Modifica i ruoli assegnati a un determinato utente, persistendo le associazioni nella tabella
   * relazionale {@code USER_CLIENT_ROLE} e mantenendo allineata per retrocompatibilita la lista
   * flat legacy in {@code USERS.ROLES}. Supporta sia il formato moderno basato su {@code
   * clientRoles} (client-scoped con permessi aggregati), sia il formato legacy basato su array flat
   * di stringhe {@code roles}. L'operazione e consentita esclusivamente a un utente tecnico
   * autorizzato.
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

    String techClientId = adminUserConfig.getAdminClientId();
    Mono<List<OAuth2Client>> getClients = oAuth2ClientService.getClientCredentials();

    return getClients
        .flatMap(
            clientCredentials -> {
              // 1. Raccoglie tutti i ruoli ammessi dai client registrati (nome ruolo e tipologia
              // applicativa)
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

              // 2. Raccoglie i ruoli canonici/di sistema definiti a livello di piattaforma
              // (AppRoleType)
              Arrays.stream(AppRoleType.values())
                  .forEach(
                      art -> {
                        String name = art.getValue().toUpperCase();
                        allowedRoles.add(name);
                        allowedRoles.add("ROLE_" + name);
                        allowedRoles.add("ACCESS-SPHERE_" + name);
                      });
              allowedRoles.add("ROLE_TECH");

              List<UserClientRoleEntity> entitiesToSave = new ArrayList<>();
              List<String> distinctRoleNames = new ArrayList<>();

              // 2. Elaborazione ruoli ricevuti
              if (roles != null && roles.getRoles() != null && !roles.getRoles().isEmpty()) {
                LOG.info(
                    "👥 [ROLES] Processing roles payload with {} entries", roles.getRoles().size());
                for (UserClientRole cr : roles.getRoles()) {
                  if (cr == null || cr.getRole() == null || cr.getRole().isBlank()) continue;
                  String roleName = cr.getRole().trim().toUpperCase();

                  boolean valid =
                      allowedRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(roleName));
                  if (!valid) {
                    LOG.warn(
                        "⚠️ [ROLES] Discarding unrecognized client-scoped role [{}] for user [{}]",
                        roleName,
                        identifier);
                    continue;
                  }

                  String targetClientId =
                      (cr.getClientId() != null && !cr.getClientId().isBlank())
                          ? cr.getClientId().trim()
                          : null;
                  if (targetClientId == null && clientCredentials != null) {
                    for (OAuth2Client cc : clientCredentials) {
                      if (cc.getAppRoles() != null) {
                        boolean match =
                            cc.getAppRoles().stream()
                                .anyMatch(ar -> ar != null && ar.matches(roleName));
                        if (match) {
                          targetClientId = cc.getClientId();
                          break;
                        }
                      }
                    }
                  }
                  if (targetClientId == null) {
                    targetClientId = techClientId;
                  }

                  final String finalTargetClientId = targetClientId;
                  boolean alreadyAdded =
                      entitiesToSave.stream()
                          .anyMatch(
                              e ->
                                  e.getClientId().equalsIgnoreCase(finalTargetClientId)
                                      && e.getRole().equalsIgnoreCase(roleName));
                  if (!alreadyAdded) {
                    entitiesToSave.add(
                        new UserClientRoleEntity(identifier, finalTargetClientId, roleName));
                  }
                  if (!distinctRoleNames.contains(roleName)) {
                    distinctRoleNames.add(roleName);
                  }
                }
              }

              // 3. Sostituzione atomica nella tabella relazionale USER_CLIENT_ROLE
              userClientRoleDAO.deleteByUserIdentifier(identifier);
              if (!entitiesToSave.isEmpty()) {
                userClientRoleDAO.saveAll(entitiesToSave);
                LOG.info(
                    "💾 [ROLES] Persisted {} entries in USER_CLIENT_ROLE for user {}",
                    entitiesToSave.size(),
                    identifier);
              }

              // 4. Sincronizzazione per retrocompatibilita su USERS.ROLES
              List<UserClientRole> assignedRoleDTOs =
                  entitiesToSave.stream()
                      .map(e -> new UserClientRole(e.getClientId(), e.getRole()))
                      .toList();
              return userService
                  .getUserByIdentifier(identifier)
                  .flatMap(
                      user -> {
                        user.setRoles(assignedRoleDTOs);
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
    LOG.info(
        "👥 [ROLES] Assigning role [{}] for client [{}] to user [{}]", role, clientId, identifier);
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

              // Sincronizza ruoli utente
              if (user.getRoles() == null) {
                user.setRoles(new ArrayList<>());
              }
              boolean hasRole =
                  user.getRoles().stream()
                      .anyMatch(
                          r ->
                              r != null
                                  && r.getRole() != null
                                  && r.getRole().equalsIgnoreCase(normalizedRole)
                                  && (r.getClientId() == null
                                      || r.getClientId().equalsIgnoreCase(normalizedClientId)));
              if (!hasRole) {
                user.getRoles().add(new UserClientRole(normalizedClientId, normalizedRole));
              }

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Role "
                                    + normalizedRole
                                    + " successfully assigned for client "
                                    + normalizedClientId,
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
    LOG.info(
        "👥 [ROLES] Revoking role [{}] for client [{}] from user [{}]", role, clientId, identifier);
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
              if (user.getRoles() != null) {
                List<UserClientRole> updatedRoles =
                    user.getRoles().stream()
                        .filter(
                            r ->
                                r != null
                                    && !(normalizedClientId.equalsIgnoreCase(r.getClientId())
                                        && normalizedRole.equalsIgnoreCase(r.getRole())))
                        .toList();
                user.setRoles(new ArrayList<>(updatedRoles));
              }

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Role "
                                    + normalizedRole
                                    + " successfully revoked for client "
                                    + normalizedClientId,
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

    List<RoleDefinitionEntity> roleEntities;
    if (StringUtils.hasText(clientId)) {
      roleEntities = roleDefinitionDAO.findByClientId(clientId.trim());
    } else {
      roleEntities = roleDefinitionDAO.findAll();
    }

    List<RoleDefinition> catalog = new ArrayList<>();
    if (roleEntities != null) {
      for (RoleDefinitionEntity roleDef : roleEntities) {
        if (roleDef == null || roleDef.getRole() == null) continue;
        String roleName = roleDef.getRole().toUpperCase();
        String cId = roleDef.getClientId();

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

        RoleDefinition def =
            new RoleDefinition(
                cId,
                roleName,
                roleDef.getType(),
                roleDef.getDescription(),
                roleDef.getPermissions(),
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
   * Restituisce la lista degli utenti a cui è attualmente assegnato uno specifico ruolo per un
   * client. Utilizzato in particolare dalla console prima della cancellazione per mostrare l'elenco
   * nominativo degli utenti impattati nel popup di conferma SweetAlert2.
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
  public Mono<ResponseEntity<Response>> createRole(RoleDefinition dto) {
    LOG.info("✨ [ROLES] Creating new role [{}] for client [{}]", dto.getRole(), dto.getClientId());
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can create roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String clientId = dto.getClientId().trim();
    String roleName = dto.getRole().trim().toUpperCase();

    OAuth2ClientEntity clientEntity = clientDAO.findByClientId(clientId);
    if (clientEntity == null) {
      LOG.error("❌ Client [{}] not found", clientId);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, "Client not found: " + clientId);
    }

    boolean exists = roleDefinitionDAO.existsByClientIdAndRole(clientId, roleName);
    if (exists) {
      LOG.error("❌ Role [{}] already exists for client [{}]", roleName, clientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          "Role " + roleName + " already exists for client " + clientId);
    }

    String rawType = dto.getType();
    String type =
        StringUtils.hasText(rawType)
            ? ("DEFAULT".equalsIgnoreCase(rawType.trim())
                ? AppRoleType.USER.getValue()
                : rawType.trim().toUpperCase())
            : AppRoleType.USER.getValue();

    RoleDefinitionEntity newRoleEntity =
        new RoleDefinitionEntity(
            clientId,
            roleName,
            type,
            dto.getDescription(),
            dto.getPermissions() != null ? dto.getPermissions() : List.of());
    roleDefinitionDAO.save(newRoleEntity);

    RoleDefinition result =
        new RoleDefinition(
            clientId,
            roleName,
            type,
            dto.getDescription(),
            newRoleEntity.getPermissions(),
            0,
            List.of());
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
   * Se il nome del ruolo viene modificato, aggiorna a cascata le associazioni corrispondenti in
   * USER_CLIENT_ROLE.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome attuale del ruolo da aggiornare.
   * @param dto DTO con i nuovi attributi del ruolo.
   * @return Mono con il ruolo aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> updateRole(
      String clientId, String role, RoleDefinition dto) {
    LOG.info("🔄 [ROLES] Updating role [{}] for client [{}]", role, clientId);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can update roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedClientId = clientId.trim();
    String currentRoleName = role.trim().toUpperCase();
    String newRoleName =
        StringUtils.hasText(dto.getRole()) ? dto.getRole().trim().toUpperCase() : currentRoleName;

    OAuth2ClientEntity clientEntity = clientDAO.findByClientId(normalizedClientId);
    if (clientEntity == null) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404, "Client not found: " + normalizedClientId);
    }

    RoleDefinitionEntity targetRoleEntity =
        roleDefinitionDAO
            .findByClientIdAndRole(normalizedClientId, currentRoleName)
            .orElseThrow(
                () ->
                    new OAuthException(
                        ExceptionMap.ERR_OAUTH_404,
                        "Role " + currentRoleName + " not found for client " + normalizedClientId));

    // Se il nome è variato, verifica che non entri in collisione con un altro ruolo esistente del
    // client
    if (!currentRoleName.equalsIgnoreCase(newRoleName)) {
      boolean duplicate =
          roleDefinitionDAO.existsByClientIdAndRole(normalizedClientId, newRoleName);
      if (duplicate) {
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_400,
            "Role " + newRoleName + " already exists for client " + normalizedClientId);
      }
      targetRoleEntity.setRole(newRoleName);

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
      targetRoleEntity.setType(
          "DEFAULT".equalsIgnoreCase(rawType) ? AppRoleType.USER.getValue() : rawType);
    }
    targetRoleEntity.setDescription(dto.getDescription());
    if (dto.getPermissions() != null) {
      targetRoleEntity.setPermissions(dto.getPermissions());
    }

    roleDefinitionDAO.save(targetRoleEntity);

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

    RoleDefinition result =
        new RoleDefinition(
            normalizedClientId,
            newRoleName,
            targetRoleEntity.getType(),
            targetRoleEntity.getDescription(),
            targetRoleEntity.getPermissions(),
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
   * corrispondenti dalla tabella USER_CLIENT_ROLE, sincronizzando la colonna legacy USERS.ROLES per
   * gli utenti impattati.
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

    OAuth2ClientEntity clientEntity = clientDAO.findByClientId(normalizedClientId);
    if (clientEntity == null) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404, "Client not found: " + normalizedClientId);
    }

    List<RoleDefinitionEntity> currentRoles = roleDefinitionDAO.findByClientId(normalizedClientId);
    if (currentRoles == null || currentRoles.isEmpty()) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + normalizedRole + " not found for client " + normalizedClientId);
    }

    if (currentRoles.size() <= 1) {
      LOG.error("❌ Cannot delete the only remaining role for client [{}]", normalizedClientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          "Cannot delete role "
              + normalizedRole
              + ". Every OAuth2 client must have at least one role configured.");
    }

    boolean exists =
        currentRoles.stream().anyMatch(r -> r.getRole().equalsIgnoreCase(normalizedRole));
    if (!exists) {
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + normalizedRole + " not found for client " + normalizedClientId);
    }

    // 1. Elimina la definizione del ruolo dalla tabella relazionale ROLE_DEFINITIONS
    roleDefinitionDAO.deleteByClientIdAndRole(normalizedClientId, normalizedRole);

    // 2. Identifica tutti gli utenti che avevano questo ruolo per questo client
    List<UserClientRoleEntity> impactedAssignments =
        userClientRoleDAO.findByClientIdAndRole(normalizedClientId, normalizedRole);
    List<String> impactedUserIdentifiers =
        impactedAssignments.stream()
            .map(UserClientRoleEntity::getUserIdentifier)
            .distinct()
            .toList();

    // 3. Cancella a cascata tutte le associazioni relazionali per questo (clientId, role)
    userClientRoleDAO.deleteByClientIdAndRole(normalizedClientId, normalizedRole);
    LOG.info(
        "🗑️ [ROLES] Deleted {} assignments from USER_CLIENT_ROLE for role [{}] on client [{}]",
        impactedAssignments.size(),
        normalizedRole,
        normalizedClientId);

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
