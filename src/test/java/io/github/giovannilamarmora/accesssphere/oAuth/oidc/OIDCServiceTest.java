package io.github.giovannilamarmora.accesssphere.oAuth.oidc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OIDCServiceTest {

  @Mock private OAuth2ClientService oAuth2ClientService;
  @Mock private TokenService tokenService;
  @Mock private OAuth2Authorization oAuth2Authorization;
  @Mock private UserService userService;

  @InjectMocks private OIDCService oidcService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(oidcService, "clientService", oAuth2ClientService);
    ReflectionTestUtils.setField(oidcService, "tokenService", tokenService);
    ReflectionTestUtils.setField(oidcService, "accessTokenData", oAuth2Authorization);
    ReflectionTestUtils.setField(oidcService, "userService", userService);
  }

  @Test
  @DisplayName("userInfo should resolve bearer token and return OAuthTokenResponse")
  void userInfo_bearerSuccess() {
    when(oAuth2Authorization.getClientId()).thenReturn("CLIENT-01");
    when(oAuth2Authorization.getType()).thenReturn(OAuthType.BEARER);

    OAuth2Client credential = new OAuth2Client();
    credential.setClientId("CLIENT-01");
    credential.setAuthType(OAuthType.BEARER);
    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-01"))
        .thenReturn(Mono.just(credential));

    JWTData jwtData = new JWTData();
    jwtData.setType(OAuthType.BEARER);
    jwtData.setEmail("user@example.com");
    jwtData.setSub("user_sub");
    when(tokenService.parseToken("Bearer valid-token", credential)).thenReturn(jwtData);

    User user = new User();
    user.setUsername("user_sub");
    user.setEmail("user@example.com");
    when(userService.getUserByEmail("user@example.com")).thenReturn(Mono.just(user));

    MockServerHttpRequest request =
        MockServerHttpRequest.get("/v1/userInfo?include_user_data=true").build();

    StepVerifier.create(oidcService.userInfo("Bearer valid-token", request))
        .assertNext(
            response -> {
              assertNotNull(response.getUserInfo());
              assertEquals("user_sub", response.getUserInfo().getSub());
              assertNotNull(response.getUser());
              assertEquals("user_sub", response.getUser().getUsername());
            })
        .verifyComplete();
  }
}
