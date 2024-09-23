package io.github.giovannilamarmora.accesssphere.grpc;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleGrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleTokenResponse;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
@Logged
public class GrpcService {

  private static final Logger LOG = LoggerFilter.getLogger(GrpcService.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static URI getGoogleOAuthLocation(
      String scope, String redirectUri, String accessType, ClientCredential clientCredential) {
    try {
      String redirect =
          GoogleGrpcService.startGoogleAuthorization(
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
  public static GoogleModel authenticateOAuth(
      String code, String scope, String redirectUri, ClientCredential clientCredential) {
    LOG.debug(
        "Starting google authentication with redirect_uri {} and scope {}", redirectUri, scope);
    TokenResponse googleTokenResponse;
    try {
      googleTokenResponse =
          GoogleGrpcService.getTokenResponse(
              code,
              clientCredential.getExternalClientId(),
              clientCredential.getClientSecret(),
              List.of(scope.split(" ")),
              clientCredential.getAccessType().value(),
              redirectUri);
      GoogleIdToken.Payload userInfo =
          GoogleGrpcService.getUserInfo(
              googleTokenResponse.get("id_token").toString(),
              clientCredential.getExternalClientId());
      LOG.debug("Obtained user is {}", Utils.mapper().writeValueAsString(userInfo));
      return new GoogleModel(
          GoogleTokenResponse.setTokenResponse(googleTokenResponse),
          userInfo,
          GrpcMapper.fromGoogleDataToJWTData(userInfo, clientCredential));
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("An error happen during oAuth Google Login, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static GoogleModel userInfo(String idToken, ClientCredential clientCredential) {
    LOG.debug("Starting google userInfo with client_id {}", clientCredential.getClientId());
    GoogleIdToken.Payload userInfo;
    try {
      userInfo = GoogleGrpcService.getUserInfo(idToken, clientCredential.getExternalClientId());
      LOG.debug("Obtained userInfo is {}", Utils.mapper().writeValueAsString(userInfo));
      return new GoogleModel(null, userInfo, null);
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("An error happen during oAuth Google UserInfo, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static void logout(String accessToken) {
    LOG.debug("Starting google revoke");
    GoogleGrpcService.logout(accessToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static GoogleModel refreshToken(String refreshToken, ClientCredential clientCredential) {
    LOG.debug("Starting google refresh token");
    TokenResponse googleTokenResponse;
    try {
      googleTokenResponse =
          GoogleGrpcService.refreshGoogleOAuthToken(
              refreshToken,
              clientCredential.getExternalClientId(),
              clientCredential.getClientSecret());
      GoogleIdToken.Payload userInfo =
          GoogleGrpcService.getUserInfo(
              googleTokenResponse.get("id_token").toString(),
              clientCredential.getExternalClientId());
      LOG.debug("Obtained user is {}", Utils.mapper().writeValueAsString(userInfo));
      return new GoogleModel(
          GoogleTokenResponse.setTokenResponse(googleTokenResponse),
          userInfo,
          GrpcMapper.fromGoogleDataToJWTData(userInfo, clientCredential));
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("An error happen during oAuth Google Refresh Token, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }
}
