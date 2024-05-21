package io.github.giovannilamarmora.accesssphere.oAuth.model;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GrantType {
  AUTHORIZATION_CODE("authorization_code"),
  PASSWORD("password"),
  REFRESH_TOKEN("refresh_token");

  private final String type;

  GrantType(String type) {
    this.type = type;
  }

  public String type() {
    return type;
  }

  private static final Logger LOG = LoggerFactory.getLogger(GrantType.class);

  public static GrantType fromType(String type) {
    for (GrantType grantType : values()) {
      if (grantType.type.equalsIgnoreCase(type)) {
        return grantType;
      }
    }
    LOG.error("Invalid grant_type for {}", type);
    throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid o not supported grant_type!");
  }
}
