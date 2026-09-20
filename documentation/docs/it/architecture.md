# Architettura

## Strati applicativi

```text
Frontend / API consumer
        |
        v
Spring WebFlux controllers
        |
        v
Services e validatori
        |
        +--> OAuth/token/MFA engines
        |
        +--> JPA repositories
        |
        v
MySQL/MariaDB
```

### Controller

I controller espongono gli endpoint HTTP, validano header e body tramite
Jakarta Validation e restituiscono `Mono<ResponseEntity<...>>`. I contratti
OpenAPI sono annotati direttamente sulle interfacce/controller e vengono
pubblicati da Springdoc.

### Servizi

I service contengono la logica di dominio: autenticazione, emissione token,
validazione dei client, gestione utenti, MFA e snapshot delle impostazioni.
Il controller non deve essere usato come punto per duplicare questa logica.

### Persistenza

Il progetto usa il datasource JDBC/JPA per la persistenza relazionale e
configura anche il collegamento R2DBC usato dall'applicazione. In locale i
parametri sono definiti in `src/main/resources/application.yml`.

## Moduli

| Modulo | Package indicativo | Responsabilità |
| --- | --- | --- |
| OAuth | `oAuth` | Authorization Code, token e logout |
| Token | `token` | JWT/JWE, exchange e validazione |
| User | `data.user` | Account, profilo e amministrazione |
| Client | `client` | Credenziali e redirect client |
| MFA | `mfa` | TOTP e step-up authentication |
| Settings | `settings` | Branding, policy, backup e restore |
| Email | `email` | Template e invio test |
| Config | `config` | Session ID, filtri, CORS e sicurezza |

## Filtri trasversali

Le richieste passano attraverso filtri per session ID, request/response
logging, CORS e rate limiting. Le risorse pubbliche includono documentazione,
Swagger, file statici e pagine di login; gli endpoint protetti richiedono gli
header previsti dal flusso OAuth.

## Documentazione nel jar

La fase Maven `generate-resources` esegue la build VitePress. L'output viene
copiato in `target/classes/static/docs` e incluso nel jar Spring Boot. Il
controller espone URL senza `.html`, mentre i file HTML restano dettagli
interni dell'artefatto.
