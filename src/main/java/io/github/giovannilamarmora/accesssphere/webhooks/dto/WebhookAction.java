package io.github.giovannilamarmora.accesssphere.webhooks.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eventi di sistema che possono innescare una notifica Webhook.
 */
@Schema(description = "Azione o evento di sistema che attiva l'esecuzione del Webhook")
public enum WebhookAction {
  @Schema(description = "Scatenato all'eliminazione definitiva di un utente")
  DELETE_USER,

  @Schema(description = "Scatenato alla creazione di un nuovo utente")
  CREATE_USER,

  @Schema(description = "Scatenato alla registrazione di un nuovo utente (sinonimo di CREATE_USER)")
  REGISTER_USER,

  @Schema(description = "Scatenato alla modifica del profilo o dati di un utente esistente")
  UPDATE_USER,

  @Schema(description = "Scatenato al blocco dell'account utente")
  LOCK_USER,

  @Schema(description = "Scatenato allo sblocco dell'account utente")
  UNLOCK_USER,

  @Schema(description = "Scatenato al reset o modifica password di un utente")
  RESET_PASSWORD
}
