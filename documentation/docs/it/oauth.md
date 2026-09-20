# OAuth 2.0

## Ruoli degli attori

- **Resource owner**: l'utente che concede l'accesso.
- **Client**: applicazione frontend o servizio che richiede token.
- **Authorization server**: Access Sphere.
- **Resource server**: API che valida il token emesso.

## Authorization Code

Il client avvia:

```http
GET /v1/oauth2/authorize
  ?response_type=code
  &access_type=ONLINE
  &client_id=CLIENT_ID
  &redirect_uri=https%3A%2F%2Ffrontend.example%2Fcallback
  &scope=openid%20email
  &state=STATE
```

I parametri obbligatori sono `response_type`, `access_type`, `client_id`,
`redirect_uri` e `scope`. `state` deve essere generato dal client e verificato
al ritorno per prevenire CSRF. Il server restituisce un code al redirect URI.

## Scambio del code

```http
POST /v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

client_id=CLIENT_ID&
grant_type=authorization_code&
code=AUTHORIZATION_CODE&
redirect_uri=https%3A%2F%2Ffrontend.example%2Fcallback
```

Il client confidential deve autenticarsi con l'header Basic quando richiesto.
Un client pubblico dovrebbe usare PKCE. Non esporre mai un client secret nel
browser.

## Refresh token

```http
POST /v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

client_id=CLIENT_ID&
grant_type=refresh_token&
refresh_token=REFRESH_TOKEN&
scope=openid
```

Conserva i refresh token in modo più sicuro degli access token e revocali al
logout o quando sospetti una compromissione.

## Token exchange e logout

`POST /v1/oauth2/token/exchange` permette lo scambio verso un altro client
secondo le regole di autorizzazione. `POST /v1/oauth2/logout` invalida la
sessione associata al token.

Sono disponibili anche i percorsi legacy `/v1/oAuth/2.0/*` per retrocompatibilità.
