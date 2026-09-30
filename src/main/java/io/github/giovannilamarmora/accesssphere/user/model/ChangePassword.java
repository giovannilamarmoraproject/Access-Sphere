package io.github.giovannilamarmora.accesssphere.user.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modello DTO che rappresenta il payload per le richieste di reset della password (Step 1)
 * e la conferma di aggiornamento password tramite token di verifica (Step 2).
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description =
        "Payload for password reset requests (Step 1) and password update confirmations (Step 2)")
public class ChangePassword extends GenericDTO {

  @Schema(
      description =
          "Optional custom email template identifier or category for password reset notification",
      example = "RESET_PASSWORD")
  private String templateId;

  @Schema(
      description = "Email address of the user requesting password reset (used in Step 1)",
      example = "john.doe@example.com")
  private String email;

  @Schema(
      description =
          "New user password encoded in Base64 according to security guidelines (used in Step 2)",
      example = "UGFzc3dvcmQxMjMh")
  private String password;

  @Schema(
      description =
          "One-time verification reset token received by email or generated via API (used in Step 2)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String token;

  @Schema(
      description = "Identifier of the OAuth2 Client associated with the reset request",
      example = "CLIENT-SAMPLE-001")
  private String clientId;

  @Schema(
      description = "Dynamic key-value parameters to interpolate inside the email template",
      example = "{\"appName\": \"Access-Sphere\"}")
  private Map<String, String> params;

  public ChangePassword(
      String templateId, String email, String password, String token, Map<String, String> params) {
    this.templateId = templateId;
    this.email = email;
    this.password = password;
    this.token = token;
    this.params = params;
  }
}
