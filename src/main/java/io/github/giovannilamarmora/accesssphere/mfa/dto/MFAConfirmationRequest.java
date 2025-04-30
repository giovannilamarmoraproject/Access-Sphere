package io.github.giovannilamarmora.accesssphere.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MFAConfirmationRequest(
    @NotBlank(message = "The user ID must be provided to identify the account.") String identifier,
    @NotNull(message = "The MFA label indicates which method is being used and is required.")
        TOTPLabel label,
    @NotNull(message = "The MFA type (e.g., TOTP, SMS) must be specified.") MFAType type,
    @NotBlank(message = "The OTP code is required to complete the verification.") String otp) {}
