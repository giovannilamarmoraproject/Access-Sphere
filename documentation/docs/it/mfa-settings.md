# MFA e impostazioni

## MFA TOTP

Il flusso MFA è composto da:

1. `POST /v1/mfa/setup` genera secret e dati necessari per configurare
   l'applicazione autenticatrice;
2. l'utente legge il QR code o inserisce il secret;
3. `POST /v1/mfa/confirm` verifica il primo codice e attiva MFA;
4. `POST /v1/mfa/verify` verifica i codici durante il login o lo step-up;
5. `POST /v1/mfa/manage` abilita, disabilita o aggiorna la configurazione.

Non registrare secret TOTP o recovery code nei log. Mostra i recovery code una
sola volta e richiedi una nuova autenticazione per operazioni sensibili.

## Impostazioni e branding

```http
GET /v1/app/settings
PUT /v1/app/settings
```

Le impostazioni comprendono nome applicazione, tagline, logo, favicon, tema,
lingua predefinita e modalità della pagina login. La console sincronizza le
impostazioni pubbliche con il frontend e conserva le preferenze locali senza
cancellarle al logout.

## Backup

```http
GET /v1/app/settings/backup
POST /v1/app/settings/restore
```

Un backup è uno snapshot amministrativo, non un file pubblico. Conservalo
cifrato, limita l'accesso e verifica la compatibilità dell'ambiente prima del
restore.
