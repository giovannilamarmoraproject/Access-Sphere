package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;

public class TechUserValidator {

  private static final Logger LOG = LoggerFilter.getLogger(TechUserValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateTechRoles(OAuth2Client oAuth2Client, List<String> userRoles) {
    if (ObjectToolkit.isNullOrEmpty(userRoles)) {
      LOG.error("The User must have a role to proceed {}", userRoles);
      throw new TechnicalException(
          ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    OAuthValidator.validateClientRoles(oAuth2Client);

    if (oAuth2Client.getAppRoles().stream()
        .filter(java.util.Objects::nonNull)
        .noneMatch(appRole -> userRoles.stream().anyMatch(appRole::matches))) {
      LOG.error(
          "The User must have a valid role as {} to proceed {}",
          oAuth2Client.getAppRoles(),
          userRoles);
      throw new TechnicalException(
          ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }
  }
}
