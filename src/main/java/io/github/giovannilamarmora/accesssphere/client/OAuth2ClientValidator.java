package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookValidator;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Validatore per le configurazioni del client OAuth2 (ruoli applicativi e webhooks).
 */
@Component
public class OAuth2ClientValidator {

  private static final Logger LOG = LoggerFilter.getLogger(OAuth2ClientValidator.class);

  public static void validateClientRoles(OAuth2Client oAuth2Client) {
    if (ObjectToolkit.isNullOrEmpty(oAuth2Client.getAppRoles())) {
      LOG.error("❌ The Client must have a valid role configuration to proceed");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_ROLES,
          "Invalid Roles Configuration! At least one role is required.");
    }
    oAuth2Client
        .getAppRoles()
        .forEach(
            role -> {
              if (ObjectToolkit.isNullOrEmpty(role.getRole())) {
                LOG.error("❌ The Client must have a valid role configuration to proceed");
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_400,
                    ExceptionType.INVALID_ROLES,
                    "Invalid Roles Configuration! At least one role is required.");
              }
              if (ObjectToolkit.isNullOrEmpty(role.getType())) {
                LOG.error("❌ The Client must have a valid role type configuration to proceed");
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_400,
                    ExceptionType.INVALID_ROLES,
                    "Invalid Roles Configuration! At least one role type is required.");
              }
            });
  }

  /**
   * Convalida le configurazioni Webhook eventualmente associate al client OAuth2.
   *
   * @param oAuth2Client Oggetto di configurazione client OAuth2.
   */
  public static void validateClientWebhooks(OAuth2Client oAuth2Client) {
    if (!ObjectToolkit.isNullOrEmpty(oAuth2Client.getWebhooks())) {
      WebhookValidator.validateWebhooks(oAuth2Client.getWebhooks());
    }
  }
}
