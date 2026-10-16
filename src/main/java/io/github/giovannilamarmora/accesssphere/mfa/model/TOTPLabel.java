package io.github.giovannilamarmora.accesssphere.mfa.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Enumerazione per le etichette applicative associate a un metodo MFA configurato.
 */
@Getter
@Schema(description = "Label identifying the authenticator app provider or email channel")
public enum TOTPLabel {
  GOOGLE_AUTHENTICATOR("google-authenticator"),
  MICROSOFT_AUTHENTICATOR("microsoft-authenticator"),
  EMAIL("email");

  private final String label;

  TOTPLabel(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static TOTPLabel fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toLowerCase();
    for (TOTPLabel totpLabel : TOTPLabel.values()) {
      if (totpLabel.label.equalsIgnoreCase(normalized)
          || totpLabel.name().equalsIgnoreCase(normalized)
          || totpLabel.name().replace("_", "-").equalsIgnoreCase(normalized)) {
        return totpLabel;
      }
    }
    return null;
  }
}
