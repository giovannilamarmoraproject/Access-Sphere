package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.accesssphere.email.template.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper centralizzato per la conversione tra l'entita relazionale {@link EmailTemplateEntity}, il
 * modello di dominio {@link EmailTemplate} e il Data Transfer Object esposto via API {@link
 * EmailTemplateResponse}.
 *
 * <p>Segue il pattern architetturale adottato nel modulo Client (OAuth2ClientMapper), isolando lo
 * strato di persistenza JPA dal dominio applicativo e garantendo la corretta propagazione dei campi
 * e dei parametri segnaposto.
 */
@Component
public class EmailTemplateMapper {

  /**
   * Converte una lista di entita {@link EmailTemplateEntity} in una lista di modelli di dominio
   * {@link EmailTemplate}.
   *
   * @param entities Lista di entita caricate dal database tramite DAO
   * @return Lista di modelli di dominio mappati
   */
  public static List<EmailTemplate> toModelList(List<EmailTemplateEntity> entities) {
    if (ObjectToolkit.isNullOrEmpty(entities)) return List.of();
    return entities.stream().map(EmailTemplateMapper::toModel).toList();
  }

  /**
   * Converte un'entita JPA {@link EmailTemplateEntity} nel relativo modello di dominio {@link
   * EmailTemplate}.
   *
   * @param entity Entita di persistenza
   * @return Modello di dominio popolato, oppure null se l'input e null
   */
  public static EmailTemplate toModel(EmailTemplateEntity entity) {
    EmailTemplate model = new EmailTemplate();
    BeanUtils.copyProperties(entity, model);
    if (!ObjectToolkit.isNullOrEmpty(entity.getParams())) {
      model.setParams(new HashMap<>(entity.getParams()));
    }
    return model;
  }

  /**
   * Converte un modello di dominio {@link EmailTemplate} nell'entita di persistenza {@link
   * EmailTemplateEntity}.
   *
   * @param model Modello di dominio
   * @return Entita di persistenza pronta per essere salvata via JPA DAO
   */
  public static EmailTemplateEntity toEntity(EmailTemplate model) {
    if (model == null) return null;
    EmailTemplateEntity entity = new EmailTemplateEntity();
    BeanUtils.copyProperties(model, entity);
    if (model.getParams() != null) {
      entity.setParams(new HashMap<>(model.getParams()));
    }
    return entity;
  }

  /**
   * Converte una lista di modelli di dominio {@link EmailTemplate} in una lista di entita {@link
   * EmailTemplateEntity}.
   *
   * @param models Lista di modelli di dominio
   * @return Lista di entita JPA
   */
  public static List<EmailTemplateEntity> toEntityList(List<EmailTemplate> models) {
    if (ObjectToolkit.isNullOrEmpty(models)) return List.of();
    return models.stream().map(EmailTemplateMapper::toEntity).toList();
  }

  /**
   * Converte una lista di modelli di dominio {@link EmailTemplate} in una lista di DTO {@link
   * EmailTemplateResponse}.
   *
   * @param models Lista di modelli di dominio
   * @return Lista di DTO per la risposta API REST
   */
  public static List<EmailTemplateResponse> toResponseList(List<EmailTemplate> models) {
    if (ObjectToolkit.isNullOrEmpty(models)) return List.of();
    return models.stream().map(EmailTemplateMapper::toResponse).toList();
  }

  /**
   * Converte un modello di dominio {@link EmailTemplate} nel DTO di esposizione {@link
   * EmailTemplateResponse}.
   *
   * @param model Modello di dominio
   * @return DTO popolato per l'interfaccia utente o API
   */
  public static EmailTemplateResponse toResponse(EmailTemplate model) {
    EmailTemplateResponse dto = new EmailTemplateResponse();
    BeanUtils.copyProperties(model, dto);
    if (model.getParams() != null) {
      dto.setParams(new HashMap<>(model.getParams()));
    }
    return dto;
  }

  /**
   * Converte un DTO {@link EmailTemplateResponse} nel modello di dominio {@link EmailTemplate}.
   *
   * @param dto DTO inviato dal client REST
   * @return Modello di dominio valorizzato
   */
  public static EmailTemplate toModel(EmailTemplateResponse dto) {
    if (dto == null) return null;
    EmailTemplate model = new EmailTemplate();
    BeanUtils.copyProperties(dto, model);
    if (dto.getParams() != null) {
      model.setParams(new HashMap<>(dto.getParams()));
    }
    return model;
  }

  /**
   * Converte un DTO {@link EmailTemplateResponse} direttamente nell'entita JPA (overload di
   * retrocompatibilita).
   *
   * @param dto DTO di input
   * @return Entita di persistenza
   */
  public static EmailTemplateEntity toEntity(EmailTemplateResponse dto) {
    if (dto == null) return null;
    return toEntity(toModel(dto));
  }

  /**
   * Aggiorna i campi di un modello di dominio {@link EmailTemplate} con i valori ricevuti dal DTO
   * {@link EmailTemplateResponse}.
   *
   * @param target Modello di dominio da aggiornare
   * @param source DTO contenente i nuovi valori
   */
  public static void updateModel(EmailTemplate target, EmailTemplateResponse source) {
    if (target == null || source == null) return;
    if (source.getIdentifier() != null) target.setIdentifier(source.getIdentifier().trim());
    if (source.getCategory() != null) target.setCategory(source.getCategory().trim());
    target.setClientId(
        source.getClientId() != null && !source.getClientId().trim().isEmpty()
            ? source.getClientId().trim()
            : null);
    if (source.getName() != null) target.setName(source.getName().trim());
    if (source.getSubject() != null) target.setSubject(source.getSubject().trim());
    if (source.getDescription() != null) target.setDescription(source.getDescription());
    if (source.getTemplate() != null) target.setTemplate(source.getTemplate());
    if (source.getParams() != null) target.setParams(new HashMap<>(source.getParams()));
    if (source.getLocale() != null) target.setLocale(source.getLocale().trim());
  }

  /**
   * Aggiorna i campi di un'entita JPA {@link EmailTemplateEntity} con i dati provenienti dal
   * modello di dominio {@link EmailTemplate}.
   *
   * @param target Entita JPA da aggiornare a database
   * @param source Modello di dominio contenente i nuovi valori
   */
  public static void updateEntity(EmailTemplateEntity target, EmailTemplate source) {
    if (target == null || source == null) return;
    if (source.getIdentifier() != null) target.setIdentifier(source.getIdentifier().trim());
    if (source.getCategory() != null) target.setCategory(source.getCategory().trim());
    target.setClientId(
        source.getClientId() != null && !source.getClientId().trim().isEmpty()
            ? source.getClientId().trim()
            : null);
    if (source.getName() != null) target.setName(source.getName().trim());
    if (source.getSubject() != null) target.setSubject(source.getSubject().trim());
    if (source.getDescription() != null) target.setDescription(source.getDescription());
    if (source.getTemplate() != null) target.setTemplate(source.getTemplate());
    if (source.getParams() != null) target.setParams(new HashMap<>(source.getParams()));
    if (source.getLocale() != null) target.setLocale(source.getLocale().trim());
  }

  /**
   * Aggiorna i campi di un'entita JPA con i dati provenienti dal DTO (overload di utilita).
   *
   * @param target Entita JPA da aggiornare
   * @param source DTO contenente i nuovi valori
   */
  public static void updateEntity(EmailTemplateEntity target, EmailTemplateResponse source) {
    if (target == null || source == null) return;
    updateEntity(target, toModel(source));
  }
}
