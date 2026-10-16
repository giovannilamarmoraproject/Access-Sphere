package io.github.giovannilamarmora.accesssphere.webhooks;

import static org.junit.jupiter.api.Assertions.*;

import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebhookUtilsTest {

  @Test
  @DisplayName("cleanBearerToken should strip Bearer prefix case-insensitively")
  void testCleanBearerToken() {
    assertEquals("token123", WebhookUtils.cleanBearerToken("Bearer token123"));
    assertEquals("token123", WebhookUtils.cleanBearerToken("bearer token123"));
    assertEquals("token123", WebhookUtils.cleanBearerToken("BEARER token123"));
    assertEquals("token123", WebhookUtils.cleanBearerToken("token123"));
    assertNull(WebhookUtils.cleanBearerToken(null));
    assertNull(WebhookUtils.cleanBearerToken(""));
  }

  @Test
  @DisplayName("filterMatchingWebhooks should return only webhooks with matching action")
  void testFilterMatchingWebhooks() {
    Webhook w1 = new Webhook("DeleteHook", "https://api.example.com/delete", WebhookAction.DELETE_USER);
    Webhook w2 = new Webhook("RegisterHook", "https://api.example.com/register", WebhookAction.REGISTER_USER);

    List<Webhook> webhooks = List.of(w1, w2);

    List<Webhook> deleteHooks = WebhookUtils.filterMatchingWebhooks(webhooks, WebhookAction.DELETE_USER);
    assertEquals(1, deleteHooks.size());
    assertEquals("DeleteHook", deleteHooks.getFirst().getName());

    List<Webhook> regHooks = WebhookUtils.filterMatchingWebhooks(webhooks, WebhookAction.REGISTER_USER);
    assertEquals(1, regHooks.size());
    assertEquals("RegisterHook", regHooks.getFirst().getName());

    assertTrue(WebhookUtils.filterMatchingWebhooks(null, WebhookAction.DELETE_USER).isEmpty());
    assertTrue(WebhookUtils.filterMatchingWebhooks(List.of(), WebhookAction.DELETE_USER).isEmpty());
  }

  @Test
  @DisplayName("isValidWebhookUrl should validate HTTP/HTTPS URLs properly")
  void testIsValidWebhookUrl() {
    assertTrue(WebhookUtils.isValidWebhookUrl("https://example.com/webhook"));
    assertTrue(WebhookUtils.isValidWebhookUrl("http://localhost:8080/hook"));
    assertFalse(WebhookUtils.isValidWebhookUrl("ftp://example.com"));
    assertFalse(WebhookUtils.isValidWebhookUrl("not-a-url"));
    assertFalse(WebhookUtils.isValidWebhookUrl(""));
    assertFalse(WebhookUtils.isValidWebhookUrl(null));
  }
}
