package io.github.giovannilamarmora.accesssphere.oAuth.model;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.LoggerFilter;
import org.slf4j.Logger;

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

  private static final Logger LOG = LoggerFilter.getLogger(GrantType.class);

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
