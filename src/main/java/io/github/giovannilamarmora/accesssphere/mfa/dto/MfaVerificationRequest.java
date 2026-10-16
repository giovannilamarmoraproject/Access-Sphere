package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload per la verifica del codice OTP durante il flusso di login OAuth 2.1 o step-up authentication.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payload to verify MFA code during authentication or step-up challenge")
public record MfaVerificationRequest(
    @NotNull(message = "The MFA type (e.g., TOTP, EMAIL) must be specified.")
        @Schema(description = "MFA method used for verification (TOTP or EMAIL)", example = "TOTP")
        MFAType mfaMethod,
    @NotBlank(message = "The OTP code is required to complete the verification.")
        @Schema(description = "6-digit OTP code", example = "123456")
        String otp,
    @Schema(
        description = "Optional redirect URI to redirect the user after successful verification",
        example = "https://app.example.com/callback")
        String redirectUri,
    @Schema(
        description = "Flag to remember this device and skip MFA on future logins for 30 days",
        example = "true")
        Boolean rememberDevice) {}
