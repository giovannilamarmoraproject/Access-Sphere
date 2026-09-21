package io.github.giovannilamarmora.accesssphere.data.roles;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.client.ClientMapper;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppRoleTest {

  private final ObjectMapper mapper = MapperUtils.mapper().build();

  @Test
  @DisplayName("Should deserialize AppRole from plain string")
  void shouldDeserializeAppRoleFromString() throws JsonProcessingException {
    String json = "\"ROLE_ADMIN\"";
    AppRole role = mapper.readValue(json, AppRole.class);

    assertNotNull(role);
    assertEquals("ROLE_ADMIN", role.getRole());
    assertEquals(AppRoleType.DEFAULT.getValue(), role.getType());
    assertTrue(role.isDefault());
  }

  @Test
  @DisplayName("Should deserialize AppRole from full JSON object")
  void shouldDeserializeAppRoleFromObject() throws JsonProcessingException {
    String json =
        "{\"role\":\"ACCESS_SPHERE_ADMIN\",\"type\":\"DEFAULT\",\"description\":\"Default token Type\"}";
    AppRole role = mapper.readValue(json, AppRole.class);

    assertNotNull(role);
    assertEquals("ACCESS_SPHERE_ADMIN", role.getRole());
    assertEquals("DEFAULT", role.getType());
    assertEquals("Default token Type", role.getDescription());
    assertTrue(role.isDefault());
    assertTrue(role.matches("access_sphere_admin"));
  }

  @Test
  @DisplayName("Should deserialize AppRole with alias name and technical type")
  void shouldDeserializeAppRoleWithAlias() throws JsonProcessingException {
    String json =
        "{\"name\":\"ROLE_TECH\",\"type\":\"technical\",\"description\":\"Technical role\"}";
    AppRole role = mapper.readValue(json, AppRole.class);

    assertNotNull(role);
    assertEquals("ROLE_TECH", role.getRole());
    assertEquals("TECHNICAL", role.getType());
    assertFalse(role.isDefault());
  }

  @Test
  @DisplayName("Should deserialize List of AppRoles with mixed formats in ClientCredential")
  void shouldDeserializeMixedAppRolesList() throws JsonProcessingException {
    String json =
        "{\"clientId\":\"TEST-CLIENT\",\"appRoles\":[\"ROLE_ADMIN\",{\"role\":\"ROLE_TECH\",\"type\":\"TECHNICAL\"}]}";
    ClientCredential client = mapper.readValue(json, ClientCredential.class);

    assertNotNull(client);
    assertNotNull(client.getAppRoles());
    assertEquals(2, client.getAppRoles().size());
    assertEquals("ROLE_ADMIN", client.getAppRoles().get(0).getRole());
    assertTrue(client.getAppRoles().get(0).isDefault());
    assertEquals("ROLE_TECH", client.getAppRoles().get(1).getRole());
    assertEquals("TECHNICAL", client.getAppRoles().get(1).getType());
  }

  @Test
  @DisplayName("ClientMapper.parseAppRoles should handle JSON array, plain strings and null")
  void shouldParseAppRolesResiliently() {
    assertNull(ClientMapper.parseAppRoles(null));
    assertNull(ClientMapper.parseAppRoles(""));

    // JSON array
    List<AppRole> parsedJson =
        ClientMapper.parseAppRoles(
            "[{\"role\":\"ROLE_ADMIN\"},{\"role\":\"ROLE_USER\",\"type\":\"USER\"}]");
    assertNotNull(parsedJson);
    assertEquals(2, parsedJson.size());
    assertEquals("ROLE_ADMIN", parsedJson.get(0).getRole());
    assertEquals("DEFAULT", parsedJson.get(0).getType());

    // Legacy space/comma separated strings
    List<AppRole> parsedDelimited = ClientMapper.parseAppRoles("ROLE_ADMIN, ROLE_TECH ROLE_USER");
    assertNotNull(parsedDelimited);
    assertEquals(3, parsedDelimited.size());
    assertEquals("ROLE_ADMIN", parsedDelimited.get(0).getRole());
    assertEquals("ROLE_TECH", parsedDelimited.get(1).getRole());
    assertEquals("ROLE_USER", parsedDelimited.get(2).getRole());
  }

  @Test
  @DisplayName("OAuthValidator.validateClientRoles should enforce mandatory DEFAULT role")
  void shouldValidateMandatoryDefaultRole() {
    ClientCredential clientWithoutRoles = new ClientCredential();
    assertThrows(
        OAuthException.class, () -> OAuthValidator.validateClientRoles(clientWithoutRoles));

    ClientCredential clientWithoutDefault = new ClientCredential();
    clientWithoutDefault.setAppRoles(List.of(new AppRole("ROLE_USER", "USER", "User role")));
    OAuthException ex =
        assertThrows(
            OAuthException.class, () -> OAuthValidator.validateClientRoles(clientWithoutDefault));
    assertTrue(ex.getMessage().contains("DEFAULT"));

    ClientCredential clientWithDefault = new ClientCredential();
    clientWithDefault.setAppRoles(List.of(new AppRole("ROLE_ADMIN", "DEFAULT", "Admin role")));
    assertDoesNotThrow(() -> OAuthValidator.validateClientRoles(clientWithDefault));
  }
}
