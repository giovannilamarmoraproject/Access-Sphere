# API reference

The generated specification is the operational source of truth:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## OAuth

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/v1/oauth2/authorize` | Starts Authorization Code |
| POST | `/v1/oauth2/token` | Issues or refreshes tokens |
| POST | `/v1/oauth2/token/exchange` | Exchanges a token |
| POST | `/v1/oauth2/logout` | Logs out |

Legacy routes are also available under `/v1/oAuth/2.0`.

## Users and profiles

| Method | Endpoint |
| --- | --- |
| GET/POST | `/v1/users` |
| DELETE | `/v1/users/{identifier}` |
| PATCH | `/v1/users/{identifier}/status` |
| GET/PUT | `/v1/users/profile`, `/v1/users/update` |
| POST | `/v1/users/password/reset-request` |
| POST | `/v1/users/password/reset` |
| POST | `/v1/users/change/password/request` |
| POST | `/v1/users/change/password` |

## Clients, MFA, settings and email

| Area | Endpoints |
| --- | --- |
| Clients | `/v1/clients`, `/v1/clients/{clientId}` |
| MFA | `/v1/mfa/setup`, `/confirm`, `/verify`, `/manage` |
| Settings | `/v1/app/settings`, `/backup`, `/restore` |
| Email | `/v1/email-templates`, `/categories`, `/{id}`, `/send-test` |

Use `/api-docs` for exact request bodies, schemas, parameters and response
codes generated from the current controllers.

## Headers

```http
Authorization: Bearer <access-token>
Session-ID: <session-id>
Content-Type: application/json
```

Public and authentication endpoints may have different requirements. Always
verify the OpenAPI document for the environment you are calling.
