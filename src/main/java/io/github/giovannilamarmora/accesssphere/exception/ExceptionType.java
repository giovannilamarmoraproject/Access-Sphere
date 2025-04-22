package io.github.giovannilamarmora.accesssphere.exception;

import lombok.Getter;

@Getter
public enum ExceptionType implements io.github.giovannilamarmora.utils.exception.ExceptionType {
  /**
   * @ERR_OAUTH_400 ExceptionType
   */
  USERNAME_EMAIL_TAKEN("username"),
  BAD_INPUT_ERROR("generic"),
  INVALID_ROLES("roles"),
  INVALID_AUTHORIZATION("bearer"),
  INVALID_CLIENT_ID("client_id"),
  INVALID_GRANT_TYPE("grant_type"),
  INVALID_REDIRECT_URI("redirect_uri"),

  /**
   * @ERR_MFA_400 ExceptionType
   */
  INVALID_MFA_METHOD("mfa_method"),
  INVALID_OTP_CODE("otp-code");

  private final String param;

  ExceptionType(String param) {
    this.param = param;
  }

  public static ExceptionType fromParam(String value, boolean throwException) {
    for (ExceptionType type : ExceptionType.values()) {
      if (type.param.equals(value)) {
        return type;
      }
    }
    if (throwException) throw new IllegalArgumentException("No enum constant with value: " + value);
    return null;
  }
}
