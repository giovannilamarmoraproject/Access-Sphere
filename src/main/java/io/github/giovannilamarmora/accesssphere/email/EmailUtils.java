package io.github.giovannilamarmora.accesssphere.email;

import io.github.giovannilamarmora.accesssphere.email.sender.EmailParameterResolver;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility di dominio per il modulo Email (normalizzazione metadati, risoluzione locale a cascata,
 * rilevamento HTML, compilazione segnaposto e verifica di perimetro multi-tenant).
 */
public final class EmailUtils {

  private EmailUtils() {}

  /**
   * Determina se il corpo del messaggio contiene markup HTML.
   *
   * @param text Corpo testuale dell'email.
   * @return true se contiene tag HTML comuni, false altrimenti.
   */
  public static boolean isHtmlContent(String text) {
    if (ObjectToolkit.isNullOrEmpty(text)) return false;
    return text.contains("<html")
        || text.contains("<div")
        || text.contains("<p>")
        || text.contains("<br")
        || text.contains("<h1")
        || text.contains("<table")
        || text.contains("<!DOCTYPE");
  }

  /**
   * Compila un template HTML/testo risolvendo i parametri di default del template, i parametri
   * dinamici di contesto e i namespace gerarchici tramite {@link EmailParameterResolver}.
   *
   * @param rawTemplate Testo/HTML grezzo con segnaposto {@code {{KEY}}}.
   * @param templateParams Mappa dei parametri predefiniti configurati sul template.
   * @param dynamicParams Mappa dei parametri dinamici forniti all'atto dell'invio.
   * @return Stringa compilata con tutti i segnaposto risolti.
   */
  public static String renderTemplate(
      String rawTemplate,
      Map<String, String> templateParams,
      Map<String, String> dynamicParams) {
    if (ObjectToolkit.isNullOrEmpty(rawTemplate)) return "";

    Map<String, String> resolvedInput = EmailParameterResolver.resolve(dynamicParams);
    Map<String, String> mergedParams = new LinkedHashMap<>(resolvedInput);

    if (!ObjectToolkit.isNullOrEmpty(templateParams)) {
      Map<String, String> resolvedTemplateParams =
          Utils.getFinalMapFromValue(resolvedInput, templateParams);
      resolvedTemplateParams.forEach(
          (key, value) -> {
            if (!isUnresolvedPlaceholder(value)) {
              mergedParams.put(key, value);
            }
          });
    }

    Map<String, String> finalParams = EmailParameterResolver.resolve(mergedParams);
    return replacePlaceholders(rawTemplate, finalParams);
  }

  /**
   * Sostituisce i segnaposto {@code {{KEY}}} all'interno di una stringa utilizzando la mappa di
   * parametri fornita.
   *
   * @param rawTemplate Testo/HTML grezzo.
   * @param params Mappa chiave-valore dei parametri.
   * @return Testo con i segnaposto sostituiti.
   */
  public static String replacePlaceholders(String rawTemplate, Map<String, String> params) {
    if (ObjectToolkit.isNullOrEmpty(rawTemplate) || ObjectToolkit.isNullOrEmpty(params)) {
      return rawTemplate;
    }
    String rendered = rawTemplate;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (key != null && !isUnresolvedPlaceholder(value)) {
        rendered = rendered.replace("{{" + key + "}}", value);
      }
    }
    return rendered;
  }

  /**
   * Seleziona il miglior template email da una lista di candidati applicando il fallback sulla
   * lingua: 1. Corrispondenza esatta del locale; 2. Corrispondenza sul prefisso lingua (es. "it" da
   * "it-IT"); 3. Template con locale nullo o "default"; 4. Primo template disponibile.
   *
   * @param templates Lista di template candidati per categoria/client.
   * @param locale Codice lingua richiesto.
   * @return Optional contenente il template selezionato.
   */
  public static Optional<EmailTemplate> resolveTemplateByLocale(
      List<EmailTemplate> templates, String locale) {
    if (ObjectToolkit.isNullOrEmpty(templates)) return Optional.empty();

    if (!ObjectToolkit.isNullOrEmpty(locale)) {
      Optional<EmailTemplate> exactMatch =
          templates.stream().filter(t -> locale.equalsIgnoreCase(t.getLocale())).findFirst();
      if (exactMatch.isPresent()) return exactMatch;

      String langPrefix = locale.contains("-") ? locale.split("-")[0] : locale;
      Optional<EmailTemplate> prefixMatch =
          templates.stream()
              .filter(
                  t ->
                      t.getLocale() != null
                          && (t.getLocale().equalsIgnoreCase(langPrefix)
                              || t.getLocale()
                                  .toLowerCase()
                                  .startsWith(langPrefix.toLowerCase() + "-")))
              .findFirst();
      if (prefixMatch.isPresent()) return prefixMatch;
    }

    Optional<EmailTemplate> defaultMatch =
        templates.stream()
            .filter(
                t ->
                    ObjectToolkit.isNullOrEmpty(t.getLocale())
                        || "default".equalsIgnoreCase(t.getLocale()))
            .findFirst();
    if (defaultMatch.isPresent()) return defaultMatch;

    return Optional.of(templates.getFirst());
  }

  /**
   * Normalizza i campi principali di un {@link EmailTemplate} (categoria, identificatore, clientId
   * e locale).
   *
   * @param template Modello di dominio da normalizzare.
   * @param pathIdentifier Identificatore opzionale passato come path variable REST.
   */
  public static void normalizeTemplate(EmailTemplate template, String pathIdentifier) {
    if (template == null) return;

    String category = normalizeCategory(template.getCategory());
    template.setCategory(category);

    EmailTemplateCategory categoryEnum = EmailTemplateCategory.fromCode(category);
    String lookupId =
        !ObjectToolkit.isNullOrEmpty(pathIdentifier)
            ? pathIdentifier.trim()
            : (!ObjectToolkit.isNullOrEmpty(template.getIdentifier())
                ? template.getIdentifier().trim()
                : categoryEnum.getDefaultIdentifier());

    template.setIdentifier(
        !ObjectToolkit.isNullOrEmpty(template.getIdentifier())
            ? template.getIdentifier().trim()
            : lookupId);
    template.setClientId(normalizeClientId(template.getClientId()));
    template.setLocale(normalizeLocale(template.getLocale()));
  }

  public static String normalizeCategory(String category) {
    return !ObjectToolkit.isNullOrEmpty(category) ? category.trim().toUpperCase() : "RESET_PASSWORD";
  }

  public static String normalizeClientId(String clientId) {
    if (ObjectToolkit.isNullOrEmpty(clientId) || "GLOBAL".equalsIgnoreCase(clientId.trim())) {
      return null;
    }
    return clientId.trim();
  }

  public static String normalizeLocale(String locale) {
    if (ObjectToolkit.isNullOrEmpty(locale)
        || "GENERIC".equalsIgnoreCase(locale.trim())
        || "ALL".equalsIgnoreCase(locale.trim())) {
      return null;
    }
    return locale.trim();
  }

  /**
   * Verifica se l'operatore ha visibilita/autorizzazione sul client associato al template. I
   * template globali (clientId == null) sono gestibili solo dagli amministratori globali.
   */
  public static boolean hasClientAccess(
      String templateClientId, Set<String> authorizedClientIds, boolean isAdmin) {
    if (isAdmin) return true;
    String normalizedClientId = normalizeClientId(templateClientId);
    return normalizedClientId != null
        && !ObjectToolkit.isNullOrEmpty(authorizedClientIds)
        && authorizedClientIds.contains(normalizedClientId.toUpperCase());
  }

  private static boolean isUnresolvedPlaceholder(String value) {
    return value == null || (value.startsWith("{{") && value.endsWith("}}"));
  }
}
