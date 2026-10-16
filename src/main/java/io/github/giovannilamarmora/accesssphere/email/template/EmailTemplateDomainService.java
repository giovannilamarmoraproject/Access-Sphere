package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.email.EmailUtils;
import io.github.giovannilamarmora.accesssphere.email.exception.EmailException;
import io.github.giovannilamarmora.accesssphere.email.template.database.EmailRepository;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Domain Service puro per l'aggregato {@link EmailTemplate}.
 *
 * <p>Responsabilita:
 *
 * <ul>
 *   <li>Gestisce la logica di dominio, la risoluzione a cascata multilingua/multi-tenant e la
 *       persistenza relazionale dei Template Email tramite {@link EmailRepository}.
 *   <li>Completamente disaccoppiato dai controlli di sessione HTTP o permessi operatore,
 *       permettendo l'invocazione diretta da flussi di sistema (es. {@code UserAccountService}).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String EMAIL_DOMAIN_LOG = "Email Domain";

  @Autowired private EmailRepository emailRepository;
  @Autowired private AdminUserService techUserService;

  /**
   * Recupera la lista completa di tutti i Template Email registrati a database.
   *
   * @return {@link Mono} con la lista di {@link EmailTemplate}.
   */
  public Mono<List<EmailTemplate>> getAllTemplates() {
    LOG.info("📋 {} Retrieving all email templates from Database", EMAIL_DOMAIN_LOG);
    List<EmailTemplate> models = emailRepository.findAll().orElse(List.of());
    LOG.info("📋 {} Found {} email templates in Database", EMAIL_DOMAIN_LOG, models.size());
    return Mono.just(models);
  }

  /**
   * Recupera un singolo Template Email tramite il suo identificatore univoco.
   *
   * @param identifier Identificatore univoco del template.
   * @return {@link Mono} con il modello {@link EmailTemplate}.
   */
  public Mono<EmailTemplate> getTemplateByIdentifier(String identifier) {
    LOG.info("🔍 {} Searching for email template with identifier: {}", EMAIL_DOMAIN_LOG, identifier);
    EmailTemplate template =
        emailRepository
            .findFirstByIdentifier(identifier)
            .orElseThrow(
                () -> {
                  LOG.error(
                      "❌ {} Email template with identifier [{}] not found in Database",
                      EMAIL_DOMAIN_LOG,
                      identifier);
                  return new EmailException(
                      ExceptionMap.ERR_OAUTH_404,
                      "Email template with identifier " + identifier + " not found");
                });
    return Mono.just(template);
  }

  /**
   * Recupera un singolo Template Email tramite il suo ID primario.
   *
   * @param id ID primario del template.
   * @return {@link Mono} con il modello {@link EmailTemplate}.
   */
  public Mono<EmailTemplate> getTemplateById(Long id) {
    LOG.info("🔍 {} Searching for email template with ID: {}", EMAIL_DOMAIN_LOG, id);
    EmailTemplate template =
        emailRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  LOG.error(
                      "❌ {} Email template with ID [{}] not found in Database",
                      EMAIL_DOMAIN_LOG,
                      id);
                  return new EmailException(
                      ExceptionMap.ERR_OAUTH_404, "Email template with ID " + id + " not found");
                });
    return Mono.just(template);
  }

  /**
   * Risoluzione avanzata del template email basata su Categoria, Locale e Client (Multi-Tenant).
   * Risolve a cascata: 1. Client specifico; 2. Client tecnico di sistema; 3. Template globale
   * (clientId nullo); 4. Fallback per identifier diretto o predefinito della categoria.
   *
   * @param category Categoria o identificatore del template.
   * @param locale Codice lingua richiesto.
   * @param clientId Client ID OAuth2 opzionale.
   * @return {@link Mono} con il {@link EmailTemplate} risolto.
   */
  public Mono<EmailTemplate> getTemplateByCategoryLocaleAndClient(
      String category, String locale, String clientId) {
    String effectiveCategory = EmailUtils.normalizeCategory(category);
    LOG.info(
        "🔍 {} Searching template for category=[{}], locale=[{}], clientId=[{}]",
        EMAIL_DOMAIN_LOG,
        effectiveCategory,
        locale,
        clientId);

    // 1. Ricerca per Client dedicato
    if (!ObjectToolkit.isNullOrEmpty(clientId)) {
      Optional<EmailTemplate> clientMatch =
          findAndResolveByClient(clientId.trim(), effectiveCategory, locale);
      if (clientMatch.isPresent()) {
        LOG.info(
            "✅ {} Template found for dedicated Client=[{}], ID=[{}]",
            EMAIL_DOMAIN_LOG,
            clientId,
            clientMatch.get().getId());
        return Mono.just(clientMatch.get());
      }
      LOG.warn(
          "⚠️ {} No template found for clientId=[{}] and category=[{}]. Falling back to default",
          EMAIL_DOMAIN_LOG,
          clientId,
          effectiveCategory);
    }

    // 2. Ricerca per Client Tecnico predefinito
    String techClientId = techUserService.getAdminClientId();
    if (!techClientId.equalsIgnoreCase(clientId)) {
      Optional<EmailTemplate> techMatch =
          findAndResolveByClient(techClientId, effectiveCategory, locale);
      if (techMatch.isPresent()) {
        LOG.info(
            "✅ {} Template found for Tech Client [{}] and category=[{}], ID=[{}]",
            EMAIL_DOMAIN_LOG,
            techClientId,
            effectiveCategory,
            techMatch.get().getId());
        return Mono.just(techMatch.get());
      }
    }

    // 3. Ricerca tra i template Globali (clientId IS NULL)
    List<EmailTemplate> globalTemplates =
        emailRepository.findByClientIdIsNullAndCategory(effectiveCategory).orElse(List.of());
    Optional<EmailTemplate> globalMatch =
        EmailUtils.resolveTemplateByLocale(globalTemplates, locale);
    if (globalMatch.isPresent()) {
      LOG.info(
          "✅ {} Global Template found for category=[{}], ID=[{}]",
          EMAIL_DOMAIN_LOG,
          effectiveCategory,
          globalMatch.get().getId());
      return Mono.just(globalMatch.get());
    }

    // 4. Fallback per identifier diretto, default della categoria o legacy
    Optional<EmailTemplate> fallbackMatch =
        resolveFallbackByIdentifier(effectiveCategory, locale);
    if (fallbackMatch.isPresent()) {
      return Mono.just(fallbackMatch.get());
    }

    LOG.error(
        "❌ {} No email template found for category=[{}], locale=[{}], clientId=[{}]",
        EMAIL_DOMAIN_LOG,
        category,
        locale,
        clientId);
    throw new EmailException(
        ExceptionMap.ERR_OAUTH_404, "Email template not found for category: " + effectiveCategory);
  }

  /**
   * Inserisce o aggiorna un Template Email a database previa validazione e controllo duplicati.
   *
   * @param emailTemplate Modello di dominio da salvare.
   * @return {@link Mono} con il {@link EmailTemplate} salvato.
   */
  @Transactional
  public Mono<EmailTemplate> saveTemplate(EmailTemplate emailTemplate) {
    return saveOrUpdateTemplate(null, emailTemplate);
  }

  /**
   * Aggiorna un Template Email esistente identificato da path variable (o ne crea uno nuovo se non
   * censito).
   *
   * @param pathIdentifier Identificatore univoco da path variable.
   * @param emailTemplate Modello di dominio con i dati aggiornati.
   * @return {@link Mono} con il {@link EmailTemplate} aggiornato.
   */
  @Transactional
  public Mono<EmailTemplate> updateTemplate(String pathIdentifier, EmailTemplate emailTemplate) {
    return saveOrUpdateTemplate(pathIdentifier, emailTemplate);
  }

  /**
   * Elimina definitivamente un Template Email dal database tramite il suo identificatore univoco.
   *
   * @param identifier Identificatore del template da rimuovere.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteTemplate(String identifier) {
    LOG.info("🗑️ {} Removing email template from Database: {}", EMAIL_DOMAIN_LOG, identifier);
    EmailTemplate existing =
        emailRepository
            .findFirstByIdentifier(identifier)
            .orElseThrow(
                () -> {
                  LOG.error(
                      "❌ {} Email template with identifier [{}] does not exist",
                      EMAIL_DOMAIN_LOG,
                      identifier);
                  return new EmailException(
                      ExceptionMap.ERR_OAUTH_404,
                      "Email template with identifier " + identifier + " not found");
                });

    emailRepository.deleteById(existing.getId());
    LOG.info(
        "✅ {} Email template successfully deleted: identifier=[{}], ID=[{}]",
        EMAIL_DOMAIN_LOG,
        identifier,
        existing.getId());
  }

  private Mono<EmailTemplate> saveOrUpdateTemplate(
      String pathIdentifier, EmailTemplate emailTemplate) {
    EmailTemplateValidator.validateTemplate(emailTemplate);
    EmailUtils.normalizeTemplate(emailTemplate, pathIdentifier);

    Optional<EmailTemplate> existingOpt = findExistingTemplate(pathIdentifier, emailTemplate);
    Long excludeId = existingOpt.map(EmailTemplate::getId).orElse(emailTemplate.getId());

    List<EmailTemplate> conflicts =
        emailRepository
            .findConflictingTemplates(
                emailTemplate.getClientId(),
                emailTemplate.getCategory(),
                emailTemplate.getLocale(),
                excludeId)
            .orElse(List.of());
    EmailTemplateValidator.validateNoDuplicates(
        conflicts,
        emailTemplate.getCategory(),
        emailTemplate.getClientId(),
        emailTemplate.getLocale());

    EmailTemplate modelToSave;
    if (existingOpt.isPresent()) {
      modelToSave = existingOpt.get();
      EmailTemplateMapper.updateModel(modelToSave, emailTemplate);
    } else {
      modelToSave = emailTemplate;
    }

    EmailTemplate saved = emailRepository.save(modelToSave);
    LOG.info(
        "✅ {} Email template saved: ID=[{}], identifier=[{}], category=[{}], clientId=[{}], locale=[{}]",
        EMAIL_DOMAIN_LOG,
        saved.getId(),
        saved.getIdentifier(),
        saved.getCategory(),
        saved.getClientId(),
        saved.getLocale());
    return Mono.just(saved);
  }

  private Optional<EmailTemplate> findExistingTemplate(
      String pathIdentifier, EmailTemplate emailTemplate) {
    if (!ObjectToolkit.isNullOrEmpty(pathIdentifier)) {
      Optional<EmailTemplate> byPath = emailRepository.findFirstByIdentifier(pathIdentifier.trim());
      if (byPath.isPresent()) return byPath;
    }
    if (!ObjectToolkit.isNullOrEmpty(emailTemplate.getIdentifier())) {
      Optional<EmailTemplate> byDto =
          emailRepository.findFirstByIdentifier(emailTemplate.getIdentifier().trim());
      if (byDto.isPresent()) return byDto;
    }
    if (emailTemplate.getId() != null) {
      return emailRepository.findById(emailTemplate.getId());
    }
    return Optional.empty();
  }

  private Optional<EmailTemplate> findAndResolveByClient(
      String clientId, String category, String locale) {
    List<EmailTemplate> templates =
        emailRepository.findByClientIdAndCategory(clientId, category).orElse(List.of());
    return EmailUtils.resolveTemplateByLocale(templates, locale);
  }

  private Optional<EmailTemplate> resolveFallbackByIdentifier(
      String effectiveCategory, String locale) {
    Optional<EmailTemplate> directMatch =
        EmailUtils.resolveTemplateByLocale(
            emailRepository.findByIdentifier(effectiveCategory).orElse(List.of()), locale);
    if (directMatch.isPresent()) return directMatch;

    EmailTemplateCategory catEnum = EmailTemplateCategory.fromCode(effectiveCategory);
    Optional<EmailTemplate> defaultIdMatch =
        EmailUtils.resolveTemplateByLocale(
            emailRepository.findByIdentifier(catEnum.getDefaultIdentifier()).orElse(List.of()),
            locale);
    if (defaultIdMatch.isPresent()) return defaultIdMatch;

    if (!ObjectToolkit.isNullOrEmpty(catEnum.getLegacyIdentifier())) {
      return EmailUtils.resolveTemplateByLocale(
          emailRepository.findByIdentifier(catEnum.getLegacyIdentifier()).orElse(List.of()),
          locale);
    }
    return Optional.empty();
  }
}
