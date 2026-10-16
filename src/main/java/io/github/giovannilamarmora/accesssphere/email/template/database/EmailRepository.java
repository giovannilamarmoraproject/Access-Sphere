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
 * Repository di dominio per l'aggregato {@link EmailTemplate}. Incapsula l'accesso a {@link
 * IEmailTemplateDAO} e la mappatura Entity <-> Model tramite {@link EmailTemplateMapper}.
 */
@Repository
public class EmailRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IEmailTemplateDAO emailTemplateDAO;

  public Optional<List<EmailTemplate>> findAll() {
    LOG.debug("📧 Getting all Email Templates ordered by creation date desc");
    List<EmailTemplateEntity> entities = emailTemplateDAO.findAllByOrderByCreationDateDesc();
    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Email Templates not found in Database");
      return Optional.empty();
    }
    LOG.debug("⚖️ Found {} Email Templates in Database", entities.size());
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  public Optional<EmailTemplate> findById(Long id) {
    LOG.debug("📧 Getting Email Template for ID: {}", id);
    Optional<EmailTemplateEntity> entityOpt = emailTemplateDAO.findById(id);
    if (entityOpt.isEmpty()) {
      LOG.warn("❌ Email Template with ID: {} not found in Database", id);
      return Optional.empty();
    }
    return Optional.ofNullable(EmailTemplateMapper.toModel(entityOpt.get()));
  }

  public Optional<List<EmailTemplate>> findByIdentifier(String identifier) {
    LOG.debug("📧 Getting Email Templates for identifier: {}", identifier);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByIdentifier(identifier);
    if (ObjectToolkit.isNullOrEmpty(entities)) {
      return Optional.empty();
    }
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  public Optional<EmailTemplate> findFirstByIdentifier(String identifier) {
    LOG.debug("📧 Getting first Email Template for identifier: {}", identifier);
    Optional<EmailTemplateEntity> entityOpt = emailTemplateDAO.findFirstByIdentifier(identifier);
    if (entityOpt.isEmpty()) {
      LOG.warn("❌ Email Template for identifier: {} not found in Database", identifier);
      return Optional.empty();
    }
    return Optional.ofNullable(EmailTemplateMapper.toModel(entityOpt.get()));
  }

  public Optional<List<EmailTemplate>> findByClientIdAndCategory(String clientId, String category) {
    LOG.debug("📧 Getting Email Templates for clientId: {} and category: {}", clientId, category);
    List<EmailTemplateEntity> entities =
        emailTemplateDAO.findByClientIdAndCategory(clientId, category);
    if (ObjectToolkit.isNullOrEmpty(entities)) {
      return Optional.empty();
    }
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

  public Optional<List<EmailTemplate>> findByClientIdIsNullAndCategory(String category) {
    LOG.debug("📧 Getting global Email Templates for category: {}", category);
    List<EmailTemplateEntity> entities = emailTemplateDAO.findByClientIdIsNullAndCategory(category);
    if (ObjectToolkit.isNullOrEmpty(entities)) {
      return Optional.empty();
    }
    return Optional.of(EmailTemplateMapper.toModelList(entities));
  }

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

  public EmailTemplate save(EmailTemplate emailTemplate) {
    LOG.debug("📧 Saving Email Template for identifier: {}", emailTemplate.getIdentifier());
    EmailTemplateEntity savedEntity =
        emailTemplateDAO.save(EmailTemplateMapper.toEntity(emailTemplate));
    LOG.debug("⚖️ Saved Email Template with ID {} in Database", savedEntity.getId());
    return EmailTemplateMapper.toModel(savedEntity);
  }

  public void deleteById(Long id) {
    LOG.debug("📧 Deleting Email Template with ID: {}", id);
    emailTemplateDAO.deleteById(id);
    LOG.debug("⚖️ Deleted Email Template with ID: {} in Database", id);
  }
}
