# Configurazione

## Tech user

Configura l'utenza tecnica:

```text
TECH_USERNAME=tech_admin
TECH_PASSWORD=<password-lunga>
TECH_CLIENT_ID=ACCESS-SPHERE-TECH
TECH_EMAIL=admin@example.com
TECH_JWT_SECRET=<secret-casuale>
```

Il tech user è utilizzato dalla console per le operazioni amministrative su
utenti, client, ruoli e impostazioni.

## CORS

Le proprietà principali sono:

```yaml
app:
  cors:
    enabled: true
    allowedOrigins: https://frontend.example.com
    allowedHeaders: Origin, Content-Type, Accept, Authorization, Location, Session-ID
    allowCredentials: true
```

Non combinare `allowedOrigins: "*"` con credenziali in produzione. Elenca
esplicitamente le origini autorizzate.

## Email

Il reset password e l'invio dei template usano il servizio configurato in:

```yaml
rest:
  client:
    email-sender:
      baseUrl: https://email-sender.giovannilamarmora.com
      path:
        sendEmailUrl: /v1/send-email
```

## Logging e payload

`logging.level.io.github.giovannilamarmora` controlla il livello dei log
applicativi. `spring.codec.max-in-memory-size` controlla la dimensione massima
dei payload reattivi, utile per upload e backup.

## Backup e restore

La console può esportare uno snapshot completo tramite
`GET /v1/app/settings/backup` e ripristinarlo tramite
`POST /v1/app/settings/restore`. Proteggi i file di backup come dati
confidenziali: possono contenere configurazioni, utenti e client.
