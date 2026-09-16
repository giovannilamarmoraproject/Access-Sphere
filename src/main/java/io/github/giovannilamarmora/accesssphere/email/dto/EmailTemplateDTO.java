package io.github.giovannilamarmora.accesssphere.email.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object per la gestione dei Template Email via API REST.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailTemplateDTO {

  private Long id;
  private String identifier;
  private String name;
  private String subject;
  private String description;
  private String template;
  private Map<String, Object> params;
  private String locale;
  private Date creationDate;
  private Date lastUpdate;
}
