package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolver centralizzato per i parametri dinamici dei Template Email in Access Sphere.
 * <p>
 * Implementa la convenzione a namespace gerarchici:
 * <ul>
 *   <li><b>USER.*</b>: parametri utente (USER.NAME, USER.SURNAME, USER.FULL_NAME, USER.USERNAME, USER.EMAIL, USER.RESET_TOKEN)</li>
 *   <li><b>APP.*</b>: parametri di sistema/branding (APP.NAME, APP.URL, APP.SUPPORT_URL)</li>
 *   <li><b>CLIENT.*</b>: parametri client OAuth2 (CLIENT.ID, CLIENT.NAME)</li>
 *   <li><b>AUTH.*</b>: parametri transazionali di sicurezza (AUTH.RESET_CODE, AUTH.VERIFY_URL, AUTH.OTP_CODE, AUTH.EXPIRE_MINUTES, AUTH.LOCK_REASON)</li>
 * </ul>
 * <p>
 * Include una strategia di normalizzazione bidirezionale per garantire retrocompatibilità trasparente
 * con i segnaposto flat legacy (es. {{NAME}} e {{USER.NAME}} puntano al medesimo valore).
 */
@Component
public class EmailParameterResolver {

  /**
   * Risolve ed espande una mappa generica di parametri assicurando che sia le chiavi namespaced
   * (es. 'USER.NAME', 'APP.URL') sia quelle legacy ('NAME', 'SITE_URL') siano reciprocamente allineate.
   *
   * @param rawParams Mappa di parametri iniziale fornita dal chiamante o dal template
   * @return Nuova mappa con tutte le chiavi espanse e normalizzate
   */
  public static Map<String, String> resolve(Map<String, String> rawParams) {
    Map<String, String> resolved = new LinkedHashMap<>();
    if (rawParams != null) {
      resolved.putAll(rawParams);
    }
    expandBidirectionalMappings(resolved);
    return resolved;
  }

  /**
   * Risolve ed estrae dinamicamente i parametri a partire da un oggetto {@link User} e da eventuali
   * parametri addizionali di evento.
   *
   * @param user Modello di dominio dell'utente destinatario
   * @param eventParams Mappa opzionale di parametri specifici dell'evento (es. link, codici)
   * @return Mappa completa con i namespace USER, APP, AUTH e relativi alias
   */
  public static Map<String, String> resolve(User user, Map<String, String> eventParams) {
    Map<String, String> resolved = new LinkedHashMap<>();

    // 1. Inserimento parametri utente nel namespace USER.*
    if (user != null) {
      if (user.getName() != null) resolved.put("USER.NAME", user.getName());
      if (user.getSurname() != null) resolved.put("USER.SURNAME", user.getSurname());
      if (user.getName() != null && user.getSurname() != null) {
        resolved.put("USER.FULL_NAME", (user.getName() + " " + user.getSurname()).trim());
      } else if (user.getName() != null) {
        resolved.put("USER.FULL_NAME", user.getName());
      }
      if (user.getUsername() != null) resolved.put("USER.USERNAME", user.getUsername());
      if (user.getEmail() != null) resolved.put("USER.EMAIL", user.getEmail());
      if (user.getTokenReset() != null) {
        resolved.put("USER.RESET_TOKEN", user.getTokenReset());
        resolved.put("AUTH.RESET_CODE", user.getTokenReset());
      }
    }

    // 2. Unione parametri addizionali passati nell'evento
    if (eventParams != null) {
      resolved.putAll(eventParams);
    }

    // 3. Normalizzazione bidirezionale per compatibilita legacy
    expandBidirectionalMappings(resolved);
    return resolved;
  }

  /**
   * Risolve parametri a partire da {@link User}, {@link OAuth2Client} e mappa di evento.
   *
   * @param user Modello utente
   * @param client Modello client OAuth2
   * @param eventParams Mappa parametri evento
   * @return Mappa arricchita con namespace USER.*, CLIENT.*, APP.* e AUTH.*
   */
  public static Map<String, String> resolve(
      User user, OAuth2Client client, Map<String, String> eventParams) {
    Map<String, String> resolved = resolve(user, eventParams);

    if (client != null && !ObjectToolkit.isNullOrEmpty(client.getClientId())) {
      resolved.put("CLIENT.ID", client.getClientId());
      resolved.put("CLIENT.NAME", client.getClientId());
      resolved.put("CLIENT_ID", client.getClientId());
    }

    expandBidirectionalMappings(resolved);
    return resolved;
  }

  /**
   * Effettua la cross-popolazione automatica tra chiavi gerarchiche e alias flat.
   * Consente ai template di utilizzare indistintamente {{USER.NAME}} o {{NAME}},
   * {{APP.URL}} o {{SITE_URL}}, {{AUTH.RESET_CODE}} o {{RESET_CODE}}.
   *
   * @param map Mappa di destinazione da sincronizzare
   */
  private static void expandBidirectionalMappings(Map<String, String> map) {
    if (map == null || map.isEmpty()) return;

    // --- USER MAPPINGS ---
    syncKeys(map, "USER.NAME", "NAME");
    syncKeys(map, "USER.SURNAME", "SURNAME");
    syncKeys(map, "USER.USERNAME", "USERNAME");
    syncKeys(map, "USER.EMAIL", "EMAIL");
    syncKeys(map, "USER.RESET_TOKEN", "RESET_CODE");
    syncKeys(map, "AUTH.RESET_CODE", "RESET_CODE");
    syncKeys(map, "AUTH.RESET_CODE", "USER.RESET_TOKEN");

    // --- APP / BRANDING MAPPINGS ---
    syncKeys(map, "APP.NAME", "SITE_SHORT");
    syncKeys(map, "APP.URL", "SITE_URL");
    syncKeys(map, "APP.SUPPORT_URL", "SUPPORT_URL");

    // --- AUTH / SECURITY MAPPINGS ---
    syncKeys(map, "AUTH.VERIFY_URL", "VERIFY_URL");
    syncKeys(map, "AUTH.OTP_CODE", "OTP_CODE");
    syncKeys(map, "AUTH.EXPIRE_MINUTES", "EXPIRATION_MINUTES");
    syncKeys(map, "AUTH.EXPIRE_MINUTES", "EXPIRE_MINUTES");
    syncKeys(map, "AUTH.LOCK_REASON", "LOCK_REASON");

    // Valori predefiniti di sicurezza se assenti o non risolti
    if (!map.containsKey("APP.NAME") || isUnresolvedPlaceholder(map.get("APP.NAME"))) {
      map.put("APP.NAME", "Access Sphere");
    }
    if (!map.containsKey("SITE_SHORT") || isUnresolvedPlaceholder(map.get("SITE_SHORT"))) {
      map.put("SITE_SHORT", "Access Sphere");
    }
    if (!map.containsKey("APP.URL") || isUnresolvedPlaceholder(map.get("APP.URL"))) {
      map.put("APP.URL", "https://accesssphere.io");
    }
    if (!map.containsKey("SITE_URL") || isUnresolvedPlaceholder(map.get("SITE_URL"))) {
      map.put("SITE_URL", "https://accesssphere.io");
    }
  }

  /**
   * Sincronizza il valore tra due chiavi: se la chiave primaria e presente copia il valore
   * nella secondaria; se e presente solo la secondaria la copia nella primaria.
   */
  private static void syncKeys(Map<String, String> map, String key1, String key2) {
    String val1 = map.get(key1);
    String val2 = map.get(key2);

    if (val1 != null && !isUnresolvedPlaceholder(val1) && (val2 == null || isUnresolvedPlaceholder(val2))) {
      map.put(key2, val1);
    } else if (val2 != null && !isUnresolvedPlaceholder(val2) && (val1 == null || isUnresolvedPlaceholder(val1))) {
      map.put(key1, val2);
    }
  }

  /**
   * Verifica se un valore e nullo o rappresenta un segnaposto non risolto nel formato {{...}}.
   */
  private static boolean isUnresolvedPlaceholder(String val) {
    return val == null || (val.startsWith("{{") && val.endsWith("}}"));
  }
}
