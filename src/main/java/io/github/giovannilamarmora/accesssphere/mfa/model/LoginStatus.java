package io.github.giovannilamarmora.accesssphere.mfa.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Stato della procedura di login MFA associata alla sessione temporanea.
 */
@Schema(description = "MFA Login Session lifecycle status: PENDING or COMPLETED")
public enum LoginStatus {
  COMPLETED,
  PENDING
}
