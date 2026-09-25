package io.github.giovannilamarmora.accesssphere.roles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionRoleManagerTest {

  @Mock private TechUserService techUserService;
  @Mock private IUserClientRoleDAO userClientRoleDAO;
  @Mock private IRoleDefinitionDAO roleDefinitionDAO;

  @InjectMocks private PermissionRoleManager permissionRoleManager;

  private OAuth2Authorization techAuth;
  private OAuth2Authorization delegatedAuth;

  @BeforeEach
  void setUp() {
    lenient().when(techUserService.getTechClientId()).thenReturn("ACCESS-SPHERE-TECH");

    techAuth = new OAuth2Authorization();
    techAuth.setSubject("tech_admin");
    techAuth.setIdentifier("tech-uuid");

    delegatedAuth = new OAuth2Authorization();
    delegatedAuth.setSubject("mario");
    delegatedAuth.setIdentifier("mario-uuid");
  }

  @Test
  @DisplayName("Super Admin has unconditional console access and all permissions")
  void testSuperAdminPermissions() {
    when(techUserService.isTechUser(techAuth)).thenReturn(true);

    assertTrue(permissionRoleManager.isSuperAdmin(techAuth));
    assertTrue(permissionRoleManager.hasConsoleAccess(techAuth));
    assertDoesNotThrow(() -> permissionRoleManager.assertHasConsoleAccess(techAuth));
    assertNull(permissionRoleManager.getManagedClientIds(techAuth));
    assertTrue(permissionRoleManager.canAccessClient(techAuth, "ANY-CLIENT"));
    assertTrue(permissionRoleManager.hasPermission(techAuth, "ANY-CLIENT", "any:permission"));
    assertDoesNotThrow(() -> permissionRoleManager.assertPermission(techAuth, "ANY-CLIENT", "any:permission"));
  }

  @Test
  @DisplayName("Non-tech user without role on ACCESS-SPHERE-TECH is denied console access")
  void testNonTechUserDeniedConsoleAccess() {
    when(techUserService.isTechUser(delegatedAuth)).thenReturn(false);
    when(userClientRoleDAO.findByUserIdentifierAndClientId("mario-uuid", "ACCESS-SPHERE-TECH"))
        .thenReturn(List.of());

    assertFalse(permissionRoleManager.hasConsoleAccess(delegatedAuth));
    assertThrows(OAuthException.class, () -> permissionRoleManager.assertHasConsoleAccess(delegatedAuth));
  }

  @Test
  @DisplayName("Delegated user with role on ACCESS-SPHERE-TECH has console access and scoped permissions on LINKATUTTO-ADMIN")
  void testDelegatedUserScopedPermissions() {
    when(techUserService.isTechUser(delegatedAuth)).thenReturn(false);

    // Has console role
    UserClientRoleEntity consoleRole =
        new UserClientRoleEntity("mario-uuid", "ACCESS-SPHERE-TECH", "ROLE_USER");
    // Has role on LINKATUTTO-ADMIN
    UserClientRoleEntity linkatuttoRole =
        new UserClientRoleEntity("mario-uuid", "LINKATUTTO-ADMIN", "ROLE_ADMIN");

    when(userClientRoleDAO.findByUserIdentifierAndClientId("mario-uuid", "ACCESS-SPHERE-TECH"))
        .thenReturn(List.of(consoleRole));
    when(userClientRoleDAO.findByUserIdentifier("mario-uuid"))
        .thenReturn(List.of(consoleRole, linkatuttoRole));

    assertTrue(permissionRoleManager.hasConsoleAccess(delegatedAuth));

    // Managed clients should contain LINKATUTTO-ADMIN and exclude ACCESS-SPHERE-TECH
    List<String> managed = permissionRoleManager.getManagedClientIds(delegatedAuth);
    assertEquals(List.of("LINKATUTTO-ADMIN"), managed);
    assertTrue(permissionRoleManager.canAccessClient(delegatedAuth, "LINKATUTTO-ADMIN"));
    assertFalse(permissionRoleManager.canAccessClient(delegatedAuth, "ACCESS-SPHERE-TECH"));
    assertFalse(permissionRoleManager.canAccessClient(delegatedAuth, "OTHER-CLIENT"));

    // Check permissions on LINKATUTTO-ADMIN
    when(userClientRoleDAO.findByUserIdentifierAndClientId("mario-uuid", "LINKATUTTO-ADMIN"))
        .thenReturn(List.of(linkatuttoRole));
    RoleDefinitionEntity roleDef =
        new RoleDefinitionEntity(
            "LINKATUTTO-ADMIN", "ROLE_ADMIN", "ADMIN", "Admin role", List.of("clients:read", "users:*"));
    when(roleDefinitionDAO.findByClientIdAndRole("LINKATUTTO-ADMIN", "ROLE_ADMIN"))
        .thenReturn(Optional.of(roleDef));

    // Allowed permissions
    assertTrue(permissionRoleManager.hasPermission(delegatedAuth, "LINKATUTTO-ADMIN", "clients:read"));
    assertTrue(permissionRoleManager.hasPermission(delegatedAuth, "LINKATUTTO-ADMIN", "users:read"));
    assertTrue(permissionRoleManager.hasPermission(delegatedAuth, "LINKATUTTO-ADMIN", "users:write"));

    // Missing permission
    assertFalse(permissionRoleManager.hasPermission(delegatedAuth, "LINKATUTTO-ADMIN", "clients:delete"));
    assertThrows(
        OAuthException.class,
        () -> permissionRoleManager.assertPermission(delegatedAuth, "LINKATUTTO-ADMIN", "clients:delete"));
  }
}
