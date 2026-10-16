package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.roles.dto.RoleCatalogListResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleCatalogResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDeleteResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleUsersListResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.accesssphere.user.dto.UserResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI per la gestione amministrativa dei Ruoli applicativi e delle assegnazioni
 * utente (RBAC Multi-Tenant).
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Implementato da {@link RolesControllerImpl}.
 *   <li>Delega la business logic e i controlli di sicurezza a {@link RolesService}.
 *   <li>Utilizza i wrapper tipizzati {@link UserResponse}, {@link RoleCatalogResponse}, {@link
 *       RoleCatalogListResponse}, {@link RoleUsersListResponse} e {@link RoleDeleteResponse}.
 * </ul>
 */
@Tag(name = OpenAPI.Tag.ROLES, description = OpenAPI.Description.ROLES)
public interface RolesController {

  @RequestMapping(
      value = "/users/{identifier}/roles",
      method = {RequestMethod.PUT, RequestMethod.PATCH})
  @Operation(
      description =
          "Replace user roles across authorized OAuth2 clients (Reserved for operators with users:roles permission)",
      summary = "Change user roles",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User roles updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid role or client configuration",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions or client out of scope",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<UserResponse>> changeRoles(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @RequestBody @Valid @NotNull(message = "Roles are required")
          List<UserClientRoleRequest> roles);

  @PostMapping(value = "/users/{identifier}/clients/{clientId}/roles/{role}")
  @Operation(
      description =
          "Assign a role for a specific client to a user (Reserved for operators with users:roles permission)",
      summary = "Assign client-scoped role",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role assigned successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions or client out of scope",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<UserResponse>> assignClientRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @PathVariable(value = "clientId") @Schema(description = "OAuth2 Client ID") String clientId,
      @PathVariable(value = "role") @Schema(description = "Role name to assign") String role);

  @DeleteMapping(value = "/users/{identifier}/clients/{clientId}/roles/{role}")
  @Operation(
      description =
          "Revoke a role for a specific client from a user (Reserved for operators with users:roles permission)",
      summary = "Revoke client-scoped role",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role revoked successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions or client out of scope",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<UserResponse>> revokeClientRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @PathVariable(value = "clientId") @Schema(description = "OAuth2 Client ID") String clientId,
      @PathVariable(value = "role") @Schema(description = "Role name to revoke") String role);

  @GetMapping(value = "/roles")
  @Operation(
      description =
          "Get catalog of defined roles across authorized OAuth2 clients or filtered by clientId (Reserved for operators with roles:manage permission)",
      summary = "Get roles catalog",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Roles catalog retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoleCatalogListResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<RoleCatalogListResponse>> getRolesCatalog(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestParam(value = "clientId", required = false)
          @Schema(description = "Optional filter by Client ID")
          String clientId);

  @GetMapping(value = "/roles/{role}/users")
  @Operation(
      description =
          "Get users assigned to a specific role for a client (Reserved for operators with roles:manage permission)",
      summary = "Get users assigned to a role",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Assigned users retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoleUsersListResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<RoleUsersListResponse>> getUsersWithRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "role") @Schema(description = "Role name") String role,
      @RequestParam(value = "clientId") @Schema(description = "OAuth2 Client ID") String clientId);

  @PostMapping(value = "/roles")
  @Operation(
      description =
          "Create a new role definition for a specific OAuth2 client (Reserved for operators with roles:manage permission)",
      summary = "Create role definition",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Role created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoleCatalogResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - duplicate role or missing required fields",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<RoleCatalogResponse>> createRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "Role definition is required")
          RoleDefinitionRequest dto);

  @PutMapping(value = "/roles/{role}")
  @Operation(
      description =
          "Update an existing role definition for a specific OAuth2 client (Reserved for operators with roles:manage permission)",
      summary = "Update role definition",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoleCatalogResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Role or Client Not Found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<RoleCatalogResponse>> updateRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "role") @Schema(description = "Current role name") String role,
      @RequestParam(value = "clientId") @Schema(description = "OAuth2 Client ID") String clientId,
      @RequestBody @Valid @NotNull(message = "Role definition is required")
          RoleDefinitionRequest dto);

  @DeleteMapping(value = "/roles/{role}")
  @Operation(
      description =
          "Delete a role from a client and revoke it from all assigned users (Reserved for operators with roles:manage permission)",
      summary = "Delete role definition and cascade revoke",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role deleted and revoked successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoleDeleteResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Role or Client Not Found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<RoleDeleteResponse>> deleteRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "role") @Schema(description = "Role name to delete") String role,
      @RequestParam(value = "clientId") @Schema(description = "OAuth2 Client ID") String clientId);
}
