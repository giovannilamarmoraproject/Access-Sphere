# User IAM e Client IAM

## Utenti

Gli endpoint amministrativi richiedono token valido e ruolo autorizzato:

```http
GET /v1/users
POST /v1/users
DELETE /v1/users/{identifier}
PATCH /v1/users/{identifier}/status
```

La registrazione accetta `client_id`, `registration_token` e
`assign_new_client` come query parameter. Il body è un oggetto utente validato.

Il profilo dell'utente autenticato usa:

```http
GET /v1/users/profile
PUT /v1/users/update
```

Le operazioni password distinguono richiesta del codice e completamento del
reset:

```http
POST /v1/users/password/reset-request
POST /v1/users/password/reset
POST /v1/users/change/password/request
POST /v1/users/change/password
```

## Client

I client OAuth definiscono identificativo, secret, scope, tipo token,
scadenza, ruoli e redirect URI:

```http
GET /v1/clients
POST /v1/clients
PUT /v1/clients
PUT /v1/clients/{clientId}
DELETE /v1/clients/{clientId}
```

Le operazioni sono amministrative. Valida sempre redirect URI e scope lato
server; il client non deve poter aumentare autonomamente i propri privilegi.

## Ruoli e permessi

Il token mantiene i dati compatibili con i frontend esistenti, inclusi `roles`
e `type`. Le applicazioni dovrebbero usare ruoli e permessi solo per decidere
la visibilità e l'autorizzazione delle proprie funzionalità, mentre il backend
deve ripetere la verifica a ogni richiesta protetta.
