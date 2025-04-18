package io.github.giovannilamarmora.accesssphere.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MFAConfirmationRequest(
    @NotBlank(message = "The user ID must be provided to identify the account.") String userID,
    @NotNull(message = "The MFA label indicates which method is being used and is required.")
        MFALabel label,
    @NotNull(message = "The MFA mfaMethod (e.g., TOTP, SMS) must be specified.") MFAType mfaMethod,
    @NotBlank(message = "The OTP code is required to complete the verification.") String otp) {}
