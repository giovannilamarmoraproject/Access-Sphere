package io.github.giovannilamarmora.accesssphere.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.user.account.UserAccountService;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserUpdateRequest;
import io.github.giovannilamarmora.accesssphere.user.model.ChangePassword;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock private UserService userService;
  @Mock private OAuth2Authorization oAuth2Authorization;
  @Mock private PermissionRoleManager permissionRoleManager;
  @Mock private WebhookService webhookService;

  @InjectMocks private UserAccountService userAccountService;

  @Test
  @DisplayName("profile() should retrieve user profile by authenticated email")
  void testProfileSuccess() {
    when(oAuth2Authorization.getEmail()).thenReturn("john@example.com");

    User user = new User();
    user.setUsername("john");
    user.setEmail("john@example.com");
    user.setIdentifier("uuid-123");
    when(userService.getUserByEmail("john@example.com")).thenReturn(Mono.just(user));

    MockServerHttpRequest request = MockServerHttpRequest.get("/v1/users/profile").build();

    StepVerifier.create(userAccountService.profile("Bearer token", request))
        .assertNext(
            resp -> {
              assertEquals("john", resp.getUsername());
              assertEquals("john@example.com", resp.getEmail());
              assertEquals("uuid-123", resp.getIdentifier());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateProfile() should update profile of currently authenticated user")
  void testUpdateProfileSuccess() {
    when(oAuth2Authorization.getEmail()).thenReturn("john@example.com");

    User existingUser = new User();
    existingUser.setUsername("john");
    existingUser.setEmail("john@example.com");
    existingUser.setIdentifier("uuid-123");

    when(userService.getUserByEmail("john@example.com")).thenReturn(Mono.just(existingUser));
    when(userService.getUserByIdentifier("uuid-123")).thenReturn(Mono.just(existingUser));
    when(permissionRoleManager.hasAdminPermission(any())).thenReturn(true);

    User updatedUser = new User();
    updatedUser.setUsername("john");
    updatedUser.setEmail("john_updated@example.com");
    updatedUser.setIdentifier("uuid-123");
    when(userService.updateUser(any(User.class), any(User.class))).thenReturn(Mono.just(updatedUser));
    when(webhookService.checkAndExecuteWebhook(eq(WebhookAction.UPDATE_USER), eq("uuid-123")))
        .thenReturn(Mono.empty());

    UserUpdateRequest updateRequest = new UserUpdateRequest();
    updateRequest.setEmail("john_updated@example.com");
    MockServerHttpRequest request = MockServerHttpRequest.put("/v1/users/profile").build();

    StepVerifier.create(userAccountService.updateProfile(updateRequest, "Bearer token", request))
        .assertNext(
            resp -> {
              assertEquals("john", resp.getUsername());
              assertEquals("john_updated@example.com", resp.getEmail());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteProfile() should allow user to delete own account")
  void testDeleteProfileSuccess() {
    when(oAuth2Authorization.getEmail()).thenReturn("john@example.com");

    User user = new User();
    user.setUsername("john");
    user.setIdentifier("uuid-123");
    when(userService.getUserByEmail("john@example.com")).thenReturn(Mono.just(user));
    when(permissionRoleManager.isAdminUser("john")).thenReturn(false);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.delete("/v1/users/profile").build());
    when(userService.deleteUserByIdentifier(eq("uuid-123"), any(), eq(exchange)))
        .thenReturn(Mono.empty());

    StepVerifier.create(userAccountService.deleteProfile("Bearer token", exchange))
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteProfile() should reject deletion of admin user")
  void testDeleteProfileAdminForbidden() {
    when(oAuth2Authorization.getEmail()).thenReturn("admin@example.com");

    User user = new User();
    user.setUsername("admin");
    user.setIdentifier("uuid-admin");
    when(userService.getUserByEmail("admin@example.com")).thenReturn(Mono.just(user));
    when(permissionRoleManager.isAdminUser("admin")).thenReturn(true);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.delete("/v1/users/profile").build());

    StepVerifier.create(userAccountService.deleteProfile("Bearer token", exchange))
        .expectError(OAuthException.class)
        .verify();
  }

  @Test
  @DisplayName("changeAuthenticatedPassword() should successfully update password when old matches")
  void testChangeAuthenticatedPasswordSuccess() {
    when(oAuth2Authorization.getEmail()).thenReturn("john@example.com");

    User user = new User();
    user.setUsername("john");
    user.setIdentifier("uuid-123");
    user.setPassword("$2a$10$hashedoldpassword");
    when(userService.getUserByEmail("john@example.com")).thenReturn(Mono.just(user));

    when(userService.matchesPassword("OldPassword123!", "$2a$10$hashedoldpassword"))
        .thenReturn(true);

    User updatedUser = new User();
    updatedUser.setIdentifier("uuid-123");
    when(userService.updateUser(eq("uuid-123"), any(User.class))).thenReturn(Mono.just(updatedUser));
    when(webhookService.checkAndExecuteWebhook(eq(WebhookAction.RESET_PASSWORD), eq("uuid-123")))
        .thenReturn(Mono.empty());

    ChangePassword cp = new ChangePassword();
    cp.setOldPassword(Base64.getEncoder().encodeToString("OldPassword123!".getBytes()));
    cp.setPassword(Base64.getEncoder().encodeToString("NewPassword123!".getBytes()));

    StepVerifier.create(userAccountService.changeAuthenticatedPassword(cp, "Bearer token"))
        .verifyComplete();
  }

  @Test
  @DisplayName("changeAuthenticatedPassword() should fail when old password does not match")
  void testChangeAuthenticatedPasswordMismatch() {
    when(oAuth2Authorization.getEmail()).thenReturn("john@example.com");

    User user = new User();
    user.setUsername("john");
    user.setIdentifier("uuid-123");
    user.setPassword("$2a$10$hashedoldpassword");
    when(userService.getUserByEmail("john@example.com")).thenReturn(Mono.just(user));

    when(userService.matchesPassword("WrongPassword123!", "$2a$10$hashedoldpassword"))
        .thenReturn(false);

    ChangePassword cp = new ChangePassword();
    cp.setOldPassword(Base64.getEncoder().encodeToString("WrongPassword123!".getBytes()));
    cp.setPassword(Base64.getEncoder().encodeToString("NewPassword123!".getBytes()));

    StepVerifier.create(userAccountService.changeAuthenticatedPassword(cp, "Bearer token"))
        .expectError(OAuthException.class)
        .verify();
  }

  @Test
  @DisplayName("changeAuthenticatedPassword() should fail when missing old or new password")
  void testChangeAuthenticatedPasswordMissing() {
    ChangePassword cp = new ChangePassword();
    cp.setPassword(Base64.getEncoder().encodeToString("NewPassword123!".getBytes()));

    assertThrows(
        OAuthException.class,
        () -> userAccountService.changeAuthenticatedPassword(cp, "Bearer token"));
  }
}
