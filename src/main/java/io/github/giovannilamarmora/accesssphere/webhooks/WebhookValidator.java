package io.github.giovannilamarmora.accesssphere.webhooks;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Validatore per le configurazioni dei Webhook associati ai Client OAuth2.
 */
@Component
public class WebhookValidator {

  private static final Logger LOG = LoggerFilter.getLogger(WebhookValidator.class);

  /**
   * Valida un insieme di configurazioni Webhook.
   *
   * @param webhooks Lista di webhook da validare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateWebhooks(List<Webhook> webhooks) {
    if (ObjectToolkit.isNullOrEmpty(webhooks)) {
      return;
    }
    webhooks.forEach(WebhookValidator::validateWebhook);
  }

  /**
   * Valida un singolo Webhook verificando la presenza di URL valido e azione associata.
   *
   * @param webhook Oggetto webhook da validare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateWebhook(Webhook webhook) {
    if (ObjectToolkit.isNullOrEmpty(webhook)) {
      LOG.error("❌ Webhook configuration cannot be null");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Webhook configuration cannot be null");
    }
    if (ObjectToolkit.isNullOrEmpty(webhook.getUrl())) {
      LOG.error("❌ Webhook URL is required");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Webhook URL is required");
    }
    if (!WebhookUtils.isValidWebhookUrl(webhook.getUrl())) {
      LOG.error("❌ Invalid Webhook URL: {}", webhook.getUrl());
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, "Invalid Webhook URL: " + webhook.getUrl());
    }
    if (ObjectToolkit.isNullOrEmpty(webhook.getAction())) {
      LOG.error("❌ Webhook action is required for URL: {}", webhook.getUrl());
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, "Webhook action is required for URL: " + webhook.getUrl());
    }
  }
}
