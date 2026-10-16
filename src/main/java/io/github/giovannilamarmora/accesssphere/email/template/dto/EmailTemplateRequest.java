package io.github.giovannilamarmora.accesssphere.email.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating and updating Email Templates via REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request payload for creating or updating an email template")
public class EmailTemplateRequest {

  @Schema(
      description = "Unique application identifier or UUID of the template",
      example = "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95")
  private String identifier;

  @Schema(
      description =
          "Application category of the template (e.g. RESET_PASSWORD, WELCOME_USER, MFA_OTP)",
      example = "RESET_PASSWORD")
  private String category;

  @Schema(
      description =
          "OAuth 2.0 Client ID associated with the template. If null or empty, the template is global",
      example = "ACCESS-SPHERE-AUTH")
  private String clientId;

  @NotBlank(message = "The field 'name' is required and cannot be blank")
  @Schema(description = "Descriptive display name of the template", example = "Reset Password")
  private String name;

  @NotBlank(message = "The field 'subject' is required and cannot be blank")
  @Schema(
      description = "Subject line of the transactional email to be dispatched",
      example = "Reset Password")
  private String subject;

  @Schema(
      description = "Textual summary describing the purpose and context of the template",
      example = "Template for sending password reset security code")
  private String description;

  @NotBlank(message = "The field 'template' is required and cannot be blank")
  @Schema(
      description =
          "Complete HTML markup for the email body, supporting dynamic placeholders in {{KEY}} format",
      example =
          "<p>Dear {{NAME}}, click on the following link to reset your password: {{RESET_URL}}</p>")
  private String template;

  @Schema(
      description = "Key-value map of standard placeholder parameters associated with the template",
      example =
          "{\"SITE_URL\": \"https://accesssphere.io\", \"NAME\": \"Mario Rossi\", \"RESET_URL\": \"https://accesssphere.io/reset\"}")
  private Map<String, String> params;

  @Schema(
      description =
          "ISO 639-1 language/locale code (e.g. 'en', 'it', 'es', 'fr', 'de'). If null or empty, the template is universal",
      example = "en")
  private String locale;
}
