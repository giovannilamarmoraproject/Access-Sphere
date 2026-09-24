package io.github.giovannilamarmora.accesssphere.token;

import static org.junit.jupiter.api.Assertions.*;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.token.model.SubjectType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class JWTDataTest {

  @Test
  @DisplayName("generateJWTData should filter roles and aggregate permissions for specific client")
  void generateJWTData_scopesRolesAndAggregatesPermissions() {
    User user = new User();
    user.setIdentifier("user-uuid-1");
    user.setUsername("mario_rossi");
    user.setName("Mario");
    user.setSurname("Rossi");
    user.setEmail("mario@example.com");

    UserClientRoleDTO linkatuttoRole =
        new UserClientRoleDTO(
            "linkatutto", "LINKATUTTO_EDITOR", "USER", List.of("links:read", "links:write"));
    UserClientRoleDTO accessSphereRole =
        new UserClientRoleDTO("ACCESS-SPHERE-TECH", "ROLE_ADMIN", "DEFAULT", List.of("*"));
    user.setClientRoles(List.of(linkatuttoRole, accessSphereRole));
    user.setRoles(List.of("LINKATUTTO_EDITOR", "ROLE_ADMIN"));

    ClientCredential linkatuttoClient = new ClientCredential();
    linkatuttoClient.setClientId("linkatutto");
    linkatuttoClient.setJwtExpiration(3600000L);

    MockServerHttpRequest request = MockServerHttpRequest.get("/v1/oauth2/token").build();

    JWTData jwtData =
        JWTData.generateJWTData(user, linkatuttoClient, SubjectType.CUSTOMER, request);

    assertNotNull(jwtData);
    assertEquals("user-uuid-1", jwtData.getIdentifier());
    assertEquals("mario_rossi", jwtData.getSub());
    // Should ONLY contain roles for 'linkatutto'
    assertNotNull(jwtData.getRoles());
    assertEquals(1, jwtData.getRoles().size());
    assertTrue(jwtData.getRoles().contains("LINKATUTTO_EDITOR"));
    assertFalse(jwtData.getRoles().contains("ROLE_ADMIN"));

    // Should contain aggregated permissions for 'linkatutto'
    assertNotNull(jwtData.getPermissions());
    assertEquals(2, jwtData.getPermissions().size());
    assertTrue(jwtData.getPermissions().contains("links:read"));
    assertTrue(jwtData.getPermissions().contains("links:write"));
  }

  @Test
  @DisplayName(
      "generateJWTData fallback to user.roles and clientCredential.appRoles when clientRoles are absent")
  void generateJWTData_fallbackLegacy() {
    User user = new User();
    user.setIdentifier("user-uuid-2");
    user.setUsername("luigi_verdi");
    user.setRoles(List.of("ROLE_ADMIN"));

    ClientCredential client = new ClientCredential();
    client.setClientId("ACCESS-SPHERE-TECH");
    client.setJwtExpiration(3600000L);
    AppRole adminRole =
        new AppRole("ROLE_ADMIN", "DEFAULT", "Admin", List.of("users:read", "users:write", "*"));
    client.setAppRoles(List.of(adminRole));

    MockServerHttpRequest request = MockServerHttpRequest.get("/v1/oauth2/token").build();

    JWTData jwtData = JWTData.generateJWTData(user, client, SubjectType.CUSTOMER, request);

    assertNotNull(jwtData);
    assertEquals(List.of("ROLE_ADMIN"), jwtData.getRoles());
    assertNotNull(jwtData.getPermissions());
    assertTrue(jwtData.getPermissions().contains("users:read"));
    assertTrue(jwtData.getPermissions().contains("users:write"));
    assertTrue(jwtData.getPermissions().contains("*"));
  }
}
