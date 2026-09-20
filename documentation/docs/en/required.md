# Requirements

## Software

| Component | Version |
| --- | --- |
| JDK | 22 |
| Maven | 3.9+ or `mvnw.cmd` |
| Node.js | 22+ |
| npm | included with Node.js |
| MySQL/MariaDB | compatible with the configured driver |
| Docker | optional for deployment |

Verify the toolchain:

```powershell
java -version
mvn -version
node --version
npm --version
```

Java must report version 22. A `JAVA_HOME` pointing to Java 8 or 11 causes
`release version 22 not supported`.

## Database

Create the local database:

```sql
CREATE DATABASE access_sphere
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Configure the connection through `application.yml`, a profile or environment
variables. Never commit real passwords.

## Secrets

Generate unique values for `AES_KEY`, `TECH_JWT_SECRET`, the technical user
password and OAuth client secrets. Use a secret manager outside development.

## Redirect URIs

Register exact redirect URIs, for example:

```text
http://localhost:8081/app/login
```

Do not use wildcards in production. The default local application domain is
`localhost:8081`.
