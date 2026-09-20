# User IAM and Client IAM

## Users

Administrative endpoints require a valid token and an authorized role:

```http
GET /v1/users
POST /v1/users
DELETE /v1/users/{identifier}
PATCH /v1/users/{identifier}/status
```

Registration accepts `client_id`, `registration_token` and
`assign_new_client` as query parameters. The request body is a validated user
object.

The authenticated profile uses:

```http
GET /v1/users/profile
PUT /v1/users/update
```

Password recovery separates code request and completion:

```http
POST /v1/users/password/reset-request
POST /v1/users/password/reset
POST /v1/users/change/password/request
POST /v1/users/change/password
```

## Clients

OAuth clients define identifiers, secrets, scopes, token type, expiration,
roles and redirect URIs:

```http
GET /v1/clients
POST /v1/clients
PUT /v1/clients
PUT /v1/clients/{clientId}
DELETE /v1/clients/{clientId}
```

These operations are administrative. Validate redirect URIs and scopes on the
server; a client must not be able to grant itself additional privileges.

## Roles and permissions

Tokens preserve legacy frontend claims such as `roles` and `type`. Consumer
applications may use roles and permissions to control their own UI, but the
backend must enforce authorization again on every protected request.
