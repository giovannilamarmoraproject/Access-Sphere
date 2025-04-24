package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import lombok.Getter;

@Getter
public enum TOTPLabel {
  GOOGLE_AUTHENTICATOR("google-authenticator"),
  MICROSOFT_AUTHENTICATOR("microsoft-authenticator"),
  AUTHY("authy"),
  LASTPASS_AUTHENTICATOR("lastpass-authenticator"),
  DUO_MOBILE("duo-mobile"),
  FREE_OTP("free-otp"),
  AEGIS("aegis"),
  AND_OTP("and-otp"),
  ONE_PASSWORD("1password"),
  BIT_WARDEN("bitwarden"),
  KEEPASS("keepass"),
  EN_PASS("enpass"),
  DASH_LANE("dashlane");

  private final String label;

  TOTPLabel(String label) {
    this.label = label;
  }

  @JsonCreator
  public static TOTPLabel fromString(String label) {
    for (TOTPLabel totpLabel : TOTPLabel.values()) {
      if (totpLabel.label.equalsIgnoreCase(label)) {
        return totpLabel;
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
