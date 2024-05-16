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
    if (ObjectUtils.isEmpty(basic)) {
      LOG.error("No Basic Auth found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateCode(String code) {
    if (ObjectUtils.isEmpty(code)) {
      LOG.error("No Code found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }
}
