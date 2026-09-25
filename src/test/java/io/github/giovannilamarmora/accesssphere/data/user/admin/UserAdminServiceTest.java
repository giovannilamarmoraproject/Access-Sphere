package io.github.giovannilamarmora.accesssphere.data.user.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

  @Mock private AdminUserService techUserService;
  @Mock private OAuth2ClientService oAuth2ClientService;
  @Mock private UserService userService;
  @Mock private OAuth2Authorization oAuth2Authorization;
  @Mock private WebhookService webhookService;

  @InjectMocks private UserAdminService userAdminService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userAdminService, "techUserService", techUserService);
    ReflectionTestUtils.setField(userAdminService, "oAuth2ClientService", oAuth2ClientService);
    ReflectionTestUtils.setField(userAdminService, "userService", userService);
    ReflectionTestUtils.setField(userAdminService, "oAuth2Authorization", oAuth2Authorization);
    ReflectionTestUtils.setField(userAdminService, "webhookService", webhookService);
  }

  @Test
  @DisplayName(
      "getUsers should return filtered list excluding tech user when caller is normal user")
  void getUsers_normalUser_filtersOutTechUser() {
    when(techUserService.isTechUser()).thenReturn(false);

    User techUser = new User();
    techUser.setUsername("tech_admin");
    techUser.setEmail("admin@accesssphere.local");

    User normalUser = new User();
    normalUser.setUsername("john_doe");
    normalUser.setEmail("john@example.com");

    when(techUserService.isTechnicalUser(techUser)).thenReturn(true);
    when(techUserService.isTechnicalUser(normalUser)).thenReturn(false);
    when(userService.getUsers()).thenReturn(Mono.just(List.of(techUser, normalUser)));

    StepVerifier.create(userAdminService.getUsers())
        .assertNext(
            users -> {
              assertEquals(1, users.size());
              assertEquals("john_doe", users.getFirst().getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "getUsers should succeed and return all users including tech user when caller is tech user")
  void getUsers_techUser_returnsAllUsers() {
    when(techUserService.isTechUser()).thenReturn(true);
    when(techUserService.getAdminClientId()).thenReturn("CLIENT-TECH");

    OAuth2Client credential = new OAuth2Client();
    credential.setClientId("CLIENT-TECH");
    AppRole appRole = new AppRole();
    appRole.setRole("ROLE_ADMIN");
    credential.setAppRoles(List.of(appRole));
    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-TECH"))
        .thenReturn(Mono.just(credential));

    when(oAuth2Authorization.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

    User techUser = new User();
    techUser.setUsername("tech_admin");
    User normalUser = new User();
    normalUser.setUsername("user1");
    when(userService.getUsers()).thenReturn(Mono.just(List.of(techUser, normalUser)));

    StepVerifier.create(userAdminService.getUsers())
        .assertNext(
            users -> {
              assertEquals(2, users.size());
              assertEquals("tech_admin", users.get(0).getUsername());
              assertEquals("user1", users.get(1).getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("unlockUser should throw forbidden when normal user targets technical user")
  void unlockUser_normalUser_targetingTechUser_throwsForbidden() {
    when(techUserService.isTechUser()).thenReturn(false);

    User techUser = new User();
    techUser.setIdentifier("uuid-tech");
    techUser.setUsername("tech_admin");

    when(userService.getUserByIdentifier("uuid-tech")).thenReturn(Mono.just(techUser));
    when(techUserService.isTechnicalUser(techUser)).thenReturn(true);

    StepVerifier.create(userAdminService.unlockUser("uuid-tech", true))
        .expectErrorMatches(
            throwable ->
                throwable instanceof OAuthException
                    && ((OAuthException) throwable)
                        .getExceptionCode()
                        .equals(ExceptionMap.ERR_TECH_403))
        .verify();
  }

  @Test
  @DisplayName("unlockUser should update user blocked state when caller is tech user")
  void unlockUser_techUser_success() {
    when(techUserService.isTechUser()).thenReturn(true);
    when(techUserService.getAdminClientId()).thenReturn("CLIENT-TECH");

    OAuth2Client credential = new OAuth2Client();
    credential.setClientId("CLIENT-TECH");
    AppRole appRole = new AppRole();
    appRole.setRole("ROLE_ADMIN");
    credential.setAppRoles(List.of(appRole));
    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-TECH"))
        .thenReturn(Mono.just(credential));

    when(oAuth2Authorization.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

    User user = new User();
    user.setIdentifier("uuid-1");
    user.setBlocked(false);

    when(userService.getUserByIdentifier("uuid-1")).thenReturn(Mono.just(user));
    when(techUserService.isTechnicalUser(user)).thenReturn(false);
    when(userService.updateUser(any(), any(User.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    StepVerifier.create(userAdminService.unlockUser("uuid-1", true))
        .assertNext(
            updated -> {
              assertTrue(updated.getBlocked());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("unlockUser should update normal user blocked state when caller is normal user")
  void unlockUser_normalUser_targetingNormalUser_success() {
    when(techUserService.isTechUser()).thenReturn(false);

    User user = new User();
    user.setIdentifier("uuid-2");
    user.setUsername("normal_user");
    user.setBlocked(false);

    when(userService.getUserByIdentifier("uuid-2")).thenReturn(Mono.just(user));
    when(techUserService.isTechnicalUser(user)).thenReturn(false);
    when(userService.updateUser(any(), any(User.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    StepVerifier.create(userAdminService.unlockUser("uuid-2", true))
        .assertNext(
            updated -> {
              assertTrue(updated.getBlocked());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteUser should throw forbidden when target is technical user")
  void deleteUser_targetingTechUser_throwsForbidden() {
    User techUser = new User();
    techUser.setIdentifier("uuid-tech");
    techUser.setUsername("tech_admin");

    when(userService.getUserByIdentifier("uuid-tech")).thenReturn(Mono.just(techUser));
    when(techUserService.isTechnicalUser(techUser)).thenReturn(true);

    StepVerifier.create(userAdminService.deleteUser("uuid-tech", null, null))
        .expectErrorMatches(
            throwable ->
                throwable instanceof OAuthException
                    && ((OAuthException) throwable)
                        .getExceptionCode()
                        .equals(ExceptionMap.ERR_TECH_403))
        .verify();
  }

  @Test
  @DisplayName("deleteUser should succeed for normal user deletion by tech user")
  void deleteUser_techUser_deletingNormalUser_success() {
    when(techUserService.isTechUser()).thenReturn(true);
    when(techUserService.getAdminClientId()).thenReturn("CLIENT-TECH");

    OAuth2Client credential = new OAuth2Client();
    credential.setClientId("CLIENT-TECH");
    AppRole appRole = new AppRole();
    appRole.setRole("ROLE_ADMIN");
    credential.setAppRoles(List.of(appRole));
    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-TECH"))
        .thenReturn(Mono.just(credential));

    when(oAuth2Authorization.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

    User normalUser = new User();
    normalUser.setIdentifier("uuid-normal");
    normalUser.setUsername("user_to_delete");

    when(userService.getUserByIdentifier("uuid-normal")).thenReturn(Mono.just(normalUser));
    when(techUserService.isTechnicalUser(normalUser)).thenReturn(false);
    doNothing().when(userService).deleteUserEntityByIdentifier("uuid-normal");
    when(webhookService.checkAndExecuteWebhook(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(userAdminService.deleteUser("uuid-normal", "bearer", null))
        .verifyComplete();

    verify(userService, times(1)).deleteUserEntityByIdentifier("uuid-normal");
  }

  @Test
  @DisplayName("deleteUser should succeed for normal user deletion by normal user")
  void deleteUser_normalUser_deletingNormalUser_success() {
    when(techUserService.isTechUser()).thenReturn(false);

    User normalUser = new User();
    normalUser.setIdentifier("uuid-normal");
    normalUser.setUsername("user_to_delete");

    when(userService.getUserByIdentifier("uuid-normal")).thenReturn(Mono.just(normalUser));
    when(techUserService.isTechnicalUser(normalUser)).thenReturn(false);
    doNothing().when(userService).deleteUserEntityByIdentifier("uuid-normal");
    when(webhookService.checkAndExecuteWebhook(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(userAdminService.deleteUser("uuid-normal", "bearer", null))
        .verifyComplete();

    verify(userService, times(1)).deleteUserEntityByIdentifier("uuid-normal");
  }
}
