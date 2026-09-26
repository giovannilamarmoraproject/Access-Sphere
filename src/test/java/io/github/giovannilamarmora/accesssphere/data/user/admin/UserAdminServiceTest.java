package io.github.giovannilamarmora.accesssphere.data.user.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.admin.dto.AddUserRequest;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.token.model.SubjectType;
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

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO
      userClientRoleDAO;

  @Mock private OAuth2Authorization oAuth2Authorization;

  @Mock
  private io.github.giovannilamarmora.accesssphere.oAuth.authorization.OAuth2AuthorizationService
      oAuth2AuthorizationService;

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager
      permissionRoleManager;

  @Mock private WebhookService webhookService;

  @InjectMocks private UserAdminService userAdminService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userAdminService, "techUserService", techUserService);
    ReflectionTestUtils.setField(userAdminService, "oAuth2ClientService", oAuth2ClientService);
    ReflectionTestUtils.setField(userAdminService, "userService", userService);
    ReflectionTestUtils.setField(userAdminService, "userClientRoleDAO", userClientRoleDAO);
    ReflectionTestUtils.setField(userAdminService, "oAuth2Authorization", oAuth2Authorization);
    ReflectionTestUtils.setField(
        userAdminService, "oAuth2AuthorizationService", oAuth2AuthorizationService);
    ReflectionTestUtils.setField(userAdminService, "permissionRoleManager", permissionRoleManager);
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
    RoleDefinition appRole = new RoleDefinition();
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
    RoleDefinition appRole = new RoleDefinition();
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
    RoleDefinition appRole = new RoleDefinition();
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

  @Test
  @DisplayName("register should succeed when user is valid and role exists for client")
  void register_success() {
    AddUserRequest user = new AddUserRequest();
    user.setUsername("john_doe");
    user.setEmail("john.doe@example.com");
    user.setPassword("Strong1Password!");
    user.setRoles(List.of(new UserClientRoleRequest("CLIENT-APP", "ACCESS-SPHERE_USER")));

    when(userService.existsByUsername("john_doe")).thenReturn(false);
    when(userService.existsByEmail("john.doe@example.com")).thenReturn(false);

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT-APP");
    client.setEnableRegistration(true);
    RoleDefinition role = new RoleDefinition("ACCESS-SPHERE_USER", "USER", "User role", List.of("users:read"));
    client.setAppRoles(List.of(role));

    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-APP"))
        .thenReturn(Mono.just(client));
    when(userService.saveUserEntityIntoDatabase(any()))
        .thenAnswer(
            inv -> {
              io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity entity =
                  inv.getArgument(0);
              User dto = new User();
              dto.setIdentifier(entity.getIdentifier());
              dto.setUsername(entity.getUsername());
              dto.setEmail(entity.getEmail());
              return dto;
            });
    when(userClientRoleDAO.saveAll(any())).thenReturn(List.of());

    StepVerifier.create(userAdminService.register(null, user))
        .assertNext(
            registered -> {
              assertNotNull(registered.getIdentifier());
              assertEquals("john_doe", registered.getUsername());
              assertEquals(1, registered.getRoles().size());
              assertEquals("ACCESS-SPHERE_USER", registered.getRoles().getFirst().getRole());
              assertEquals("CLIENT-APP", registered.getRoles().getFirst().getClientId());
            })
        .verifyComplete();

    verify(userClientRoleDAO, times(1)).saveAll(any());
    verify(userService, times(1)).saveUserEntityIntoDatabase(any());
  }

  @Test
  @DisplayName("register should throw BAD_REQUEST when username already exists")
  void register_usernameExists_throwsBadRequest() {
    AddUserRequest user = new AddUserRequest();
    user.setUsername("existing_user");
    user.setEmail("existing@example.com");
    user.setPassword("Strong1Password!");
    user.setRoles(List.of(new UserClientRoleRequest("CLIENT-APP", "ACCESS-SPHERE_USER")));

    when(userService.existsByUsername("existing_user")).thenReturn(true);

    StepVerifier.create(userAdminService.register(null, user))
        .expectErrorMatches(
            t ->
                t instanceof io.github.giovannilamarmora.accesssphere.data.user.UserException
                    && ((io.github.giovannilamarmora.accesssphere.data.user.UserException) t)
                        .getExceptionCode()
                        .equals(ExceptionMap.ERR_USER_400))
        .verify();
  }

  @Test
  @DisplayName("register should throw BAD_REQUEST when roles are null or empty")
  void register_emptyRoles_throwsBadRequest() {
    AddUserRequest user = new AddUserRequest();
    user.setUsername("john_doe");
    user.setEmail("john.doe@example.com");
    user.setPassword("Strong1Password!");
    user.setRoles(List.of());

    StepVerifier.create(userAdminService.register(null, user))
        .expectErrorMatches(
            t ->
                t instanceof io.github.giovannilamarmora.accesssphere.data.user.UserException
                    && ((io.github.giovannilamarmora.accesssphere.data.user.UserException) t)
                        .getExceptionCode()
                        .equals(ExceptionMap.ERR_USER_400))
        .verify();
  }

  @Test
  @DisplayName("register should throw FORBIDDEN when client has registration disabled")
  void register_clientRegistrationDisabled_throwsForbidden() {
    AddUserRequest user = new AddUserRequest();
    user.setUsername("john_doe");
    user.setEmail("john.doe@example.com");
    user.setPassword("Strong1Password!");
    user.setRoles(List.of(new UserClientRoleRequest("CLIENT-BLOCKED", "ACCESS-SPHERE_USER")));

    when(userService.existsByUsername("john_doe")).thenReturn(false);
    when(userService.existsByEmail("john.doe@example.com")).thenReturn(false);

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT-BLOCKED");
    client.setEnableRegistration(false);

    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-BLOCKED"))
        .thenReturn(Mono.just(client));

    StepVerifier.create(userAdminService.register(null, user))
        .expectErrorMatches(
            t ->
                t instanceof OAuthException
                    && ((OAuthException) t).getExceptionCode().equals(ExceptionMap.ERR_OAUTH_403))
        .verify();
  }

  @Test
  @DisplayName("register should throw BAD_REQUEST when requested role does not exist on client")
  void register_roleNotFoundOnClient_throwsBadRequest() {
    AddUserRequest user = new AddUserRequest();
    user.setUsername("john_doe");
    user.setEmail("john.doe@example.com");
    user.setPassword("Strong1Password!");
    user.setRoles(List.of(new UserClientRoleRequest("CLIENT-APP", "NON_EXISTING_ROLE")));

    when(userService.existsByUsername("john_doe")).thenReturn(false);
    when(userService.existsByEmail("john.doe@example.com")).thenReturn(false);

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT-APP");
    client.setEnableRegistration(true);
    client.setAppRoles(List.of(new RoleDefinition("ACCESS-SPHERE_USER")));

    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-APP"))
        .thenReturn(Mono.just(client));

    StepVerifier.create(userAdminService.register(null, user))
        .expectErrorMatches(
            t ->
                t instanceof io.github.giovannilamarmora.accesssphere.data.user.UserException
                    && ((io.github.giovannilamarmora.accesssphere.data.user.UserException) t)
                        .getExceptionCode()
                        .equals(ExceptionMap.ERR_USER_400))
        .verify();
  }

  @Test
  @DisplayName("registerUser should throw FORBIDDEN when client registration is disabled")
  void registerUser_clientRegistrationDisabled_throwsForbidden() {
    User user = new User();
    user.setUsername("google_user");
    user.setEmail("google@example.com");

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT-LOCKED");
    client.setEnableRegistration(false);

    StepVerifier.create(userAdminService.registerUser(null, user, client, true))
        .expectErrorMatches(
            t ->
                t instanceof OAuthException
                    && ((OAuthException) t).getExceptionCode().equals(ExceptionMap.ERR_OAUTH_403))
        .verify();
  }

  @Test
  @DisplayName(
      "register should succeed when client registration is disabled but caller has users:write permission")
  void register_clientRegistrationDisabled_withAdminBearer_bypassesCheckAndSucceeds() {
    AddUserRequest user = new AddUserRequest();
    user.setUsername("provisioned_user");
    user.setEmail("prov@example.com");
    user.setPassword("Strong1Password!");
    user.setRoles(List.of(new UserClientRoleRequest("CLIENT-BLOCKED", "ACCESS-SPHERE_USER")));

    when(userService.existsByUsername("provisioned_user")).thenReturn(false);
    when(userService.existsByEmail("prov@example.com")).thenReturn(false);

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT-BLOCKED");
    client.setEnableRegistration(false);
    RoleDefinition role = new RoleDefinition("ACCESS-SPHERE_USER", "USER", "User role", List.of("users:read"));
    client.setAppRoles(List.of(role));

    when(oAuth2ClientService.getClientCredentialByClientID("CLIENT-BLOCKED"))
        .thenReturn(Mono.just(client));

    OAuth2Authorization adminAuth = new OAuth2Authorization();
    adminAuth.setSubject("admin");
    adminAuth.setIdentifier("admin-uuid");
    adminAuth.setPermissions(List.of("users:write"));

    when(oAuth2AuthorizationService.getByAccessTokenOrIdToken("Bearer admin-token"))
        .thenReturn(adminAuth);
    when(permissionRoleManager.canWriteUsers(adminAuth, "CLIENT-BLOCKED")).thenReturn(true);

    when(userService.saveUserEntityIntoDatabase(any()))
        .thenAnswer(
            inv -> {
              io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity entity =
                  inv.getArgument(0);
              User dto = new User();
              dto.setIdentifier(entity.getIdentifier());
              dto.setUsername(entity.getUsername());
              dto.setEmail(entity.getEmail());
              return dto;
            });
    when(userClientRoleDAO.saveAll(any())).thenReturn(List.of());

    StepVerifier.create(userAdminService.register("Bearer admin-token", user))
        .assertNext(
            registered -> {
              assertNotNull(registered.getIdentifier());
              assertEquals("provisioned_user", registered.getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "registerUser should succeed when client registration is disabled but caller has admin bearer")
  void registerUser_clientRegistrationDisabled_withAdminBearer_bypassesCheckAndSucceeds() {
    User user = new User();
    user.setUsername("admin_created_user");
    user.setEmail("admin_created@example.com");

    OAuth2Client client = new OAuth2Client();
    client.setClientId("CLIENT-LOCKED");
    client.setEnableRegistration(false);
    client.setAppRoles(List.of(new RoleDefinition("ACCESS-SPHERE_USER", "USER", "desc", List.of())));

    OAuth2Authorization adminAuth = new OAuth2Authorization();
    adminAuth.setSubject("admin");
    adminAuth.setIdentifier("admin-uuid");
    adminAuth.setSubjectType(SubjectType.TECHNICAL);
    adminAuth.setRoles(List.of("ROLE_ADMIN"));

    when(oAuth2AuthorizationService.getByAccessTokenOrIdToken("Bearer admin-token"))
        .thenReturn(adminAuth);
    when(permissionRoleManager.canWriteUsers(adminAuth, "CLIENT-LOCKED")).thenReturn(true);

    when(userService.saveUserEntityIntoDatabase(any()))
        .thenAnswer(
            inv -> {
              io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity entity =
                  inv.getArgument(0);
              User dto = new User();
              dto.setIdentifier(entity.getIdentifier());
              dto.setUsername(entity.getUsername());
              dto.setEmail(entity.getEmail());
              return dto;
            });
    when(userClientRoleDAO.saveAll(any())).thenReturn(List.of());

    StepVerifier.create(userAdminService.registerUser("Bearer admin-token", user, client, true))
        .assertNext(
            registered -> {
              assertNotNull(registered.getIdentifier());
              assertEquals("admin_created_user", registered.getUsername());
            })
        .verifyComplete();
  }
}
