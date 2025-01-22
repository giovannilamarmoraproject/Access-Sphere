package io.github.giovannilamarmora.accesssphere.token.dto;

public enum TokenClaims {
  IDENTIFIER("identifier"),
  AUD("aud"),
  EXP("exp"),
  AZP("azp"),
  IAT("iat"),
  ISS("iss"),
  NAME("name"),
  GIVEN_NAME("given_name"),
  FAMILY_NAME("family_name"),
  PICTURE("picture"),
  AT_HASH("at_hash"),
  EMAIL("email"),
  ROLE("roles"),
  AUTH_TYPE("type"),
  ATTRIBUTES("attributes"),
  GOOGLE_TOKEN("google_token"),
  STRAPI_TOKEN("strapi_token"),
  CLIENT_ID("client_id");

  private final String claim;

  TokenClaims(String claim) {
    this.claim = claim;
  }

  public String claim() {
    return claim;
  }
}
