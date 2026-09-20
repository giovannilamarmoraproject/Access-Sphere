# Access Sphere

Access Sphere is an OAuth 2.0 authorization server and IAM console built with
Java 22, Spring Boot 3 and Spring WebFlux. It centralizes authentication,
authorization, identity management and administration for web applications and
APIs.

## What it provides

Applications can delegate login, token issuance, refresh, logout, roles, MFA,
password recovery and OAuth client management to Access Sphere instead of
implementing those security-critical features independently.

| Area | Capabilities |
| --- | --- |
| OAuth 2.0 | Authorization Code, refresh token, token exchange, logout and PKCE |
| User IAM | Registration, profile, password, status, attributes and roles |
| Client IAM | Client registration, scopes, secrets and redirect URIs |
| Authorization | Application roles, token type, permissions and admin checks |
| MFA | TOTP setup, confirmation, verification and management |
| Settings | Branding, theme, language, policy, backup and restore |
| Messaging | Email templates, categories and test delivery |
| Console | Material 3 administration UI under `/app` |

## Local endpoints

| Resource | URL |
| --- | --- |
| Administration console | `http://localhost:8081/app` |
| Console login | `http://localhost:8081/app/login` |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/api-docs` |
| Italian documentation | `http://localhost:8081/docs/it/overview` |
| English documentation | `http://localhost:8081/docs/en/overview` |

## API conventions

Application endpoints use the `/v1` prefix and usually return the `Response`
wrapper:

```json
{
  "status": 200,
  "message": "Operation completed",
  "spanId": "trace-id",
  "data": {}
}
```

Errors include an application code, HTTP status, message and `spanId`. Include
that identifier when investigating logs or reporting an incident.

## Recommended path

Read [Architecture](/en/architecture), prepare the
[Requirements](/en/required), follow the [local installation](/en/install),
configure the [application](/en/configuration), then integrate
[OAuth 2.0](/en/oauth) and consult the [API reference](/en/api).
