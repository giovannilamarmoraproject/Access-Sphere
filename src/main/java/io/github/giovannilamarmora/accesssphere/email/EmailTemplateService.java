package io.github.giovannilamarmora.accesssphere.email;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderClient;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.email.model.EmailTemplateCategory;
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
 * Service per la gestione dei Template Email salvati a Database. Permette operazioni CRUD sui
 * template, risoluzione multilingua flessibile (supporta sia template localizzati che template
 * unici validi per tutte le lingue), rendering dinamico con sostituzione dei segnaposto e invio
 * diretto di email di test tramite EmailSenderClient.
 */
@Service
@Logged
@RequiredArgsConstructor
public class EmailTemplateService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IEmailTemplateDAO emailTemplateDAO;
  @Autowired private EmailSenderClient emailSenderClient;
  @Autowired private TechUserService techUserService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<EmailTemplateDTO>> getAllTemplates() {
    return Mono.fromCallable(
        () -> {
          List<EmailTemplateEntity> entities = emailTemplateDAO.findAllByOrderByCreationDateDesc();
          return entities.stream().map(EmailTemplateMapper::toDTO).toList();
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateDTO> getTemplateById(Long id) {
    return Mono.fromCallable(
        () -> {
          EmailTemplateEntity entity =
              emailTemplateDAO
                  .findById(id)
                  .orElseThrow(
                      () ->
                          new ResponseStatusException(
                              HttpStatus.NOT_FOUND, "Email template with ID " + id + " not found"));
          return EmailTemplateMapper.toDTO(entity);
        });
  }

  /**
   * Risolve il template email dato il suo identificatore e il locale richiesto. Strategia di
   * fallback intelligente: 1. Cerca per identificatore e locale esatto (es. "it-IT" o "en-GB"). 2.
   * Se non trovato, cerca per prefisso lingua (es. "it" o "en"). 3. Se non trovato, cerca per
   * template con locale nullo, "default" o generico. 4. Se ancora non trovato, restituisce il primo
   * template disponibile per quell'identificatore. Questo garantisce che se l'amministratore
   * configura il template in una sola lingua, tale template viene comunque utilizzato senza
   * generare errori.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateDTO> getTemplateByIdentifierAndLocale(String identifier, String locale) {
    return Mono.fromCallable(
        () -> {
          LOG.info(
              "Searching for email template for identifier=[{}] and locale=[{}]",
              identifier,
              locale);
          List<EmailTemplateEntity> templates = emailTemplateDAO.findByIdentifier(identifier);

          if (templates.isEmpty()) {
            LOG.error("No email template found for identifier=[{}]", identifier);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Email template not found for identifier: " + identifier);
          }

          // 1. Corrispondenza esatta di locale
          if (!ObjectToolkit.isNullOrEmpty(locale)) {
            Optional<EmailTemplateEntity> exact =
                templates.stream().filter(t -> locale.equalsIgnoreCase(t.getLocale())).findFirst();
            if (exact.isPresent()) {
              return EmailTemplateMapper.toDTO(exact.get());
            }

            // 2. Corrispondenza per prefisso lingua (es. "it" per "it-IT")
            String langPrefix = locale.contains("-") ? locale.split("-")[0] : locale;
            Optional<EmailTemplateEntity> prefixMatch =
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
          Optional<EmailTemplateEntity> defaultMatch =
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
  public Mono<EmailTemplateDTO> getTemplateByCategoryLocaleAndClient(
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
            List<EmailTemplateEntity> clientTemplates =
                emailTemplateDAO.findByClientIdAndCategory(clientId.trim(), effectiveCategory);
            Optional<EmailTemplateEntity> clientMatch =
                resolveTemplateFromList(clientTemplates, locale);
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
          String techClientId = techUserService.getTechClientId();
          if (!techClientId.equalsIgnoreCase(clientId)) {
            List<EmailTemplateEntity> techClientTemplates =
                emailTemplateDAO.findByClientIdAndCategory(techClientId, effectiveCategory);
            Optional<EmailTemplateEntity> techMatch =
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
          List<EmailTemplateEntity> globalTemplates =
              emailTemplateDAO.findByClientIdIsNullAndCategory(effectiveCategory);
          Optional<EmailTemplateEntity> globalMatch =
              resolveTemplateFromList(globalTemplates, locale);
          if (globalMatch.isPresent()) {
            LOG.info(
                "Global Access Sphere Template found for category=[{}], ID=[{}]",
                effectiveCategory,
                globalMatch.get().getId());
            return EmailTemplateMapper.toDTO(globalMatch.get());
          }

          // 3. Fallback per retrocompatibilita: ricerca per identifier globale esatto
          List<EmailTemplateEntity> legacyTemplates =
              emailTemplateDAO.findByIdentifier(effectiveCategory);
          if (!legacyTemplates.isEmpty()) {
            Optional<EmailTemplateEntity> legacyMatch =
                resolveTemplateFromList(legacyTemplates, locale);
            if (legacyMatch.isPresent()) {
              return EmailTemplateMapper.toDTO(legacyMatch.get());
            }
          }

          // 4. Default identifier della categoria
          EmailTemplateCategory catEnum = EmailTemplateCategory.fromCode(effectiveCategory);
          List<EmailTemplateEntity> defaultIdTemplates =
              emailTemplateDAO.findByIdentifier(catEnum.getDefaultIdentifier());
          if (!defaultIdTemplates.isEmpty()) {
            Optional<EmailTemplateEntity> defMatch =
                resolveTemplateFromList(defaultIdTemplates, locale);
            if (defMatch.isPresent()) {
              return EmailTemplateMapper.toDTO(defMatch.get());
            }
          }

          // 5. Fallback retrocompatibilita: se RESET_PASSWORD verifica anche legacy
          // ACCESS_SPHERE_CODE_RESET_PASSWORD
          if (catEnum == EmailTemplateCategory.RESET_PASSWORD) {
            List<EmailTemplateEntity> legacyOldTemplates =
                emailTemplateDAO.findByIdentifier("ACCESS_SPHERE_CODE_RESET_PASSWORD");
            if (!legacyOldTemplates.isEmpty()) {
              Optional<EmailTemplateEntity> oldMatch =
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

  private Optional<EmailTemplateEntity> resolveTemplateFromList(
      List<EmailTemplateEntity> templates, String locale) {
    if (templates == null || templates.isEmpty()) return Optional.empty();

    // 1. Corrispondenza esatta di locale
    if (!ObjectToolkit.isNullOrEmpty(locale)) {
      Optional<EmailTemplateEntity> exact =
          templates.stream().filter(t -> locale.equalsIgnoreCase(t.getLocale())).findFirst();
      if (exact.isPresent()) return exact;

      // 2. Corrispondenza per prefisso lingua (es. "it" per "it-IT")
      String langPrefix = locale.contains("-") ? locale.split("-")[0] : locale;
      Optional<EmailTemplateEntity> prefixMatch =
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
    Optional<EmailTemplateEntity> defaultMatch =
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
  public Mono<EmailTemplateDTO> saveTemplate(EmailTemplateDTO dto) {
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
          List<EmailTemplateEntity> conflicts =
              emailTemplateDAO.findConflictingTemplates(clientId, category, locale, dto.getId());
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

          EmailTemplateEntity entity;
          if (dto.getId() != null) {
            entity =
                emailTemplateDAO
                    .findById(dto.getId())
                    .orElseThrow(
                        () ->
                            new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Email template with ID " + dto.getId() + " not found"));
            EmailTemplateMapper.updateEntity(entity, dto);
          } else {
            entity = EmailTemplateMapper.toEntity(dto);
          }
          EmailTemplateEntity saved = emailTemplateDAO.save(entity);
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
              if (!emailTemplateDAO.existsById(id)) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Email template with ID " + id + " not found");
              }
              emailTemplateDAO.deleteById(id);
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
                EmailTemplateEntity entity =
                    emailTemplateDAO
                        .findById(request.getTemplateId())
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Template ID " + request.getTemplateId() + " not found"));
                rawHtml = entity.getTemplate();
                if (ObjectToolkit.isNullOrEmpty(subject)) subject = entity.getSubject();
              } else if (!ObjectToolkit.isNullOrEmpty(request.getIdentifier())) {
                List<EmailTemplateEntity> list =
                    emailTemplateDAO.findByIdentifier(request.getIdentifier());
                if (list.isEmpty()) {
                  throw new ResponseStatusException(
                      HttpStatus.NOT_FOUND,
                      "Template identifier " + request.getIdentifier() + " not found");
                }
                EmailTemplateEntity entity = list.get(0);
                rawHtml = entity.getTemplate();
                if (ObjectToolkit.isNullOrEmpty(subject)) subject = entity.getSubject();
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
        .flatMap(
            emailContent ->
                emailSenderClient
                    .sendEmail(emailContent)
                    .flatMap(
                        res -> {
                          if (res.getBody() == null) {
                            throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "The emailSender microservice did not return a valid response");
                          }
                          return Mono.just(res.getBody());
                        }));
  }
}
