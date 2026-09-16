package io.github.giovannilamarmora.accesssphere.email;

import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object (JPA Repository) per la gestione dei Template Email su Database.
 */
@Repository
public interface IEmailTemplateDAO extends JpaRepository<EmailTemplateEntity, Long> {

  Optional<EmailTemplateEntity> findByIdentifierAndLocale(String identifier, String locale);

  List<EmailTemplateEntity> findByIdentifier(String identifier);

  Optional<EmailTemplateEntity> findFirstByIdentifier(String identifier);

  List<EmailTemplateEntity> findAllByOrderByCreationDateDesc();

  List<EmailTemplateEntity> findByCategory(String category);

  List<EmailTemplateEntity> findByClientId(String clientId);

  List<EmailTemplateEntity> findByClientIdAndCategory(String clientId, String category);

  List<EmailTemplateEntity> findByClientIdIsNullAndCategory(String category);

  /**
   * Cerca template con corrispondenza esatta di client (anche null), categoria e locale (anche null/vuoto)
   * escludendo un eventuale ID (utilizzato per garantire il vincolo di unicita su Client + Categoria + Lingua).
   */
  @Query(
      "SELECT e FROM EmailTemplateEntity e WHERE "
          + "((:clientId IS NULL AND (e.clientId IS NULL OR e.clientId = '')) OR e.clientId = :clientId) AND "
          + "e.category = :category AND "
          + "((:locale IS NULL AND (e.locale IS NULL OR e.locale = '')) OR e.locale = :locale) AND "
          + "(:excludeId IS NULL OR e.id <> :excludeId)")
  List<EmailTemplateEntity> findConflictingTemplates(
      @Param("clientId") String clientId,
      @Param("category") String category,
      @Param("locale") String locale,
      @Param("excludeId") Long excludeId);
}

