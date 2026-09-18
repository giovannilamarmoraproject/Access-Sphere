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
            (window.accessSphereConfig && window.accessSphereConfig.redirect_uri) ||
            (window.location.origin + window.location.pathname),
        include_user_data:
            (window.accessSphereConfig &&
                (window.accessSphereConfig.include_user_data)) ??
            false,
        include_user_info:
            (window.accessSphereConfig &&
                (window.accessSphereConfig.include_user_info)) ??
            false,
        state:
            (window.accessSphereConfig && window.accessSphereConfig.state) ||
            null,
        prompt:
            (window.accessSphereConfig && window.accessSphereConfig.prompt) ||
            null,
        authorize: "/v1/oauth2/authorize",
        token: "/v1/oauth2/token",
        logout: "/v1/oauth2/logout",
    };

    /**
     * Risolve dinamicamente il redirect_uri da utilizzare nelle richieste OAuth.
     * Dà priorità a window.accessSphereConfig.redirect_uri, poi ad authConfig.redirect_uri,
     * e infine all'URL corrente comprensivo di pathname.
     */
    function getRedirectUri() {
        if (window.accessSphereConfig && window.accessSphereConfig.redirect_uri) {
            return window.accessSphereConfig.redirect_uri;
        }
        if (authConfig.redirect_uri) {
            return authConfig.redirect_uri;
        }
        return window.location.origin + window.location.pathname;
    }

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
                (window.accessSphereConfig && window.accessSphereConfig.client_id);
        }
        const access_token_name = client_id
            ? client_id + "_access-token"
            : "access-token";
        localStorage.setItem("Client-ID", client_id);
        authConfig.access_token_name = access_token_name;
        authConfig.client_id = client_id;

        // Sincronizza dinamicamente le configurazioni fornite da window.accessSphereConfig
        if (window.accessSphereConfig) {
            if (window.accessSphereConfig.redirect_uri) {
                authConfig.redirect_uri = window.accessSphereConfig.redirect_uri;
            }
            if (window.accessSphereConfig.include_user_data !== undefined) {
                authConfig.include_user_data = window.accessSphereConfig.include_user_data;
            }
            if (window.accessSphereConfig.include_user_info !== undefined) {
                authConfig.include_user_info = window.accessSphereConfig.include_user_info;
            }
            if (window.accessSphereConfig.state !== undefined) {
                authConfig.state = window.accessSphereConfig.state;
            }
            if (window.accessSphereConfig.prompt !== undefined) {
                authConfig.prompt = window.accessSphereConfig.prompt;
            }
        }
    }

    /**
     * ---------------------------------
     * END Global Configuration
     * ---------------------------------
     */

    /**
     * ---------------------------------
     * Caricamento degli Script
     * ---------------------------------
     */
    let restScriptLoaded = false;
    let utilsScriptLoaded = false;

    // Funzione per caricare lo script solo la prima volta
    function ensureScriptLoaded(callback) {
        if (restScriptLoaded && utilsScriptLoaded) {
            callback();
            return;
        }

        /**
         * ---------------------------------
         * Caricamento dello Script Utils (prima)
         * ---------------------------------
         */
        if (!utilsScriptLoaded) {
            loadScript(
                authConfig.accessSphereUrl + "/app/shared/utils.js",
                function () {
                    utils.fetchHeader = fetchHeader;
                    utils.getSavedHeaders = getSavedHeaders;
                    utils.getCookie = getCookie;
                    utilsScriptLoaded = true;
                    callback();
                }
            );
            /**
             * ---------------------------------
             * Caricamento dello Script REST Dopo il primo
             * ---------------------------------
             */
            //if (!restScriptLoaded)
            loadScript(
                authConfig.accessSphereUrl + "/app/shared/api/rest.js",
                function () {
                    api.GET = GET;
                    api.POST = POST;
                    api.PUT = PUT;
                    api.PATCH = PATCH;
                    api.DELETE = DELETE;
                    restScriptLoaded = true;
                    callback();
                }
            );
        }
    }

    /**
     * ---------------------------------
     * END Caricamento degli Script
     * ---------------------------------
     */

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            console.log("✅ DOMContentLoaded event");
            startAuthenticationFlow();
        });
    } else {
        console.log("🔍 DOM already loaded");
        startAuthenticationFlow();
    }

    /**
     * ---------------------------------
     * Authentication Flow
     * ---------------------------------
     */
    function startAuthenticationFlow() {
        console.log("🚀 Starting authentication flow...");

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
    }

    /**
     * ---------------------------------
     * END Authentication Flow
     * ---------------------------------
     */

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
            redirect_uri: getRedirectUri(),
            scope: "openid",
            response_type: "token",
        });

        if (authConfig.include_user_data) {
            urlParams.append("include_user_data", "true");
        }
        if (authConfig.include_user_info) {
            urlParams.append("include_user_info", "true");
        }

        const state =
            (window.accessSphereConfig && window.accessSphereConfig.state) ||
            authConfig.state;
        if (state) {
            urlParams.append("state", state);
        }
        const prompt =
            (window.accessSphereConfig && window.accessSphereConfig.prompt) ||
            authConfig.prompt;
        if (prompt) {
            urlParams.append("prompt", prompt);
        }

        const url =
            authConfig.accessSphereUrl +
            authConfig.authorize +
            "?" +
            urlParams.toString();

        ensureScriptLoaded(() => {
            if (typeof api.GET !== "function") {
                return;
            }

            if (typeof utils.getSavedHeaders !== "function") {
                return;
            }
            console.log("🔍 Scripts available now, proceding with the 🔐 /authorize");

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
                    console.error("❌ Authorize Fetch error:", error);
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
        const state =
            urlParams.get("state") ||
            (window.accessSphereConfig && window.accessSphereConfig.state) ||
            authConfig.state;
        const prompt =
            urlParams.get("prompt") ||
            (window.accessSphereConfig && window.accessSphereConfig.prompt) ||
            authConfig.prompt;
        authConfig.lastReceivedState = state;

        const params = new URLSearchParams({
            client_id: authConfig.client_id,
            code: code,
            redirect_uri: getRedirectUri(),
            grant_type: "authorization_code",
            scope: scope,
        });

        if (state) {
            params.append("state", state);
        }
        if (prompt) {
            params.append("prompt", prompt);
        }
        if (authConfig.include_user_data) {
            params.append("include_user_data", "true");
        }
        if (authConfig.include_user_info) {
            params.append("include_user_info", "true");
        }

        const url =
            authConfig.accessSphereUrl + authConfig.token + "?" + params.toString();

        const cleanUrl = window.location.origin + window.location.pathname;
        window.history.replaceState(null, "", cleanUrl);

        ensureScriptLoaded(() => {
            if (typeof api.POST !== "function") {
                return;
            }

            if (typeof utils.getSavedHeaders !== "function") {
                return;
            }
            console.log("🔍 Scripts available now, proceding with the 🗝️ /token");

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
                    console.error("❌ Token Fetch error:", error);
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
                    console.error("❌ Logout Fetch error:", error);
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

            // Salva l'access token di sessione (se presente)
            if (token && token.access_token) {
                localStorage.setItem(
                    authConfig.access_token_name,
                    token.access_token
                );
            }

            const receivedState =
                (response.data && response.data.state) ||
                authConfig.lastReceivedState ||
                new URLSearchParams(window.location.search).get("state") ||
                authConfig.state ||
                null;

            // Propaga l'intero oggetto di risposta al Frontend tramite CustomEvent
            const event = new CustomEvent(eventType.value, {
                detail: {
                    success: true,
                    timestamp: Date.now(),
                    action: eventType.name,
                    data: response,
                    state: receivedState,
                    error: null,
                },
                bubbles: true,
            });
            document.dispatchEvent(event);
        } else checkAndHandleError(response, eventType);
    }

    function checkAndHandleError(response, eventType) {
        if (response && response.error) {
            localStorage.clear();
            const receivedState =
                authConfig.lastReceivedState ||
                new URLSearchParams(window.location.search).get("state") ||
                authConfig.state ||
                null;
            const event = new CustomEvent(eventType.value, {
                bubbles: true, // Importante per far propagare l'evento
                detail: {
                    success: false,
                    timestamp: Date.now(),
                    action: eventType.name,
                    data: null,
                    state: receivedState,
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
