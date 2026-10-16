package io.github.giovannilamarmora.accesssphere.webhooks.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eventi di sistema che possono innescare una notifica Webhook.
 */
@Schema(description = "Azione o evento di sistema che attiva l'esecuzione del Webhook")
public enum WebhookAction {
  @Schema(description = "Scatenato all'eliminazione definitiva di un utente")
  DELETE_USER,

  @Schema(description = "Scatenato alla registrazione o creazione di un nuovo utente")
  REGISTER_USER
}
