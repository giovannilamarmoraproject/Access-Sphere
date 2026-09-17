package io.github.giovannilamarmora.accesssphere.oAuth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenResponse extends GenericDTO {
  private AuthToken token;
  private JsonNode payload;
  private JsonNode googleToken;
  private JWTData userInfo;
  private User user;

  public OAuthTokenResponse(AuthToken token) {
    this.token = token;
  }

  public OAuthTokenResponse(JWTData userInfo) {
    this.userInfo = userInfo;
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

  public OAuthTokenResponse(AuthToken token, JsonNode payload, JWTData userInfo) {
    this.token = token;
    this.payload = payload;
    this.userInfo = userInfo;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode payload, JsonNode googleToken) {
    this.token = token;
    this.payload = payload;
    this.googleToken = googleToken;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode payload) {
    this.token = token;
    this.payload = payload;
  }

  public OAuthTokenResponse(AuthToken token, JsonNode payload, JWTData userInfo, User user) {
    this.token = token;
    this.payload = payload;
    this.userInfo = userInfo;
    this.user = user;
  }
}
