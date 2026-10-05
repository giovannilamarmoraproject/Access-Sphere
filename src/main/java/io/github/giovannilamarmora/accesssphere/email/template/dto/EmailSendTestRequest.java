package io.github.giovannilamarmora.accesssphere.email.template.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Richiesta per l'invio di un'email di test direttamente dall'interfaccia di amministrazione.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailSendTestRequest {

  @io.swagger.v3.oas.annotations.media.Schema(
      description = "Identificatore univoco applicativo o UUID del template",
      example = "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95")
  private String identifier;

  @io.swagger.v3.oas.annotations.media.Schema(
      description = "Identificativo ID primario database (deprecato, preferire identifier)",
      example = "1")
  private Long templateId;

  private String locale;

  @NotBlank(message = "The field 'to' is required and it cannot be null or blank")
  private String to;

  private String subject;
  private String customHtml;
  private Map<String, String> params;
}
