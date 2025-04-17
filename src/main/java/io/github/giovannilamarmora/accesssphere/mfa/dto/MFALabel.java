package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import lombok.Getter;

@Getter
public enum MFALabel {
  GOOGLE_AUTHENTICATOR("google-authenticator"),
  MICROSOFT_AUTHENTICATOR("microsoft-authenticator");

  private final String label;

  MFALabel(String label) {
    this.label = label;
  }

  @JsonCreator
  public static MFALabel fromString(String label) {
    for (MFALabel mfaLabel : MFALabel.values()) {
      if (mfaLabel.label.equalsIgnoreCase(label)) {
        return mfaLabel;
      }
    }
    throw new MFAException(
        ExceptionMap.ERR_MFA_400,
        String.format(
            "Invalid MFA setup: Unknown or unsupported label '%s'. Please check the request parameters.",
            label));
  }

  @JsonValue
  public String getLabel() {
    return label;
  }
}
