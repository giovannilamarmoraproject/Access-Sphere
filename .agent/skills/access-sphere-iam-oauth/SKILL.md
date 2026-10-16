---
name: access-sphere-iam-oauth
description: MUST load whenever working on OAuth 2.1 flows, PKCE, JWT claims, User/Client IAM, MFA, or Multi-Tenant delegated administration in Access-Sphere.
---

# Access-Sphere OAuth 2.1 & Layered Backend Architecture

## 1. Architectural Consistency (The "Client" Standard)

Every backend module (`oAuth`, `data.user`, `client`, `mfa`, `settings`) MUST strictly follow the exact same layered
architecture established in the `client` module:

- **Controller Layer:** Handles reactive HTTP endpoints, request mapping, payload validation, and delegates directly to
  the Service.
- **Service Layer (Business & Auth Logic):** Contains all business rules, orchestration, validation, and multi-tenant
  authorization scoping checks.
- **DAO / Repository Layer:** Handles reactive database communication via R2DBC / Reactive Data.
- **Domain / Entity / DTO Models:** Use immutable `record` structures for DTOs and clean domain models mapping database
  records.

## 2. OAuth 2.1 Strict Compliance

Access-Sphere implements the **OAuth 2.1** consolidated specification:

- **Supported Grants ONLY:** Support strictly `authorization_code`, `client_credentials`, and `refresh_token`. Never use
  legacy `password` or `implicit` grants.
- **Mandatory PKCE (S256):** Require `code_challenge` and `code_verifier` on all `authorization_code` flows. Support
  ONLY `code_challenge_method=S256`.
- **Exact Redirect URI Matching:** Validate `redirect_uri` using strict exact string equality (`.equals()`).
- **Refresh Token Rotation:** Enforce token rotation on every use.
- **Bearer Token Transport:** Accept access tokens ONLY via the `Authorization: Bearer <token>` HTTP header.

## 3. Reactive WebFlux Constraints

- Keep `Mono`/`Flux` pipelines clean and linear. Never nest `.flatMap()` inside another `.flatMap()` (extract private
  helper methods inside the Service instead).
- NEVER call `.block()`, `.toFuture().get()`, or use imperative blocking code over reactive streams.

## 4. Delegated Administration (Multi-Tenant Scoping)

- **User ↔ Client Mapping:** Enforce explicit relationship between Manager users and their `managed_clients`
  (`client_id`) inside the Service layer.
- **Read & Write Enforcement:** Any query or mutation outside the authorized `managed_clients` scope must be filtered at
  the DAO/Service level and rejected with `HTTP 403 Forbidden`.