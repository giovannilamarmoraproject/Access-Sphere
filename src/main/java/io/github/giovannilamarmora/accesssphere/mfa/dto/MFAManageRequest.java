package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload per la gestione (abilitazione, disabilitazione o cancellazione) dei metodi MFA associati
 * all'utente.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payload to manage user Multi-Factor Authentication methods")
public record MFAManageRequest(
    @NotBlank(message = "User identifier is required")
        @Schema(
            description = "Unique user identifier (UUID or username)",
            example = "usr-018e38d7-7000-7c2a-9694-811c79a941e1")
        String identifier,
    @Schema(
        description = "MFA method label (required only for DELETE action)",
        example = "google-authenticator")
        TOTPLabel label,
    @NotNull(message = "Action is required")
        @Schema(description = "Management action: ENABLE, DISABLE, or DELETE", example = "ENABLE")
        Action action) {

  public enum Action {
    ENABLE,
    DISABLE,
    DELETE
  }
}
