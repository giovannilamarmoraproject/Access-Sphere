package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO contenente i dettagli di inizializzazione MFA per l'utente (secret TOTP, URI otpauth o QR code).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details generated during MFA initialization")
public record MFASetupResponse(
    @Schema(
        description = "Plaintext base32 secret key for manual TOTP configuration (null for EMAIL)",
        example = "JBSWY3DPEHPK3PXP")
        String secret,
    @Schema(
        description = "Standard RFC 6238 otpauth URI (null for EMAIL)",
        example = "otpauth://totp/Access-Sphere:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Access-Sphere")
        String otpAuth,
    @Schema(
        description = "Base64 data URL encoded PNG QR code image for authenticator apps",
        example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
        String base64QrCodeImage) {

  /**
   * Crea una nuova istanza di {@link MFASetupResponse} aggiungendo l'immagine QR code generata.
   *
   * @param qrCode Stringa Base64 del QR code.
   * @return Nuova istanza arricchita.
   */
  public MFASetupResponse addQRCode(String qrCode) {
    return new MFASetupResponse(secret, otpAuth, qrCode);
  }
}
