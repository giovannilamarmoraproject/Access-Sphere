$(document).ready(function () {
  const urlParams = new URLSearchParams(window.location.search);
  const access_token = urlParams.get("access-token");
  const session_id = urlParams.get("session-id");

  if (access_token) {
    if (session_id) localStorage.setItem("Session-ID", session_id);
    localStorage.setItem(
      config.client_id + "_" + config.access_token,
      access_token
    );
    const cleanUrl = window.location.origin + window.location.pathname;
    window.history.replaceState(null, "", cleanUrl);
  }
  authorizeRequest(); // Se il codice non è presente, richiama authorizeToken
});

function authorizeRequest() {
  let token = getCookieOrStorage(config.access_token);

  const url = config.authorize_url;

  fetch(url, {
    method: "GET",
    headers: { Authorization: `Bearer ${token}`, ...getSavedHeaders() },
    redirect: "follow",
    mode: "cors", // no-cors, *cors, same-origin
    credentials: "include",
  })
    .then((response) => {
      fetchHeader(response.headers);
      if (response.ok) {
        checkLocationAndRedirect(response);
      } else if (!response.ok) {
        localStorage.clear();
        console.error("Authorization check failed.");
        window.location.href = config.login_url;
      }
      return response.json();
    })
    .then((response) => {
      saveTokens(response);
    })
    .catch((error) => {
      //cleanStorageAndCookies();
      console.error("Authorization check failed.", error);
      window.location.href = config.login_url;
    });
}

function checkLocationAndRedirect(response) {
  const locationHeader = response.headers.get("Location");
  const redirectUrl =
    locationHeader ??
    (response.url !== config.authorize_url ? response.url : null);

  if (redirectUrl) {
    window.location.href = redirectUrl;
  }
}

function saveTokens(response) {
  const access_token = response.data.token.access_token;
  const strapi_token = response.data.strapiToken.access_token;
  localStorage.setItem(
    config.client_id + "_" + config.access_token,
    access_token
  );
  localStorage.setItem(
    config.client_id + "_" + config.strapi_token,
    strapi_token
  );
}
