# Architecture

## Application layers

```text
Frontend / API consumer
        |
        v
Spring WebFlux controllers
        |
        v
Services and validators
        |
        +--> OAuth/token/MFA engines
        |
        +--> JPA repositories
        v
MySQL/MariaDB
```

Controllers expose HTTP endpoints, validate request data with Jakarta
Validation and return reactive `Mono<ResponseEntity<...>>` values. OpenAPI
contracts are declared on controllers and interfaces and published by
Springdoc.

Services contain domain logic for authentication, token issuance, client
validation, users, MFA and settings snapshots. Persistence is relational and
configured through the local Spring application configuration.

## Modules

| Module | Responsibility |
| --- | --- |
| OAuth | Authorization Code, token, refresh and logout flows |
| Token | JWT/JWE, exchange and validation |
| User | Accounts, profiles and administration |
| Client | OAuth credentials and redirect URI management |
| MFA | TOTP and step-up authentication |
| Settings | Branding, policies, backup and restore |
| Email | Templates and test delivery |
| Config | Session ID, filters, CORS and rate limiting |

## Cross-cutting filters

Requests pass through session ID, request/response logging, CORS and rate
limiting filters. Public resources include documentation, Swagger, static
assets and login pages. Protected API calls require the headers expected by the
OAuth flow.

## Documentation packaging

The Maven `generate-resources` phase runs the VitePress build. The generated
site is copied to `target/classes/static/docs` and packaged into the Spring
Boot jar. Clean public URLs are mapped to the generated HTML internally.
