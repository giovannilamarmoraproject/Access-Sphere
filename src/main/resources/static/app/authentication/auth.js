(function () {
  /**
   * ---------------------------------
   * Global Configuration
   * ---------------------------------
   */
  let authConfig = {
    baseUrl: window.location.origin,
    accessSphereUrl: getBaseUrl(),
    redirect_uri:
      window.accessSphereConfig.redirect_uri || window.location.origin,
    authorize: "/v1/oAuth/2.0/authorize",
    token: "/v1/oAuth/2.0/token",
    logout: "/v1/oAuth/2.0/logout",
  };

  window.api = window.api || {}; // Se api non esiste, lo crea come oggetto vuoto
  window.utils = window.utils || {}; // Se api non esiste, lo crea come oggetto vuoto

  const events = {
    ACCESS_SPHERE_AUTH: {
      name: "authorize",
      value: "ACCESS_SPHERE_AUTH",
    },
    ACCESS_SPHERE_TOKEN: {
      name: "token",
      value: "ACCESS_SPHERE_TOKEN",
    },
    ACCESS_SPHERE_LOGOUT: {
      name: "logout",
      value: "ACCESS_SPHERE_LOGOUT",
    },
  };

  function getBaseUrl() {
    const scripts = document.getElementsByTagName("script");
    for (let script of scripts) {
      if (script.src.includes("auth.js")) {
        const url = new URL(script.src);
        return url.origin;
      }
    }
    return "";
  }

  function loadScript(url, callback) {
    const script = document.createElement("script");
    script.src = url;
    script.onload = callback;
    document.head.appendChild(script);
  }

  function setClientIDGlobally(client_id = null) {
    if (!client_id) {
      client_id =
        localStorage.getItem("Client-ID") ||
        window.accessSphereConfig.client_id;
    }
    const access_token_name = client_id
      ? client_id + "_access-token"
      : "access-token";
    const strapi_token_name = client_id
      ? client_id + "_strapi-token"
      : "strapi-token";
    localStorage.setItem("Client-ID", client_id);
    authConfig.access_token_name = access_token_name;
    authConfig.strapi_token_name = strapi_token_name;
    authConfig.client_id = client_id;
  }
  /**
   * ---------------------------------
   * END Global Configuration
   * ---------------------------------
   */

  /**
   * ---------------------------------
   * Caricamento dello Script REST
   * ---------------------------------
   */
  let scriptLoaded = false;

  // Funzione per caricare lo script solo la prima volta
  function ensureScriptLoaded(callback) {
    if (scriptLoaded) {
      callback();
      return;
    }

    loadScript(
      authConfig.accessSphereUrl + "/app/shared/api/rest.js",
      function () {
        api.GET = GET;
        api.POST = POST;
        api.PUT = PUT;
        api.PATCH = PATCH;
        api.DELETE = DELETE;
        scriptLoaded = true;
        callback();
      }
    );
  }
  /**
   * ---------------------------------
   * END Caricamento dello Script REST
   * ---------------------------------
   */

  document.addEventListener("DOMContentLoaded", function () {
    /**
     * ---------------------------------
     * Caricamento dello Script Utils
     * ---------------------------------
     */
    loadScript(
      authConfig.accessSphereUrl + "/app/shared/utils.js",
      function () {
        utils.fetchHeader = fetchHeader;
        utils.getSavedHeaders = getSavedHeaders;
        utils.getCookie = getCookie;
      }
    );
    /**
     * ---------------------------------
     * END Caricamento dello Script Utils
     * ---------------------------------
     */

    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get("code");
    const access_token = urlParams.get("access-token");
    const session_id = urlParams.get("session-id");
    const client_id = urlParams.get("client-id");

    setClientIDGlobally(client_id);

    if (code) {
      exchangeCodeForToken(code);
    } else {
      if (access_token) {
        if (session_id) localStorage.setItem("Session-ID", session_id);
        localStorage.setItem(authConfig.access_token_name, access_token);
        const cleanUrl = window.location.origin + window.location.pathname;
        window.history.replaceState(null, "", cleanUrl);
      }
      authorizeToken();
    }
  });

  /**
   * ---------------------------------
   * @Authorize
   * ---------------------------------
   */
  function authorizeToken() {
    const token = localStorage.getItem(authConfig.access_token_name);
    //|| utils.getCookie("access-token");

    const urlParams = new URLSearchParams({
      client_id: authConfig.client_id,
      grant_type: "authorization_code",
      access_type: "online",
      redirect_uri: authConfig.baseUrl,
      scope: "openid",
      response_type: "token",
    });

    const url =
      authConfig.accessSphereUrl +
      authConfig.authorize +
      "?" +
      urlParams.toString();

    ensureScriptLoaded(() => {
      api
        .GET(url, token)
        .then((response) => {
          //throw new Error("Error");
          utils.fetchHeader(response.headers);
          if (response.ok) {
            const locationHeader = response.headers.get("Location");
            const redirectUrl =
              locationHeader ?? (response.url !== url ? response.url : null);

            if (redirectUrl) {
              window.location.href = redirectUrl;
              return;
            }
          }
          return response.json();
        })
        .then((response) => {
          checkResponse(response, events.ACCESS_SPHERE_AUTH);
        })
        .catch((error) => {
          console.error("Authorize Fetch error:", error);
        });
    });
  }
  /**
   * ---------------------------------
   * @END_Authorize
   * ---------------------------------
   */

  /**
   * ---------------------------------
   * @Token
   * ---------------------------------
   */
  function exchangeCodeForToken(code) {
    const urlParams = new URLSearchParams(window.location.search);
    const scope = urlParams.get("scope");

    const params = new URLSearchParams({
      client_id: authConfig.client_id,
      code: code,
      redirect_uri: authConfig.baseUrl,
      grant_type: "authorization_code",
      scope: scope,
    });

    const url =
      authConfig.accessSphereUrl + authConfig.token + "?" + params.toString();

    const cleanUrl = window.location.origin + window.location.pathname;
    window.history.replaceState(null, "", cleanUrl);

    ensureScriptLoaded(() => {
      api
        .POST(url, null, null)
        .then((response) => {
          utils.fetchHeader(response.headers);
          return response.json();
        })
        .then((data) => {
          checkResponse(data, events.ACCESS_SPHERE_TOKEN);
        })
        .catch((error) => {
          console.error("Token Fetch error:", error);
        });
    });
  }
  /**
   * ---------------------------------
   * @END_Token
   * ---------------------------------
   */

  /**
   * ---------------------------------
   * @Logout
   * ---------------------------------
   */
  function logout() {
    const token =
      localStorage.getItem(authConfig.access_token_name) ||
      utils.getCookie("access-token");

    const urlParams = new URLSearchParams({
      client_id: authConfig.client_id,
    });

    const url =
      authConfig.accessSphereUrl +
      authConfig.logout +
      "?" +
      urlParams.toString();
    ensureScriptLoaded(() => {
      api
        .POST(url, token, null)
        .then((response) => {
          utils.fetchHeader(response.headers);
          return response.json();
        })
        .then((data) => {
          checkResponse(data, events.ACCESS_SPHERE_LOGOUT);
        })
        .finally((res) => {
          localStorage.clear();
          location.reload();
        })
        .catch((error) => {
          console.error("Logout Fetch error:", error);
        });
    });
  }
  /**
   * ---------------------------------
   * @END_Logout
   * ---------------------------------
   */

  function checkResponse(response, eventType) {
    if (response && response.data) {
      const token = response.data.token;
      const strapiToken = response.data.strapiToken;
      if (token && strapiToken) {
        if (token.access_token)
          localStorage.setItem(
            authConfig.access_token_name,
            token.access_token
          );
        if (strapiToken.access_token)
          localStorage.setItem(
            authConfig.strapi_token_name,
            strapiToken.access_token
          );
      }
      const event = new CustomEvent(eventType.value, {
        detail: {
          success: true,
          timestamp: Date.now(),
          action: eventType.name,
          data: response,
          error: null,
        },
        bubbles: true,
      });
      document.dispatchEvent(event);
    } else checkAndHandleError(response, eventType);
  }

  function checkAndHandleError(response, eventType) {
    if (response.error) {
      localStorage.clear();
      const event = new CustomEvent(eventType.value, {
        bubbles: true, // Importante per far propagare l'evento
        detail: {
          success: false,
          timestamp: Date.now(),
          action: eventType.name,
          data: null,
          error: response, // Qui passi l'errore
        },
      });
      document.dispatchEvent(event);
    }
  }

  /**
   * ---------------------------------
   * Export function to be used into other Service
   * ---------------------------------
   */
  window.AccessSphere = {
    logout: () => logout(),
  };
})();
