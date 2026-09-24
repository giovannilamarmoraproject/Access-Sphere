package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerazione dei tipi di ruolo applicativi supportati nel sistema OAuth2 IAM.
 *
 * <ul>
 *   <li>{@link #USER}: Ruolo utente finale standard (predefinito per nuovi utenti e fallback canonico).
 *   <li>{@link #ADMIN}: Ruolo di amministrazione applicativa del client.
 *   <li>{@link #MANAGER}: Ruolo di gestione team, moderazione e supervisione operativa.
 *   <li>{@link #OPERATOR}: Ruolo operativo per assistenza, front-office e helpdesk.
 *   <li>{@link #TECHNICAL}: Ruolo per utenze tecniche, integrazioni Machine-to-Machine (M2M) o service-to-service.
 *   <li>{@link #AUDITOR}: Ruolo di audit, conformità normativa e reportistica in sola consultazione.
 *   <li>{@link #GUEST}: Ruolo ospite con privilegi minimi di accesso e sola lettura.
 *   <li>{@link #SUPER_ADMIN}: Ruolo super-amministratore con privilegi massimi estesi.
 * </ul>
 */
public enum AppRoleType {
  USER("USER"),
  ADMIN("ADMIN"),
  MANAGER("MANAGER"),
  OPERATOR("OPERATOR"),
  TECHNICAL("TECHNICAL"),
  AUDITOR("AUDITOR"),
  GUEST("GUEST"),
  SUPER_ADMIN("SUPER_ADMIN");

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
   * Se il valore e' nullo, "DEFAULT" (legacy) o non riconosciuto, esegue il fallback su {@link #USER}.
   *
   * @param value Stringa del tipo ruolo.
   * @return {@link AppRoleType} corrispondente o {@link #USER} se non trovato.
   */
  @JsonCreator
  public static AppRoleType fromString(String value) {
    if (value == null || value.isBlank() || "DEFAULT".equalsIgnoreCase(value.trim())) {
      return USER;
    }
    for (AppRoleType type : AppRoleType.values()) {
      if (type.value.equalsIgnoreCase(value.trim())) {
        return type;
      }
    }
    return USER;
  }
}
