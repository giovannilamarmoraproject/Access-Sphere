package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;

public class PermissionRoleManager {

  private static final Logger LOG = LoggerFilter.getLogger(RoleManager.class);

  public static void checkAndValidatePermissionRoles(
      AccessTokenData accessTokenData, ClientCredential clientCredential) {}
}
