package io.github.giovannilamarmora.accesssphere.user.account;

import io.github.giovannilamarmora.accesssphere.user.dto.UserResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserUpdateRequest;
import io.github.giovannilamarmora.accesssphere.user.model.ChangePassword;
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
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *
 * <p>Definisce gli endpoint REST dedicati all'utente finale per la consultazione del proprio
 * profilo anagrafico, l'aggiornamento dei dati personali e il flusso di recupero credenziali
 * (password reset in due fasi).
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Implementato da {@link UserAccountControllerImpl}.
 *   <li>Inoltra le richieste operative a {@link UserAccountService}.
 *   <li>Utilizza gli involucri di risposta tipizzati {@link UserResponse} e {@link Response} per la
 *       documentazione OpenAPI Swagger UI.
 * </ul>
 */
@Tag(name = OpenAPI.Tag.ACCOUNT, description = OpenAPI.Description.ACCOUNT)
public interface UserAccountController {

  /**
   * Recupera il profilo anagrafico completo dell'utente attualmente autenticato.
   *
   * @param bearer Token di accesso Bearer inviato nell'header Authorization.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link UserResponse}.
   */
  @GetMapping("/users/profile")
  @Operation(
      summary = "User Profile",
      description =
          "Retrieve the complete profile and configurations of the currently authenticated user based on session token",
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
                    schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Caller lacks permissions to view this profile",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - User profile not found in database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<UserResponse>> profile(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  /**
   * Aggiorna i dati anagrafici e personali del profilo utente specificato.
   *
   * @param identifier Identificatore univoco UUID dell'utente da aggiornare.
   * @param user DTO contenente i dati anagrafici aggiornati.
   * @param bearer Token di accesso Bearer inviato nell'header Authorization.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link Response}.
   */
  @PutMapping("/users/{identifier}")
  @Operation(
      summary = "Update User Profile",
      description =
          "Update an existing user profile's personal details and configurations. Non-technical users can only update their own profile and cannot modify assigned roles.",
      tags = OpenAPI.Tag.ACCOUNT,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid user data or validation constraints violated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description =
                "Forbidden - Cannot update profile of another user without technical/administrative privileges",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - Target user identifier not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<UserResponse>> updateUser(
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @RequestBody @Valid @NotNull(message = "User cannot be null") UserUpdateRequest user,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  /**
   * Avvia la procedura di reset password (Step 1) generando un token di recupero e inviando l'email
   * di notifica.
   *
   * @param changePassword DTO con email del destinatario, clientId e templateId opzionale.
   * @param locale Lingua di localizzazione per il template di posta (es. it-IT, en-GB).
   * @param sendEmail Flag per abilitare l'invio dell'email (true) o restituire il token
   *     direttamente (false).
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link Response}.
   */
  @PostMapping("/users/password/reset-request")
  @Operation(
      summary = "Password Reset Request (Step 1)",
      description =
          "Initiate password reset process (Step 1). Generates a secure reset token and optionally dispatches a localized reset email to the user.",
      tags = OpenAPI.Tag.ACCOUNT)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Password reset request processed successfully (email sent or reset token returned)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid email format or malformed request payload",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - User email or requested email template not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePasswordRequest(
      @RequestBody @Valid ChangePassword changePassword,
      @RequestParam(value = "locale", required = false, defaultValue = "en-GB")
          @Schema(
              description = "Locale identifier for email template localization",
              example = "it-IT",
              defaultValue = "en-GB")
          String locale,
      @RequestParam(value = "emailSend", required = false, defaultValue = "true")
          @Schema(
              description =
                  "Flag to determine whether to dispatch the reset email (true) or return the token in the response payload (false)",
              example = "true",
              defaultValue = "true")
          boolean sendEmail);

  /**
   * Completa la procedura di reset password (Step 2) verificando il token e aggiornando la
   * password.
   *
   * @param changePassword DTO contenente token di recupero e nuova password codificata in Base64.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link Response}.
   */
  @PostMapping("/users/password/reset")
  @Operation(
      summary = "Password Reset Confirmation (Step 2)",
      description =
          "Complete password reset process (Step 2). Verifies the one-time reset token and updates the user's password with the new Base64-encoded credential.",
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
            description =
                "Bad Request - Invalid Base64 password encoding or complexity requirements not met",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - Invalid or expired reset token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePassword(@RequestBody @Valid ChangePassword changePassword);
}
