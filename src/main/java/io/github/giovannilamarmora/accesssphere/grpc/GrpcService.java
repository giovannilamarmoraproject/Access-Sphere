package io.github.giovannilamarmora.accesssphere.grpc;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleModel;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleOAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Logged
public class GrpcService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  @Autowired private GoogleOAuthService googleOAuthService;

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public URI getGoogleOAuthLocation(
      String scope, String redirectUri, String accessType, ClientCredential clientCredential) {
    try {
      String redirect =
          googleOAuthService.startGoogleAuthorization(
              List.of(scope.split(" ")),
              redirectUri,
              accessType,
              clientCredential.getExternalClientId(),
              clientCredential.getClientSecret());
      return new URI(redirect);
    } catch (IOException | GeneralSecurityException | URISyntaxException e) {
      LOG.error("An error happen during oAuth Google Authorization, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public GoogleModel authenticateOAuth(
      String code, String scope, String redirectUri, ClientCredential clientCredential) {
    LOG.debug(
        "Starting google authentication with redirect_uri {} and scope {}", redirectUri, scope);
    TokenResponse googleTokenResponse;
    try {
      googleTokenResponse =
          googleOAuthService.getTokenResponse(
              code,
              clientCredential.getExternalClientId(),
              clientCredential.getClientSecret(),
              List.of(scope.split(" ")),
              clientCredential.getAccessType().value(),
              redirectUri);
      GoogleIdToken.Payload userInfo =
          googleOAuthService.getUserInfo(
              googleTokenResponse.get("id_token").toString(),
              clientCredential.getExternalClientId());
      LOG.info("Obtained user is {}", Utils.mapper().writeValueAsString(userInfo));
      return new GoogleModel(
          googleTokenResponse,
          userInfo,
          GrpcMapper.fromGoogleDataToJWTData(userInfo, clientCredential));
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("An error happen during oAuth Google Login, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public GoogleModel userInfo(String idToken, ClientCredential clientCredential) {
    LOG.debug("Starting google userInfo with client_id {}", clientCredential.getClientId());
    GoogleIdToken.Payload userInfo;
    try {
      userInfo = googleOAuthService.getUserInfo(idToken, clientCredential.getExternalClientId());
      LOG.info("Obtained userInfo is {}", Utils.mapper().writeValueAsString(userInfo));
      return new GoogleModel(null, userInfo, null);
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("An error happen during oAuth Google UserInfo, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }
}
