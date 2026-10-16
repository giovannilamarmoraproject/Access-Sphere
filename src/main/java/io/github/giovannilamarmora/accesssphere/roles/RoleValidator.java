package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionRequest;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Validatore per le operazioni sul catalogo dei ruoli applicativi e sulle assegnazioni ruoli
 * utente, inclusi i vincoli di perimetro multi-tenant (Data Scoping).
 */
@Component
public class RoleValidator {

  private static final Logger LOG = LoggerFilter.getLogger(RoleValidator.class);

  /**
   * Convalida i parametri obbligatori per la creazione o modifica di una definizione di ruolo.
   *
   * @param request DTO della richiesta.
   * @param clientId Client ID risolto da path/query o payload.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRoleRequest(RoleDefinitionRequest request, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(request)) {
      LOG.error("❌ Role definition request payload is null");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Role definition payload is required");
    }
    String effectiveClientId =
        !ObjectToolkit.isNullOrEmpty(clientId) ? clientId : request.getClientId();
    validateRoleParams(effectiveClientId, request.getRole());
  }

  /**
   * Convalida la presenza di Client ID e nome del ruolo.
   *
   * @param clientId Identificativo del Client OAuth2.
   * @param role Nome del ruolo.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRoleParams(String clientId, String role) {
    if (ObjectToolkit.isNullOrEmpty(clientId)) {
      LOG.error("❌ Client ID is missing in role operation");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Client ID is required");
    }
    if (ObjectToolkit.isNullOrEmpty(role)) {
      LOG.error("❌ Role name is missing in role operation");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, ExceptionType.INVALID_ROLES, "Role name is required");
    }
  }

  /**
   * Convalida i parametri per l'assegnazione o revoca puntuale di un ruolo a un utente.
   *
   * @param identifier UUID dell'utente.
   * @param clientId Client ID OAuth2.
   * @param role Nome del ruolo.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUserRoleParams(String identifier, String clientId, String role) {
    if (ObjectToolkit.isNullOrEmpty(identifier)) {
      LOG.error("❌ User identifier is missing in user role operation");
      throw new OAuthException(ExceptionMap.ERR_USER_400, "User identifier is required");
    }
    validateRoleParams(clientId, role);
  }

  /**
   * Verifica che un operatore non-SuperAdmin non tenti di alterare i ruoli dell'utente Admin di
   * sistema.
   *
   * @param targetUser Utente target dell'operazione.
   * @param permissionRoleManager Manager dei permessi per verificare l'utenza admin.
   * @param isAdmin Flag che indica se l'operatore chiamante ha privilegi di Super Admin.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateTargetUserNotAdmin(
      User targetUser, PermissionRoleManager permissionRoleManager, boolean isAdmin) {
    if (!isAdmin
        && targetUser != null
        && (permissionRoleManager.isAdminUser(targetUser.getUsername())
            || permissionRoleManager.isAdminUser(targetUser.getIdentifier()))) {
      LOG.error("⛔ Forbidden: Non-admin operator cannot alter roles of admin user");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, "Cannot alter admin user roles");
    }
  }

  /**
   * Verifica che il Client ID specificato rientri nel perimetro autorizzato dell'operatore.
   *
   * @param clientId Client ID target.
   * @param authorizedRoles Ruoli autorizzati dell'operatore.
   * @param isAdmin Flag che indica se l'operatore e Super Admin.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClientScope(
      String clientId, List<RoleDefinition> authorizedRoles, boolean isAdmin) {
    if (!RoleUtils.isClientAuthorized(clientId, authorizedRoles, isAdmin)) {
      LOG.error("⛔ Access denied: Operator is not authorized to manage roles for client [{}]", clientId);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  /**
   * Verifica che un operatore delegato non assegni nuovi ruoli su client esterni al proprio
   * perimetro autorizzato.
   *
   * @param requestedRoles Lista di ruoli richiesti.
   * @param existingRoles Lista di ruoli gia posseduti dall'utente target.
   * @param authorizedRoles Ruoli autorizzati dell'operatore chiamante.
   * @param isAdmin Flag che indica se l'operatore e Super Admin.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUserRolesClientScope(
      List<UserClientRole> requestedRoles,
      List<UserClientRole> existingRoles,
      List<RoleDefinition> authorizedRoles,
      boolean isAdmin) {
    if (isAdmin || ObjectToolkit.isNullOrEmpty(requestedRoles)) {
      return;
    }

    Set<String> authorizedClientIds = RoleUtils.extractAuthorizedClientIds(authorizedRoles);
    Set<String> existingKeys =
        ObjectToolkit.isNullOrEmpty(existingRoles)
            ? Set.of()
            : existingRoles.stream()
                .filter(r -> r != null && r.getClientId() != null && r.getRole() != null)
                .map(r -> RoleUtils.buildRoleKey(r.getClientId(), r.getRole()))
                .collect(Collectors.toSet());

    requestedRoles.forEach(
        role -> {
          if (role == null || ObjectToolkit.isNullOrEmpty(role.getClientId())) {
            throw new OAuthException(
                ExceptionMap.ERR_USER_400,
                ExceptionType.INVALID_ROLES,
                "Invalid Roles Configuration! Client id is required.");
          }
          String normClientId = role.getClientId().trim().toUpperCase();
          String key = RoleUtils.buildRoleKey(normClientId, role.getRole());
          if (!authorizedClientIds.contains(normClientId) && !existingKeys.contains(key)) {
            LOG.error(
                "⛔ Access denied: Operator attempted to assign role [{}] for unauthorized client [{}]",
                role.getRole(),
                normClientId);
            throw new OAuthException(
                ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
          }
        });
  }
}
