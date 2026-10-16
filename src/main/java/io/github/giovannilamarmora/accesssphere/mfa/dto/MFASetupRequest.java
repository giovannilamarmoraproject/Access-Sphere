package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAType;
import io.github.giovannilamarmora.accesssphere.mfa.model.TOTPLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload per l'inizializzazione del setup MFA (TOTP o EMAIL).
 *
 * <p>Consente all'utente autenticato di configurare una nuova modalità di sicurezza a due fattori,
 * sia mediante Authenticator App RFC 6238 (TOTP) con eventuale generazione del QR code Base64, sia
 * mediante verifica email con invio automatico del codice OTP.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payload to initialize Multi-Factor Authentication setup for a user")
public record MFASetupRequest(
    @NotBlank(message = "The user ID must be provided to identify the account.")
        @Schema(
            description = "Unique user identifier (UUID or username)",
            example = "usr-018e38d7-7000-7c2a-9694-811c79a941e1")
        String identifier,
    @Schema(
        description = "Optional label/provider for the authenticator app or email",
        example = "google-authenticator")
        TOTPLabel label,
    @NotNull(message = "The MFA type (e.g., TOTP, EMAIL) must be specified.")
        @Schema(description = "MFA type: TOTP or EMAIL", example = "TOTP")
        MFAType type,
    @Schema(
        description = "Whether to generate a Base64 QR code image (applicable to TOTP only)",
        example = "true")
        boolean generateImage) {

  /**
   * Risolve la label effettiva garantendo che il tipo EMAIL utilizzi di default la label {@link
   * TOTPLabel#EMAIL} qualora omessa nel payload.
   *
   * @return {@link TOTPLabel} risolta o null se non determinabile.
   */
  public TOTPLabel resolvedLabel() {
    if (label != null) {
      return label;
    }
    if (type == MFAType.EMAIL) {
      return TOTPLabel.EMAIL;
    }
    return null;
  }
}
