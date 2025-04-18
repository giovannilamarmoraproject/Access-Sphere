package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MFASetupRequest(
    @NotBlank(message = "The user ID must be provided to identify the account.") String userID,
    @NotNull(message = "The MFA label indicates which method is being used and is required.")
        MFALabel label,
    @NotNull(message = "The MFA method (e.g., TOTP, SMS) must be specified.") MFAType mfaMethod,
    boolean generateImage) {}
