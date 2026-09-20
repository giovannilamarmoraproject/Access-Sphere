# Prerequisiti

## Software

| Componente | Versione |
| --- | --- |
| JDK | 22 |
| Maven | 3.9+ o `mvnw.cmd` |
| Node.js | 22+ |
| npm | incluso in Node.js |
| MySQL/MariaDB | compatibile con il driver configurato |
| Docker | opzionale per il deploy |

Verifica gli strumenti:

```powershell
java -version
mvn -version
node --version
npm --version
```

`java -version` deve mostrare Java 22. Un `JAVA_HOME` che punta a Java 8 o 11
causa l'errore `release version 22 not supported`.

## Database

Il database locale predefinito è `access_sphere` su MySQL:

```sql
CREATE DATABASE access_sphere
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Configura URL, username e password in `application.yml` oppure tramite un
profilo/variabili d'ambiente. Non versionare password reali.

## Secret

Produci secret casuali per:

- `AES_KEY`, usata per dati cifrati;
- `TECH_JWT_SECRET`, usata dal tech user;
- password del tech user;
- secret dei client OAuth.

Usa un secret manager in produzione. I valori di esempio sono solo per
sviluppo e non devono essere riutilizzati.

## Dominio e redirect URI

Il client OAuth deve registrare redirect URI esatti, per esempio:

```text
http://localhost:8081/app/login
```

Non usare wildcard in produzione. Il dominio applicativo locale predefinito è
`localhost:8081`.
