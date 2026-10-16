package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.accesssphere.roles.mapper.RoleDefinitionMapper;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Application Service per l'amministrazione dei ruoli utente e del catalogo ruoli applicativi
 * (RBAC Multi-Tenant).
 *
 * <p>Responsabilita:
 *
 * <ul>
 *   <li>Funge da Gatekeeper di sicurezza e perimetro per gli endpoint di gestione ruoli ({@code
 *       /v1/roles} e {@code /v1/users/{identifier}/roles}).
 *   <li>Verifica i permessi granulari ({@link SystemPermission#USERS_ROLES} e {@link
 *       SystemPermission#ROLES_MANAGE}) e lo scoping multi-tenant per client tramite {@link
 *       PermissionRoleManager}.
 *   <li>Delega la logica di dominio e persistenza del catalogo a {@link ClientRoleDomainService} e
 *       le assegnazioni utente a {@link UserRoleDomainService} e {@link UserService}.
 *   <li>Restituisce direttamente DTO puri; la costruzione delle risposte HTTP {@link
 *       org.springframework.http.ResponseEntity} e delegata al Controller.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RolesService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String ROLES_LOG = "Roles Service";

  @Autowired private PermissionRoleManager permissionRoleManager;
  @Autowired private ClientRoleDomainService clientRoleDomainService;
  @Autowired private UserRoleDomainService userRoleDomainService;
  @Autowired private UserService userService;

  /**
   * Sostituisce i ruoli assegnati a un determinato utente applicando i controlli di sicurezza e lo
   * scoping multi-tenant sui client gestiti dall'operatore.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param roleRequests Lista dei ruoli da assegnare.
   * @return {@link Mono} con il {@link UserDataResponse} aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserDataResponse> changeRoles(
      String identifier, List<UserClientRoleRequest> roleRequests) {
    SystemPermission permission = SystemPermission.USERS_ROLES;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);

    LOG.info("👥 {} Changing user roles for identifier: {}", ROLES_LOG, identifier);
    List<UserClientRole> requestedRoles = UserClientRoleMapper.requestToModelList(roleRequests);

    return userService
        .getUserByIdentifier(identifier)
        .map(
            targetUser ->
                applyUserRolesChange(targetUser, requestedRoles, authorizedRoles, isAdmin))
        .map(UserMapper::toResponse)
        .doOnSuccess(
            u -> LOG.info("✅ {} User roles updated for identifier: {}", ROLES_LOG, identifier));
  }

  /**
   * Assegna un singolo ruolo contestualizzato a uno specifico Client OAuth2 per l'utente indicato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param clientId Identificativo univoco del client OAuth2 target.
   * @param role Nome del ruolo da assegnare.
   * @return {@link Mono} con il {@link UserDataResponse} aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserDataResponse> assignClientRole(String identifier, String clientId, String role) {
    RoleValidator.validateUserRoleParams(identifier, clientId, role);

    SystemPermission permission = SystemPermission.USERS_ROLES;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);
    RoleValidator.validateClientScope(clientId, authorizedRoles, isAdmin);

    LOG.info(
        "👥 {} Assigning role [{}] for client [{}] to user [{}]",
        ROLES_LOG,
        role,
        clientId,
        identifier);

    return userService
        .getUserByIdentifier(identifier)
        .map(
            targetUser -> {
              RoleValidator.validateTargetUserNotAdmin(targetUser, permissionRoleManager, isAdmin);
              List<UserClientRole> updatedRoles =
                  userRoleDomainService.assignUserRoleForClient(identifier, clientId, role);
              targetUser.setRoles(updatedRoles);
              return targetUser;
            })
        .map(UserMapper::toResponse);
  }

  /**
   * Revoca un singolo ruolo contestualizzato a uno specifico Client OAuth2 per l'utente indicato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param clientId Identificativo univoco del client OAuth2 target.
   * @param role Nome del ruolo da revocare.
   * @return {@link Mono} con il {@link UserDataResponse} aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserDataResponse> revokeClientRole(String identifier, String clientId, String role) {
    RoleValidator.validateUserRoleParams(identifier, clientId, role);

    SystemPermission permission = SystemPermission.USERS_ROLES;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);
    RoleValidator.validateClientScope(clientId, authorizedRoles, isAdmin);

    LOG.info(
        "👥 {} Revoking role [{}] for client [{}] from user [{}]",
        ROLES_LOG,
        role,
        clientId,
        identifier);

    return userService
        .getUserByIdentifier(identifier)
        .map(
            targetUser -> {
              RoleValidator.validateTargetUserNotAdmin(targetUser, permissionRoleManager, isAdmin);
              List<UserClientRole> updatedRoles =
                  userRoleDomainService.revokeUserRoleForClient(identifier, clientId, role);
              targetUser.setRoles(updatedRoles);
              return targetUser;
            })
        .map(UserMapper::toResponse);
  }

  /**
   * Restituisce il catalogo dei ruoli applicativi filtrato per il perimetro autorizzato
   * dell'operatore ed arricchito con conteggio e riepilogo degli utenti assegnati.
   *
   * @param clientId Filtro opzionale per Client ID OAuth2.
   * @return {@link Mono} con la lista di {@link RoleDefinitionResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<RoleDefinitionResponse>> getRolesCatalog(String clientId) {
    SystemPermission permission = SystemPermission.ROLES_MANAGE;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);

    if (!ObjectToolkit.isNullOrEmpty(clientId)) {
      RoleValidator.validateClientScope(clientId, authorizedRoles, isAdmin);
    }

    LOG.info("📋 {} Getting roles catalog, clientId filter: [{}]", ROLES_LOG, clientId);

    List<RoleDefinition> definitions = clientRoleDomainService.getRolesByClientFilter(clientId);
    if (!isAdmin) {
      Set<String> authorizedClientIds = RoleUtils.extractAuthorizedClientIds(authorizedRoles);
      definitions =
          definitions.stream()
              .filter(
                  d ->
                      !ObjectToolkit.isNullOrEmpty(d.getClientId())
                          && authorizedClientIds.contains(d.getClientId().trim().toUpperCase()))
              .toList();
    }

    List<RoleDefinition> enriched =
        userRoleDomainService.enrichRoleDefinitionsWithAssignedUsers(definitions);
    return Mono.just(RoleDefinitionMapper.toResponseList(enriched));
  }

  /**
   * Restituisce la lista sintetizzata degli utenti a cui e attualmente assegnato uno specifico
   * ruolo per un client.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome identificativo del ruolo.
   * @return {@link Mono} con la lista di {@link UserSummaryDTO}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<UserSummaryDTO>> getUsersWithRole(String clientId, String role) {
    RoleValidator.validateRoleParams(clientId, role);

    SystemPermission permission = SystemPermission.ROLES_MANAGE;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);
    RoleValidator.validateClientScope(clientId, authorizedRoles, isAdmin);

    LOG.info("👥 {} Getting users with role [{}] for client [{}]", ROLES_LOG, role, clientId);
    return Mono.just(userRoleDomainService.getUsersAssignedToRole(clientId, role));
  }

  /**
   * Crea un nuovo ruolo applicativo per il Client specificato previa verifica del perimetro
   * autorizzato.
   *
   * @param request DTO contenente i parametri di definizione del nuovo ruolo.
   * @return {@link Mono} con il {@link RoleDefinitionResponse} creato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<RoleDefinitionResponse> createRole(RoleDefinitionRequest request) {
    RoleValidator.validateRoleRequest(request, request != null ? request.getClientId() : null);

    SystemPermission permission = SystemPermission.ROLES_MANAGE;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);
    RoleValidator.validateClientScope(request.getClientId(), authorizedRoles, isAdmin);

    LOG.info(
        "✨ {} Creating new role [{}] for client [{}]",
        ROLES_LOG,
        request.getRole(),
        request.getClientId());

    RoleDefinition model = RoleDefinitionMapper.toModel(request);
    RoleDefinition created = clientRoleDomainService.createRoleDefinition(model);
    return Mono.just(RoleDefinitionMapper.toResponse(created));
  }

  /**
   * Aggiorna metadati, tipologia e permessi di un ruolo esistente su uno specifico Client OAuth2,
   * propagando a cascata eventuali rinomine sulle assegnazioni utente.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome attuale del ruolo da aggiornare.
   * @param request DTO con i nuovi attributi del ruolo.
   * @return {@link Mono} con il {@link RoleDefinitionResponse} aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<RoleDefinitionResponse> updateRole(
      String clientId, String role, RoleDefinitionRequest request) {
    RoleValidator.validateRoleParams(clientId, role);
    RoleValidator.validateRoleRequest(request, clientId);

    SystemPermission permission = SystemPermission.ROLES_MANAGE;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);
    RoleValidator.validateClientScope(clientId, authorizedRoles, isAdmin);

    LOG.info("🔄 {} Updating role [{}] for client [{}]", ROLES_LOG, role, clientId);

    RoleDefinition model = RoleDefinitionMapper.toModel(request, clientId);
    RoleDefinition updated = clientRoleDomainService.updateRoleDefinition(clientId, role, model);
    userRoleDomainService.cascadeRenameRoleAssignments(clientId, role, updated.getRole());
    userRoleDomainService.enrichRoleDefinitionWithAssignedUsers(updated);

    return Mono.just(RoleDefinitionMapper.toResponse(updated));
  }

  /**
   * Elimina un ruolo applicativo dal Client e revoca a cascata tutte le assegnazioni utente
   * corrispondenti dalla tabella {@code USER_CLIENT_ROLE}.
   *
   * @param clientId Identificativo univoco del client OAuth2.
   * @param role Nome del ruolo da eliminare.
   * @return {@link Mono} con il conteggio degli utenti impattati dalla revoca.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Integer> deleteRole(String clientId, String role) {
    RoleValidator.validateRoleParams(clientId, role);

    SystemPermission permission = SystemPermission.ROLES_MANAGE;
    List<RoleDefinition> authorizedRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(permission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(permission);
    RoleValidator.validateClientScope(clientId, authorizedRoles, isAdmin);

    LOG.info("🗑️ {} Deleting role [{}] from client [{}]", ROLES_LOG, role, clientId);

    clientRoleDomainService.deleteRoleDefinition(clientId, role);
    int impactedUsersCount = userRoleDomainService.cascadeDeleteRoleAssignments(clientId, role);
    return Mono.just(impactedUsersCount);
  }

  private User applyUserRolesChange(
      User targetUser,
      List<UserClientRole> requestedRoles,
      List<RoleDefinition> authorizedRoles,
      boolean isAdmin) {
    RoleValidator.validateTargetUserNotAdmin(targetUser, permissionRoleManager, isAdmin);
    RoleValidator.validateUserRolesClientScope(
        requestedRoles, targetUser.getRoles(), authorizedRoles, isAdmin);

    List<UserClientRole> effectiveRoles =
        RoleUtils.mergeScopedUserRoles(
            requestedRoles, targetUser.getRoles(), authorizedRoles, isAdmin);
    List<UserClientRole> updatedRoles =
        userRoleDomainService.updateUserRoles(targetUser.getIdentifier(), effectiveRoles);
    targetUser.setRoles(updatedRoles);
    return targetUser;
  }
}
