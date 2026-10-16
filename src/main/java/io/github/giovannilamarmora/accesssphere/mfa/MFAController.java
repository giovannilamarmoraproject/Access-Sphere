package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAManageRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
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
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link MFASetupResponse}.
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
            responseCode = "201",
            description = "MFA setup initialized successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFASetupResponse.class))),
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
  Mono<ResponseEntity<MFASetupResponse>> setupMfa(
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
   * @return {@link Mono} con {@link ResponseEntity} contenente esito di attivazione in {@link MFAResponse}.
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
            description = "MFA confirmed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFAResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid OTP or mismatched configuration",
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
  Mono<ResponseEntity<MFAResponse>> confirmMfa(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid MFAConfirmationRequest mfaConfirmationRequest,
      ServerWebExchange exchange);

  /**
   * Endpoint per validare il codice OTP immesso durante l'accesso utente (Login Challenge).
   *
   * @param bearer Token temporaneo (temp_token) Bearer generato nella prima fase di autenticazione.
   * @param mfaRequest Payload contenente metodo utilizzato, codice OTP e flag 'rememberDevice'.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente i token OAuth definitivi.
   */
  @PostMapping("/verify")
  @Operation(
      summary = "MFA Verification (Login Challenge)",
      description =
          "Validate OTP code during OAuth 2.1 authentication flow using the temporary token",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA validation successful, OAuth 2.1 access tokens issued",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid OTP code or expired session",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid temp_token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<Response>> verifyMfa(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid MfaVerificationRequest mfaRequest,
      ServerWebExchange exchange);

  /**
   * Endpoint per inviare o reinviare un codice OTP all'utente via email.
   *
   * @param bearer Token Bearer opzionale (tempToken del flusso di login o token utente autenticato).
   * @param identifier Identificativo opzionale dell'utente per richieste autorizzate.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link MFAResponse}.
   */
  @PostMapping("/send-otp")
  @Operation(
      summary = "Send / Resend MFA OTP",
      description =
          "Dispatches an MFA OTP code via email during OAuth 2.1 login challenge or upon authenticated request",
      tags = OpenAPI.Tag.MFA)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA OTP sent successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFAResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Unable to resolve user or email address not configured",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<MFAResponse>> sendOtp(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestParam(value = "identifier", required = false)
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      ServerWebExchange exchange);

  /**
   * Endpoint per la gestione globale dello stato MFA dell'utente (ENABLE, DISABLE, DELETE).
   *
   * @param bearer Token JWT Bearer dell'utente autenticato o dell'operatore autorizzato.
   * @param manageRequest Payload con identificativo, azione ed eventuale label.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link MFAResponse}.
   */
  @PostMapping("/manage")
  @Operation(
      summary = "Manage MFA Methods",
      description = "Enable, disable or delete Multi-Factor Authentication methods for an account",
      tags = OpenAPI.Tag.MFA,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA action completed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MFAResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid action or method not found",
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
  Mono<ResponseEntity<MFAResponse>> manageMfa(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid MFAManageRequest manageRequest,
      ServerWebExchange exchange);
}
