/**
 * ===================================================================
 * ACCESS SPHERE - GLOBAL APPLICATION CONFIGURATION & VERSIONING
 * ===================================================================
 * Modifica qui la versione per aggiornarla automaticamente in tutta l'applicazione.
 * Verrà sincronizzata su Landing page, Login showcase, modali di info e ovunque presente.
 */
const APP_VERSION = "2.0.0";
const appVersion = "v" + APP_VERSION.replace(/^v/i, ""); // Retrocompatibilità

/**
 * Restituisce la versione formattata
 * @param {boolean} prefix Se true include 'v' (es. "v2.0.0"), altrimenti "2.0.0"
 */
function getAppVersion(prefix = false) {
  const clean = APP_VERSION.replace(/^v/i, "");
  return prefix ? "v" + clean : clean;
}

/**
 * Applica automaticamente la versione a tutti gli elementi HTML dedicati nel DOM
 */
function applyAppVersion() {
  const versionWithV = getAppVersion(true);
  const versionPlain = getAppVersion(false);

  if (typeof document !== "undefined") {
    // 1. Elementi generici con attributo data-app-version="v|full|plain"
    document.querySelectorAll("[data-app-version]").forEach((el) => {
      const format = el.getAttribute("data-app-version");
      if (format === "v") {
        el.textContent = versionWithV;
      } else if (format === "full") {
        el.textContent = "Access Sphere " + versionWithV;
      } else {
        el.textContent = versionPlain;
      }
    });

    // 2. Badge showcase versione nella pagina di Login
    const showcaseVersion = document.querySelector(".showcase-version-text");
    if (showcaseVersion) {
      showcaseVersion.textContent = "Access Sphere " + versionWithV;
    }

    // 3. Badge versione nella Landing Page principale
    const landingVersion = document.querySelector(".landing-version-badge");
    if (landingVersion) {
      landingVersion.textContent = versionWithV;
    }

    // 4. Elementi con classe app-version-text
    document.querySelectorAll(".app-version-text").forEach((el) => {
      el.textContent = versionWithV;
    });
  }
}

// Inizializzazione automatica al caricamento del DOM
/**
 * Restituisce l'anno corrente per i copyright
 */
function getCurrentYear() {
  return new Date().getFullYear();
}

/**
 * Calcola e applica automaticamente l'anno corrente a tutti i testi di copyright dell'applicazione
 */
function applyAutoCopyright() {
  const currentYear = getCurrentYear();

  if (typeof document !== "undefined") {
    // 1. Elementi con classe copyright-year o attributo data-current-year
    document.querySelectorAll(".copyright-year, [data-current-year]").forEach((el) => {
      el.textContent = currentYear;
    });

    // 2. Elementi con classe footer_copyright_text (se non gestiti direttamente da i18n)
    document.querySelectorAll(".footer_copyright_text").forEach((el) => {
      if (!el.getAttribute("data-i18n-managed")) {
        el.innerHTML = `&copy; ${currentYear} Access Sphere - Tutti i diritti riservati`;
      }
    });

    // 3. Elementi generici con classe auto-copyright
    document.querySelectorAll(".auto-copyright").forEach((el) => {
      el.innerHTML = el.innerHTML.replace(/202[0-9](-202[0-9])?/g, currentYear);
    });
  }
}

// Inizializzazione automatica al caricamento del DOM
if (typeof document !== "undefined") {
  const initAppConfig = () => {
    applyAppVersion();
    applyAutoCopyright();
  };
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initAppConfig);
  } else {
    initAppConfig();
  }
}

function getConfig() {
  const configClientID = "ACCESS-SPHERE-TECH";
  const configRedirectUri = window.location.origin + "/app";

  const urlConfig = {
    baseUrl: window.location.origin,
    authorize: "/v1/oAuth/2.0/authorize",
    token: "/v1/oAuth/2.0/token",
    users: "/v1/users",
    logout: "/v1/oAuth/2.0/logout",
    client: "/v1/clients",
    register: "/v1/users/register",
    edit: "/v1/users/update",
    delete: "/v1/users",
    forgot: "/v1/users/change/password/request",
    reset: "/v1/users/change/password",
    verifyOTP: "/v1/mfa/verify?include_user_data=true",
    param: "?",
    divider: "&",
    access_type: "access_type=online",
    client_id: "client_id=" + configClientID,
    redirect_uri: "redirect_uri=" + configRedirectUri,
    scope: "scope=openid",
    login_type_bearer: "type=bearer",
    login_type_google: "type=google",
    response_type: "response_type=token",
  };

  return {
    client_id: configClientID,
    redirect_uri: configRedirectUri,
    access_token: "access-token",
    strapi_token: "strapi-token",
    login_url: window.location.origin + "/app/login",
    authorize_url:
      urlConfig.baseUrl +
      urlConfig.authorize +
      urlConfig.param +
      urlConfig.client_id +
      urlConfig.divider +
      urlConfig.access_type +
      urlConfig.divider +
      urlConfig.redirect_uri +
      urlConfig.divider +
      urlConfig.scope +
      urlConfig.divider +
      urlConfig.response_type,
    token_url: window.location.origin + urlConfig.token,
    users_url: urlConfig.baseUrl + urlConfig.users,
    logout_url:
      urlConfig.baseUrl +
      urlConfig.logout +
      urlConfig.param +
      urlConfig.client_id,
    client_id_url: urlConfig.baseUrl + urlConfig.client,
    register_user_url: urlConfig.baseUrl + urlConfig.register,
    forgot_password_url: urlConfig.baseUrl + urlConfig.forgot,
    reset_password_url: urlConfig.baseUrl + urlConfig.reset,
    edit_user_url: urlConfig.baseUrl + urlConfig.edit,
    delete_user_url: urlConfig.baseUrl + urlConfig.delete,
    verify_otp_url: urlConfig.baseUrl + urlConfig.verifyOTP,
  };
}

function getVersion() {
  return sweetalert("info", "Access Sphere - " + appVersion);
}
