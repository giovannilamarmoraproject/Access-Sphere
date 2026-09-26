package io.github.giovannilamarmora.accesssphere.roles.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializzatore personalizzato Jackson per il modello di dominio {@link RoleDefinition}.
 * Gestisce in modo polimorfico e resiliente diversi formati in ingresso:
 *
 * <ul>
 *   <li>Oggetto JSON completo (es. {"clientId": "...", "role": "ADMIN", "type": "ADMIN",
 *       "permissions": [...]})
 *   <li>Valore testuale semplice (es. "ROLE_USER"), convertito in RoleDefinition con tipo USER
 *   <li>Oggetti con alias per il nome ruolo (es. {"name": "ADMIN"} o {"roleName": "ADMIN"} o
 *       {"value": "ADMIN"})
 * </ul>
 */
public class RoleDefinitionDeserializer extends JsonDeserializer<RoleDefinition> {

  @Override
  public RoleDefinition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonToken token = p.currentToken();

    if (token == JsonToken.VALUE_STRING) {
      String text = p.getText();
      if (text == null || text.isBlank()) {
        return null;
      }
      return new RoleDefinition(text.trim().toUpperCase(), AppRoleType.USER.getValue());
    }

    if (token == JsonToken.START_OBJECT) {
      JsonNode node = p.getCodec().readTree(p);
      return parseNode(node);
    }

    return null;
  }

  private RoleDefinition parseNode(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }

    Long id = null;
    if (node.hasNonNull("id")) {
      id = node.get("id").asLong();
    }

    String clientId = null;
    if (node.hasNonNull("clientId")) {
      clientId = node.get("clientId").asText();
    } else if (node.hasNonNull("client_id")) {
      clientId = node.get("client_id").asText();
    }

    String role = null;
    if (node.hasNonNull("role")) {
      role = node.get("role").asText();
    } else if (node.hasNonNull("name")) {
      role = node.get("name").asText();
    } else if (node.hasNonNull("roleName")) {
      role = node.get("roleName").asText();
    } else if (node.hasNonNull("value")) {
      role = node.get("value").asText();
    }

    if (role != null) {
      role = role.trim().toUpperCase();
    }

    String type = AppRoleType.USER.getValue();
    if (node.hasNonNull("type")) {
      String rawType = node.get("type").asText();
      if (rawType != null && !rawType.isBlank()) {
        type =
            "DEFAULT".equalsIgnoreCase(rawType.trim())
                ? AppRoleType.USER.getValue()
                : rawType.trim().toUpperCase();
      }
    }

    String description = null;
    if (node.hasNonNull("description")) {
      description = node.get("description").asText();
    } else if (node.hasNonNull("desc")) {
      description = node.get("desc").asText();
    }

    List<String> permissions = new ArrayList<>();
    if (node.hasNonNull("permissions") && node.get("permissions").isArray()) {
      for (JsonNode permNode : node.get("permissions")) {
        if (permNode != null && !permNode.isNull()) {
          String perm = permNode.asText();
          if (perm != null && !perm.isBlank()) {
            permissions.add(perm.trim());
          }
        }
      }
    }

    RoleDefinition def = new RoleDefinition();
    def.setId(id);
    if (clientId != null && !clientId.isBlank()) {
      def.setClientId(clientId.trim().toUpperCase());
    }
    def.setRole(role);
    def.setType(type);
    def.setDescription(description);
    def.setPermissions(permissions);
    return def;
  }
}
