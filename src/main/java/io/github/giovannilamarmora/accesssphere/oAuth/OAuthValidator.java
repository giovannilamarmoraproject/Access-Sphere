package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.HashSet;
import java.util.List;

@Component
public class OAuthValidator {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClient(
      ClientCredential clientCredential, String accessType, String redirectUri, String scope) {

    if (!accessType.equalsIgnoreCase(clientCredential.getAccessType().value())) {
      LOG.error(
          "AccessType should be {} instead of {}",
          clientCredential.getAccessType().value(),
          accessType);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    if (!clientCredential.getRedirect_uri().contains(redirectUri)) {
      LOG.error(
          "Redirect Uri should be {} instead of {}",
          clientCredential.getRedirect_uri(),
          redirectUri);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    List<String> scopes = List.of(scope.split(" "));
    if (!new HashSet<>(clientCredential.getScopes()).containsAll(scopes)) {
      LOG.error("Scopes should be {} instead of {}", clientCredential.getScopes(), scope);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateBasic(String basic) {
    if (ObjectUtils.isEmpty(basic) || !basic.contains("Basic")) {
      LOG.error("No Basic Auth found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }

  public static void validateOAuthToken(String client_id, String grant_type) {
    if (ObjectUtils.isEmpty(client_id)) {
      LOG.error("No Client ID found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    if (ObjectUtils.isEmpty(grant_type)) {
      LOG.error("No Grant Type found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    if (!grant_type.equalsIgnoreCase("authorization_code")) {
      LOG.error("Grant Type must be authorization_code");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }

  public static void validateOAuthGoogle(
      ClientCredential clientCredential, String code, String scope, String redirect_uri) {
    if (ObjectUtils.isEmpty(code)) {
      LOG.error("No Code found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    if (ObjectUtils.isEmpty(redirect_uri)) {
      LOG.error("No Redirect URI found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    if (!clientCredential.getRedirect_uri().contains(redirect_uri)) {
      LOG.error(
          "Redirect Uri should be {} instead of {}",
          clientCredential.getRedirect_uri(),
          redirect_uri);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }

    List<String> scopes = List.of(scope.split(" "));
    if (!new HashSet<>(clientCredential.getScopes()).containsAll(scopes)) {
      LOG.error("Scopes should be {} instead of {}", clientCredential.getScopes(), scope);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }
}
