package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.email.sender.EmailSender;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.database.EmailRepository;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplateCategory;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Service per la gestione dei Template Email di Access Sphere.
 *
 * <p>Responsabilita architetturali e interconnessioni:
 *
 * <ul>
 *   <li>Comunica esclusivamente con {@link EmailRepository} per tutte le operazioni di persistenza,
 *       rimanendo disaccoppiato dall'entita JPA tramite il modello di dominio {@link
 *       EmailTemplate}.
 *   <li>Fornisce risoluzione flessibile del template con fallback multilingua e scoping
 *       multi-tenant.
 *   <li>Effettua rendering dinamico con sostituzione dei segnaposto ed invio di email di test
 *       tramite {@link EmailSender}.
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class EmailTemplateService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private EmailRepository emailRepository;
  @Autowired private EmailSender emailSender;
  @Autowired private AdminUserService techUserService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<EmailTemplateResponse>> getAllTemplates() {
    return Mono.fromCallable(
        () -> {
          List<EmailTemplate> models = emailRepository.findAll().orElse(List.of());
          return EmailTemplateMapper.toDTOList(models);
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateResponse> getTemplateById(Long id) {
    return Mono.fromCallable(
        () -> {
          EmailTemplate model =
              emailRepository
                  .findById(id)
                  .orElseThrow(
                      () ->
                          new ResponseStatusException(
                              HttpStatus.NOT_FOUND, "Email template with ID " + id + " not found"));
          return EmailTemplateMapper.toDTO(model);
        });
  }

  /**
   * Risolve il template email dato il suo identificatore e il locale richiesto. Strategia di
   * fallback intelligente: 1. Cerca per identificatore e locale esatto (es. "it-IT" o "en-GB"). 2.
   * Se non trovato, cerca per prefisso lingua (es. "it" o "en"). 3. Se non trovato, cerca per
   * template con locale nullo, "default" o generico. 4. Se ancora non trovato, restituisce il primo
   * template disponibile per quell'identificatore.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateResponse> getTemplateByIdentifierAndLocale(
      String identifier, String locale) {
    return Mono.fromCallable(
        () -> {
          LOG.info(
              "Searching for email template for identifier=[{}] and locale=[{}]",
              identifier,
              locale);
          List<EmailTemplate> templates =
              emailRepository.findByIdentifier(identifier).orElse(List.of());

          if (templates.isEmpty()) {
            LOG.error("No email template found for identifier=[{}]", identifier);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Email template not found for identifier: " + identifier);
          }

          // 1. Corrispondenza esatta di locale
          if (!ObjectToolkit.isNullOrEmpty(locale)) {
            Optional<EmailTemplate> exact =
                templates.stream().filter(t -> locale.equalsIgnoreCase(t.getLocale())).findFirst();
            if (exact.isPresent()) {
              return EmailTemplateMapper.toDTO(exact.get());
            }

            // 2. Corrispondenza per prefisso lingua (es. "it" per "it-IT")
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
            if (prefixMatch.isPresent()) {
              return EmailTemplateMapper.toDTO(prefixMatch.get());
            }
          }

          // 3. Fallback su locale 'default' o nullo/vuoto
          Optional<EmailTemplate> defaultMatch =
              templates.stream()
                  .filter(
                      t ->
                          ObjectToolkit.isNullOrEmpty(t.getLocale())
                              || "default".equalsIgnoreCase(t.getLocale()))
                  .findFirst();
          if (defaultMatch.isPresent()) {
            return EmailTemplateMapper.toDTO(defaultMatch.get());
          }

          // 4. Fallback sul primo template disponibile per quell'identificatore
          LOG.warn(
              "No language match found for identifier=[{}], locale=[{}]. Using first available template with locale=[{}]",
              identifier,
              locale,
              templates.getFirst().getLocale());
          return EmailTemplateMapper.toDTO(templates.getFirst());
        });
  }

  /**
   * Risoluzione avanzata del template email basata su Categoria, Locale e Client (Multi-Tenant).
   * Risolve a cascata: 1. Match per Client specifico + Categoria + Locale esatto. 2. Match per
   * Client specifico + Categoria + Prefisso lingua (es. "it"). 3. Match per Client specifico +
   * Categoria + Locale nullo/default. 4. Fallback Globale (clientId IS NULL): Categoria + Locale
   * esatto. 5. Fallback Globale (clientId IS NULL): Categoria + Prefisso lingua. 6. Fallback
   * Globale (clientId IS NULL): Categoria + Locale nullo/default. 7. Fallback per
   * retrocompatibilita su identifier diretto.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateResponse> getTemplateByCategoryLocaleAndClient(
      String category, String locale, String clientId) {
    return Mono.fromCallable(
        () -> {
          LOG.info(
              "Searching for email template for category=[{}], locale=[{}], clientId=[{}]",
              category,
              locale,
              clientId);

          String effectiveCategory =
              !ObjectToolkit.isNullOrEmpty(category) ? category.trim() : "RESET_PASSWORD";

          // 1. Se e specificato un clientId, cerca tra i template dedicati a quel client
          if (!ObjectToolkit.isNullOrEmpty(clientId)) {
            List<EmailTemplate> clientTemplates =
                emailRepository
                    .findByClientIdAndCategory(clientId.trim(), effectiveCategory)
                    .orElse(List.of());
            Optional<EmailTemplate> clientMatch = resolveTemplateFromList(clientTemplates, locale);
            if (clientMatch.isPresent()) {
              LOG.info(
                  "Template found for dedicated Client=[{}], ID=[{}]",
                  clientId,
                  clientMatch.get().getId());
              return EmailTemplateMapper.toDTO(clientMatch.get());
            }
            LOG.warn(
                "No specific template found for clientId=[{}] and category=[{}]. Falling back to Global Access Sphere template",
                clientId,
                effectiveCategory);
          }

          // 2. Ricerca tra i template del Client Tecnico predefinito
          String techClientId = techUserService.getAdminClientId();
          if (!techClientId.equalsIgnoreCase(clientId)) {
            List<EmailTemplate> techClientTemplates =
                emailRepository
                    .findByClientIdAndCategory(techClientId, effectiveCategory)
                    .orElse(List.of());
            Optional<EmailTemplate> techMatch =
                resolveTemplateFromList(techClientTemplates, locale);
            if (techMatch.isPresent()) {
              LOG.info(
                  "Template found for Tech Client {} for category=[{}], ID=[{}]",
                  techClientId,
                  effectiveCategory,
                  techMatch.get().getId());
              return EmailTemplateMapper.toDTO(techMatch.get());
            }
          }

          // 3. Ricerca tra i template Globali di Access Sphere (clientId IS NULL)
          List<EmailTemplate> globalTemplates =
              emailRepository.findByClientIdIsNullAndCategory(effectiveCategory).orElse(List.of());
          Optional<EmailTemplate> globalMatch = resolveTemplateFromList(globalTemplates, locale);
          if (globalMatch.isPresent()) {
            LOG.info(
                "Global Access Sphere Template found for category=[{}], ID=[{}]",
                effectiveCategory,
                globalMatch.get().getId());
            return EmailTemplateMapper.toDTO(globalMatch.get());
          }

          // 3. Fallback per retrocompatibilita: ricerca per identifier globale esatto
          List<EmailTemplate> legacyTemplates =
              emailRepository.findByIdentifier(effectiveCategory).orElse(List.of());
          if (!legacyTemplates.isEmpty()) {
            Optional<EmailTemplate> legacyMatch = resolveTemplateFromList(legacyTemplates, locale);
            if (legacyMatch.isPresent()) {
              return EmailTemplateMapper.toDTO(legacyMatch.get());
            }
          }

          // 4. Default identifier della categoria
          EmailTemplateCategory catEnum = EmailTemplateCategory.fromCode(effectiveCategory);
          List<EmailTemplate> defaultIdTemplates =
              emailRepository.findByIdentifier(catEnum.getDefaultIdentifier()).orElse(List.of());
          if (!defaultIdTemplates.isEmpty()) {
            Optional<EmailTemplate> defMatch = resolveTemplateFromList(defaultIdTemplates, locale);
            if (defMatch.isPresent()) {
              return EmailTemplateMapper.toDTO(defMatch.get());
            }
          }

          // 5. Fallback retrocompatibilita: se RESET_PASSWORD verifica anche legacy
          // ACCESS_SPHERE_CODE_RESET_PASSWORD
          if (catEnum == EmailTemplateCategory.RESET_PASSWORD) {
            List<EmailTemplate> legacyOldTemplates =
                emailRepository
                    .findByIdentifier("ACCESS_SPHERE_CODE_RESET_PASSWORD")
                    .orElse(List.of());
            if (!legacyOldTemplates.isEmpty()) {
              Optional<EmailTemplate> oldMatch =
                  resolveTemplateFromList(legacyOldTemplates, locale);
              if (oldMatch.isPresent()) {
                return EmailTemplateMapper.toDTO(oldMatch.get());
              }
            }
          }

          LOG.error(
              "No email template found for category=[{}], locale=[{}], clientId=[{}]",
              category,
              locale,
              clientId);
          throw new ResponseStatusException(
              HttpStatus.NOT_FOUND,
              "Template email non trovato per la categoria: " + effectiveCategory);
        });
  }

  private Optional<EmailTemplate> resolveTemplateFromList(
      List<EmailTemplate> templates, String locale) {
    if (templates == null || templates.isEmpty()) return Optional.empty();

    // 1. Corrispondenza esatta di locale
    if (!ObjectToolkit.isNullOrEmpty(locale)) {
      Optional<EmailTemplate> exact =
          templates.stream().filter(t -> locale.equalsIgnoreCase(t.getLocale())).findFirst();
      if (exact.isPresent()) return exact;

      // 2. Corrispondenza per prefisso lingua (es. "it" per "it-IT")
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

    // 3. Fallback su locale 'default' o nullo/vuoto
    Optional<EmailTemplate> defaultMatch =
        templates.stream()
            .filter(
                t ->
                    ObjectToolkit.isNullOrEmpty(t.getLocale())
                        || "default".equalsIgnoreCase(t.getLocale()))
            .findFirst();
    if (defaultMatch.isPresent()) return defaultMatch;

    // 4. Primo template disponibile
    return Optional.of(templates.get(0));
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateResponse> saveTemplate(EmailTemplateResponse dto) {
    return Mono.fromCallable(
        () -> {
          // Normalizzazione Categoria
          String category =
              (dto.getCategory() != null && !dto.getCategory().trim().isEmpty())
                  ? dto.getCategory().trim().toUpperCase()
                  : "RESET_PASSWORD";
          dto.setCategory(category);

          // Normalizzazione Identifier (se non specificato, assegna default della categoria)
          EmailTemplateCategory catEnum = EmailTemplateCategory.fromCode(category);
          if (dto.getIdentifier() == null || dto.getIdentifier().trim().isEmpty()) {
            dto.setIdentifier(catEnum.getDefaultIdentifier());
          } else {
            dto.setIdentifier(dto.getIdentifier().trim());
          }

          // Normalizzazione Client ID (se vuoto o "GLOBAL", diventa null per indicare template
          // Globale)
          String clientId =
              (dto.getClientId() != null
                      && !dto.getClientId().trim().isEmpty()
                      && !"GLOBAL".equalsIgnoreCase(dto.getClientId().trim()))
                  ? dto.getClientId().trim()
                  : null;
          dto.setClientId(clientId);

          // Normalizzazione Locale (se vuoto o "ALL"/"GENERIC", diventa null per indicare lingua
          // Globale)
          String locale =
              (dto.getLocale() != null
                      && !dto.getLocale().trim().isEmpty()
                      && !"GENERIC".equalsIgnoreCase(dto.getLocale().trim())
                      && !"ALL".equalsIgnoreCase(dto.getLocale().trim()))
                  ? dto.getLocale().trim()
                  : null;
          dto.setLocale(locale);

          // Controllo rigoroso di unicita:
          // Non possono esistere due template con stessa Categoria, stesso ClientId e stessa Lingua
          List<EmailTemplate> conflicts =
              emailRepository
                  .findConflictingTemplates(clientId, category, locale, dto.getId())
                  .orElse(List.of());
          if (!conflicts.isEmpty()) {
            String clientDesc =
                clientId != null ? ("Client [" + clientId + "]") : "Global (Access Sphere Default)";
            String localeDesc = locale != null ? ("Locale [" + locale + "]") : "Global / Fallback";
            String errorMsg =
                String.format(
                    "An email template already exists for category [%s], %s and %s.",
                    category, clientDesc, localeDesc);
            LOG.warn("Email template duplicate conflict: {}", errorMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
          }

          EmailTemplate model;
          if (dto.getId() != null) {
            model =
                emailRepository
                    .findById(dto.getId())
                    .orElseThrow(
                        () ->
                            new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Email template with ID " + dto.getId() + " not found"));
            EmailTemplateMapper.updateModel(model, dto);
          } else {
            model = EmailTemplateMapper.toModel(dto);
          }
          EmailTemplate saved = emailRepository.save(model);
          LOG.info(
              "Email template successfully saved: ID=[{}], category=[{}], clientId=[{}], locale=[{}]",
              saved.getId(),
              saved.getCategory(),
              saved.getClientId(),
              saved.getLocale());
          return EmailTemplateMapper.toDTO(saved);
        });
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> deleteTemplate(Long id) {
    return Mono.fromCallable(
            () -> {
              if (!emailRepository.existsById(id)) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Email template with ID " + id + " not found");
              }
              emailRepository.deleteById(id);
              LOG.info("Email template successfully deleted: ID=[{}]", id);
              return true;
            })
        .then();
  }

  /**
   * Invia un'email di prova compilata direttamente dall'interfaccia di amministrazione. Esegue la
   * sostituzione dei segnaposto {{KEY}} con i valori specificati nella richiesta.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendTestEmail(EmailSendTestRequest request) {
    return Mono.fromCallable(
            () -> {
              String rawHtml;
              String subject = request.getSubject();

              if (!ObjectToolkit.isNullOrEmpty(request.getCustomHtml())) {
                rawHtml = request.getCustomHtml();
                if (ObjectToolkit.isNullOrEmpty(subject)) subject = "Test Email - Access Sphere";
              } else if (request.getTemplateId() != null) {
                EmailTemplate model =
                    emailRepository
                        .findById(request.getTemplateId())
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Template ID " + request.getTemplateId() + " not found"));
                rawHtml = model.getTemplate();
                if (ObjectToolkit.isNullOrEmpty(subject)) subject = model.getSubject();
              } else if (!ObjectToolkit.isNullOrEmpty(request.getIdentifier())) {
                List<EmailTemplate> list =
                    emailRepository.findByIdentifier(request.getIdentifier()).orElse(List.of());
                if (list.isEmpty()) {
                  throw new ResponseStatusException(
                      HttpStatus.NOT_FOUND,
                      "Template identifier " + request.getIdentifier() + " not found");
                }
                EmailTemplate model = list.get(0);
                rawHtml = model.getTemplate();
                if (ObjectToolkit.isNullOrEmpty(subject)) subject = model.getSubject();
              } else {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A templateId, identifier or customHtml is required");
              }

              // Sostituzione segnaposto {{CHIAVE}}
              Map<String, String> params =
                  request.getParams() != null ? request.getParams() : new HashMap<>();
              String renderedHtml = rawHtml;
              for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                  renderedHtml =
                      renderedHtml.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
              }

              return EmailContent.builder()
                  .to(request.getTo())
                  .subject(subject)
                  .text(renderedHtml)
                  .sentDate(new Date())
                  .build();
            })
        .flatMap(emailContent -> emailSender.sendEmail(emailContent));
  }
}
