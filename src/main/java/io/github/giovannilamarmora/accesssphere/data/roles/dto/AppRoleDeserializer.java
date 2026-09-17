package io.github.giovannilamarmora.accesssphere.data.roles.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * Deserializzatore personalizzato Jackson per il DTO {@link AppRole}.
 * Gestisce in modo polimorfico e resiliente diversi formati in ingresso:
 * <ul>
 *   <li>Oggetto JSON completo (es. {"role": "ACCESS_SPHERE_ADMIN", "type": "DEFAULT", "description": "..."})</li>
 *   <li>Valore testuale semplice (es. "ROLE_ADMIN"), mappato come ruolo con tipo "DEFAULT"</li>
 *   <li>Oggetti con alias per il nome ruolo (es. {"name": "ADMIN"} o {"roleName": "ADMIN"})</li>
 * </ul>
 */
public class AppRoleDeserializer extends JsonDeserializer<AppRole> {

  @Override
  public AppRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonToken token = p.currentToken();

    if (token == JsonToken.VALUE_STRING) {
      String text = p.getText();
      if (text == null || text.isBlank()) {
        return null;
      }
      return new AppRole(text.trim().toUpperCase(), AppRoleType.DEFAULT.getValue(), null);
    }

    if (token == JsonToken.START_OBJECT) {
      JsonNode node = p.getCodec().readTree(p);
      return parseNode(node);
    }

    return null;
  }

  private AppRole parseNode(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    Long id = null;
    if (node.hasNonNull("id")) {
      id = node.get("id").asLong();
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

    String type = AppRoleType.DEFAULT.getValue();
    if (node.hasNonNull("type")) {
      String rawType = node.get("type").asText();
      if (rawType != null && !rawType.isBlank()) {
        type = rawType.trim().toUpperCase();
      }
    }

    String description = null;
    if (node.hasNonNull("description")) {
      description = node.get("description").asText();
    } else if (node.hasNonNull("desc")) {
      description = node.get("desc").asText();
    }

    AppRole appRole = new AppRole();
    appRole.setId(id);
    appRole.setRole(role);
    appRole.setType(type);
    appRole.setDescription(description);
    return appRole;
  }
}
