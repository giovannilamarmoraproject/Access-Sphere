package io.github.giovannilamarmora.accesssphere.roles.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/**
 * Catalogo canonico dei permessi di sistema per il backoffice IAM di Access-Sphere. Modella i
 * privilegi atomici 'risorsa:azione' necessari per autorizzare chiamate amministrative.
 */
@Getter
public enum SystemPermission {
  ALL("*", "Accesso globale e totale a tutte le risorse (Super Admin)"),

  // Perimetro Utenti
  USERS_READ("users:read", "Visualizzazione dell'anagrafica e lista utenti"),
  USERS_WRITE("users:write", "Creazione e modifica dei dati utente"),
  USERS_DELETE("users:delete", "Cancellazione definitiva di un utente"),
  USERS_LOCK("users:lock", "Blocco o sblocco dello stato di accesso utente"),
  USERS_ROLES("users:roles", "Assegnazione e revoca dei ruoli applicativi dell'utente"),

  // Perimetro Client OAuth2
  CLIENTS_READ("clients:read", "Consultazione delle registrazioni dei client OAuth2"),
  CLIENTS_WRITE("clients:write", "Registrazione e modifica delle configurazioni dei client"),
  CLIENTS_DELETE("clients:delete", "Eliminazione di un client OAuth2 registrato"),

  // Perimetro Impostazioni & Sistema
  SETTINGS_READ("settings:read", "Consultazione impostazioni globali e branding"),
  SETTINGS_WRITE("settings:write", "Modifica del branding, temi e configurazioni globali"),
  BACKUP_MANAGE("backup:manage", "Creazione, export e ripristino snapshot dell'applicazione"),
  EMAILS_MANAGE("emails:manage", "Gestione dei template email di sistema");

  private final String value;
  private final String description;

  SystemPermission(String value, String description) {
    this.value = value;
    this.description = description;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * Converte una stringa di permesso nel corrispondente enum {@link SystemPermission}.
   *
   * @param value Valore stringa del permesso (es. "users:read").
   * @return Enum trovato o null se non riconosciuto.
   */
  @JsonCreator
  public static SystemPermission fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Arrays.stream(values())
        .filter(p -> p.value.equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Restituisce la lista di tutti i codici permesso di sistema standard (escluso ALL).
   *
   * @return Lista di permessi stringa.
   */
  public static List<String> getAllStandardPermissions() {
    return Arrays.stream(values()).filter(p -> p != ALL).map(SystemPermission::getValue).toList();
  }
}
