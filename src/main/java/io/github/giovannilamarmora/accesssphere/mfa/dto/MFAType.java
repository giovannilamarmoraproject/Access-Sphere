package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import lombok.Getter;

@Getter
public enum MFAType {
  TOTP("totp");

  private final String type;

  MFAType(String type) {
    this.type = type;
  }

  @JsonCreator
  public static MFAType fromString(String type) {
    for (MFAType mfaType : MFAType.values()) {
      if (mfaType.type.equalsIgnoreCase(type)) {
        return mfaType;
      }
    }
    throw new MFAException(
        ExceptionMap.ERR_MFA_400,
        String.format(
            "Invalid MFA setup: Unknown or unsupported mfaMethod '%s'. Please check the request parameters.",
            type));
  }

  @JsonValue
  public String getType() {
    return type;
  }
}
