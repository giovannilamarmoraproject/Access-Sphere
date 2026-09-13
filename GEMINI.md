# Access-Sphere: Memory & Architecture Context (GEMINI.md)

## 1. Project Overview & Mission
**Access-Sphere** is an enterprise-ready Identity and Access Management (IAM) and OAuth 2.0 Authorization Server built with **Spring Boot 3 (Reactive / WebFlux)**, **R2DBC / Reactive Data**, and a modern web management console powered by **HTML5 / Tailwind CSS / Vanilla JS** and Google Material Design 3 (M3) styling.

---

## 2. Technical Stack
- **Backend**: Java 22, Spring Boot 3.4.0 (WebFlux, Reactive Security, R2DBC, Reactor Core).
- **Frontend**: Thymeleaf templates & static HTML (`src/main/resources/static/app/`), Tailwind CSS, Google Sans Flex typography, FontAwesome 6, SweetAlert2.
- **Data & Caching**: Reactive SQL / H2 / MariaDB / PostgreSQL driver integrations, encrypted client secrets, TOTP MFA.
- **Styling**: Material 3 glassmorphism (`m3-dashboard.css`, `m3-themes.css`).

---

## 3. Core Architectural Modules
1. **OAuth 2.0 Engine** (`io.github.giovannilamarmora.accesssphere.oAuth`):
   - Authorization code, client credentials, token generation, refresh tokens, PKCE.
2. **User IAM** (`io.github.giovannilamarmora.accesssphere.data.user`):
   - User registration, status management, profile editing, password policies, roles & permissions.
3. **Client IAM** (`io.github.giovannilamarmora.accesssphere.client`):
   - OAuth 2.0 client app registration, secret rotation, scopes, redirect URIs.
4. **MFA Engine** (`io.github.giovannilamarmora.accesssphere.mfa`):
   - TOTP secret generation, QR code generation, recovery codes, step-up authentication.
5. **App Settings & Customization** (`io.github.giovannilamarmora.accesssphere.settings`):
   - Dynamic application branding (Name, Tagline, Logo, Favicon).
   - Theme engine (presets: Cosmic Purple, Emerald Matrix, Ocean Blue, Cyberpunk Sunset, Midnight Slate, Forest Minimal).
   - Internationalization defaults (`it`, `en`, `auto`).
   - Login view customization (Showcase vs. Minimal, background image & opacity).
   - Application Backup & Restore (Full JSON snapshot export & import).

---

## 4. UI/UX Rules & Invariant Design Principles
> **CRITICAL RULE**: Do not alter, redesign, distort, or rebuild UI structures unless explicitly instructed by the user. Maintain pixel-perfect fidelity to the original design.

1. **Dashboard Hero Layout**:
   - The Welcome Hero ("Benvenuto nella Console di Amministrazione") sits directly on the page background (`<div class="mb-8">`), NOT wrapped in a `.m3-bento-card` or border box.
   - The right side houses the system clock pill and service status badge.
2. **KPI Bento Grid**:
   - Metric cards (Utenti IAM, Client Registrati, MFA Attivo, ID Sessione) use `.m3-bento-card`.
   - **Dynamic Data Invariant**: Never attach `data-i18n` attributes to DOM elements whose content is dynamically injected by JavaScript (e.g., `#stat-recap-session`, `#stat-recap-users`, `#stat-recap-clients`). Doing so causes `translateDOM()` to overwrite real data with static translation labels.
3. **Action Controls & Header**:
   - Circular action buttons use `.m3-icon-btn`.
   - The Logout button MUST use `.m3-icon-btn.m3-icon-btn-danger` with the red/salmon coral accent (`color: #FFB4AB`, `background: rgba(255, 180, 171, 0.08)`, `border: 1px solid rgba(255, 180, 171, 0.22)`).
   - The header across ALL pages (`dashboard.html`, `users.html`, `clients.html`, `user.html`, `register.html`, `edit.html`, `roles.html`, `mfa.html`, `client.html`) must contain the standard navigation tabs:
     - **Panoramica** (`/app`)
     - **Utenti** (`/app/users`)
     - **Client OAuth2** (`/app/clients`)
     - **Impostazioni** (`/app/settings`)
     - **API Docs** (external GitHub link)
4. **Storage & Session Hygiene**:
   - `cleanStorageAndCookies()` must NEVER clear user preferences and application branding.
   - Preserved localStorage keys on logout or login initialization:
     - `access_sphere_theme`
     - `access_sphere_settings`
     - `access_sphere_language`
     - `app_language`
     - `access_sphere_app_name`
5. **Backend Configuration Synchronization**:
   - On page load, `loadPublicSettings()` loads `/v1/app/settings/public` and applies the active theme and default language.
   - When settings are modified and saved, `saveAdminSettings()` updates the backend and persists preferences in `localStorage`.

---

## 5. Coding & Workflow Guidelines
- Always verify changes by running tests (`mvn test`).
- Keep code clean, modular, and well-commented.
- Never re-introduce removed Stripe calls or legacy mock dependencies.
- Act strictly within the scope requested by the user.
