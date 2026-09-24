package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deserializzatore personalizzato Jackson per il campo 'redirect_uri' in {@link OAuth2Client}.
 * Permette la gestione polimorfica trasparente in ingresso per diversi formati:
 *
 * <ul>
 *   <li>Oggetto JSON chiave-valore (es. {"fallback_uri": "...", "redirect_uri": "..."})
 *   <li>Stringa JSON serializzata (es. "{\"fallback_uri\": \"...\", \"redirect_uri\": \"...\"}")
 *   <li>Stringa semplice di URI separati da spazi/virgole (con conversione automatica alla chiave
 *       'redirect_uri')
 * </ul>
 *
 * <p>Interconnesso direttamente con {@link OAuth2Client} tramite l'annotazione
 * {@code @JsonDeserialize(using = RedirectUriDeserializer.class)}.
 */
public class RedirectUriDeserializer extends JsonDeserializer<Map<String, String>> {

  /**
   * Esegue la deserializzazione del token JSON corrente in una mappa {@code Map<String, String>}.
   *
   * @param p Parser JSON Jackson
   * @param ctxt Contesto di deserializzazione
   * @return Mappa contenente le coppie URI configurate, oppure null se il campo e assente/vuoto.
   * @throws IOException in caso di errori di I/O durante la lettura dello stream JSON.
   */
  @Override
  public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonToken token = p.currentToken();

    // Gestione oggetto JSON nativo es. {"fallback_uri": "...", "redirect_uri": "..."}
    if (token == JsonToken.START_OBJECT) {
      return p.readValueAs(new TypeReference<Map<String, String>>() {});
    }

    // Gestione valore testuale (stringa JSON serializzata o singolo URL/lista di URL)
    if (token == JsonToken.VALUE_STRING) {
      String text = p.getText();
      if (text == null || text.isBlank()) {
        return null;
      }
      text = text.trim();

      // Se la stringa inizia con parentesi graffa, tenta il parse come mappa JSON
      if (text.startsWith("{")) {
        try {
          return Mapper.readObject(text, new TypeReference<Map<String, String>>() {});
        } catch (Exception ignored) {
          // In caso di parsing fallito procede al fallback come mappa singola
        }
      }

      // Fallback per stringa semplice: inserisce il valore sotto la chiave standard 'redirect_uri'
      Map<String, String> map = new LinkedHashMap<>();
      map.put(RedirectUris.REDIRECT_URI.url(), text);
      return map;
    }

    return null;
  }
}
