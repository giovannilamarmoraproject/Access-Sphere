package io.github.giovannilamarmora.accesssphere.data.user.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

  @Mock private TechUserService techUserService;
  @Mock private ClientService clientService;
  @Mock private UserService userService;
  @Mock private AccessTokenData accessTokenData;
  @Mock private WebhookService webhookService;

  @InjectMocks private UserAdminService userAdminService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userAdminService, "techUserService", techUserService);
    ReflectionTestUtils.setField(userAdminService, "clientService", clientService);
    ReflectionTestUtils.setField(userAdminService, "userService", userService);
    ReflectionTestUtils.setField(userAdminService, "accessTokenData", accessTokenData);
    ReflectionTestUtils.setField(userAdminService, "webhookService", webhookService);
  }

  @Test
  @DisplayName("getUsers should throw forbidden when not a tech user")
  void getUsers_notTechUser_throws() {
    when(techUserService.isTechUser()).thenReturn(false);

    assertThrows(OAuthException.class, () -> userAdminService.getUsers());
  }

  @Test
  @DisplayName("getUsers should succeed when tech user with valid roles")
  void getUsers_techUser_success() {
    when(techUserService.isTechUser()).thenReturn(true);
    when(techUserService.getTechClientId()).thenReturn("CLIENT-TECH");

    ClientCredential credential = new ClientCredential();
    credential.setClientId("CLIENT-TECH");
    io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole appRole =
        new io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole();
    appRole.setRole("ROLE_ADMIN");
    credential.setAppRoles(List.of(appRole));
    when(clientService.getClientCredentialByClientID("CLIENT-TECH"))
        .thenReturn(Mono.just(credential));

    when(accessTokenData.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

    User user = new User();
    user.setUsername("user1");
    when(userService.getUsersFromDatabase()).thenReturn(Mono.just(List.of(user)));

    StepVerifier.create(userAdminService.getUsers())
        .assertNext(
            users -> {
              assertEquals(1, users.size());
              assertEquals("user1", users.get(0).getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("unlockUser should throw forbidden when caller is not tech user")
  void unlockUser_notTechUser_throws() {
    when(techUserService.isTechUser()).thenReturn(false);

    assertThrows(OAuthException.class, () -> userAdminService.unlockUser("uuid-1", true));
  }

  @Test
  @DisplayName("unlockUser should update user blocked state when caller is tech user")
  void unlockUser_techUser_success() {
    when(techUserService.isTechUser()).thenReturn(true);
    when(techUserService.getTechClientId()).thenReturn("CLIENT-TECH");

    ClientCredential credential = new ClientCredential();
    credential.setClientId("CLIENT-TECH");
    io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole appRole =
        new io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole();
    appRole.setRole("ROLE_ADMIN");
    credential.setAppRoles(List.of(appRole));
    when(clientService.getClientCredentialByClientID("CLIENT-TECH"))
        .thenReturn(Mono.just(credential));

    when(accessTokenData.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

    User user = new User();
    user.setIdentifier("uuid-1");
    user.setBlocked(false);

    when(userService.getUserByIdentifier("uuid-1")).thenReturn(Mono.just(user));
    when(userService.updateUser(any(User.class), eq(false)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(userAdminService.unlockUser("uuid-1", true))
        .assertNext(
            updated -> {
              assertTrue(updated.getBlocked());
            })
        .verifyComplete();
  }
}
