package io.github.giovannilamarmora.accesssphere.email;

import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import java.util.HashMap;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper per la conversione tra entita di persistenza EmailTemplateEntity, il relativo DTO di
 * esposizione EmailTemplateDTO e l'oggetto StrapiEmailTemplate utilizzato dal motore di invio email
 * esistente.
 */
@Component
public class EmailTemplateMapper {

  public static EmailTemplateDTO toDTO(EmailTemplateEntity entity) {
    if (entity == null) return null;
    EmailTemplateDTO dto = new EmailTemplateDTO();
    BeanUtils.copyProperties(entity, dto);
    if (entity.getParams() != null) {
      dto.setParams(new HashMap<>(entity.getParams()));
    }
    return dto;
  }

  public static EmailTemplateEntity toEntity(EmailTemplateDTO dto) {
    if (dto == null) return null;
    EmailTemplateEntity entity = new EmailTemplateEntity();
    BeanUtils.copyProperties(dto, entity);
    if (dto.getParams() != null) {
      entity.setParams(new HashMap<>(dto.getParams()));
    }
    return entity;
  }

  public static void updateEntity(EmailTemplateEntity target, EmailTemplateDTO source) {
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
}
