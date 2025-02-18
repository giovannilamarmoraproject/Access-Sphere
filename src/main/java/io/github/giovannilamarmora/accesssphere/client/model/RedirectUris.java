package io.github.giovannilamarmora.accesssphere.client.model;

public enum RedirectUris {
  REDIRECT_URI("redirect_uri"),
  FALLBACK_URI("fallback_uri"),
  POST_LOGIN_URL("post_login_redirect_uri");

  private final String url;

  RedirectUris(String url) {
    this.url = url;
  }

  public String url() {
    return url;
  }
}
