package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;

public class TechUserValidator extends TechUserConfig {

  private static final Logger LOG = LoggerFilter.getLogger(TechUserValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateTechRoles(ClientCredential clientCredential, List<String> userRoles) {
    if (ObjectToolkit.isNullOrEmpty(userRoles)) {
      LOG.error("The User must have a role to proceed {}", userRoles);
      throw new TechnicalException(
          ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    OAuthValidator.validateClientRoles(clientCredential);

    if (clientCredential.getAppRoles().stream()
        .noneMatch(appRole -> userRoles.contains(appRole.getRole()))) {
      LOG.error(
          "The User must have a valid role as {} to proceed {}",
          clientCredential.getAppRoles(),
          userRoles);
      throw new TechnicalException(
          ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public static void validateTechClient(
      List<ClientCredential> clients, AccessTokenData accessTokenData, String techClientID) {
    ClientCredential clientCredentialToCheck =
        clients.stream()
            .filter(c -> c.getClientId().equalsIgnoreCase(techClientID))
            .toList()
            .getFirst();
    validateTechRoles(clientCredentialToCheck, accessTokenData.getRoles());
  }
}
