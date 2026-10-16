package io.github.giovannilamarmora.accesssphere.webhooks;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientDomainService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.webhooks.client.WebhookClient;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookBody;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

  @Mock private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Mock private WebhookClient webhookClient;
  @Mock private OAuthService oAuthService;

  @InjectMocks private WebhookService webhookService;

  @Test
  @DisplayName("checkAndExecuteWebhook should complete immediately if bearer or exchange is null")
  void testNullBearerOrExchange() {
    ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

    StepVerifier.create(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, null, "user-1", exchange))
        .verifyComplete();

    StepVerifier.create(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, "Bearer token", "user-1", null))
        .verifyComplete();

    verifyNoInteractions(oAuth2ClientDomainService);
  }

  @Test
  @DisplayName("checkAndExecuteWebhook should complete when no matching webhooks exist")
  void testNoMatchingWebhooks() {
    ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT_A");
    client.setWebhooks(
        List.of(new Webhook("RegHook", "https://example.com/reg", WebhookAction.REGISTER_USER)));

    when(oAuth2ClientDomainService.getOAuth2Clients()).thenReturn(Mono.just(List.of(client)));

    StepVerifier.create(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, "Bearer token", "user-1", exchange))
        .verifyComplete();

    verifyNoInteractions(oAuthService);
    verifyNoInteractions(webhookClient);
  }

  @Test
  @DisplayName("checkAndExecuteWebhook should execute token exchange and call WebhookClient on matching action")
  void testSuccessfulWebhookExecution() {
    ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT_A");
    client.setWebhooks(
        List.of(new Webhook("DelHook", "https://example.com/del", WebhookAction.DELETE_USER)));

    when(oAuth2ClientDomainService.getOAuth2Clients()).thenReturn(Mono.just(List.of(client)));

    OAuthTokenResponse tokenResponse = new OAuthTokenResponse();
    AuthToken token = new AuthToken();
    token.setAccess_token("exchanged-token-123");
    tokenResponse.setToken(token);

    Response resp = new Response(HttpStatus.OK.value(), "OK", "span-1", tokenResponse);
    ResponseEntity<Response> responseEntity = ResponseEntity.ok(resp);

    when(oAuthService.tokenExchange(anyString(), any(), any())).thenReturn(Mono.just(responseEntity));
    when(webhookClient.execute(
            eq("https://example.com/del"), eq("exchanged-token-123"), any(WebhookBody.class)))
        .thenReturn(Mono.just(ResponseEntity.ok().build()));

    StepVerifier.create(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, "Bearer token", "user-1", exchange))
        .verifyComplete();

    verify(webhookClient, times(1))
        .execute(
            eq("https://example.com/del"), eq("exchanged-token-123"), any(WebhookBody.class));
  }

  @Test
  @DisplayName("checkAndExecuteWebhook should handle token exchange error gracefully without terminating stream")
  void testTokenExchangeErrorResilience() {
    ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT_A");
    client.setWebhooks(
        List.of(new Webhook("DelHook", "https://example.com/del", WebhookAction.DELETE_USER)));

    when(oAuth2ClientDomainService.getOAuth2Clients()).thenReturn(Mono.just(List.of(client)));
    when(oAuthService.tokenExchange(anyString(), any(), any()))
        .thenReturn(Mono.error(new RuntimeException("OAuth Error")));

    StepVerifier.create(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, "Bearer token", "user-1", exchange))
        .verifyComplete();

    verifyNoInteractions(webhookClient);
  }
}
