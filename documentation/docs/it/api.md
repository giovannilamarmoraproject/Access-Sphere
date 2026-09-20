# Riferimento API

La specifica generata dal codice è la fonte operativa più aggiornata:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## OAuth

| Metodo | Endpoint | Descrizione |
| --- | --- | --- |
| GET | `/v1/oauth2/authorize` | Avvia Authorization Code |
| POST | `/v1/oauth2/token` | Emette o rinnova token |
| POST | `/v1/oauth2/token/exchange` | Scambia token |
| POST | `/v1/oauth2/logout` | Logout |

Legacy: `/v1/oAuth/2.0/authorize`, `/token`, `/token/exchange`, `/logout`.

## Utenti e profilo

| Metodo | Endpoint |
| --- | --- |
| GET/POST | `/v1/users` |
| DELETE | `/v1/users/{identifier}` |
| PATCH | `/v1/users/{identifier}/status` |
| GET/PUT | `/v1/users/profile`, `/v1/users/update` |
| POST | `/v1/users/password/reset-request` |
| POST | `/v1/users/password/reset` |
| POST | `/v1/users/change/password/request` |
| POST | `/v1/users/change/password` |

## Client, MFA, settings ed email

| Area | Endpoint |
| --- | --- |
| Client | `/v1/clients`, `/v1/clients/{clientId}` |
| MFA | `/v1/mfa/setup`, `/confirm`, `/verify`, `/manage` |
| Settings | `/v1/app/settings`, `/backup`, `/restore` |
| Email | `/v1/email-templates`, `/categories`, `/{id}`, `/send-test` |

Per metodi, body, schema e codici di risposta consulta sempre `/api-docs`.

## Header

```http
Authorization: Bearer <access-token>
Session-ID: <session-id>
Content-Type: application/json
```

Gli endpoint pubblici e quelli di autenticazione possono avere requisiti
diversi. Non assumere che un endpoint sia pubblico senza verificare il codice
del controller o lo schema OpenAPI dell'ambiente.
