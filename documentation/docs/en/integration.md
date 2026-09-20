# Frontend integration

## Load the authentication script

Define configuration before loading `auth.js`:

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

The script loads the REST helpers and resolves the server origin from its own
URL. The redirect URI must exactly match the registered client URI.

## Events

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

## Protected requests

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

Never place secrets in frontend code. Use PKCE for public clients and validate
state, issuer, audience, expiration and subject in the consuming application.
