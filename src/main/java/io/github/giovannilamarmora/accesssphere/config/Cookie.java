package io.github.giovannilamarmora.accesssphere.config;

import lombok.Getter;

@Getter
public enum Cookie {
  COOKIE_TOKEN("REGISTRATION-TOKEN"),
  COOKIE_REDIRECT_URI("REDIRECT-URI"),
  COOKIE_ACCESS_TOKEN("access-token"),
  COOKIE_STRAPI_TOKEN("strapi-token");

  public final String cookie;

  Cookie(String cookie) {
    this.cookie = cookie;
  }
}
