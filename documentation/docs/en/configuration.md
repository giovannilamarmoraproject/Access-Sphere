# Configuration

## Technical user

Configure the administrative technical user:

```text
TECH_USERNAME=tech_admin
TECH_PASSWORD=<long-password>
TECH_CLIENT_ID=ACCESS-SPHERE-TECH
TECH_EMAIL=admin@example.com
TECH_JWT_SECRET=<random-secret>
```

This account is used by the console for administrative user, client, role and
settings operations.

## CORS

```yaml
app:
  cors:
    enabled: true
    allowedOrigins: https://frontend.example.com
    allowedHeaders: Origin, Content-Type, Accept, Authorization, Location, Session-ID
    allowCredentials: true
```

Do not combine wildcard origins with credentials in production. Explicitly list
the trusted origins.

## Email delivery

Password recovery and template delivery use:

```yaml
rest:
  client:
    email-sender:
      baseUrl: https://email-sender.giovannilamarmora.com
      path:
        sendEmailUrl: /v1/send-email
```

## Logging and payload limits

`logging.level.io.github.giovannilamarmora` controls application logging.
`spring.codec.max-in-memory-size` controls the maximum reactive payload size
for uploads and backups.

## Backup and restore

`GET /v1/app/settings/backup` exports an administrative snapshot and
`POST /v1/app/settings/restore` imports one. Treat backups as confidential:
they may contain users, clients and application configuration.
