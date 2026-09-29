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

  // Perimetro Ruoli Applicativi
  ROLES_MANAGE("roles:manage", "Creazione, modifica e gestione dei ruoli applicativi e permessi"),

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
   * Supporta il formato canonico plurale (es. "users:read"), la wildcard ("*" o "ALL")
   * e le chiavi storiche singolari per retrocompatibilita'.
   *
   * @param value Valore stringa del permesso.
   * @return Enum trovato o null se non riconosciuto.
   */
  @JsonCreator
  public static SystemPermission fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String val = value.trim();
    if (val.equals("*") || val.equalsIgnoreCase("ALL")) {
      return ALL;
    }
    for (SystemPermission p : values()) {
      if (p.value.equalsIgnoreCase(val) || p.name().equalsIgnoreCase(val)) {
        return p;
      }
    }
    // Mappatura esplicita di retrocompatibilita' per vecchi record singolari
    return switch (val.toLowerCase()) {
      case "user:read" -> USERS_READ;
      case "user:write" -> USERS_WRITE;
      case "user:delete" -> USERS_DELETE;
      case "user:lock" -> USERS_LOCK;
      case "user:roles", "user:role" -> USERS_ROLES;
      case "client:read" -> CLIENTS_READ;
      case "client:write" -> CLIENTS_WRITE;
      case "client:delete" -> CLIENTS_DELETE;
      case "role:manage" -> ROLES_MANAGE;
      case "setting:read" -> SETTINGS_READ;
      case "setting:write" -> SETTINGS_WRITE;
      case "email:manage" -> EMAILS_MANAGE;
      default -> null;
    };
  }

  /**
   * Restituisce la lista di tutti i codici permesso di sistema standard (escluso ALL).
   *
   * @return Lista di permessi stringa.
   */
  public static List<String> getAllStandardPermissions() {
    return Arrays.stream(values()).filter(p -> p != ALL).map(SystemPermission::getValue).toList();
  }

  /**
   * Mappa eventuali forme singolari storiche nel valore canonico plurale di sistema.
   *
   * @param perm Stringa del permesso da convertire.
   * @return Valore canonico plurale standard.
   */
  public static String canonicalize(String perm) {
    if (perm == null) return "";
    String p = perm.trim().toLowerCase();
    return switch (p) {
      case "user:read" -> "users:read";
      case "user:write" -> "users:write";
      case "user:delete" -> "users:delete";
      case "user:lock" -> "users:lock";
      case "user:roles", "user:role" -> "users:roles";
      case "client:read" -> "clients:read";
      case "client:write" -> "clients:write";
      case "client:delete" -> "clients:delete";
      case "role:manage" -> "roles:manage";
      case "setting:read" -> "settings:read";
      case "setting:write" -> "settings:write";
      case "email:manage" -> "emails:manage";
      default -> p;
    };
  }

  /**
   * Verifica se un permesso concesso (es. da token o da ruolo su DB) soddisfa il permesso richiesto,
   * supportando:
   * <ul>
   *   <li>Wildcard globale ('*' o 'ALL');</li>
   *   <li>Corrispondenza canonica plurale esatta (case-insensitive);</li>
   *   <li>Wildcard di risorsa (es. 'users:*' autorizza 'users:read', 'users:write', ecc.);</li>
   *   <li>Privilegio atomico di gestione (es. 'roles:manage' o ':manage');</li>
   *   <li>Retrocompatibilita' per formati singolari preesistenti a DB.</li>
   * </ul>
   *
   * @param grantedPermission Permesso posseduto dall'utente/ruolo.
   * @param requiredPermission Permesso richiesto dall'operazione protetta.
   * @return true se autorizzato, false altrimenti.
   */
  public static boolean matches(String grantedPermission, String requiredPermission) {
    if (grantedPermission == null || requiredPermission == null) {
      return false;
    }
    String granted = grantedPermission.trim().toLowerCase();
    String required = requiredPermission.trim().toLowerCase();

    if (granted.isEmpty() || required.isEmpty()) {
      return false;
    }
    // 1. Wildcard globale: '*' o 'ALL' garantisce accesso a qualsiasi risorsa
    if (granted.equals("*") || granted.equals("all")) {
      return true;
    }
    // 2. Corrispondenza diretta esatta (case-insensitive)
    if (granted.equalsIgnoreCase(required)) {
      return true;
    }
    // 3. Wildcard per risorsa (es. 'users:*' autorizza 'users:read', 'users:write', ecc.)
    if (granted.endsWith(":*")) {
      String prefix = granted.substring(0, granted.length() - 1); // es. "users:"
      if (required.startsWith(prefix)) {
        return true;
      }
    }
    // 4. Privilegio 'manage' sulla risorsa (es. 'roles:manage' autorizza azioni su 'roles:*')
    if (granted.endsWith(":manage")) {
      String res = granted.substring(0, granted.length() - 7); // es. "roles"
      if (required.startsWith(res + ":")
          || (res.equals("roles") && required.equals("users:roles"))) {
        return true;
      }
    }
    // 5. Retrocompatibilita': se il DB ha ancora il vecchio formato singolare
    if (canonicalize(granted).equalsIgnoreCase(canonicalize(required))) {
      return true;
    }
    return false;
  }

  /**
   * Verifica se un permesso concesso soddisfa questo specifico {@link SystemPermission}.
   *
   * @param grantedPermission Permesso stringa concesso all'utente.
   * @return true se il permesso concesso soddisfa questo enum.
   */
  public boolean matches(String grantedPermission) {
    if (this == ALL) {
      if (grantedPermission == null) return false;
      String g = grantedPermission.trim().toLowerCase();
      return g.equals("*") || g.equals("all");
    }
    return matches(grantedPermission, this.value);
  }
}
