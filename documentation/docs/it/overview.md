# Access Sphere

Access Sphere è un authorization server OAuth 2.0 e una console IAM
enterprise-oriented costruita con Java 22, Spring Boot 3 e Spring WebFlux.
Fornisce autenticazione, autorizzazione, gestione delle identità e una
console amministrativa pronta per essere usata da applicazioni web e API.

## Cosa risolve

Un'applicazione consumer non deve implementare da sola login, emissione token,
refresh, logout, ruoli, MFA, reset password e gestione client. Access Sphere
centralizza queste responsabilità e permette al frontend di consumare un
contratto OAuth 2.0 stabile.

## Capacità principali

| Area | Funzionalità |
| --- | --- |
| OAuth 2.0 | Authorization Code, token, refresh token, token exchange, logout e PKCE |
| IAM utenti | Registrazione, profilo, password, stato, attributi e ruoli |
| IAM client | Creazione, modifica, eliminazione, scope e redirect URI |
| Autorizzazione | Ruoli applicativi, tipo token, permessi e controllo amministrativo |
| MFA | Setup TOTP, conferma, verifica e gestione dello stato |
| Impostazioni | Branding, tema, lingua, policy, backup e restore |
| Comunicazioni | Template email, categorie e invio di test |
| Console | Dashboard web sotto `/app` con interfaccia Material 3 |

## Accessi locali

Con il profilo locale attivo:

| Risorsa | URL |
| --- | --- |
| Console | `http://localhost:8081/app` |
| Login console | `http://localhost:8081/app/login` |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/api-docs` |
| Documentazione italiana | `http://localhost:8081/docs/it/overview` |
| Documentazione inglese | `http://localhost:8081/docs/en/overview` |

## Convenzioni API

Gli endpoint applicativi usano il prefisso `/v1`. Le risposte applicative
seguono normalmente il wrapper `Response`:

```json
{
  "status": 200,
  "message": "Operation completed",
  "spanId": "trace-id",
  "data": {}
}
```

Gli errori includono codice applicativo, stato HTTP, messaggio e `spanId`.
Usa quest'ultimo quando analizzi i log o chiedi supporto.

## Percorso consigliato

1. Leggi [Architettura](/it/architecture).
2. Prepara [Prerequisiti](/it/required).
3. Segui [Installazione](/it/install).
4. Configura il tech user in [Configurazione](/it/configuration).
5. Integra il client con [OAuth 2.0](/it/oauth).
6. Consulta il [Riferimento API](/it/api).
