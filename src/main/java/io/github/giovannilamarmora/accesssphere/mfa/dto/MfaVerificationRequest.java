package io.github.giovannilamarmora.accesssphere.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MfaVerificationRequest(
    @NotNull(message = "The MFA mfaMethod (e.g., TOTP, SMS) must be specified.") MFAType mfaMethod,
    @NotBlank(message = "The OTP code is required to complete the verification.") String otp,
    String redirectUri,
    Boolean rememberDevice) {}
