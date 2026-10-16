package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload per la conferma e l'attivazione definitiva del metodo MFA impostato.
 *
 * <p>Verifica che il codice OTP a 6 cifre inserito corrisponda alla chiave segreta temporanea
 * memorizzata durante la fase di setup, abilitando l'MFA per l'account.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payload to confirm and activate an MFA method for a user")
public record MFAConfirmationRequest(
    @NotBlank(message = "The user ID must be provided to identify the account.")
        @Schema(
            description = "Unique user identifier (UUID or username)",
            example = "usr-018e38d7-7000-7c2a-9694-811c79a941e1")
        String identifier,
    @Schema(
        description = "Optional label/provider of the method being confirmed",
        example = "google-authenticator")
        TOTPLabel label,
    @NotNull(message = "The MFA type (e.g., TOTP, EMAIL) must be specified.")
        @Schema(description = "MFA type: TOTP or EMAIL", example = "TOTP")
        MFAType type,
    @NotBlank(message = "The OTP code is required to complete the verification.")
        @Schema(description = "6-digit OTP code", example = "123456")
        String otp) {

  /**
   * Risolve la label effettiva garantendo che il tipo EMAIL utilizzi di default la label {@link
   * TOTPLabel#EMAIL} qualora omessa nel payload.
   *
   * @return {@link TOTPLabel} risolta o null.
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
