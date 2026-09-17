package io.github.giovannilamarmora.accesssphere.data.roles.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerazione dei tipi di ruolo applicativi supportati nel sistema OAuth2 IAM.
 * <ul>
 *   <li>{@link #DEFAULT}: Ruolo fondamentale obbligatorio per ogni client OAuth2. Non eliminabile.</li>
 *   <li>{@link #TECHNICAL}: Ruolo per utenze tecniche, integrazioni M2M o service-to-service.</li>
 *   <li>{@link #ADMIN}: Ruolo di amministrazione applicativa.</li>
 *   <li>{@link #SUPER_ADMIN}: Ruolo super-amministratore con privilegi estesi di sistema.</li>
 *   <li>{@link #USER}: Ruolo utente finale standard.</li>
 * </ul>
 */
public enum AppRoleType {
  DEFAULT("DEFAULT"),
  TECHNICAL("TECHNICAL"),
  ADMIN("ADMIN"),
  SUPER_ADMIN("SUPER_ADMIN"),
  USER("USER");

  private final String value;

  AppRoleType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * Converte resilientemente una stringa nel tipo {@link AppRoleType} corrispondente.
   * Se il valore e' nullo o non riconosciuto, esegue il fallback sul valore di default {@link #DEFAULT}.
   *
   * @param value Stringa del tipo ruolo.
   * @return {@link AppRoleType} corrispondente o {@link #DEFAULT} se non trovato.
   */
  @JsonCreator
  public static AppRoleType fromString(String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT;
    }
    for (AppRoleType type : AppRoleType.values()) {
      if (type.value.equalsIgnoreCase(value.trim())) {
        return type;
      }
    }
    return DEFAULT;
  }
}
