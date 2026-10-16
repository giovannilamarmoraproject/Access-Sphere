package io.github.giovannilamarmora.accesssphere.webhooks;

import static org.junit.jupiter.api.Assertions.*;

import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebhookValidatorTest {

  @Test
  @DisplayName("validateWebhooks should succeed on valid webhooks list")
  void testValidateWebhooksSuccess() {
    Webhook webhook = new Webhook("Delete Hook", "https://example.com/delete", WebhookAction.DELETE_USER);
    assertDoesNotThrow(() -> WebhookValidator.validateWebhooks(List.of(webhook)));
    assertDoesNotThrow(() -> WebhookValidator.validateWebhooks(null));
    assertDoesNotThrow(() -> WebhookValidator.validateWebhooks(List.of()));
  }

  @Test
  @DisplayName("validateWebhook should throw when webhook is null")
  void testValidateWebhookNull() {
    assertThrows(OAuthException.class, () -> WebhookValidator.validateWebhook(null));
  }

  @Test
  @DisplayName("validateWebhook should throw when URL is empty or null")
  void testValidateWebhookMissingUrl() {
    Webhook w1 = new Webhook("Test", null, WebhookAction.DELETE_USER);
    Webhook w2 = new Webhook("Test", "", WebhookAction.DELETE_USER);

    assertThrows(OAuthException.class, () -> WebhookValidator.validateWebhook(w1));
    assertThrows(OAuthException.class, () -> WebhookValidator.validateWebhook(w2));
  }

  @Test
  @DisplayName("validateWebhook should throw when URL is malformed")
  void testValidateWebhookInvalidUrl() {
    Webhook w = new Webhook("Test", "invalid-url", WebhookAction.DELETE_USER);
    assertThrows(OAuthException.class, () -> WebhookValidator.validateWebhook(w));
  }

  @Test
  @DisplayName("validateWebhook should throw when action is null")
  void testValidateWebhookMissingAction() {
    Webhook w = new Webhook("Test", "https://example.com/hook", null);
    assertThrows(OAuthException.class, () -> WebhookValidator.validateWebhook(w));
  }
}
