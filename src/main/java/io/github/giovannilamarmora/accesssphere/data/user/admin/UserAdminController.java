package io.github.giovannilamarmora.accesssphere.data.user.admin;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Contratto OpenAPI per la gestione amministrativa degli utenti IAM (Tech User / Admin). */
public interface UserAdminController {

  @GetMapping("/users")
  @Operation(
      description = "Get the full list of users, works only with Tech User or Tech Roles",
      summary = "User List",
      tags = OpenAPI.Tag.USERS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User list retrieved successfully",
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
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> userList(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @PostMapping("/users")
  @Operation(
      description = "Register a new user into IAM system",
      summary = "User Registration",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
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
            responseCode = "409",
            description = "Conflict, user already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> registerUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "User cannot be null") User user,
      @RequestParam(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestParam(value = "registration_token")
          @Schema(
              description = OpenAPI.Params.Description.REGISTRATION_TOKEN,
              example = OpenAPI.Params.Example.REGISTRATION_TOKEN)
          String registration_token,
      @RequestParam(value = "assign_new_client", defaultValue = "false")
          @Schema(description = OpenAPI.Params.Description.ASSIGN_NEW_CLIENT, example = "false")
          Boolean assignNewClient);

  @Deprecated(since = "2.0.0")
  @PostMapping("/users/register")
  @Operation(
      description = "Deprecated: use POST /v1/users instead. Register a new user into IAM system",
      summary = "User Registration (Deprecated)",
      tags = OpenAPI.Tag.USERS,
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
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
            responseCode = "409",
            description = "Conflict, user already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> registerUserLegacy(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "User cannot be null") User user,
      @RequestParam(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestParam(value = "registration_token")
          @Schema(
              description = OpenAPI.Params.Description.REGISTRATION_TOKEN,
              example = OpenAPI.Params.Example.REGISTRATION_TOKEN)
          String registration_token,
      @RequestParam(value = "assign_new_client", defaultValue = "false")
          @Schema(description = OpenAPI.Params.Description.ASSIGN_NEW_CLIENT, example = "false")
          Boolean assignNewClient);

  @DeleteMapping("/users/{identifier}")
  @Operation(
      description = "Delete an existing user",
      summary = "User Delete",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User deleted successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
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
  Mono<ResponseEntity<Response>> deleteUser(
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerWebExchange exchange);

  @PatchMapping("/users/{identifier}/status")
  @Operation(
      description = "Unlock or lock a user account",
      summary = "Unlock User",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User status updated successfully",
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
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> unlockUser(
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
      @RequestParam(value = "block")
          @Schema(
              description = OpenAPI.Params.Description.UNLOCK,
              example = OpenAPI.Params.Example.UNLOCK)
          Boolean block);
}
