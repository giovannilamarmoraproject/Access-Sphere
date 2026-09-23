package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionDTO;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface RolesController {

  @RequestMapping(
      value = "/users/{identifier}/roles",
      method = {RequestMethod.PUT, RequestMethod.PATCH})
  @Operation(
      description = "Change user roles, only Tech User",
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
                    schema = @Schema(implementation = User.class))),
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changeRoles(
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
      @RequestBody @Valid @NotNull(message = "Roles are required") UserRoles roles);

  @PostMapping(value = "/users/{identifier}/clients/{clientId}/roles/{role}")
  @Operation(
      description = "Assign a role for a specific client to a user, only Tech User",
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
                    schema = @Schema(implementation = User.class))),
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
  Mono<ResponseEntity<Response>> assignClientRole(
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
      @PathVariable(value = "clientId")
          @Schema(description = "OAuth2 Client ID")
          String clientId,
      @PathVariable(value = "role")
          @Schema(description = "Role name to assign")
          String role);

  @DeleteMapping(value = "/users/{identifier}/clients/{clientId}/roles/{role}")
  @Operation(
      description = "Revoke a role for a specific client from a user, only Tech User",
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
                    schema = @Schema(implementation = User.class))),
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
  Mono<ResponseEntity<Response>> revokeClientRole(
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
      @PathVariable(value = "clientId")
          @Schema(description = "OAuth2 Client ID")
          String clientId,
      @PathVariable(value = "role")
          @Schema(description = "Role name to revoke")
          String role);

  @GetMapping(value = "/roles")
  @Operation(
      description = "Get full catalog of defined roles across all OAuth2 clients or filtered by clientId, only Tech User",
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
                    schema = @Schema(implementation = RoleDefinitionDTO.class))),
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
  Mono<ResponseEntity<Response>> getRolesCatalog(
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
      description = "Get users assigned to a specific role for a client, only Tech User",
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
                    schema = @Schema(implementation = UserSummaryDTO.class))),
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
  Mono<ResponseEntity<Response>> getUsersWithRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "role")
          @Schema(description = "Role name")
          String role,
      @RequestParam(value = "clientId")
          @Schema(description = "OAuth2 Client ID")
          String clientId);

  @PostMapping(value = "/roles")
  @Operation(
      description = "Create a new role definition for a specific OAuth2 client, only Tech User",
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
                    schema = @Schema(implementation = RoleDefinitionDTO.class))),
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
  Mono<ResponseEntity<Response>> createRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "Role definition is required") RoleDefinitionDTO dto);

  @PutMapping(value = "/roles/{role}")
  @Operation(
      description = "Update an existing role definition for a specific OAuth2 client, only Tech User",
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
                    schema = @Schema(implementation = RoleDefinitionDTO.class))),
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
  Mono<ResponseEntity<Response>> updateRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "role")
          @Schema(description = "Current role name")
          String role,
      @RequestParam(value = "clientId")
          @Schema(description = "OAuth2 Client ID")
          String clientId,
      @RequestBody @Valid @NotNull(message = "Role definition is required") RoleDefinitionDTO dto);

  @DeleteMapping(value = "/roles/{role}")
  @Operation(
      description = "Delete a role from a client and revoke it from all assigned users, only Tech User",
      summary = "Delete role definition and cascade revoke",
      tags = OpenAPI.Tag.ROLES,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role deleted and revoked successfully"),
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
  Mono<ResponseEntity<Response>> deleteRole(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "role")
          @Schema(description = "Role name to delete")
          String role,
      @RequestParam(value = "clientId")
          @Schema(description = "OAuth2 Client ID")
          String clientId);
}
