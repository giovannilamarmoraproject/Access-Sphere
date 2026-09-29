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
   * Converte una stringa di permesso nel corrispondente enum {@link SystemPermission},
   * supportando sia il formato plurale (es. "users:read") che singolare (es. "user:read"),
   * oltre alla wildcard ("*" o "ALL").
   *
   * @param value Valore stringa del permesso.
   * @return Enum trovato o null se non riconosciuto.
   */
  @JsonCreator
  public static SystemPermission fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = normalizePermission(value);
    if (normalized.equals("*")) {
      return ALL;
    }
    return Arrays.stream(values())
        .filter(p -> normalizePermission(p.value).equalsIgnoreCase(normalized))
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

  /**
   * Normalizza una stringa di permesso per consentire il confronto agnostico rispetto
   * a singolare/plurale, maiuscole/minuscole e spazi.
   * Riconduce ad esempio "users:read", "user:read", "USERS:READ" a "user:read",
   * e "*" / "ALL" a "*".
   *
   * @param permission Stringa del permesso da normalizzare.
   * @return Permesso normalizzato in formato canonico singolare minuscolo, oppure stringa vuota se nullo.
   */
  public static String normalizePermission(String permission) {
    if (permission == null || permission.isBlank()) {
      return "";
    }
    String p = permission.trim().toLowerCase();
    if (p.equals("*") || p.equalsIgnoreCase("all")) {
      return "*";
    }
    int colonIndex = p.indexOf(':');
    if (colonIndex > 0) {
      String resource = p.substring(0, colonIndex).trim();
      String action = p.substring(colonIndex + 1).trim();
      // Normalizza il plurale rimuovendo la 's' finale dalla risorsa (es. users -> user, clients -> client)
      if (resource.endsWith("s")) {
        resource = resource.substring(0, resource.length() - 1);
      }
      if (action.equals("roles")) {
        action = "role";
      }
      return resource + ":" + action;
    }
    return p;
  }

  /**
   * Verifica se un permesso concesso (es. da token o da ruolo su DB) soddisfa il permesso richiesto,
   * supportando:
   * <ul>
   *   <li>Wildcard globale ('*' o 'ALL');</li>
   *   <li>Equivalenza bidirezionale singolare/plurale (es. 'users:read' soddisfa 'user:read' e viceversa);</li>
   *   <li>Wildcard di risorsa (es. 'users:*' o 'user:*' soddisfa qualsiasi azione sulla risorsa);</li>
   *   <li>Privilegio di gestione atomico (es. 'users:manage' o 'roles:manage' soddisfa 'users:read', 'users:write', ecc.);</li>
   *   <li>Case-insensitivity e tolleranza agli spazi.</li>
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
    String normGranted = normalizePermission(grantedPermission);
    String normRequired = normalizePermission(requiredPermission);

    if (normGranted.isEmpty() || normRequired.isEmpty()) {
      return false;
    }
    // 1. Wildcard globale: '*' garantisce accesso a qualsiasi risorsa
    if (normGranted.equals("*")) {
      return true;
    }
    // 2. Corrispondenza diretta (dopo normalizzazione singolare/plurale)
    if (normGranted.equalsIgnoreCase(normRequired)) {
      return true;
    }
    // 3. Wildcard per risorsa (es. 'user:*' autorizza 'user:read', 'user:write', ecc.)
    if (normGranted.endsWith(":*")) {
      String grantedResource = normGranted.substring(0, normGranted.length() - 2);
      if (normRequired.startsWith(grantedResource + ":")) {
        return true;
      }
    }
    // 4. Privilegio 'manage' sulla risorsa (es. 'user:manage' autorizza qualsiasi azione su 'user:*')
    if (normGranted.endsWith(":manage")) {
      String grantedResource = normGranted.substring(0, normGranted.length() - 7);
      if (normRequired.startsWith(grantedResource + ":")) {
        return true;
      }
    }
    // 5. Interconnessione: 'role:manage' copre anche l'assegnazione dei ruoli agli utenti ('user:role')
    if (normGranted.equals("role:manage") && normRequired.equals("user:role")) {
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
      String norm = normalizePermission(grantedPermission);
      return norm.equals("*");
    }
    return matches(grantedPermission, this.value);
  }
}
