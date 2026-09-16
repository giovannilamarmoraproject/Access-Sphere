package io.github.giovannilamarmora.accesssphere.email.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import java.util.Map;
import lombok.*;

/** Data Transfer Object per la gestione dei Template Email via API REST. */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailTemplateDTO extends GenericDTO {

  private String identifier;
  private String category;
  private String clientId;
  private String name;
  private String subject;
  private String description;
  private String template;
  private Map<String, String> params;
  private String locale;
}
