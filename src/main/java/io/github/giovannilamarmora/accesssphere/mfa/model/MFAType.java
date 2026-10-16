package io.github.giovannilamarmora.accesssphere.mfa.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumerazione tipizzata dei canali e provider supportati per l'autenticazione a due fattori (MFA).
 */
@Schema(description = "Supported Multi-Factor Authentication types: TOTP (RFC 6238) or EMAIL OTP")
public enum MFAType {
  @Schema(description = "Time-Based One-Time Password compliant with RFC 6238")
  TOTP,

  @Schema(description = "One-Time Password delivered securely via Email")
  EMAIL
}
