package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuthValidator {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClient(
      ClientCredential clientCredential,
      String responseType,
      String accessType,
      String redirectUri,
      String scope) {

    if (!responseType.equalsIgnoreCase("code")) {
      LOG.error("The Response Type should be code instead of {}", responseType);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid response_type provided!");
    }

    if (!accessType.equalsIgnoreCase(clientCredential.getAccessType().value())) {
      LOG.error(
          "The Access Type provided should be {} instead of {}",
          clientCredential.getAccessType().value(),
          accessType);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid access_type provided!");
    }

    if (!clientCredential.getRedirect_uri().contains(redirectUri)) {
      LOG.error(
          "The Redirect Uri provided should be {} instead of {}",
          clientCredential.getRedirect_uri(),
          redirectUri);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid redirect_uri provided!");
    }

    List<String> scopes = List.of(scope.split(" "));
    if (!new HashSet<>(clientCredential.getScopes()).containsAll(scopes)) {
      LOG.error(
          "The Scopes provided should be {} instead of {}", clientCredential.getScopes(), scope);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid scope provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateBasicAuth(String basic, String grantType) {
    if (ObjectUtils.isEmpty(basic) || !basic.contains("Basic")) {
      LOG.error("No Basic Auth found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid basic authorization!");
    }
    if (ObjectUtils.isEmpty(grantType) || !grantType.equalsIgnoreCase(GrantType.PASSWORD.type())) {
      LOG.error("The Grant Type provided should be password instead of {}", grantType);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid grant_type provided!");
    }
  }

  public static void validateOAuthToken(String client_id, String grant_type) {
    if (ObjectUtils.isEmpty(client_id)) {
      LOG.error("No Client ID found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "No client_id provided!");
    }

    if (ObjectUtils.isEmpty(grant_type)) {
      LOG.error("No Grant Type found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "No grant_type provided!");
    }
  }

  public static void validateOAuthGoogle(
      ClientCredential clientCredential,
      String code,
      String scope,
      String redirect_uri,
      String grantType) {

    if (ObjectUtils.isEmpty(grantType)
        || !grantType.equalsIgnoreCase(GrantType.AUTHORIZATION_CODE.type())) {
      LOG.error("The Grant Type provided should be authorization_code instead of {}", grantType);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid grant_type provided!");
    }

    if (ObjectUtils.isEmpty(code)) {
      LOG.error("No Code found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "The param code is a required field!");
    }

    if (ObjectUtils.isEmpty(redirect_uri)) {
      LOG.error("No Redirect URI found");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, "The param redirect_uri is a required field!");
    }

    if (!clientCredential.getRedirect_uri().contains(redirect_uri)) {
      LOG.error(
          "The Redirect URI provided should be {} instead of {}",
          clientCredential.getRedirect_uri(),
          redirect_uri);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid redirect_uri provided!");
    }

    List<String> scopes = List.of(scope.split(" "));
    if (!new HashSet<>(clientCredential.getScopes()).containsAll(scopes)) {
      LOG.error("Scopes provided should be {} instead of {}", clientCredential.getScopes(), scope);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid scope provided!");
    }
  }
}
