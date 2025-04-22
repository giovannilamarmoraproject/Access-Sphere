package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.mfa.dto.*;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface MFAController {

  /**
   * Endpoint to initialize the setup of Multi-Factor Authentication (MFA) for a specific user.
   *
   * <p>This method generates a new TOTP (Time-based One-Time Password) secret for the user, which
   * can be used with apps like Google Authenticator or Microsoft Authenticator. If requested, it
   * also returns a QR code encoded in Base64 that the user can scan to configure their
   * authenticator app.
   *
   * <p>The user must be authenticated via a Bearer JWT token, and identified through the {@code
   * userID} field in the request body.
   *
   * <p>If {@code generateImage} is set to true in the request, the response will include the QR
   * code image.
   *
   * @param bearer Bearer JWT token used to authenticate the user. Must be passed in the
   *     Authorization header.
   * @param mfaSetupRequest The request payload containing the MFA mfaMethod, user identifier, and
   *     whether to generate a QR code.
   * @param exchange The {@link ServerWebExchange} used to access request details.
   * @return A {@link ResponseEntity} wrapping a {@link Response} object with the operation status,
   *     message, and optional data (e.g., MFA secret and Base64-encoded QR code).
   * @see MFASetupRequest
   * @see MFASetupResponse
   * @see Response
   */
  @PostMapping("/setup")
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
   * Endpoint to confirm the MFA code for a specific user.
   *
   * <p>This method verifies the code entered by the user (e.g., an OTP code from an authenticator
   * app or received via SMS). If the code is valid, the MFA method is confirmed and enabled for the
   * user.
   *
   * <p>The user must be authenticated via a Bearer JWT token, and identified through the {@code
   * userID} field in the request body.
   *
   * @param bearer Bearer JWT token used to authenticate the user. Must be passed in the
   *     Authorization header.
   * @param mfaConfirmationRequest The request payload containing the user identifier and OTP code.
   * @param exchange The {@link ServerWebExchange} used to access request details.
   * @return A {@link ResponseEntity} wrapping a {@link Response} object with the operation status
   *     and message.
   * @see MFAConfirmationRequest
   * @see Response
   */
  @PostMapping("/confirm")
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
   * Endpoint to verify the MFA code for a specific user during login.
   *
   * <p>This method checks the provided OTP code against all active MFA methods registered for the
   * user. The verification is considered successful if at least one method validates the code.
   * Otherwise, an error is returned indicating that the code is invalid.
   *
   * <p>The user is identified via the {@code identifier} field in the request body.
   *
   * @param mfaVerifyRequest The request payload containing the user identifier and OTP code.
   * @param exchange The {@link ServerWebExchange} used to access request details.
   * @return A {@link ResponseEntity} wrapping a {@link Response} object with the operation status
   *     and message.
   * @see MfaVerificationRequest
   * @see Response
   */
  @PostMapping("/verify")
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
   * Endpoint to manage MFA methods for a specific user.
   *
   * <p>This method allows enabling, disabling, or deleting one or more MFA methods for the user
   * identified by the {@code identifier} field in the request body.
   *
   * <p>The request must include a list of MFA methods, each identified by its {@code type} and
   * {@code label}, and an {@code action} field indicating the desired operation: ENABLE, DISABLE,
   * or DELETE.
   *
   * <p>The user must be authenticated via a Bearer JWT token.
   *
   * @param bearer Bearer JWT token used to authenticate the user. Must be passed in the
   *     Authorization header.
   * @param mfaManageRequest The request payload containing the user identifier, action, and methods
   *     to manage.
   * @param exchange The {@link ServerWebExchange} used to access request details.
   * @return A {@link ResponseEntity} wrapping a {@link Response} object with the operation status
   *     and message.
   * @see MFAManageRequest
   * @see Response
   */
  @PostMapping("/manage")
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
