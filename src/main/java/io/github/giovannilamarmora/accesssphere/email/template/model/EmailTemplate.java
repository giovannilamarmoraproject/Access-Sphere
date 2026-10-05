package io.github.giovannilamarmora.accesssphere.email.template.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.*;

/**
 * Modello di dominio per i Template Email di Access Sphere.
 * Rappresenta la struttura dati del template disaccoppiata dall'entita JPA di persistenza relazionale,
 * arricchita con annotazioni OpenAPI per la documentazione Swagger.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Modello di dominio per la definizione e gestione di un template email")
public class EmailTemplate extends GenericDTO {

  @Schema(description = "Identificativo univoco record database", example = "1")
  private Long id;

  @Schema(
      description = "Identificatore univoco applicativo o UUID del template",
      example = "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95")
  private String identifier;

  @Schema(
      description = "Categoria applicativa del template (es. RESET_PASSWORD, WELCOME_USER, ecc.)",
      example = "RESET_PASSWORD")
  private String category;

  @Schema(
      description = "Client ID OAuth 2.0 associato al template. Se nullo, il template e globale di sistema",
      example = "ACCESS-SPHERE-AUTH")
  private String clientId;

  @Schema(description = "Nome descrittivo del template", example = "Reset Password")
  private String name;

  @Schema(description = "Oggetto dell'email da inviare", example = "Reset Password")
  private String subject;

  @Schema(
      description = "Descrizione testuale dello scopo del template",
      example = "Template for sending password reset code")
  private String description;

  @Schema(description = "Corpo HTML completo del template email")
  private String template;

  @Schema(
      description = "Mappa dei parametri segnaposto standard associati al template",
      example = "{\"SITE_URL\": \"https://accesssphere.io\", \"NAME\": \"Mario Rossi\"}")
  private Map<String, String> params;

  @Schema(
      description = "Codice lingua/locale (es. 'it', 'en'). Se nullo o vuoto, il template e universale",
      example = "en")
  private String locale;
}
