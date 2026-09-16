package io.github.giovannilamarmora.accesssphere.email.dto;

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

  private Long templateId;
  private String identifier;
  private String locale;

  @NotBlank(message = "Il destinatario (to) non puo essere vuoto")
  private String to;

  private String subject;
  private String customHtml;
  private Map<String, String> params;
}
