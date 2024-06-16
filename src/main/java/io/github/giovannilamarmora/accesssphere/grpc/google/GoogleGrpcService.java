package io.github.giovannilamarmora.accesssphere.grpc.google;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Logged
@RequiredArgsConstructor
public class GoogleGrpcService {

  private static final Logger LOG = LoggerFilter.getLogger(GoogleGrpcService.class);
  private static final JsonFactory JSON_FACTORY = null;

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static String startGoogleAuthorization(
      List<String> scopes,
      String redirect_uri,
      String access_type,
      String clientId,
      String clientSecret)
      throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    AuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, getClientSecrets(clientId, clientSecret), scopes)
            .setAccessType(access_type)
            .build();
    AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
    authorizationUrl.setRedirectUri(redirect_uri);
    return authorizationUrl.build();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static TokenResponse getTokenResponse(
      String code,
      String clientId,
      String clientSecret,
      List<String> scopes,
      String access_type,
      String redirect_uri)
      throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    try {
      AuthorizationCodeFlow flow =
          new GoogleAuthorizationCodeFlow.Builder(
                  httpTransport, JSON_FACTORY, getClientSecrets(clientId, clientSecret), scopes)
              .setAccessType(access_type)
              .build();
      AuthorizationCodeTokenRequest tokenRequest = flow.newTokenRequest(code);
      tokenRequest.setRedirectUri(redirect_uri);
      return tokenRequest.execute();
    } catch (TokenResponseException e) {
      LOG.error(
          "An error happen during get access token with google, message is {}",
          !ObjectUtils.isEmpty(e.getDetails())
                  && !ObjectUtils.isEmpty(e.getDetails().getErrorDescription())
              ? e.getDetails().getErrorDescription()
              : e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static GoogleIdToken.Payload getUserInfo(String accessToken, String clientId)
      throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    GoogleIdTokenVerifier verifier =
        new GoogleIdTokenVerifier.Builder(httpTransport, JSON_FACTORY)
            .setAudience(Collections.singletonList(clientId))
            .build();
    GoogleIdToken idToken = verifier.verify(accessToken);
    if (idToken != null) {
      return idToken.getPayload();
    } else {
      LOG.error("The google access token is invalid");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static TokenResponse refreshGoogleOAuthToken(
      String refreshToken, String clientId, String clientSecret)
      throws GeneralSecurityException, IOException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    try {
      return new GoogleRefreshTokenRequest(
              httpTransport, JSON_FACTORY, refreshToken, clientId, clientSecret)
          .execute();
    } catch (TokenResponseException e) {
      LOG.error(
          "An error happen during refreshing token with google, message is {}",
          !ObjectUtils.isEmpty(e.getDetails())
                  && !ObjectUtils.isEmpty(e.getDetails().getErrorDescription())
              ? e.getDetails().getErrorDescription()
              : e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.GRPC)
  public static void logout(String accessToken) {
    // Crea una nuova istanza di GoogleCredential senza token di accesso
    Credential credential = new GoogleCredential.Builder().build();

    // Crea una nuova istanza di HttpRequestFactory
    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(credential);

    HttpRequest request = null;
    HttpResponse response = null;
    try {
      request =
          requestFactory.buildGetRequest(
              new GenericUrl("https://oauth2.googleapis.com/revoke?token=" + accessToken));
      response = request.execute();
    } catch (IOException e) {
      LOG.error(
          "An error happen during get revoke token with google, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    // Leggi la risposta
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      LOG.error(
          "An error happen during get revoke token with google, status code is {}", statusCode);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  private static GoogleClientSecrets getClientSecrets(String clientId, String clientSecret) {
    GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
    GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
    details.setClientId(clientId);
    details.setClientSecret(clientSecret);
    clientSecrets.setInstalled(details);
    return clientSecrets;
  }
}
