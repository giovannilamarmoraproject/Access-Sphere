//const config = getConfig();

document.addEventListener("DOMContentLoaded", function () {
  cleanStorageAndCookies();
  const loginForm = document.getElementById("loginForm");

  // Event listener per il form
  if (loginForm) {
    //cleanStorageAndCookies();
    loginForm.addEventListener("submit", async function (event) {
      event.preventDefault(); // Previene il comportamento predefinito di invio del form
      await login();
    });
  }
});

async function login() {
  const currentUrl = new URL(window.location.href);
  const clientId = currentUrl.searchParams.get("client_id") || config.client_id;
  const redirectUri =
    currentUrl.searchParams.get("redirect_uri") || config.redirect_uri;

  // Chiama la funzione login con i dati raccolti
  await doLogin(clientId, redirectUri);
}

async function doLogin(clientId, redirectUri) {
  if (isLoggingIn()) {
    console.log("Already logging in...");
    return;
  }
  loggingIn();

  console.log("Executing Login...");
  const email = document.getElementById("emailInput").value;
  const password = document.getElementById("passwordInput").value;
  const encode = btoa(email + ":" + password);

  const url = new URL(config.token_url);
  url.searchParams.set("grant_type", "password");
  if (clientId) url.searchParams.set("client_id", clientId);
  if (redirectUri) url.searchParams.set("redirect_uri", redirectUri);

  try {
    const token = await POST(url, encode, null, "Basic ");
    const responseData = await token.json();

    if (responseData.error) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      const redirect_uri = token.headers.get("location");
      if (redirect_uri) {
        window.location.href = `${redirect_uri}?client-id=${clientId}&access-token=${
          responseData.data.token.access_token
        }&session-id=${token.headers.get("Session-ID")}`;
      }
    }
  } catch (err) {
    console.error("Errore nel login:", err);
    throw err;
  } finally {
    loggingOut(); // Reset dello stato per permettere nuovi login
  }
}

function doGoogleLogin() {
  const currentUrl = new URL(window.location.href);
  //const clientId = currentUrl.searchParams.get("client_id") || config.client_id;
  //const redirectUri =
  //  currentUrl.searchParams.get("redirect_uri") || config.redirect_uri;
  const clientId = currentUrl.searchParams.get("client_id");
  const redirectUri = currentUrl.searchParams.get("redirect_uri");

  if (!clientId)
    return sweetalert(
      "error",
      currentTranslations.googleLogin_error_title,
      currentTranslations.googleLogin_error_text
    );

  const url = new URL(window.location.origin + "/v1/oAuth/2.0/authorize");
  url.searchParams.set("access_type", "online");
  url.searchParams.set(
    "scope",
    "openid https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
  );
  if (clientId) url.searchParams.set("client_id", clientId);
  if (redirectUri) url.searchParams.set("redirect_uri", redirectUri);
  url.searchParams.set("type", "google");
  url.searchParams.set("response_type", "code");

  window.location.href = url.toString();
}
