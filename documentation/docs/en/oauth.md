# OAuth 2.0

## Actors

- **Resource owner**: the user granting access.
- **Client**: the frontend or service requesting tokens.
- **Authorization server**: Access Sphere.
- **Resource server**: an API validating issued tokens.

## Authorization Code request

```http
GET /v1/oauth2/authorize
  ?response_type=code
  &access_type=ONLINE
  &client_id=CLIENT_ID
  &redirect_uri=https%3A%2F%2Ffrontend.example%2Fcallback
  &scope=openid%20email
  &state=STATE
```

`response_type`, `access_type`, `client_id`, `redirect_uri` and `scope` are
required. Generate `state` in the client and validate it after the redirect to
prevent CSRF. Access Sphere returns an authorization code to the registered
redirect URI.

## Code exchange

```http
POST /v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

client_id=CLIENT_ID&
grant_type=authorization_code&
code=AUTHORIZATION_CODE&
redirect_uri=https%3A%2F%2Ffrontend.example%2Fcallback
```

Confidential clients authenticate with Basic when required. Public clients
should use PKCE. Never expose a client secret in browser code.

## Refresh

```http
POST /v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

client_id=CLIENT_ID&
grant_type=refresh_token&
refresh_token=REFRESH_TOKEN&
scope=openid
```

Protect refresh tokens more carefully than access tokens and revoke them on
logout or suspected compromise.

## Token exchange and logout

`POST /v1/oauth2/token/exchange` exchanges a token for another client subject to
authorization rules. `POST /v1/oauth2/logout` invalidates the session
associated with the token. Legacy paths under `/v1/oAuth/2.0/*` remain
available for compatibility.
