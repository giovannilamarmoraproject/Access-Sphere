package io.github.giovannilamarmora.accesssphere.data.user.account;

import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
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
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI per le operazioni self-service e di gestione account utente (profilo e
 * password).
 */
public interface UserAccountController {

  @GetMapping("/users/profile")
  @Operation(
      description = "Obtaining the profile of the current User",
      summary = "User Profile",
      tags = OpenAPI.Tag.ACCOUNT,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved successfully",
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
  Mono<ResponseEntity<Response>> profile(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @Deprecated(since = "2.0.0")
  @PutMapping("/users/update")
  @Operation(
      description = "Deprecated: use PUT /v1/users/{identifier} instead. Update an existing user profile",
      summary = "User Update (Deprecated)",
      tags = OpenAPI.Tag.ACCOUNT,
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> updateUser(
      @RequestBody @Valid @NotNull(message = "User cannot be null") User user,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @PutMapping("/users/{identifier}")
  @Operation(
      description = "Update an existing user profile",
      summary = "User Update",
      tags = OpenAPI.Tag.ACCOUNT)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> updateUser(
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @RequestBody @Valid @NotNull(message = "User cannot be null") User user,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @PostMapping("/users/password/reset-request")
  @Operation(
      description = "Send Email to change the user password (Step 1)",
      summary = "Password Reset Request (Step 1)",
      tags = OpenAPI.Tag.ACCOUNT)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email successfully sent or token generated",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePasswordRequest(
      @RequestBody @Valid ChangePassword changePassword,
      @RequestParam(value = "locale", required = false, defaultValue = "en-GB") String locale,
      @RequestParam(value = "emailSend", required = false, defaultValue = "true")
          boolean sendEmail);

  @Deprecated(since = "2.0.0")
  @PostMapping("/users/change/password/request")
  @Operation(
      description = "Deprecated: use POST /v1/users/password/reset-request instead. Send Email to change the user password (Step 1)",
      summary = "Change Password Step 1 (Deprecated)",
      tags = OpenAPI.Tag.ACCOUNT,
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email successfully sent or token generated",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePasswordRequestLegacy(
      @RequestBody @Valid ChangePassword changePassword,
      @RequestParam(value = "locale", required = false, defaultValue = "en-GB") String locale,
      @RequestParam(value = "emailSend", required = false, defaultValue = "true")
          boolean sendEmail);

  @PostMapping("/users/password/reset")
  @Operation(
      description = "Change the user password using reset token (Step 2)",
      summary = "Password Reset Confirmation (Step 2)",
      tags = OpenAPI.Tag.ACCOUNT)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password updated successfully",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePassword(@RequestBody @Valid ChangePassword changePassword);

  @Deprecated(since = "2.0.0")
  @PostMapping("/users/change/password")
  @Operation(
      description = "Deprecated: use POST /v1/users/password/reset instead. Change the user password using reset token (Step 2)",
      summary = "Change Password Step 2 (Deprecated)",
      tags = OpenAPI.Tag.ACCOUNT,
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password updated successfully",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePasswordLegacy(@RequestBody @Valid ChangePassword changePassword);
}
