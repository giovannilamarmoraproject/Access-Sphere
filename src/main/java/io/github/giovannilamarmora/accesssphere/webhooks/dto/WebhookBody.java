package io.github.giovannilamarmora.accesssphere.webhooks.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON inviato verso l'endpoint esterno quando viene scatenato un Webhook.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payload JSON trasmesso nelle notifiche HTTP inviate tramite Webhook")
public class WebhookBody {

  @Schema(
      description = "Identificativo univoco dell'entita' oggetto dell'evento (es. UUID utente)",
      example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
  private String identifier;

  @Schema(
      description = "Tipo di azione o evento di sistema che ha innescato il Webhook",
      example = "DELETE_USER")
  private WebhookAction action;
}
