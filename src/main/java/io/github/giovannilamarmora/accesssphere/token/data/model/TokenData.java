package io.github.giovannilamarmora.accesssphere.token.data.model;

import lombok.Getter;

@Getter
public enum TokenData {
  ACCESS_TOKEN("access_token");

  private final String token;

  TokenData(String token) {
    this.token = token;
  }
}
