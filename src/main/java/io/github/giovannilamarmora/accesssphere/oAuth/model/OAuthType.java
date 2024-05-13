package io.github.giovannilamarmora.accesssphere.oAuth.model;

public enum OAuthType {
  BEARER("Bearer"),
  GOOGLE("google");

  private final String type;

  OAuthType(String type) {
    this.type = type;
  }

  public String type() {
    return type;
  }
}
