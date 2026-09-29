package io.github.giovannilamarmora.accesssphere.roles;

import static org.junit.jupiter.api.Assertions.*;

import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemPermissionTest {

  @Test
  @DisplayName("fromString correctly resolves canonical plural and wildcard strings to enum")
  void testFromString() {
    assertEquals(SystemPermission.USERS_READ, SystemPermission.fromString("users:read"));
    assertEquals(SystemPermission.USERS_READ, SystemPermission.fromString("USERS:READ"));
    assertEquals(SystemPermission.USERS_WRITE, SystemPermission.fromString("users:write"));
    assertEquals(SystemPermission.CLIENTS_READ, SystemPermission.fromString("clients:read"));
    assertEquals(SystemPermission.ALL, SystemPermission.fromString("*"));
    assertEquals(SystemPermission.ALL, SystemPermission.fromString("ALL"));
    assertEquals(SystemPermission.ALL, SystemPermission.fromString("all"));

    // Singular without fallback is not resolved
    assertNull(SystemPermission.fromString("user:read"));
    assertNull(SystemPermission.fromString("user:write"));
    assertNull(SystemPermission.fromString("client:read"));
    assertNull(SystemPermission.fromString("nonexistent:permission"));
    assertNull(SystemPermission.fromString(null));
    assertNull(SystemPermission.fromString(""));
  }

  @Test
  @DisplayName("matches verifies strict plural matching, wildcards and manage privileges")
  void testMatches() {
    // Exact plural matching
    assertTrue(SystemPermission.matches("users:read", "users:read"));
    assertTrue(SystemPermission.matches("USERS:READ", "users:read"));
    assertTrue(SystemPermission.matches("clients:write", "CLIENTS:WRITE"));

    // Global wildcards
    assertTrue(SystemPermission.matches("*", "users:read"));
    assertTrue(SystemPermission.matches("ALL", "clients:delete"));
    assertTrue(SystemPermission.matches("all", "settings:write"));

    // Resource wildcards
    assertTrue(SystemPermission.matches("users:*", "users:read"));
    assertTrue(SystemPermission.matches("users:*", "users:write"));
    assertTrue(SystemPermission.matches("clients:*", "clients:delete"));

    // Manage privileges
    assertTrue(SystemPermission.matches("roles:manage", "roles:read"));
    assertTrue(SystemPermission.matches("roles:manage", "users:roles"));

    // Singular format rejected without fallback
    assertFalse(SystemPermission.matches("user:read", "users:read"));
    assertFalse(SystemPermission.matches("users:read", "user:read"));
    assertFalse(SystemPermission.matches("client:read", "clients:read"));
    assertFalse(SystemPermission.matches("clients:read", "client:read"));
    assertFalse(SystemPermission.matches("user:*", "users:read"));

    // Negative matches
    assertFalse(SystemPermission.matches("users:read", "users:write"));
    assertFalse(SystemPermission.matches("clients:read", "users:read"));
    assertFalse(SystemPermission.matches("settings:read", "clients:write"));
    assertFalse(SystemPermission.matches(null, "users:read"));
    assertFalse(SystemPermission.matches("users:read", null));
  }

  @Test
  @DisplayName("SystemPermission.matches instance method works properly")
  void testInstanceMatches() {
    assertTrue(SystemPermission.USERS_READ.matches("users:read"));
    assertTrue(SystemPermission.USERS_READ.matches("USERS:READ"));
    assertTrue(SystemPermission.USERS_READ.matches("users:*"));
    assertTrue(SystemPermission.USERS_READ.matches("*"));
    assertTrue(SystemPermission.USERS_READ.matches("ALL"));
    assertFalse(SystemPermission.USERS_READ.matches("user:read"));
    assertFalse(SystemPermission.USERS_READ.matches("users:write"));
    assertFalse(SystemPermission.USERS_READ.matches("clients:read"));

    assertTrue(SystemPermission.CLIENTS_READ.matches("clients:read"));
    assertFalse(SystemPermission.CLIENTS_READ.matches("client:read"));
    assertTrue(SystemPermission.CLIENTS_WRITE.matches("clients:write"));
    assertFalse(SystemPermission.CLIENTS_WRITE.matches("client:write"));

    assertTrue(SystemPermission.ALL.matches("*"));
    assertTrue(SystemPermission.ALL.matches("ALL"));
    assertFalse(SystemPermission.ALL.matches("users:read"));
  }

  @Test
  @DisplayName("getAllStandardPermissions returns all permissions except ALL")
  void testGetAllStandardPermissions() {
    List<String> perms = SystemPermission.getAllStandardPermissions();
    assertFalse(perms.contains("*"));
    assertTrue(perms.contains("users:read"));
    assertTrue(perms.contains("users:write"));
    assertTrue(perms.contains("clients:read"));
    assertTrue(perms.contains("roles:manage"));
  }
}
