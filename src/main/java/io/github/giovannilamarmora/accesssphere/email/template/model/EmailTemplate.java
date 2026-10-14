package io.github.giovannilamarmora.accesssphere.email.template.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import java.util.Map;
import lombok.*;

/**
 * Modello di dominio per i Template Email di Access Sphere.
 * Rappresenta la struttura dati del template disaccoppiata dall'entita JPA di persistenza relazionale
 * e dai contratti di esposizione DTO OpenAPI.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailTemplate extends GenericDTO {

  private Long id;
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
