package io.github.giovannilamarmora.accesssphere.data.roles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.database.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserConfig;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.RolesService;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RolesServiceTest {

  @Mock private OAuth2ClientService oAuth2ClientService;
  @Mock private UserService userService;
  @Mock private AdminUserService techUserService;
  @Mock private AdminUserConfig adminUserConfig;

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO
      userClientRoleDAO;

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO
      roleDefinitionDAO;

  @Mock private IOAuth2ClientDAO clientDAO;
  @Mock private IUserDAO userDAO;

  @InjectMocks private RolesService rolesService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(rolesService, "oAuth2ClientService", oAuth2ClientService);
    ReflectionTestUtils.setField(rolesService, "userService", userService);
    ReflectionTestUtils.setField(rolesService, "techUserService", techUserService);
    ReflectionTestUtils.setField(rolesService, "adminUserConfig", adminUserConfig);
    ReflectionTestUtils.setField(rolesService, "userClientRoleDAO", userClientRoleDAO);
    ReflectionTestUtils.setField(rolesService, "roleDefinitionDAO", roleDefinitionDAO);
    ReflectionTestUtils.setField(rolesService, "clientDAO", clientDAO);
    ReflectionTestUtils.setField(rolesService, "userDAO", userDAO);
    lenient().when(adminUserConfig.getAdminClientId()).thenReturn("ACCESS-SPHERE-AUTH");
  }

  @Test
  @DisplayName("changeRoles should throw 403 Forbidden when caller is not a tech user")
  void changeRoles_notTechUser_throwsForbidden() {
    when(techUserService.isTechUser()).thenReturn(false);

    List<UserClientRole> userRoles = List.of(new UserClientRole("ROLE_ADMIN"));
    assertThrows(OAuthException.class, () -> rolesService.changeRoles("user-123", userRoles));
  }

  @Test
  @DisplayName(
      "changeRoles should accept client roles, deduplicate them, and accept system roles like SUPER_ADMIN")
  void changeRoles_success_deduplicatesAndIncludesSystemRoles() {
    when(techUserService.isTechUser()).thenReturn(true);

    OAuth2Client client1 = new OAuth2Client();
    client1.setClientId("linkatutto");
    client1.setAppRoles(List.of(new RoleDefinition("LINKATUTTO_ADMIN", "DEFAULT")));

    OAuth2Client client2 = new OAuth2Client();
    client2.setClientId("access_sphere");
    client2.setAppRoles(List.of(new RoleDefinition("ACCESS_SPHERE_ADMIN", "ADMIN")));

    when(oAuth2ClientService.getClientCredentials())
        .thenReturn(Mono.just(List.of(client1, client2)));

    User user = new User();
    user.setIdentifier("user-123");
    user.setUsername("giovannilamarmora");
    user.setRoles(List.of(new UserClientRole("linkatutto", "LINKATUTTO_ADMIN")));
    when(userService.getUserByIdentifier("user-123")).thenReturn(Mono.just(user));

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    when(userService.updateUser(eq("user-123"), userCaptor.capture()))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    List<UserClientRole> payload =
        List.of(
            new UserClientRole("linkatutto", "LINKATUTTO_ADMIN"),
            new UserClientRole("linkatutto", "LINKATUTTO_ADMIN"), // duplicato intenzionale
            new UserClientRole("access_sphere", "ACCESS_SPHERE_ADMIN"),
            new UserClientRole("SUPER_ADMIN"), // ruolo di sistema
            new UserClientRole("UNKNOWN_RANDOM_ROLE") // non valido, deve essere scartato
            );

    StepVerifier.create(rolesService.changeRoles("user-123", payload))
        .assertNext(
            (ResponseEntity<Response> response) -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());

              User updated = (User) response.getBody().getData();
              assertNotNull(updated);

              List<UserClientRole> assignedRoles = updated.getRoles();
              assertNotNull(assignedRoles);
              // Deve contenere LINKATUTTO_ADMIN (una sola volta), ACCESS_SPHERE_ADMIN, SUPER_ADMIN
              assertEquals(3, assignedRoles.size());
              List<String> roleNames = assignedRoles.stream().map(UserClientRole::getRole).toList();
              assertTrue(roleNames.contains("LINKATUTTO_ADMIN"));
              assertTrue(roleNames.contains("ACCESS_SPHERE_ADMIN"));
              assertTrue(roleNames.contains("SUPER_ADMIN"));
              assertFalse(roleNames.contains("UNKNOWN_RANDOM_ROLE"));
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("changeRoles should accept modern clientRoles and persist them to USER_CLIENT_ROLE")
  void changeRoles_withModernClientRoles_success() {
    when(techUserService.isTechUser()).thenReturn(true);

    OAuth2Client client1 = new OAuth2Client();
    client1.setClientId("linkatutto");
    client1.setAppRoles(List.of(new RoleDefinition("LINKATUTTO_ADMIN", "DEFAULT")));

    OAuth2Client client2 = new OAuth2Client();
    client2.setClientId("ACCESS-SPHERE-AUTH");
    client2.setAppRoles(List.of(new RoleDefinition("ROLE_ADMIN", "DEFAULT")));

    when(oAuth2ClientService.getClientCredentials())
        .thenReturn(Mono.just(List.of(client1, client2)));

    User user = new User();
    user.setIdentifier("user-456");
    user.setUsername("testuser");
    when(userService.getUserByIdentifier("user-456")).thenReturn(Mono.just(user));
    when(userService.updateUser(eq("user-456"), any(User.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    List<UserClientRole> payload =
        List.of(
            new UserClientRole("linkatutto", "LINKATUTTO_ADMIN"),
            new UserClientRole("ACCESS-SPHERE-AUTH", "ROLE_ADMIN"));

    StepVerifier.create(rolesService.changeRoles("user-456", payload))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              User updated = (User) response.getBody().getData();
              assertNotNull(updated);
              List<String> roleNames =
                  updated.getRoles().stream().map(UserClientRole::getRole).toList();
              assertTrue(roleNames.contains("LINKATUTTO_ADMIN"));
              assertTrue(roleNames.contains("ROLE_ADMIN"));
              verify(userClientRoleDAO).deleteByUserIdentifier("user-456");
              verify(userClientRoleDAO).saveAll(any());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("assignClientRole should save new mapping and synchronize legacy user roles")
  void assignClientRole_success() {
    when(techUserService.isTechUser()).thenReturn(true);

    User user = new User();
    user.setIdentifier("user-789");
    user.setUsername("giovanni");
    user.setRoles(
        new java.util.ArrayList<>(List.of(new UserClientRole("tech-client", "ROLE_TECH"))));

    when(userService.getUserByIdentifier("user-789")).thenReturn(Mono.just(user));
    when(userService.updateUser(eq("user-789"), any(User.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));
    when(userClientRoleDAO.existsByUserIdentifierAndClientIdAndRole(
            "user-789", "linkatutto", "ROLE_ADMIN"))
        .thenReturn(false);

    StepVerifier.create(rolesService.assignClientRole("user-789", "linkatutto", "ROLE_ADMIN"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              User updated = (User) response.getBody().getData();
              List<String> roleNames =
                  updated.getRoles().stream().map(UserClientRole::getRole).toList();
              assertTrue(roleNames.contains("ROLE_ADMIN"));
              assertTrue(roleNames.contains("ROLE_TECH"));
              verify(userClientRoleDAO).save(any());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "revokeClientRole should delete mapping and remove from legacy roles if not present in other clients")
  void revokeClientRole_success() {
    when(techUserService.isTechUser()).thenReturn(true);

    User user = new User();
    user.setIdentifier("user-789");
    user.setUsername("giovanni");
    user.setRoles(
        new java.util.ArrayList<>(
            List.of(
                new UserClientRole("linkatutto", "ROLE_ADMIN"),
                new UserClientRole("tech-client", "ROLE_TECH"))));

    when(userService.getUserByIdentifier("user-789")).thenReturn(Mono.just(user));
    when(userService.updateUser(eq("user-789"), any(User.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    StepVerifier.create(rolesService.revokeClientRole("user-789", "linkatutto", "ROLE_ADMIN"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              User updated = (User) response.getBody().getData();
              List<String> roleNames =
                  updated.getRoles().stream().map(UserClientRole::getRole).toList();
              assertFalse(roleNames.contains("ROLE_ADMIN"));
              assertTrue(roleNames.contains("ROLE_TECH"));
              verify(userClientRoleDAO)
                  .deleteByUserIdentifierAndClientIdAndRole("user-789", "linkatutto", "ROLE_ADMIN");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getRolesCatalog should return catalog enriched with assigned users")
  void getRolesCatalog_success() {
    when(techUserService.isTechUser()).thenReturn(true);

    io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity roleDef =
        new io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity(
            "linkatutto", "ROLE_EDITOR", "ADMIN", null, List.of("links:write"));
    when(roleDefinitionDAO.findAll()).thenReturn(List.of(roleDef));

    io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity ucr =
        new io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity(
            "u-1", "LINKATUTTO", "ROLE_EDITOR");
    when(userClientRoleDAO.findByClientIdAndRole("LINKATUTTO", "ROLE_EDITOR"))
        .thenReturn(List.of(ucr));

    UserEntity userEntity = new UserEntity();
    userEntity.setIdentifier("u-1");
    userEntity.setUsername("mario");
    userEntity.setName("Mario");
    userEntity.setSurname("Rossi");
    userEntity.setEmail("mario@example.com");
    when(userDAO.findAllByIdentifierIn(List.of("u-1"))).thenReturn(List.of(userEntity));

    StepVerifier.create(rolesService.getRolesCatalog(null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              List<?> catalog = (List<?>) response.getBody().getData();
              assertEquals(1, catalog.size());
              RoleDefinition dto = (RoleDefinition) catalog.get(0);
              assertEquals("LINKATUTTO", dto.getClientId());
              assertEquals("ROLE_EDITOR", dto.getRole());
              assertEquals(1, dto.getAssignedUsersCount());
              assertEquals("mario", dto.getAssignedUsers().get(0).getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("createRole should add AppRole to client and persist")
  void createRole_success() {
    when(techUserService.isTechUser()).thenReturn(true);

    OAuth2ClientEntity clientEntity = new OAuth2ClientEntity();
    clientEntity.setClientId("LINKATUTTO");

    when(clientDAO.findByClientId("LINKATUTTO")).thenReturn(clientEntity);
    when(roleDefinitionDAO.existsByClientIdAndRole("LINKATUTTO", "ROLE_NEW")).thenReturn(false);

    RoleDefinition newRoleDTO =
        new RoleDefinition("LINKATUTTO", "ROLE_NEW", "DEFAULT", "Desc", List.of("links:read"));

    StepVerifier.create(rolesService.createRole(newRoleDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              verify(roleDefinitionDAO).save(any());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteRole should remove AppRole and delete from USER_CLIENT_ROLE and USERS.ROLES")
  void deleteRole_success() {
    when(techUserService.isTechUser()).thenReturn(true);

    OAuth2ClientEntity clientEntity = new OAuth2ClientEntity();
    clientEntity.setClientId("linkatutto");

    when(clientDAO.findByClientId("linkatutto")).thenReturn(clientEntity);

    io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity r1 =
        new io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity(
            "linkatutto", "ROLE_OBSOLETE", "USER", null, List.of());
    io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity r2 =
        new io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity(
            "linkatutto", "ROLE_REMAINING", "USER", null, List.of());
    when(roleDefinitionDAO.findByClientId("linkatutto")).thenReturn(List.of(r1, r2));

    io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity ucr =
        new io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity(
            "u-99", "linkatutto", "ROLE_OBSOLETE");
    when(userClientRoleDAO.findByClientIdAndRole("linkatutto", "ROLE_OBSOLETE"))
        .thenReturn(List.of(ucr));

    StepVerifier.create(rolesService.deleteRole("linkatutto", "ROLE_OBSOLETE"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              verify(roleDefinitionDAO).deleteByClientIdAndRole("linkatutto", "ROLE_OBSOLETE");
              verify(userClientRoleDAO).deleteByClientIdAndRole("linkatutto", "ROLE_OBSOLETE");
            })
        .verifyComplete();
  }
}
