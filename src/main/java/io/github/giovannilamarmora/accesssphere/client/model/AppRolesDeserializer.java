package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.giovannilamarmora.accesssphere.client.ClientMapper;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole;
import java.io.IOException;
import java.util.List;

/**
 * Deserializzatore Jackson personalizzato per il campo 'appRoles' / 'app_roles' in {@link ClientCredential}.
 * Supporta in ingresso molteplici formati:
 * <ul>
 *   <li>Array nativo JSON di oggetti {@link AppRole} o di stringhe</li>
 *   <li>Stringa JSON serializzata (es. "[{\"role\": \"...\"}]")</li>
 *   <li>Stringa delimitata da virgole o spazi (es. "ROLE_ADMIN, ROLE_TECH")</li>
 * </ul>
 */
public class AppRolesDeserializer extends JsonDeserializer<List<AppRole>> {

  @Override
  public List<AppRole> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonToken token = p.currentToken();

    if (token == JsonToken.START_ARRAY) {
      return p.readValueAs(new TypeReference<List<AppRole>>() {});
    }

    if (token == JsonToken.VALUE_STRING) {
      String text = p.getText();
      return ClientMapper.parseAppRoles(text);
    }

    return null;
  }
}
