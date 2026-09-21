package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;

public class RoleManager {

  private static final Logger LOG = LoggerFilter.getLogger(RoleManager.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public static Boolean hasPermissionRoles(
      AccessTokenData accessTokenData, ClientCredential clientCredential) {
    if (accessTokenData.getSubjectType().equals(SubjectType.TECHNICAL)) {
      LOG.info("The subject {} has Technical Roles", accessTokenData.getSubjectType());
      return true;
    }
    boolean hasTechRole =
        clientCredential.getAppRoles().stream()
            .map(AppRole::getRole)
            .anyMatch(accessTokenData.getRoles()::contains);
    if (hasTechRole)
      LOG.info("The subject {} has the correct Roles", accessTokenData.getSubjectType());
    return hasTechRole;
  }
}
