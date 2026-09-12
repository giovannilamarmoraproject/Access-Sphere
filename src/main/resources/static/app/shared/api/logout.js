function logout() {
  console.log("Logout process started...");

  // Pulisce immediatamente tutti i token, storage e cookie di sessione
  if (typeof cleanStorageAndCookies === "function") {
    cleanStorageAndCookies();
  } else {
    try {
      localStorage.clear();
      if (typeof deleteSelectedCookies === "function") deleteSelectedCookies();
    } catch (e) {
      console.error("Storage clean error:", e);
    }
  }

  const token = typeof getCookieOrStorage === "function" && typeof config !== "undefined" && config.access_token
    ? getCookieOrStorage(config.access_token)
    : (localStorage.getItem("access-token") || "");

  const logoutUrl = typeof config !== "undefined" && config.logout_url
    ? config.logout_url
    : (window.location.origin + "/v1/oAuth/2.0/logout");

  const loginUrl = typeof config !== "undefined" && config.login_url
    ? config.login_url
    : (window.location.origin + "/app/login");

  let redirected = false;
  const doRedirect = () => {
    if (!redirected) {
      redirected = true;
      window.location.href = loginUrl;
    }
  };

  // Notifica il server della disconnessione (best effort)
  try {
    fetch(logoutUrl, {
      method: "POST",
      mode: "cors",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(typeof getSavedHeaders === "function" ? getSavedHeaders() : {}),
      },
      credentials: "include",
    })
      .then((response) => {
        if (typeof fetchHeader === "function") fetchHeader(response.headers);
        return response.json().catch(() => null);
      })
      .finally(() => {
        doRedirect();
      })
      .catch((error) => {
        console.warn("Logout notify ended:", error);
        doRedirect();
      });
  } catch (err) {
    doRedirect();
  }

  // Fallback di sicurezza: reindirizzamento garantito entro 350ms
  setTimeout(doRedirect, 350);
}
