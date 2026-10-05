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
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.*;
import java.util.stream.Collectors;
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

  @Autowired private EmailTemplateDomainService emailTemplateDomainService;
  @Autowired private EmailRepository emailRepository;
  @Autowired private EmailSender emailSender;
  @Autowired private AdminUserService techUserService;
  @Autowired private PermissionRoleManager permissionRoleManager;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<EmailTemplateResponse>> getAllTemplates() {
    SystemPermission emailPermission = SystemPermission.EMAILS_MANAGE;
    List<RoleDefinition> hasCorrectRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(emailPermission);

    return emailTemplateDomainService
        .getAllTemplates()
        .map(
            allTemplates -> {
              if (permissionRoleManager.hasAdminPermission(emailPermission)) {
                return allTemplates;
              }

              // Estrae i clientId per i quali l'utente possiede almeno un ruolo abilitato
              Set<String> authorizedClientIds =
                  hasCorrectRoles.stream()
                      .map(RoleDefinition::getClientId)
                      .filter(clientId -> !ObjectToolkit.isNullOrEmpty(clientId))
                      .map(String::toUpperCase)
                      .collect(Collectors.toSet());

              // Filtra la lista restituendo esclusivamente i client autorizzati
              return allTemplates.stream()
                  .filter(
                      emailTemplate ->
                          emailTemplate.getClientId() != null
                              && authorizedClientIds.contains(
                                  emailTemplate.getClientId().toUpperCase()))
                  .toList();
            })
        .map(EmailTemplateMapper::toResponseList);
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
              return EmailTemplateMapper.toResponse(clientMatch.get());
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
              return EmailTemplateMapper.toResponse(techMatch.get());
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
            return EmailTemplateMapper.toResponse(globalMatch.get());
          }

          // 3. Fallback per retrocompatibilita: ricerca per identifier globale esatto
          List<EmailTemplate> legacyTemplates =
              emailRepository.findByIdentifier(effectiveCategory).orElse(List.of());
          if (!legacyTemplates.isEmpty()) {
            Optional<EmailTemplate> legacyMatch = resolveTemplateFromList(legacyTemplates, locale);
            if (legacyMatch.isPresent()) {
              return EmailTemplateMapper.toResponse(legacyMatch.get());
            }
          }

          // 4. Default identifier della categoria
          EmailTemplateCategory catEnum = EmailTemplateCategory.fromCode(effectiveCategory);
          List<EmailTemplate> defaultIdTemplates =
              emailRepository.findByIdentifier(catEnum.getDefaultIdentifier()).orElse(List.of());
          if (!defaultIdTemplates.isEmpty()) {
            Optional<EmailTemplate> defMatch = resolveTemplateFromList(defaultIdTemplates, locale);
            if (defMatch.isPresent()) {
              return EmailTemplateMapper.toResponse(defMatch.get());
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
                return EmailTemplateMapper.toResponse(oldMatch.get());
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

  /**
   * Salva o aggiorna un template email a database. Delega all'overload con identificatore nullo per
   * le creazioni dirette.
   *
   * @param dto DTO del template inviato dal client
   * @return Mono con il DTO del template salvato
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateResponse> saveTemplate(EmailTemplateResponse dto) {
    return saveTemplate(null, dto);
  }

  /**
   * Salva o aggiorna un template email a database, supportando l'identificatore univoco da path
   * variable.
   *
   * <p>Flusso logico ed interconnessioni:
   *
   * <ul>
   *   <li>Risolve l'identificatore prioritario da path variable o dal payload DTO.
   *   <li>Normalizza la categoria, il client ID e il locale multilingua.
   *   <li>Cerca un template preesistente tramite {@link
   *       EmailRepository#findFirstByIdentifier(String)} (o fallback su ID numerico se fornito da
   *       client legacy).
   *   <li>Verifica l'assenza di conflitti di unicita sulla tripla (Categoria, ClientId, Lingua)
   *       escludendo l'ID del template in fase di aggiornamento.
   *   <li>Aggiorna il modello di dominio esistente o ne istanzia uno nuovo tramite {@link
   *       EmailTemplateMapper}.
   *   <li>Persiste a database tramite {@link EmailRepository#save(EmailTemplate)}.
   * </ul>
   *
   * @param pathIdentifier Identificatore univoco specificato come variabile di percorso REST
   *     (opzionale)
   * @param dto DTO contenente i dati del template da salvare o aggiornare
   * @return Mono contenente il DTO del template salvato con metadati aggiornati
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateResponse> saveTemplate(
      String pathIdentifier, EmailTemplateResponse dto) {
    return Mono.fromCallable(
        () -> {
          // Risoluzione identificatore prioritario (da path variable o da corpo richiesta)
          String lookupIdentifier =
              !ObjectToolkit.isNullOrEmpty(pathIdentifier)
                  ? pathIdentifier.trim()
                  : (dto.getIdentifier() != null ? dto.getIdentifier().trim() : null);

          // Normalizzazione Categoria
          String category =
              (dto.getCategory() != null && !dto.getCategory().trim().isEmpty())
                  ? dto.getCategory().trim().toUpperCase()
                  : "RESET_PASSWORD";
          dto.setCategory(category);

          // Normalizzazione Identifier (se non specificato, assegna identificatore predefinito
          // della categoria)
          EmailTemplateCategory catEnum = EmailTemplateCategory.fromCode(category);
          if (dto.getIdentifier() == null || dto.getIdentifier().trim().isEmpty()) {
            dto.setIdentifier(
                !ObjectToolkit.isNullOrEmpty(lookupIdentifier)
                    ? lookupIdentifier
                    : catEnum.getDefaultIdentifier());
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

          // Ricerca di un template preesistente tramite identifier (oppure fallback su ID per
          // retrocompatibilita)
          Optional<EmailTemplate> existingOpt = Optional.empty();
          if (!ObjectToolkit.isNullOrEmpty(lookupIdentifier)) {
            existingOpt = emailRepository.findFirstByIdentifier(lookupIdentifier);
          }
          if (existingOpt.isEmpty()
              && dto.getIdentifier() != null
              && !dto.getIdentifier().trim().isEmpty()) {
            existingOpt = emailRepository.findFirstByIdentifier(dto.getIdentifier().trim());
          }
          if (existingOpt.isEmpty() && dto.getId() != null) {
            existingOpt = emailRepository.findById(dto.getId());
          }

          // Risoluzione ID di esclusione per il controllo dei duplicati
          Long excludeId = existingOpt.map(EmailTemplate::getId).orElse(dto.getId());

          // Controllo rigoroso di unicita:
          // Non possono esistere due template con stessa Categoria, stesso ClientId e stessa Lingua
          List<EmailTemplate> conflicts =
              emailRepository
                  .findConflictingTemplates(clientId, category, locale, excludeId)
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

          // Se il template esiste gia, aggiorna i suoi campi mantenendo immutato l'ID relazionale
          EmailTemplate model;
          if (existingOpt.isPresent()) {
            model = existingOpt.get();
            EmailTemplateMapper.updateModel(model, dto);
          } else {
            model = EmailTemplateMapper.toModel(dto);
          }
          EmailTemplate saved = emailRepository.save(model);
          LOG.info(
              "Email template successfully saved: ID=[{}], identifier=[{}], category=[{}], clientId=[{}], locale=[{}]",
              saved.getId(),
              saved.getIdentifier(),
              saved.getCategory(),
              saved.getClientId(),
              saved.getLocale());
          return EmailTemplateMapper.toResponse(saved);
        });
  }

  /**
   * Elimina un template email dal database dato il suo identificatore univoco applicativo.
   *
   * @param identifier Identificatore del template da eliminare
   * @return Mono vuoto al completamento
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> deleteTemplate(String identifier) {
    return Mono.fromCallable(
            () -> {
              EmailTemplate template =
                  emailRepository
                      .findFirstByIdentifier(identifier)
                      .orElseThrow(
                          () ->
                              new ResponseStatusException(
                                  HttpStatus.NOT_FOUND,
                                  "Email template with identifier " + identifier + " not found"));
              emailRepository.deleteById(template.getId());
              LOG.info(
                  "Email template successfully deleted: identifier=[{}], ID=[{}]",
                  identifier,
                  template.getId());
              return true;
            })
        .then();
  }

  /**
   * Invia un'email di prova compilata direttamente dall'interfaccia di amministrazione. Risolve il
   * template via identificatore univoco (o via ID legacy) e sostituisce i segnaposto {{KEY}} con i
   * parametri specificati.
   *
   * @param request DTO contenente destinatario, identificatore template e mappa parametri
   * @return Mono contenente l'esito della spedizione tramite {@link EmailSender}
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
              } else if (!ObjectToolkit.isNullOrEmpty(request.getIdentifier())) {
                EmailTemplate model =
                    emailRepository
                        .findFirstByIdentifier(request.getIdentifier())
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Template identifier "
                                        + request.getIdentifier()
                                        + " not found"));
                rawHtml = model.getTemplate();
                if (ObjectToolkit.isNullOrEmpty(subject)) subject = model.getSubject();
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
              } else {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "An identifier or customHtml is required");
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
