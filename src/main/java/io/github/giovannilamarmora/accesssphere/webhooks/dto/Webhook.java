package io.github.giovannilamarmora.accesssphere.webhooks.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Configurazione di un Webhook associato a eventi di sistema")
public class Webhook {

  @Schema(description = "Nome identificativo del Webhook", example = "User Registration Webhook")
  private String name;

  @Schema(description = "URL di destinazione per le notifiche HTTP POST del Webhook", example = "https://external-service.example.com/webhooks")
  private String url;

  @Schema(description = "Azione/evento che innesca la chiamata del Webhook", example = "DELETE_USER")
  private WebhookAction action;
}
