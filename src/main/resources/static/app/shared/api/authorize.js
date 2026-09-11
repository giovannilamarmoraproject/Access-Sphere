// Cattura sincrona immediata dei parametri del token prima di qualunque altra chiamata
(function () {
  try {
    const urlParams = new URLSearchParams(window.location.search);
    const access_token = urlParams.get("access-token");
    const session_id = urlParams.get("session-id");
    const client_id = urlParams.get("client-id") || "ACCESS-SPHERE-TECH";

    if (access_token) {
      if (session_id) localStorage.setItem("Session-ID", session_id);
      localStorage.setItem("access-token", access_token);
      localStorage.setItem(client_id + "_access-token", access_token);
      localStorage.setItem("ACCESS-SPHERE-TECH_access-token", access_token);
      
      const cleanUrl = window.location.origin + window.location.pathname;
      window.history.replaceState(null, "", cleanUrl);
    }
  } catch (e) {
    console.error("Early token parse error:", e);
  }
})();

$(document).ready(function () {
  authorizeRequest();
});

function authorizeRequest() {
  const token = getCookieOrStorage(config.access_token);
  if (!token) {
    // Se non siamo in una pagina pubblica e non c'è token, vai a login
    if (window.location.pathname.startsWith("/app/") && window.location.pathname !== "/app/login") {
      console.warn("Nessun token di accesso trovato. Reindirizzamento al login...");
      window.location.href = config.login_url;
    }
    return;
  }

  const url = config.authorize_url;

  fetch(url, {
    method: "GET",
    headers: { Authorization: `Bearer ${token}`, ...getSavedHeaders() },
    redirect: "follow",
    mode: "cors",
    credentials: "include",
  })
    .then((response) => {
      fetchHeader(response.headers);
      if (response.ok) {
        checkLocationAndRedirect(response);
      } else {
        console.warn("Authorization verification status:", response.status);
      }
      return response.json().catch(() => null);
    })
    .then((response) => {
      if (response) {
        saveTokens(response);
      }
    })
    .catch((error) => {
      console.error("Authorization check network error:", error);
    });
}

function checkLocationAndRedirect(response) {
  if (window.location.pathname.startsWith("/app/") && window.location.pathname !== "/app/login") {
    return;
  }
  const locationHeader = response.headers.get("Location");
  if (locationHeader && locationHeader !== window.location.href) {
    window.location.href = locationHeader;
  }
}

function saveTokens(response) {
  if (!response || !response.data) return;
  const access_token = response.data.token ? response.data.token.access_token : null;
  const strapi_token = response.data.strapiToken ? response.data.strapiToken.access_token : null;
  if (access_token) {
    localStorage.setItem(config.client_id + "_" + config.access_token, access_token);
    localStorage.setItem("access-token", access_token);
  }
  if (strapi_token) {
    localStorage.setItem(config.client_id + "_" + config.strapi_token, strapi_token);
  }
}
