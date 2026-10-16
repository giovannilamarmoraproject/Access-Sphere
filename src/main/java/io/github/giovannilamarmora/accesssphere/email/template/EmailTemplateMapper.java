package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.EmailUtils;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDataResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateRequest;
import io.github.giovannilamarmora.accesssphere.email.template.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper per la conversione tra {@link EmailTemplateEntity}, il modello di dominio {@link
 * EmailTemplate} e i DTO {@link EmailTemplateRequest} / {@link EmailTemplateDataResponse}.
 */
@Component
public class EmailTemplateMapper {

  public static List<EmailTemplateDataResponse> toResponseList(List<EmailTemplate> models) {
    if (ObjectToolkit.isNullOrEmpty(models)) return List.of();
    return models.stream().map(EmailTemplateMapper::toResponse).toList();
  }

  public static EmailTemplateDataResponse toResponse(EmailTemplate model) {
    if (model == null) return null;
    EmailTemplateDataResponse response = new EmailTemplateDataResponse();
    BeanUtils.copyProperties(model, response);
    if (model.getParams() != null) {
      response.setParams(new HashMap<>(model.getParams()));
    }
    return response;
  }

  public static EmailTemplate toModel(EmailTemplateRequest request) {
    if (request == null) return null;
    EmailTemplate model = new EmailTemplate();
    BeanUtils.copyProperties(request, model);
    if (request.getParams() != null) {
      model.setParams(new HashMap<>(request.getParams()));
    }
    return model;
  }

  public static List<EmailTemplate> toModelList(List<EmailTemplateEntity> entities) {
    if (ObjectToolkit.isNullOrEmpty(entities)) return List.of();
    return entities.stream().map(EmailTemplateMapper::toModel).toList();
  }

  public static EmailTemplate toModel(EmailTemplateEntity entity) {
    if (entity == null) return null;
    EmailTemplate model = new EmailTemplate();
    BeanUtils.copyProperties(entity, model);
    if (!ObjectToolkit.isNullOrEmpty(entity.getParams())) {
      model.setParams(new HashMap<>(entity.getParams()));
    }
    return model;
  }

  public static EmailTemplateEntity toEntity(EmailTemplate model) {
    if (model == null) return null;
    EmailTemplateEntity entity = new EmailTemplateEntity();
    BeanUtils.copyProperties(model, entity);
    if (model.getParams() != null) {
      entity.setParams(new HashMap<>(model.getParams()));
    }
    return entity;
  }

  public static void updateModel(EmailTemplate target, EmailTemplate source) {
    if (target == null || source == null) return;
    if (source.getIdentifier() != null) target.setIdentifier(source.getIdentifier().trim());
    if (source.getCategory() != null) target.setCategory(source.getCategory().trim());
    target.setClientId(EmailUtils.normalizeClientId(source.getClientId()));
    target.setLocale(EmailUtils.normalizeLocale(source.getLocale()));
    if (source.getName() != null) target.setName(source.getName().trim());
    if (source.getSubject() != null) target.setSubject(source.getSubject().trim());
    if (source.getDescription() != null) target.setDescription(source.getDescription());
    if (source.getTemplate() != null) target.setTemplate(source.getTemplate());
    if (source.getParams() != null) target.setParams(new HashMap<>(source.getParams()));
  }
}
