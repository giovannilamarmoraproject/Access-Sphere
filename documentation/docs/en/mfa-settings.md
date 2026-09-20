# MFA and application settings

## TOTP flow

1. `POST /v1/mfa/setup` creates the secret and authenticator data.
2. The user scans the QR code or enters the secret.
3. `POST /v1/mfa/confirm` verifies the first code and enables MFA.
4. `POST /v1/mfa/verify` verifies codes during login or step-up.
5. `POST /v1/mfa/manage` changes the MFA state or configuration.

Never log TOTP secrets or recovery codes. Show recovery codes once and require
re-authentication for sensitive changes.

## Branding and settings

```http
GET /v1/app/settings
PUT /v1/app/settings
```

Settings include application name, tagline, logo, favicon, theme, default
language and login page mode. The console synchronizes public settings with
the frontend and preserves user preferences when logging out.

## Backup

```http
GET /v1/app/settings/backup
POST /v1/app/settings/restore
```

Backups are administrative snapshots, not public files. Store them encrypted,
restrict access and verify environment compatibility before restoring.
