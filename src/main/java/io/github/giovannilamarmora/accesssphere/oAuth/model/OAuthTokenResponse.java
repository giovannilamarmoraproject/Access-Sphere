package io.github.giovannilamarmora.accesssphere.oAuth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
  private Object strapiToken;
  private JWTData userInfo;
  private User user;

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
}
