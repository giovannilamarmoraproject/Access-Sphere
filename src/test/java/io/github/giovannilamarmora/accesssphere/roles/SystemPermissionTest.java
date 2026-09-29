package io.github.giovannilamarmora.accesssphere.roles;

import static org.junit.jupiter.api.Assertions.*;

import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemPermissionTest {

  @Test
  @DisplayName("canonicalize correctly maps legacy singular to canonical plural format")
  void testCanonicalize() {
    assertEquals("users:read", SystemPermission.canonicalize("users:read"));
    assertEquals("users:read", SystemPermission.canonicalize("user:read"));
    assertEquals("users:read", SystemPermission.canonicalize("USERS:READ"));
    assertEquals("clients:read", SystemPermission.canonicalize("clients:read"));
    assertEquals("clients:read", SystemPermission.canonicalize("client:read"));
    assertEquals("clients:write", SystemPermission.canonicalize("client:write"));
    assertEquals("clients:delete", SystemPermission.canonicalize("client:delete"));
    assertEquals("roles:manage", SystemPermission.canonicalize("role:manage"));
    assertEquals("users:roles", SystemPermission.canonicalize("user:role"));
    assertEquals("settings:read", SystemPermission.canonicalize("setting:read"));
    assertEquals("emails:manage", SystemPermission.canonicalize("email:manage"));
    assertEquals("", SystemPermission.canonicalize(""));
    assertEquals("", SystemPermission.canonicalize(null));
  }

  @Test
  @DisplayName("matches verifies permission bidirectional equality and wildcards")
  void testMatches() {
    // Bidirectional singular vs plural
    assertTrue(SystemPermission.matches("user:read", "users:read"));
    assertTrue(SystemPermission.matches("users:read", "user:read"));
    assertTrue(SystemPermission.matches("client:read", "clients:read"));
    assertTrue(SystemPermission.matches("clients:read", "client:read"));
    assertTrue(SystemPermission.matches("CLIENTS:WRITE", "client:write"));

    // Global wildcards
    assertTrue(SystemPermission.matches("*", "users:read"));
    assertTrue(SystemPermission.matches("ALL", "clients:delete"));
    assertTrue(SystemPermission.matches("all", "settings:write"));

    // Resource wildcards
    assertTrue(SystemPermission.matches("user:*", "users:read"));
    assertTrue(SystemPermission.matches("users:*", "user:write"));
    assertTrue(SystemPermission.matches("client:*", "clients:delete"));

    // Manage privileges
    assertTrue(SystemPermission.matches("user:manage", "users:read"));
    assertTrue(SystemPermission.matches("user:manage", "users:delete"));
    assertTrue(SystemPermission.matches("roles:manage", "role:read"));
    assertTrue(SystemPermission.matches("role:manage", "users:roles"));

    // Negative matches
    assertFalse(SystemPermission.matches("users:read", "users:write"));
    assertFalse(SystemPermission.matches("client:read", "users:read"));
    assertFalse(SystemPermission.matches("settings:read", "clients:write"));
    assertFalse(SystemPermission.matches(null, "users:read"));
    assertFalse(SystemPermission.matches("users:read", null));
  }

  @Test
  @DisplayName("SystemPermission.matches instance method works properly")
  void testInstanceMatches() {
    assertTrue(SystemPermission.USERS_READ.matches("users:read"));
    assertTrue(SystemPermission.USERS_READ.matches("user:read"));
    assertTrue(SystemPermission.USERS_READ.matches("user:*"));
    assertTrue(SystemPermission.USERS_READ.matches("*"));
    assertTrue(SystemPermission.USERS_READ.matches("ALL"));
    assertFalse(SystemPermission.USERS_READ.matches("users:write"));
    assertFalse(SystemPermission.USERS_READ.matches("client:read"));

    assertTrue(SystemPermission.CLIENTS_READ.matches("client:read"));
    assertTrue(SystemPermission.CLIENTS_READ.matches("clients:read"));
    assertTrue(SystemPermission.CLIENTS_WRITE.matches("client:write"));
    assertTrue(SystemPermission.CLIENTS_WRITE.matches("clients:write"));

    assertTrue(SystemPermission.ALL.matches("*"));
    assertTrue(SystemPermission.ALL.matches("ALL"));
    assertFalse(SystemPermission.ALL.matches("users:read"));
  }

  @Test
  @DisplayName("fromString correctly resolves singular, plural, and wildcard strings to enum")
  void testFromString() {
    assertEquals(SystemPermission.USERS_READ, SystemPermission.fromString("users:read"));
    assertEquals(SystemPermission.USERS_READ, SystemPermission.fromString("user:read"));
    assertEquals(SystemPermission.USERS_WRITE, SystemPermission.fromString("user:write"));
    assertEquals(SystemPermission.USERS_WRITE, SystemPermission.fromString("users:write"));
    assertEquals(SystemPermission.CLIENTS_READ, SystemPermission.fromString("client:read"));
    assertEquals(SystemPermission.CLIENTS_READ, SystemPermission.fromString("clients:read"));
    assertEquals(SystemPermission.ALL, SystemPermission.fromString("*"));
    assertEquals(SystemPermission.ALL, SystemPermission.fromString("ALL"));
    assertEquals(SystemPermission.ALL, SystemPermission.fromString("all"));
    assertNull(SystemPermission.fromString("nonexistent:permission"));
    assertNull(SystemPermission.fromString(null));
    assertNull(SystemPermission.fromString(""));
  }
}
