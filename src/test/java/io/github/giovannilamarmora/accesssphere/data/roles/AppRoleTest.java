package io.github.giovannilamarmora.accesssphere.data.roles;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientMapper;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
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
  @DisplayName("Should deserialize AppRole from plain string with USER type as default")
  void shouldDeserializeAppRoleFromString() throws JsonProcessingException {
    String json = "\"ROLE_USER\"";
    AppRole role = mapper.readValue(json, AppRole.class);

    assertNotNull(role);
    assertEquals("ROLE_USER", role.getRole());
    assertEquals(AppRoleType.USER.getValue(), role.getType());
    assertTrue(role.isUser());
  }

  @Test
  @DisplayName("Should deserialize AppRole from full JSON object")
  void shouldDeserializeAppRoleFromObject() throws JsonProcessingException {
    String json =
        "{\"role\":\"ACCESS_SPHERE_ADMIN\",\"type\":\"ADMIN\",\"description\":\"Administrator Type\"}";
    AppRole role = mapper.readValue(json, AppRole.class);

    assertNotNull(role);
    assertEquals("ACCESS_SPHERE_ADMIN", role.getRole());
    assertEquals("ADMIN", role.getType());
    assertEquals("Administrator Type", role.getDescription());
    assertFalse(role.isUser());
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
    assertFalse(role.isUser());
  }

  @Test
  @DisplayName("Should deserialize List of AppRoles with mixed formats in ClientCredential")
  void shouldDeserializeMixedAppRolesList() throws JsonProcessingException {
    String json =
        "{\"clientId\":\"TEST-CLIENT\",\"appRoles\":[\"ROLE_USER\",{\"role\":\"ROLE_TECH\",\"type\":\"TECHNICAL\"}]}";
    OAuth2Client client = mapper.readValue(json, OAuth2Client.class);

    assertNotNull(client);
    assertNotNull(client.getAppRoles());
    assertEquals(2, client.getAppRoles().size());
    assertEquals("ROLE_USER", client.getAppRoles().get(0).getRole());
    assertTrue(client.getAppRoles().get(0).isUser());
    assertEquals("ROLE_TECH", client.getAppRoles().get(1).getRole());
    assertEquals("TECHNICAL", client.getAppRoles().get(1).getType());
  }

  @Test
  @DisplayName("ClientMapper.parseAppRoles should handle JSON array, plain strings and null")
  void shouldParseAppRolesResiliently() {
    assertNull(OAuth2ClientMapper.parseAppRoles(null));
    assertNull(OAuth2ClientMapper.parseAppRoles(""));

    // JSON array
    List<AppRole> parsedJson =
        OAuth2ClientMapper.parseAppRoles(
            "[{\"role\":\"ROLE_USER\"},{\"role\":\"ROLE_ADMIN\",\"type\":\"ADMIN\"}]");
    assertNotNull(parsedJson);
    assertEquals(2, parsedJson.size());
    assertEquals("ROLE_USER", parsedJson.get(0).getRole());
    assertEquals("USER", parsedJson.get(0).getType());

    // Legacy space/comma separated strings
    List<AppRole> parsedDelimited =
        OAuth2ClientMapper.parseAppRoles("ROLE_ADMIN, ROLE_TECH ROLE_USER");
    assertNotNull(parsedDelimited);
    assertEquals(3, parsedDelimited.size());
    assertEquals("ROLE_ADMIN", parsedDelimited.get(0).getRole());
    assertEquals("ROLE_TECH", parsedDelimited.get(1).getRole());
    assertEquals("ROLE_USER", parsedDelimited.get(2).getRole());
  }

  @Test
  @DisplayName("OAuthValidator.validateClientRoles should enforce at least one role")
  void shouldValidateMandatoryRole() {
    OAuth2Client clientWithoutRoles = new OAuth2Client();
    assertThrows(
        OAuthException.class, () -> OAuthValidator.validateClientRoles(clientWithoutRoles));

    OAuth2Client clientWithUserRole = new OAuth2Client();
    clientWithUserRole.setAppRoles(List.of(new AppRole("ROLE_USER", "USER", "User role")));
    assertDoesNotThrow(() -> OAuthValidator.validateClientRoles(clientWithUserRole));
  }
}
