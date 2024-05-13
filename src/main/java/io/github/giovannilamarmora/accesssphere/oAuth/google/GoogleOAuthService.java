package io.github.giovannilamarmora.accesssphere.oAuth.google;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Logged
@RequiredArgsConstructor
public class GoogleOAuthService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private final HttpTransport transport = new NetHttpTransport();

  public String startGoogleAuthorization(
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

  public TokenResponse getTokenResponse(
      String code,
      String clientId,
      String clientSecret,
      List<String> scopes,
      String access_type,
      String redirect_uri)
      throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    AuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, getClientSecrets(clientId, clientSecret), scopes)
            .setAccessType(access_type)
            .build();
    AuthorizationCodeTokenRequest tokenRequest = flow.newTokenRequest(code);
    tokenRequest.setRedirectUri(redirect_uri);
    return tokenRequest.execute();
  }

  public GoogleIdToken.Payload getUserInfo(String accessToken, String clientId)
      throws IOException, GeneralSecurityException {
    GoogleIdTokenVerifier verifier =
        new GoogleIdTokenVerifier.Builder(transport, JSON_FACTORY)
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

  private GoogleClientSecrets getClientSecrets(String clientId, String clientSecret) {
    GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
    GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
    details.setClientId(clientId);
    details.setClientSecret(clientSecret);
    clientSecrets.setInstalled(details);
    return clientSecrets;
  }
}
