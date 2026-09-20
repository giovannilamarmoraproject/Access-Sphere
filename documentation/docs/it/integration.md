# Integrazione frontend

## Caricamento dello script

Definisci la configurazione prima di caricare `auth.js`:

```html
<script>
  window.accessSphereConfig = {
    client_id: "YOUR_CLIENT_ID",
    redirect_uri: "https://frontend.example.com/callback",
    include_user_data: false,
    include_user_info: false
  };
</script>
<script src="https://access-sphere.example.com/app/authentication/auth.js"></script>
```

Lo script carica gli helper REST e risolve automaticamente l'origine del
server dal proprio URL.

## Eventi

```javascript
for (const name of [
  "ACCESS_SPHERE_AUTH",
  "ACCESS_SPHERE_TOKEN",
  "ACCESS_SPHERE_LOGOUT"
]) {
  document.addEventListener(name, ({ detail }) => {
    if (detail.success) console.log(name, detail.data);
    else console.error(name, detail.error);
  });
}
```

## Protezione delle chiamate

```javascript
await fetch(`${baseUrl}/v1/users/profile`, {
  headers: {
    Authorization: `Bearer ${accessToken}`,
    "Session-ID": sessionId,
    Accept: "application/json"
  },
  credentials: "include"
});
```

Non inserire secret nel frontend. Usa PKCE per client pubblici e verifica
sempre `state`, issuer, audience, scadenza e subject del token.
