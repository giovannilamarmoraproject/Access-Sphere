package io.github.giovannilamarmora.accesssphere.email.template.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Richiesta per l'invio di un'email di test direttamente dall'interfaccia di amministrazione o via API REST.
 *
 * <p>Consente di collaudare il rendering e l'inoltro SMTP di un template esistente (tramite {@code identifier})
 * oppure fornendo un markup HTML live alternativo prima della persistenza definitiva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payload for testing and dispatching email templates via SMTP service")
public class EmailSendTestRequest {

  @Schema(
      description = "Unique application identifier or UUID of the template to test",
      example = "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95")
  private String identifier;

  @Schema(
      description = "Primary database record ID (deprecated, prefer 'identifier')",
      example = "1")
  private Long templateId;

  @Schema(
      description = "Language/locale code of the template variant to test (e.g. 'en', 'it')",
      example = "en")
  private String locale;

  @NotBlank(message = "The field 'to' is required and it cannot be null or blank")
  @Schema(
      description = "Recipient email address to receive the test message",
      example = "user@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String to;

  @Schema(
      description = "Optional custom subject line to override the standard template subject in the test email",
      example = "[TEST] Reset Password")
  private String subject;

  @Schema(
      description = "Optional custom HTML markup to test rendering and layout live without saving to the database",
      example = "<p>Hello {{NAME}}, this is a live test preview!</p>")
  private String customHtml;

  @Schema(
      description = "Key-value map of dynamic placeholder parameters to replace {{KEY}} tags in the template",
      example = "{\"NAME\": \"{{USER.NAME}}\", \"RESET_CODE\": \"{{AUTH.RESET_CODE}}\"}")
  private Map<String, String> params;
}
