package io.github.giovannilamarmora.accesssphere.webhooks;

import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.net.URI;
import java.util.List;

/**
 * Utility di supporto per il modulo Webhook (pulizia token di autorizzazione, filtraggio e
 * validazione URL).
 */
public final class WebhookUtils {

  private WebhookUtils() {}

  /**
   * Rimuove il prefisso "Bearer " dal token fornito in modo case-insensitive.
   *
   * @param bearer Token di autorizzazione grezzo.
   * @return Token di accesso pulito, oppure null se la stringa in ingresso e vuota.
   */
  public static String cleanBearerToken(String bearer) {
    if (ObjectToolkit.isNullOrEmpty(bearer)) {
      return null;
    }
    return bearer.replaceFirst("^(?i)Bearer\\s+", "").trim();
  }

  /**
   * Filtra la lista dei webhook configurati restituendo solo quelli abilitati per l'azione richiesta.
   * Supporta l'interoperabilita' trasparente tra CREATE_USER e REGISTER_USER.
   *
   * @param webhooks Lista di webhook configurati per il client.
   * @param action Azione/evento target da filtrare.
   * @return Lista di webhook corrispondenti all'azione.
   */
  public static List<Webhook> filterMatchingWebhooks(
      List<Webhook> webhooks, WebhookAction action) {
    if (ObjectToolkit.isNullOrEmpty(webhooks) || action == null) {
      return List.of();
    }
    return webhooks.stream()
        .filter(w -> !ObjectToolkit.isNullOrEmpty(w) && isMatchingAction(w.getAction(), action))
        .toList();
  }

  /**
   * Verifica se l'azione configurata sul webhook corrisponde all'evento scatenato,
   * garantendo l'interoperabilita' bidirezionale tra CREATE_USER e REGISTER_USER.
   *
   * @param configuredAction Azione censita nel webhook del client.
   * @param eventAction Azione/evento scatenato a runtime dal sistema.
   * @return true se le azioni coincidono o sono sinonimi compatibili.
   */
  public static boolean isMatchingAction(WebhookAction configuredAction, WebhookAction eventAction) {
    if (configuredAction == null || eventAction == null) {
      return false;
    }
    if (configuredAction.equals(eventAction)) {
      return true;
    }
    return (configuredAction == WebhookAction.CREATE_USER && eventAction == WebhookAction.REGISTER_USER)
        || (configuredAction == WebhookAction.REGISTER_USER && eventAction == WebhookAction.CREATE_USER);
  }

  /**
   * Verifica se la stringa fornita rappresenta un URL HTTP o HTTPS valido.
   *
   * @param url Stringa URL da verificare.
   * @return true se l'URL e valido e ha schema http/https, false altrimenti.
   */
  public static boolean isValidWebhookUrl(String url) {
    if (ObjectToolkit.isNullOrEmpty(url)) {
      return false;
    }
    try {
      URI uri = URI.create(url.trim());
      String scheme = uri.getScheme();
      return scheme != null
          && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
          && uri.getHost() != null;
    } catch (Exception ignored) {
      return false;
    }
  }
}
