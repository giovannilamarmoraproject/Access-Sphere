package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.roles.dto.RoleCatalogListResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleCatalogResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDeleteResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleUsersListResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.accesssphere.user.dto.UserResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per l'amministrazione dei Ruoli applicativi e delle assegnazioni utente.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link RolesController}.
 *   <li>Delega verifiche RBAC, scoping multi-tenant e logica applicativa a {@link RolesService}.
 *   <li>Costruisce le risposte HTTP {@link ResponseEntity} con i wrapper tipizzati e distributed
 *       tracing ({@link TraceUtils#getSpanID()}).
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.ROLES, description = OpenAPI.Description.ROLES)
public class RolesControllerImpl implements RolesController {

  @Autowired private RolesService rolesService;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<UserResponse>> changeRoles(
      String bearer, String identifier, List<UserClientRoleRequest> roles) {
    return rolesService
        .changeRoles(identifier, roles)
        .map(
            updatedUser -> {
              UserResponse response =
                  new UserResponse(
                      HttpStatus.OK.value(),
                      "User roles for %s successfully changed!".formatted(updatedUser.getUsername()),
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<UserResponse>> assignClientRole(
      String bearer, String identifier, String clientId, String role) {
    return rolesService
        .assignClientRole(identifier, clientId, role)
        .map(
            updatedUser -> {
              UserResponse response =
                  new UserResponse(
                      HttpStatus.OK.value(),
                      "Role %s successfully assigned for client %s"
                          .formatted(RoleUtils.normalizeUpperCase(role), clientId.trim()),
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<UserResponse>> revokeClientRole(
      String bearer, String identifier, String clientId, String role) {
    return rolesService
        .revokeClientRole(identifier, clientId, role)
        .map(
            updatedUser -> {
              UserResponse response =
                  new UserResponse(
                      HttpStatus.OK.value(),
                      "Role %s successfully revoked for client %s"
                          .formatted(RoleUtils.normalizeUpperCase(role), clientId.trim()),
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<RoleCatalogListResponse>> getRolesCatalog(
      String bearer, String clientId) {
    return rolesService
        .getRolesCatalog(clientId)
        .map(
            catalog -> {
              RoleCatalogListResponse response =
                  new RoleCatalogListResponse(
                      HttpStatus.OK.value(),
                      "Roles catalog successfully retrieved",
                      TraceUtils.getSpanID(),
                      catalog);
              return ResponseEntity.ok(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<RoleUsersListResponse>> getUsersWithRole(
      String bearer, String role, String clientId) {
    return rolesService
        .getUsersWithRole(clientId, role)
        .map(
            assignedUsers -> {
              RoleUsersListResponse response =
                  new RoleUsersListResponse(
                      HttpStatus.OK.value(),
                      "Assigned users successfully retrieved",
                      TraceUtils.getSpanID(),
                      assignedUsers);
              return ResponseEntity.ok(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<RoleCatalogResponse>> createRole(
      String bearer, RoleDefinitionRequest dto) {
    return rolesService
        .createRole(dto)
        .map(
            created -> {
              RoleCatalogResponse response =
                  new RoleCatalogResponse(
                      HttpStatus.CREATED.value(),
                      "Role %s successfully created for client %s"
                          .formatted(created.getRole(), created.getClientId()),
                      TraceUtils.getSpanID(),
                      created);
              return ResponseEntity.status(HttpStatus.CREATED).body(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<RoleCatalogResponse>> updateRole(
      String bearer, String role, String clientId, RoleDefinitionRequest dto) {
    return rolesService
        .updateRole(clientId, role, dto)
        .map(
            updated -> {
              RoleCatalogResponse response =
                  new RoleCatalogResponse(
                      HttpStatus.OK.value(),
                      "Role %s updated successfully".formatted(updated.getRole()),
                      TraceUtils.getSpanID(),
                      updated);
              return ResponseEntity.ok(response);
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<RoleDeleteResponse>> deleteRole(
      String bearer, String role, String clientId) {
    return rolesService
        .deleteRole(clientId, role)
        .map(
            impactedUsersCount -> {
              RoleDeleteResponse response =
                  new RoleDeleteResponse(
                      HttpStatus.OK.value(),
                      "Role %s successfully deleted from client %s and revoked from %d users"
                          .formatted(
                              RoleUtils.normalizeUpperCase(role),
                              clientId.trim(),
                              impactedUsersCount),
                      TraceUtils.getSpanID(),
                      null);
              return ResponseEntity.ok(response);
            });
  }
}
