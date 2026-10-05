package io.github.giovannilamarmora.accesssphere.email.template.database;

import io.github.giovannilamarmora.accesssphere.email.template.EmailTemplateMapper;
import io.github.giovannilamarmora.accesssphere.email.template.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository di dominio per l'aggregato Template Email (EmailTemplate).
 * <p>
 * Responsabilita architetturali:
 * <ul>
 *   <li>Incapsula l'accesso a basso livello a {@link IEmailTemplateDAO}.</li>
 *   <li>Esegue lo switch e la conversione bidirezionale Entity &lt;-&gt; Domain Model tramite {@link EmailTemplateMapper}.</li>
 *   <li>Isola il Service di business (EmailTemplateService) e i task di seeding (ProjectInitializerService) dalla persistenza JPA.</li>
 *   <li>Rappresenta l'equivalente architetturale di OAuth2ClientRepository nel modulo Client.</li>
 * </ul>
 */
@Repository
public class EmailRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IEmailTemplateDAO emailTemplateDAO;

  /**
   * Recupera tutti i template email ordinati per data di creazione decrescente.
   *
   * @return Optional contenente la lista di modelli {@link EmailTemplate}, o Optional.empty() se vuota.
   */
  public Optional<List<EmailTemplate>> findAll() {
    LOG.debug("📧 Getting all Email Templates ordered by creation date desc");
    List<EmailTemplateEntity> getEntityList = emailTemplateDAO.findAllByOrderByCreationDateDesc();

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("❌ Email Templates not found in Database");
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} Email Templates in Database", getEntityList.size());
    return Optional.of(EmailTemplateMapper.toModelList(getEntityList));
  }

  /**
   * Recupera un template email dato il suo identificativo primario di database (ID).
   *
   * @param id Identificativo ID del template
   * @return Optional con il modello {@link EmailTemplate}, oppure Optional.empty() se non presente.
   */
  public Optional<EmailTemplate> findById(Long id) {
    LOG.debug("📧 Getting Email Template for ID: {}", id);
    Optional<EmailTemplateEntity> entityOpt = emailTemplateDAO.findById(id);

    if (entityOpt.isEmpty()) {
      LOG.warn("❌ Email Template with ID: {} not found in Database", id);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found Email Template with ID: {} in Database", id);
    return Optional.ofNullable(EmailTemplateMapper.toModel(entityOpt.get()));
  }

  /**
   * Recupera i template email associati a uno specifico identificatore UUID o codice applicativo.
   *
   * @param identifier Identificatore o UUID del template (es. reset password UUID)
   * @return Optional con la lista di modelli {@link EmailTemplate}, oppure Optional.empty() se non presenti.
   */
  public Optional<List<EmailTemplate>> findByIdentifier(String identifier) {
    LOG.debug("📧 Getting Email Templates for identifier: {}", identifier);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByIdentifier(identifier);

    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Email Templates for identifier: {} not found in Database", identifier);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} Email Templates for identifier: {} in Database", entities.size(), identifier);
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  /**
   * Recupera un template email per identificatore e locale esatto.
   *
   * @param identifier Identificatore UUID o codice del template
   * @param locale Codice lingua (es. 'it', 'en')
   * @return Optional con il modello {@link EmailTemplate}, oppure Optional.empty()
   */
  public Optional<EmailTemplate> findByIdentifierAndLocale(String identifier, String locale) {
    LOG.debug("📧 Getting Email Template for identifier: {} and locale: {}", identifier, locale);
    Optional<EmailTemplateEntity> entityOpt = emailTemplateDAO.findByIdentifierAndLocale(identifier, locale);

    if (entityOpt.isEmpty()) {
      LOG.warn("❌ Email Template for identifier: {} and locale: {} not found in Database", identifier, locale);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found Email Template for identifier: {} and locale: {} in Database", identifier, locale);
    return Optional.ofNullable(EmailTemplateMapper.toModel(entityOpt.get()));
  }

  /**
   * Recupera i template email appartenenti a uno specifico client OAuth2 e categoria.
   *
   * @param clientId Client ID OAuth2
   * @param category Categoria del template (es. RESET_PASSWORD, WELCOME_USER)
   * @return Optional con la lista di modelli {@link EmailTemplate}
   */
  public Optional<List<EmailTemplate>> findByClientIdAndCategory(String clientId, String category) {
    LOG.debug("📧 Getting Email Templates for clientId: {} and category: {}", clientId, category);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByClientIdAndCategory(clientId, category);

    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Email Templates for clientId: {} and category: {} not found in Database", clientId, category);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} Email Templates for clientId: {} and category: {} in Database", entities.size(), clientId, category);
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  /**
   * Recupera i template email globali di sistema (con clientId nullo) per una data categoria.
   *
   * @param category Categoria del template
   * @return Optional con la lista di modelli globali {@link EmailTemplate}
   */
  public Optional<List<EmailTemplate>> findByClientIdIsNullAndCategory(String category) {
    LOG.debug("📧 Getting global Email Templates for category: {}", category);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByClientIdIsNullAndCategory(category);

    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Global Email Templates for category: {} not found in Database", category);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} global Email Templates for category: {} in Database", entities.size(), category);
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  /**
   * Recupera i template email associati a uno specifico client OAuth2.
   *
   * @param clientId Client ID OAuth2
   * @return Optional con la lista di modelli {@link EmailTemplate}
   */
  public Optional<List<EmailTemplate>> findByClientId(String clientId) {
    LOG.debug("📧 Getting Email Templates for clientId: {}", clientId);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByClientId(clientId);

    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Email Templates for clientId: {} not found in Database", clientId);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} Email Templates for clientId: {} in Database", entities.size(), clientId);
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  /**
   * Recupera tutti i template registrati per una determinata categoria.
   *
   * @param category Categoria applicativa (es. RESET_PASSWORD, WELCOME_USER)
   * @return Optional con la lista di modelli {@link EmailTemplate}
   */
  public Optional<List<EmailTemplate>> findByCategory(String category) {
    LOG.debug("📧 Getting Email Templates for category: {}", category);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByCategory(category);

    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Email Templates for category: {} not found in Database", category);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} Email Templates for category: {} in Database", entities.size(), category);
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  /**
   * Verifica se esistono template in conflitto per la combinazione (ClientId, Categoria, Locale)
   * escludendo un eventuale ID specificato in fase di modifica.
   *
   * @param clientId Client ID
   * @param category Categoria
   * @param locale Locale/Lingua
   * @param excludeId ID record da escludere
   * @return Optional con la lista di template in conflitto
   */
  public Optional<List<EmailTemplate>> findConflictingTemplates(
      String clientId, String category, String locale, Long excludeId) {
    LOG.debug(
        "📧 Checking conflicting Email Templates for clientId: {}, category: {}, locale: {}, excludeId: {}",
        clientId,
        category,
        locale,
        excludeId);
    List<EmailTemplateEntity> conflicts =
        emailTemplateDAO.findConflictingTemplates(clientId, category, locale, excludeId);

    if (ObjectToolkit.isNullOrEmpty(conflicts)) {
      return Optional.empty();
    }

    return Optional.of(EmailTemplateMapper.toModelList(conflicts));
  }

  /**
   * Verifica l'esistenza di un template email dato il suo ID primario.
   *
   * @param id ID del template
   * @return true se presente, false altrimenti
   */
  public Boolean existsById(Long id) {
    LOG.debug("📧 Checking if Email Template with ID {} exists", id);
    return emailTemplateDAO.existsById(id);
  }

  /**
   * Salva o aggiorna un template email persistendolo a database previa conversione Model -&gt; Entity.
   *
   * @param emailTemplate Modello di dominio da persistere
   * @return Modello di dominio aggiornato con ID generato
   */
  public EmailTemplate save(EmailTemplate emailTemplate) {
    LOG.debug("📧 Saving Email Template for identifier: {}", emailTemplate.getIdentifier());
    EmailTemplateEntity mapEntity = EmailTemplateMapper.toEntity(emailTemplate);

    EmailTemplateEntity savedEntity = emailTemplateDAO.save(mapEntity);

    LOG.debug("⚖️ Saved Email Template with ID {} in Database", savedEntity.getId());
    return EmailTemplateMapper.toModel(savedEntity);
  }

  /**
   * Elimina un template email dal database per ID.
   *
   * @param id ID del template da eliminare
   */
  public void deleteById(Long id) {
    LOG.debug("📧 Deleting Email Template with ID: {}", id);
    emailTemplateDAO.deleteById(id);
    LOG.debug("⚖️ Deleted Email Template with ID: {} in Database", id);
  }
}
