package io.github.giovannilamarmora.accesssphere.oAuth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.api.client.auth.oauth2.TokenResponse;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenResponse {
  private AuthToken token;
  private String strapiJwt;
  private TokenResponse googleToken;
  private User user;

  public OAuthTokenResponse(AuthToken token, TokenResponse googleToken) {
    this.token = token;
    this.googleToken = googleToken;
  }

  public OAuthTokenResponse(AuthToken token, User user) {
    this.token = token;
    this.user = user;
  }

  public OAuthTokenResponse(AuthToken token, String strapiJwt, User user) {
    this.token = token;
    this.strapiJwt = strapiJwt;
    this.user = user;
  }

  public OAuthTokenResponse(AuthToken token, TokenResponse googleToken, User user) {
    this.token = token;
    this.googleToken = googleToken;
    this.user = user;
  }

  public OAuthTokenResponse(AuthToken token, String strapiJwt) {
    this.token = token;
    this.strapiJwt = strapiJwt;
  }
}
