package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAManageRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupResponseWrapper;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI REST per la gestione e l'autenticazione a due fattori (MFA).
 *
 * <p>Definisce le operazioni di:
 * <ul>
 *   <li>Inizializzazione del setup MFA (generazione secret TOTP o primo OTP Email).</li>
 *   <li>Conferma e attivazione definitiva del metodo configurato con OTP a 6 cifre.</li>
 *   <li>Verifica del codice OTP durante il login OAuth 2.1 con temp_token Bearer.</li>
 *   <li>Invio o reinvio on-demand del codice OTP via email.</li>
 *   <li>Gestione dello stato (abilitazione, disabilitazione o rimozione dei metodi).</li>
 * </ul>
 */
@Tag(name = OpenAPI.Tag.MFA, description = OpenAPI.Description.MFA)
public interface MFAController {

  /**
   * Endpoint per inizializzare il setup di un metodo MFA per l'utente (TOTP o EMAIL).
   *
   * @param bearer Token JWT Bearer dell'utente autenticato o dell'operatore autorizzato.
   * @param mfaSetupRequest Payload con identificativo utente, tipo MFA ed eventuale richiesta QR code.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link MFASetupResponseWrapper}.
   */
  @PostMapping("/setup")
  @Operation(
      summary = "MFA Setup",
      description =
          "Initialize Multi-Factor Authentication setup for a user by generating TOTP secret / QR code or dispatching an Email OTP code",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA setup initialized successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFASetupResponseWrapper.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid payload or configuration",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<Response>> setupMfa(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid MFASetupRequest mfaSetupRequest,
      ServerWebExchange exchange);

  /**
   * Endpoint per confermare il codice OTP e attivare il metodo MFA appena configurato.
   *
   * @param bearer Token JWT Bearer dell'utente.
   * @param mfaConfirmationRequest Payload con identificativo, tipo e codice OTP a 6 cifre.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente esito di attivazione.
   */
  @PostMapping("/confirm")
  @Operation(
      summary = "MFA Confirmation",
      description = "Verify and confirm an MFA setup code to activate Multi-Factor Authentication",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA confirmed and activated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFAResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid OTP code or user not found",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<Response>> confirmMfa(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid MFAConfirmationRequest mfaConfirmationRequest,
      ServerWebExchange exchange);

  /**
   * Endpoint per verificare il codice OTP durante il flusso di login OAuth 2.1 o step-up.
   *
   * @param mfaVerifyRequest Payload con metodo MFA, codice OTP e opzione device fidato.
   * @param bearer Token temporaneo (temp_token) Bearer rilasciato nella prima fase di login.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente i token finali di sessione.
   */
  @PostMapping("/verify")
  @Operation(
      summary = "MFA Verification",
      description =
          "Verify MFA code during OAuth 2.1 authentication flow using temporary Bearer token to issue final access tokens",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA verified successfully, access tokens issued",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid OTP code",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Expired or invalid temporary MFA token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<Response>> verifyMfaCode(
      @RequestBody @Valid MfaVerificationRequest mfaVerifyRequest,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerWebExchange exchange);

  /**
   * Endpoint per inviare o reinviare il codice OTP via email.
   *
   * @param bearer Token temporaneo Bearer di login oppure token di accesso utente autenticato.
   * @param identifier Identificativo opzionale dell'utente.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con conferma dell'invio.
   */
  @PostMapping("/send-otp")
  @Operation(
      summary = "Send MFA OTP",
      description = "Dispatch or resend a one-time MFA security code via Email",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA OTP code dispatched successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFAResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - User not found or Email MFA not configured",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing credentials",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<Response>> sendOtp(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestParam(value = "identifier", required = false)
          @Schema(description = "Optional user identifier")
          String identifier,
      ServerWebExchange exchange);

  /**
   * Endpoint per gestire lo stato dei metodi MFA (abilitazione globale, disabilitazione o cancellazione).
   *
   * @param bearer Token JWT Bearer dell'operatore o utente.
   * @param mfaManageRequest Payload con identificativo, azione e label.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con esito dell'azione.
   */
  @PostMapping("/manage")
  @Operation(
      summary = "MFA Management",
      description = "Manage MFA methods for user (enable, disable, or delete specific method)",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA settings managed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFAResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Method not found or invalid action",
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<Response>> manageMfa(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid MFAManageRequest mfaManageRequest,
      ServerWebExchange exchange);
}
