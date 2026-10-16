package io.github.giovannilamarmora.accesssphere.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.admin.UserAdminService;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

  @Mock private UserService userService;
  @Mock private PermissionRoleManager permissionRoleManager;
  @Mock private WebhookService webhookService;

  @InjectMocks private UserAdminService userAdminService;

  @Test
  @DisplayName("getUser() should succeed when operator has admin permission")
  void testGetUserAdminSuccess() {
    User user = new User();
    user.setUsername("alice");
    user.setIdentifier("uuid-alice");
    when(userService.getUserByIdentifier("uuid-alice")).thenReturn(Mono.just(user));
    when(permissionRoleManager.hasAdminPermission(SystemPermission.USERS_READ)).thenReturn(true);

    StepVerifier.create(userAdminService.getUser("uuid-alice", "Bearer token"))
        .assertNext(
            resp -> {
              assertEquals("alice", resp.getUsername());
              assertEquals("uuid-alice", resp.getIdentifier());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getUser() should succeed when user is accessing their own details")
  void testGetUserSelfSuccess() {
    User user = new User();
    user.setUsername("bob");
    user.setIdentifier("uuid-bob");
    when(userService.getUserByIdentifier("uuid-bob")).thenReturn(Mono.just(user));
    when(permissionRoleManager.hasAdminPermission(SystemPermission.USERS_READ)).thenReturn(false);
    when(permissionRoleManager.isCurrentUser("uuid-bob")).thenReturn(true);

    StepVerifier.create(userAdminService.getUser("uuid-bob", "Bearer token"))
        .assertNext(
            resp -> {
              assertEquals("bob", resp.getUsername());
              assertEquals("uuid-bob", resp.getIdentifier());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getUser() should succeed when operator and target user share client")
  void testGetUserSameClientSuccess() {
    User user = new User();
    user.setUsername("charlie");
    user.setIdentifier("uuid-charlie");

    UserClientRole ucr = new UserClientRole();
    ucr.setClientId("CLIENT-A");
    ucr.setRole("USER");
    user.setRoles(List.of(ucr));

    when(userService.getUserByIdentifier("uuid-charlie")).thenReturn(Mono.just(user));
    when(permissionRoleManager.hasAdminPermission(SystemPermission.USERS_READ)).thenReturn(false);
    when(permissionRoleManager.isCurrentUser("uuid-charlie")).thenReturn(false);

    RoleDefinition rd = new RoleDefinition();
    rd.setClientId("CLIENT-A");
    rd.setRole("MANAGER");
    when(permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.USERS_READ))
        .thenReturn(List.of(rd));

    StepVerifier.create(userAdminService.getUser("uuid-charlie", "Bearer token"))
        .assertNext(
            resp -> {
              assertEquals("charlie", resp.getUsername());
              assertEquals("uuid-charlie", resp.getIdentifier());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getUser() should fail with 403 when operator and target user belong to different clients")
  void testGetUserClientMismatchForbidden() {
    User user = new User();
    user.setUsername("dave");
    user.setIdentifier("uuid-dave");

    UserClientRole ucr = new UserClientRole();
    ucr.setClientId("CLIENT-B");
    ucr.setRole("USER");
    user.setRoles(List.of(ucr));

    when(userService.getUserByIdentifier("uuid-dave")).thenReturn(Mono.just(user));
    when(permissionRoleManager.hasAdminPermission(SystemPermission.USERS_READ)).thenReturn(false);
    when(permissionRoleManager.isCurrentUser("uuid-dave")).thenReturn(false);

    RoleDefinition rd = new RoleDefinition();
    rd.setClientId("CLIENT-A");
    rd.setRole("MANAGER");
    when(permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.USERS_READ))
        .thenReturn(List.of(rd));

    StepVerifier.create(userAdminService.getUser("uuid-dave", "Bearer token"))
        .expectError(OAuthException.class)
        .verify();
  }

  @Test
  @DisplayName("getUsers() should filter users by authorized clients for non-admin operator")
  void testGetUsersClientFiltered() {
    User user1 = new User();
    user1.setUsername("u1");
    UserClientRole r1 = new UserClientRole();
    r1.setClientId("CLIENT-A");
    user1.setRoles(List.of(r1));

    User user2 = new User();
    user2.setUsername("u2");
    UserClientRole r2 = new UserClientRole();
    r2.setClientId("CLIENT-B");
    user2.setRoles(List.of(r2));

    RoleDefinition opRole = new RoleDefinition();
    opRole.setClientId("CLIENT-A");
    when(permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.USERS_READ))
        .thenReturn(List.of(opRole));
    when(permissionRoleManager.hasAdminPermission(SystemPermission.USERS_READ)).thenReturn(false);
    when(userService.getUsers()).thenReturn(Mono.just(List.of(user1, user2)));

    StepVerifier.create(userAdminService.getUsers("Bearer token"))
        .assertNext(
            list -> {
              assertEquals(1, list.size());
              assertEquals("u1", list.get(0).getUsername());
            })
        .verifyComplete();
  }
}
