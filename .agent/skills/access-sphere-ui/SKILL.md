---
name: access-sphere-ui
description: MUST load whenever modifying or creating HTML templates, CSS, Vanilla JS, i18n dictionaries, or UI components in Access-Sphere.
---

# Access-Sphere UI/UX & Material Design 3 Invariants

> **CRITICAL RULE**: Never alter, redesign, or distort existing UI structures unless explicitly asked. Maintain
> pixel-perfect fidelity.

## 1. Layout & Hero Rules

- **Hero Sections:** Dashboard and Settings Hero reside directly on the page background (`<div class="mb-8">`), NEVER
  inside `.m3-bento-card` or bordered boxes.
- **Hero Right Actions:** Dashboard Hero holds system clock pill + service status badge; Settings Hero holds Reset
  Defaults + Save Settings buttons.
- **KPI Bento Grid:** Metric cards use `.m3-bento-card`.
- **Dynamic DOM Invariant:** NEVER put `data-i18n` on elements populated dynamically via JS (`#stat-recap-session`,
  `#stat-recap-users`, `#stat-recap-clients`) to prevent `translateDOM()` from overwriting live data.

## 2. Header, Actions & Logout Button

- Circular action buttons use `.m3-icon-btn`.
- Logout button MUST use `.m3-icon-btn.m3-icon-btn-danger` with coral accent (`color: #FFB4AB`,
  `background: rgba(255, 180, 171, 0.08)`, `border: 1px solid rgba(255, 180, 171, 0.22)`).
- Unified M3 Header across ALL pages (`dashboard.html`, `users.html`, `clients.html`, `user.html`, `register.html`,
  `edit.html`, `roles.html`, `mfa.html`, `client.html`, `emails.html`, `role.html`):
    - **Panoramica** (`/app`)
    - **Identità & Accessi**: Utenti (`/app/users`), Nuovo Utente (`/app/users/register`), Ruoli (`/app/role`)
    - **Applicazioni**: Client OAuth2 (`/app/clients`), Nuovo Client (`/app/clients/register`)
    - **Sistema**: Template Email (`/app/emails`), Impostazioni (`/app/settings`), API Docs
      (`/webjars/swagger-ui/index.html`)

## 3. Storage, Theme & i18n

- **Preserve Storage on Logout:** `cleanStorageAndCookies()` must NEVER delete: `access_sphere_theme`,
  `access_sphere_settings`, `access_sphere_language`, `app_language`, `access_sphere_app_name`.
- **Settings Sync:** `loadPublicSettings()` calls `/v1/app/settings/public`; `saveAdminSettings()` updates backend +
  `localStorage`.
- **Strict M3 Palette:** Forbidden to use hardcoded hex/rgb colors. Use ONLY CSS theme variables
  (`--md-sys-color-primary`, `--md-sys-color-surface`, `--md-sys-color-outline`, etc.) from `m3-dashboard.css` /
  `m3-themes.css`.
- **Mandatory i18n:** Zero hardcoded text in HTML/JS/SweetAlert2. Always bind `data-i18n` or `data-i18n-placeholder` and
  immediately update both `it` and `en` language dictionaries.