package io.github.giovannilamarmora.accesssphere.oAuth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenResponse {
  private AuthToken token;
  private JsonNode strapiToken;
  private JsonNode googleToken;
  private JWTData userInfo;
  private User user;

  public OAuthTokenResponse(AuthToken token) {
    this.token = token;
  }

  public OAuthTokenResponse(AuthToken token, JWTData userInfo) {
    this.token = token;
    this.userInfo = userInfo;
  }

  public OAuthTokenResponse(AuthToken token, JWTData userInfo, User user) {
    this.token = token;
    this.userInfo = userInfo;
    this.user = user;
  }

  public OAuthTokenResponse(User user, JWTData userInfo, AuthToken token) {
    this.user = user;
    this.userInfo = userInfo;
    this.token = token;
  }

  public OAuthTokenResponse(AuthToken token, User user) {
    this.token = token;
    this.user = user;
  }

  public OAuthTokenResponse(JWTData userInfo, User user) {
    this.userInfo = userInfo;
    this.user = user;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode strapiToken, JWTData userInfo) {
    this.token = token;
    this.strapiToken = strapiToken;
    this.userInfo = userInfo;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode strapiToken, JsonNode googleToken) {
    this.token = token;
    this.strapiToken = strapiToken;
    this.googleToken = googleToken;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode strapiToken) {
    this.token = token;
    this.strapiToken = strapiToken;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode strapiToken, JWTData userInfo, User user) {
    this.token = token;
    this.strapiToken = strapiToken;
    this.userInfo = userInfo;
    this.user = user;
  }
}
