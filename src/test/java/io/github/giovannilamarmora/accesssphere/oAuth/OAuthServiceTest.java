package io.github.giovannilamarmora.accesssphere.oAuth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

  @Mock private ClientService clientService;
  @Mock private AccessTokenService accessTokenService;
  @Mock private UserService userService;

  @InjectMocks private OAuthService oAuthService;

  private ClientCredential clientCredential;
  private AccessTokenData accessTokenData;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(oAuthService, "clientService", clientService);
    ReflectionTestUtils.setField(oAuthService, "accessTokenService", accessTokenService);
    ReflectionTestUtils.setField(oAuthService, "userService", userService);

    clientCredential = new ClientCredential();
    clientCredential.setClientId("TEST-CLIENT");
    clientCredential.setAuthType(OAuthType.BEARER);
    clientCredential.setAppRoles(
        List.of(new AppRole("ROLE_USER", AppRoleType.DEFAULT.getValue(), "Default user role")));

    accessTokenData = new AccessTokenData();
    accessTokenData.setClientId("TEST-CLIENT");
    accessTokenData.setIdentifier("user-uuid-123");
    accessTokenData.setRoles(List.of("ROLE_USER"));
    accessTokenData.setAccessExpireDate(System.currentTimeMillis() + 3600000);
  }

  @Test
  @DisplayName("authorize should not include user data when include_user_data is absent or false")
  void authorize_withoutUserData() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/v1/oauth2/authorize")
            .header("Authorization", "Bearer valid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(clientService.getClientCredentialByClientID("TEST-CLIENT"))
        .thenReturn(Mono.just(clientCredential));
    when(accessTokenService.getByAccessTokenOrIdToken("Bearer valid-token"))
        .thenReturn(accessTokenData);

    StepVerifier.create(
            oAuthService.authorize(
                "token",
                "online",
                "TEST-CLIENT",
                "http://localhost/callback",
                "openid",
                null,
                "Bearer valid-token",
                "state-123",
                exchange))
        .assertNext(
            (ResponseEntity<?> res) -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertInstanceOf(Response.class, res.getBody());
              Response response = (Response) res.getBody();
              assertInstanceOf(OAuthTokenResponse.class, response.getData());
              OAuthTokenResponse tokenData = (OAuthTokenResponse) response.getData();
              assertNotNull(tokenData.getToken());
              assertNull(tokenData.getUser(), "User should be null when include_user_data=false");
            })
        .verifyComplete();

    verify(userService, never()).getUserByIdentifier(any());
  }

  @Test
  @DisplayName("authorize should include user data when include_user_data=true is requested")
  void authorize_withUserData() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/v1/oauth2/authorize?include_user_data=true")
            .header("Authorization", "Bearer valid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    User mockUser = new User();
    mockUser.setIdentifier("user-uuid-123");
    mockUser.setUsername("testuser");
    mockUser.setEmail("test@example.com");
    mockUser.setAttributes(Map.of("strapi-token", "strapi-jwt-secret-xyz"));

    when(clientService.getClientCredentialByClientID("TEST-CLIENT"))
        .thenReturn(Mono.just(clientCredential));
    when(accessTokenService.getByAccessTokenOrIdToken("Bearer valid-token"))
        .thenReturn(accessTokenData);
    when(userService.getUserByIdentifier("user-uuid-123")).thenReturn(Mono.just(mockUser));

    StepVerifier.create(
            oAuthService.authorize(
                "token",
                "online",
                "TEST-CLIENT",
                "http://localhost/callback",
                "openid",
                null,
                "Bearer valid-token",
                "state-123",
                exchange))
        .assertNext(
            (ResponseEntity<?> res) -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertInstanceOf(Response.class, res.getBody());
              Response response = (Response) res.getBody();
              assertInstanceOf(OAuthTokenResponse.class, response.getData());
              OAuthTokenResponse tokenData = (OAuthTokenResponse) response.getData();
              assertNotNull(tokenData.getToken());
              assertNotNull(
                  tokenData.getUser(), "User must be present when include_user_data=true");
              assertEquals("testuser", tokenData.getUser().getUsername());
              assertEquals(
                  "strapi-jwt-secret-xyz", tokenData.getUser().getAttributes().get("strapi-token"));
            })
        .verifyComplete();

    verify(userService, times(1)).getUserByIdentifier("user-uuid-123");
  }
}
