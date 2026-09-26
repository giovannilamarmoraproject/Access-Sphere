package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import java.io.IOException;
import java.util.List;

/**
 * Deserializzatore Jackson personalizzato per il campo 'appRoles' / 'app_roles' in {@link
 * OAuth2Client}. Supporta in ingresso molteplici formati:
 *
 * <ul>
 *   <li>Array nativo JSON di oggetti {@link RoleDefinition} o di stringhe
 *   <li>Stringa JSON serializzata (es. "[{\"role\": \"...\"}]")
 *   <li>Stringa delimitata da virgole o spazi (es. "ROLE_ADMIN, ROLE_TECH")
 * </ul>
 */
public class AppRolesDeserializer extends JsonDeserializer<List<RoleDefinition>> {

  @Override
  public List<RoleDefinition> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonToken token = p.currentToken();

    if (token == JsonToken.START_ARRAY) {
      return p.readValueAs(new TypeReference<List<RoleDefinition>>() {});
    }

    if (token == JsonToken.VALUE_STRING) {
      String text = p.getText();
      return OAuth2ClientMapper.parseAppRoles(text);
    }

    return null;
  }
}
