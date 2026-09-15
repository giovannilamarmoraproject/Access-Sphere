let translations = {};
let errorCode = {};
let currentLanguage = "en"; // Default language
let currentTranslations = {};

const SUPPORTED_LANGUAGES = ["it", "en", "fr", "es", "de"];

function detectLanguage() {
  const savedLang = localStorage.getItem("access_sphere_language") || localStorage.getItem("app_language") || "auto";
  if (SUPPORTED_LANGUAGES.includes(savedLang)) {
    currentLanguage = savedLang;
    return;
  }
  // "auto" or undefined: detect from browser
  const browserLanguage = (navigator.language || navigator.userLanguage || "en").slice(0, 2).toLowerCase();
  currentLanguage = SUPPORTED_LANGUAGES.includes(browserLanguage) ? browserLanguage : "en";
}

// 1. Synchronously populate from cache if present
try {
  detectLanguage();
  const cached = sessionStorage.getItem("i18n_cache_" + currentLanguage);
  if (cached) {
    currentTranslations = JSON.parse(cached);
    translations[currentLanguage] = currentTranslations;
  }
} catch (e) {}

// 2. FOUC Guard: prevents flashing of Italian text when target language is not Italian
(function initFoucGuard() {
  if (typeof document === "undefined") return;
  detectLanguage();
  if (currentLanguage !== "it") {
    if (!document.getElementById("i18n-fouc-guard")) {
      const style = document.createElement("style");
      style.id = "i18n-fouc-guard";
      style.textContent = "html:not(.i18n-ready) body { opacity: 0 !important; } html.i18n-ready body { opacity: 1 !important; transition: opacity 0.12s ease-in; }";
      if (document.head) {
        document.head.appendChild(style);
      } else {
        document.addEventListener("DOMContentLoaded", () => {
          if (!document.documentElement.classList.contains("i18n-ready")) {
            document.head.appendChild(style);
          }
        });
      }
      setTimeout(() => {
        document.documentElement.classList.add("i18n-ready");
      }, 180);
    }
  } else {
    document.documentElement.classList.add("i18n-ready");
  }
})();

async function loadTranslations() {
  detectLanguage();

  // If in-memory translations exist from cache, translate immediately before network
  if (currentTranslations && Object.keys(currentTranslations).length > 0) {
    translateDOM();
    try {
      applyTranslations();
    } catch (e) {}
    document.documentElement.classList.add("i18n-ready");
  }

  try {
    const response = await fetch(`/app/i18n/languages/${currentLanguage}.json`);
    if (response.ok) {
      currentTranslations = await response.json();
      translations[currentLanguage] = currentTranslations;
      try {
        sessionStorage.setItem("i18n_cache_" + currentLanguage, JSON.stringify(currentTranslations));
      } catch (e) {}
    } else {
      const fallbackLang = (currentLanguage === "en") ? "it" : "en";
      const fallback = await fetch(`/app/i18n/languages/${fallbackLang}.json`);
      currentTranslations = await fallback.json();
      translations[currentLanguage] = currentTranslations;
    }
  } catch (error) {
    console.warn("Error loading language file, falling back to modular language json:", error);
    try {
      const fallbackLang = (currentLanguage === "en") ? "it" : "en";
      const fallback = await fetch(`/app/i18n/languages/${fallbackLang}.json`);
      currentTranslations = await fallback.json();
      translations[currentLanguage] = currentTranslations;
    } catch (e) {
      console.error("Error loading translations:", e);
    }
  }

  if (!translations[currentLanguage]) {
    translations[currentLanguage] = currentTranslations || {};
  }

  // 1. Translate elements with data-i18n
  translateDOM();

  // 2. Safely apply legacy ID-based translations
  try {
    applyTranslations();
  } catch (e) {
    console.warn("Legacy applyTranslations warning:", e);
  }

  // 3. Keep language radio selector in sync
  syncLanguageUI(currentLanguage);

  // 4. Reveal page smoothly with zero flicker
  document.documentElement.classList.add("i18n-ready");
}

function t(key, defaultText = "") {
  const dict = currentTranslations || (typeof translations !== "undefined" ? translations[currentLanguage] : null);
  if (!dict) return defaultText || key;
  return dict[key] !== undefined ? dict[key] : (defaultText || key);
}
window.t = t;

function translateDOM() {
  const dict = currentTranslations || translations[currentLanguage];
  if (!dict) return;

  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.getAttribute("data-i18n");
    if (!key || dict[key] === undefined) return;
    const translation = dict[key];

    // Check if translation contains HTML markup
    if (/<[a-z][\s\S]*>/i.test(translation)) {
      el.innerHTML = translation;
    } else {
      el.textContent = translation;
    }
  });

  document.querySelectorAll("[data-i18n-placeholder]").forEach((el) => {
    const key = el.getAttribute("data-i18n-placeholder");
    if (dict[key] !== undefined) {
      el.placeholder = dict[key];
    }
  });
  document.querySelectorAll("[data-i18n-title]").forEach((el) => {
    const key = el.getAttribute("data-i18n-title");
    if (dict[key] !== undefined) {
      el.title = dict[key];
    }
  });
}

function syncLanguageUI(lang) {
  const savedPref = localStorage.getItem("access_sphere_language") || localStorage.getItem("app_language") || "auto";
  const radioAuto = document.getElementById("lang-radio-auto");
  if (radioAuto) radioAuto.checked = (savedPref === "auto");

  SUPPORTED_LANGUAGES.forEach((code) => {
    const radio = document.getElementById("lang-radio-" + code);
    if (radio) {
      radio.checked = (savedPref === code || (savedPref === "auto" && currentLanguage === code));
    }
    const btn = document.getElementById("lang-btn-" + code);
    if (btn) {
      btn.classList.toggle("text-purple-300", savedPref === code || (savedPref === "auto" && currentLanguage === code));
      btn.classList.toggle("font-bold", savedPref === code || (savedPref === "auto" && currentLanguage === code));
    }
  });

  // Floating and Navigation Language Switcher Buttons
  const btnAuto = document.getElementById("lang-btn-auto");
  if (btnAuto) {
    btnAuto.classList.toggle("text-purple-300", savedPref === "auto");
    btnAuto.classList.toggle("font-bold", savedPref === "auto");
  }
}

async function switchLanguage(lang) {
  if (lang === "auto") {
    localStorage.setItem("access_sphere_language", "auto");
    localStorage.setItem("app_language", "auto");
    detectLanguage();
  } else {
    if (!SUPPORTED_LANGUAGES.includes(lang)) lang = "en";
    currentLanguage = lang;
    localStorage.setItem("access_sphere_language", lang);
    localStorage.setItem("app_language", lang);
  }
  await loadTranslations();
  if (typeof loadErrorCode === "function") await loadErrorCode();
  window.dispatchEvent(new CustomEvent("languageChanged", { detail: { language: currentLanguage, mode: lang } }));
}

async function loadErrorCode() {
  try {
    const response = await fetch("/app/i18n/errorCode.json");
    const error_translations = await response.json();
    detectLanguage();
    errorCode = error_translations[currentLanguage] || error_translations["en"];
  } catch (error) {
    console.error("Error loading error code:", error);
  }
}

function getErrorCode(error) {
  if (typeof isUnauthorizedError === "function" && isUnauthorizedError(error)) {
    if (!window.location.pathname.includes("/login")) {
      console.warn("🔒 getErrorCode: Unauthorized error detected, performing logout...", error);
      if (typeof logout === "function") {
        logout();
      } else {
        if (typeof cleanStorageAndCookies === "function") cleanStorageAndCookies();
        else localStorage.clear();
        window.location.href = (typeof config !== "undefined" && config && config.login_url)
          ? config.login_url
          : (window.location.origin + "/app/login");
      }
    }
  }

  const errorData = errorCode[error.exception];
  if (errorData) {
    if (errorData.message) return errorData;
    else {
      return {
        title: errorData.title,
        message: error.message,
      };
    }
  }
  return {
    title: error.exception,
    message: error.message,
  };
}

function applyLanguageOld(id, text, innerHTML = false) {
  const data = document.getElementById(id);
  if (data && !innerHTML) data.textContent = text;
  else if (data && innerHTML) data.innerHTML = text;
}

function applyLanguage(selector, text, innerHTML = false) {
  if (text === undefined || text === null) return;
  let elements;

  if (selector.startsWith(".")) {
    // Se inizia con ".", selezioniamo tutti gli elementi con quella classe
    elements = document.querySelectorAll(selector);
  } else {
    // Altrimenti, assumiamo che sia un ID
    const element = document.getElementById(selector);
    elements = element ? [element] : [];
  }

  // Applichiamo il testo a tutti gli elementi trovati
  elements.forEach((element) => {
    if (!innerHTML) {
      element.textContent = text;
    } else {
      element.innerHTML = text;
    }
  });
}

function applyLanguageInputPlaceholder(selector, text) {
  if (text === undefined || text === null) return;
  let elements;

  if (selector.startsWith(".")) {
    // Se inizia con ".", selezioniamo tutti gli elementi con quella classe
    elements = document.querySelectorAll(selector);
  } else {
    // Altrimenti, assumiamo che sia un ID
    const element = document.getElementById(selector);
    elements = element ? [element] : [];
  }

  // Applichiamo il testo a tutti gli elementi trovati
  elements.forEach((element) => {
    element.placeholder = text;
  });
}

document.addEventListener("DOMContentLoaded", loadTranslations);
document.addEventListener("DOMContentLoaded", loadErrorCode);

/*
 *----------------------------------------------------------
 * i18n
 *----------------------------------------------------------
 */
function applyTranslations() {
  const dict = (translations && translations[currentLanguage]) || currentTranslations || {};
  if (!dict || Object.keys(dict).length === 0) return;

  /*
   *----------------------------------------------------------
   * Login Page Section
   *----------------------------------------------------------
   */
  applyLanguage(
    "login_page_tab_title",
    dict.login_page_tab_title
  );
  applyLanguage(
    "login_page_forgot_password",
    dict.login_page_forgot_password
  );
  applyLanguage("loginButton", dict.loginButton);
  applyLanguage(".googleLogin", dict.googleLogin);
  applyLanguage(
    "login_page_sign_up_text",
    dict.login_page_sign_up_text
  );
  if (dict.footer_copyright_text) {
    let customCopyright = null;
    let appName = "Access Sphere";
    try {
      const s = localStorage.getItem("access_sphere_settings");
      if (s) {
        const parsed = JSON.parse(s);
        if (parsed.footerCopyright && parsed.footerCopyright.trim()) {
          customCopyright = parsed.footerCopyright;
        }
        if (parsed.appName && parsed.appName.trim()) {
          appName = parsed.appName.trim();
        }
      }
      const directCopyright = localStorage.getItem("access_sphere_footer_copyright");
      if (directCopyright && directCopyright.trim()) {
        customCopyright = directCopyright.trim();
      }
      const savedName = localStorage.getItem("access_sphere_app_name");
      if (savedName && savedName.trim()) {
        appName = savedName.trim();
      }
    } catch (e) {}

    let textToApply = customCopyright;
    if (!textToApply) {
      textToApply = dict.footer_copyright_text.replace(
        "#YEAR#",
        new Date().getFullYear()
      );
      if (appName && appName !== "Access Sphere") {
        textToApply = textToApply.replace(/Access Sphere/g, appName);
      }
    } else {
      textToApply = textToApply.replace("#YEAR#", new Date().getFullYear()).replace(/202[0-9]/g, new Date().getFullYear());
    }

    applyLanguage(
      ".footer_copyright_text",
      textToApply,
      true
    );
  }
  /*
   *----------------------------------------------------------
   * Forgot Page Section
   *----------------------------------------------------------
   */
  applyLanguage(
    "forgot_page_nav_title",
    translations[currentLanguage].forgot_page_nav_title
  );
  applyLanguage(
    "forgot_page_title",
    translations[currentLanguage].forgot_page_title
  );
  applyLanguage(
    "forgot_page_subtitle",
    translations[currentLanguage].forgot_page_subtitle
  );
  applyLanguage(
    "forgot_page_reset_button",
    translations[currentLanguage].forgot_page_reset_button
  );
  /*
   *----------------------------------------------------------
   * Reset Password Page Section
   *----------------------------------------------------------
   */
  applyLanguage(
    "reset_page_title",
    translations[currentLanguage].reset_page_title
  );
  applyLanguage(
    "reset_page_nav_title",
    translations[currentLanguage].reset_page_nav_title
  );
  applyLanguage(
    "reset_page_subtitle",
    translations[currentLanguage].reset_page_subtitle
  );
  applyLanguage(
    "reset_page_input_code",
    translations[currentLanguage].reset_page_input_code
  );
  applyLanguage(
    "reset_page_input_code",
    translations[currentLanguage].reset_page_input_code
  );
  applyLanguageInputPlaceholder(
    "resetPasswordCode",
    translations[currentLanguage].resetPasswordCode
  );
  applyLanguageInputPlaceholder(
    "resetPasswordInput",
    translations[currentLanguage].resetPasswordInput
  );
  applyLanguageInputPlaceholder(
    "repeatPasswordInput",
    translations[currentLanguage].repeatPasswordInput
  );
  applyLanguage(
    "changePasswordButton",
    translations[currentLanguage].changePasswordButton
  );
  /**
   * OTP
   */
  applyLanguage(
    "otp_select_method_page_nav_title",
    translations[currentLanguage].otp_select_method_page_nav_title
  );
  applyLanguage(
    "otp_select_method_page_subtitle",
    translations[currentLanguage].otp_select_method_page_subtitle
  );
  applyLanguage(
    "otp_verification_code_button",
    translations[currentLanguage].otp_verification_code_button
  );
  applyLanguage(
    "totp_verification_code_title",
    translations[currentLanguage].totp_verification_code_title
  );
  applyLanguage(
    "totp_verification_code_text",
    translations[currentLanguage].totp_verification_code_text
  );
  applyLanguage(
    "totp_verification_code_check",
    translations[currentLanguage].totp_verification_code_check
  );
  applyLanguage("verifyOTP", translations[currentLanguage].verifyOTP);
  /**
   * Menu
   */
  applyLanguage("menu_user", translations[currentLanguage].menu_user);
  applyLanguage("menu_register", translations[currentLanguage].menu_register);
  applyLanguage(
    "menu_version",
    translations[currentLanguage].menu_version,
    true
  );
  /**
   * users.html
   */
  applyLanguage("users_title", translations[currentLanguage].users_title, true);
  applyLanguage(
    "users_table_col_1",
    translations[currentLanguage].users_table_col_1
  );
  applyLanguage(
    "users_table_col_2",
    translations[currentLanguage].users_table_col_2
  );
  applyLanguage(
    "users_table_col_3",
    translations[currentLanguage].users_table_col_3
  );
  applyLanguage(
    "users_table_col_4",
    translations[currentLanguage].users_table_col_4
  );
  applyLanguage(
    "users_table_col_5",
    translations[currentLanguage].users_table_col_5
  );
  applyLanguage(
    "users_table_col_6",
    translations[currentLanguage].users_table_col_6
  );
  applyLanguage(
    "users_table_col_7",
    translations[currentLanguage].users_table_col_7
  );
  /**
   * user.html
   */
  applyLanguage(
    "user_details_title",
    translations[currentLanguage].user_details_title,
    true
  );
  applyLanguage(
    "user_details_name",
    translations[currentLanguage].user_details_name
  );
  applyLanguage(
    "user_details_birth",
    translations[currentLanguage].user_details_birth
  );
  applyLanguage(
    "user_details_gender",
    translations[currentLanguage].user_details_gender
  );
  applyLanguage(
    "user_details_nationality",
    translations[currentLanguage].user_details_nationality
  );
  applyLanguage(
    "user_details_ssn",
    translations[currentLanguage].user_details_ssn
  );
  applyLanguage(
    "user_details_email",
    translations[currentLanguage].user_details_email
  );
  applyLanguage(
    "user_details_phoneNumber",
    translations[currentLanguage].user_details_phoneNumber
  );
  applyLanguage(
    "user_details_occupation",
    translations[currentLanguage].user_details_occupation
  );
  applyLanguage(
    "user_details_education",
    translations[currentLanguage].user_details_education
  );
  applyLanguage(".status_active", translations[currentLanguage].status_active);
  applyLanguage(
    ".status_blocked",
    translations[currentLanguage].status_blocked
  );
  applyLanguage(
    ".status_not_active",
    translations[currentLanguage].status_not_active
  );
  applyLanguage("unlock_user", translations[currentLanguage].unlock_user, true);
  applyLanguage("lock_user", translations[currentLanguage].lock_user, true);
  applyLanguage(
    "user_details_status",
    translations[currentLanguage].user_details_status
  );
  applyLanguage(
    "user_details_user_roles",
    translations[currentLanguage].user_details_user_roles,
    true
  );
  applyLanguage(
    "user_details_mfa",
    translations[currentLanguage].user_details_mfa,
    true
  );
  applyLanguage(
    "user_details_mfa_enable",
    translations[currentLanguage].user_details_mfa_enable,
    true
  );
  applyLanguage(
    "user_details_mfa_disable",
    translations[currentLanguage].user_details_mfa_disable,
    true
  );
  applyLanguage(
    ".user_details_mfa_delete",
    translations[currentLanguage].user_details_mfa_delete,
    true
  );
  applyLanguage(
    "user_details_roles",
    translations[currentLanguage].user_details_roles
  );
  /**
   * MFA Dettagli into User Details
   */
  applyLanguage(
    ".user_details_mfa_enabled",
    translations[currentLanguage].user_details_mfa_enabled
  );
  applyLanguage(
    ".user_details_mfa_creationDate",
    translations[currentLanguage].user_details_mfa_creationDate
  );
  applyLanguage(
    ".user_details_mfa_updateDate",
    translations[currentLanguage].user_details_mfa_updateDate
  );
  applyLanguage(
    ".user_details_mfa_type",
    translations[currentLanguage].user_details_mfa_type
  );
  applyLanguage(
    ".user_details_mfa_label",
    translations[currentLanguage].user_details_mfa_label
  );
  applyLanguage(
    ".user_details_mfa_confirmed",
    translations[currentLanguage].user_details_mfa_confirmed
  );
  applyLanguage(
    ".user_details_mfa_confirmed_true",
    translations[currentLanguage].user_details_mfa_confirmed_true
  );
  applyLanguage(
    ".user_details_mfa_confirmed_false",
    translations[currentLanguage].user_details_mfa_confirmed_false
  );
  /**
   * MFA Dettagli into User Details
   */
  applyLanguage(
    "user_details_attributes",
    translations[currentLanguage].user_details_attributes,
    true
  );
  applyLanguage(
    ".user_details_attributes_strapi-token",
    translations[currentLanguage].user_details_attributes_strapi_token
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings",
    translations[currentLanguage].user_details_attributes_money_stats_settings
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings_currency",
    translations[currentLanguage]
      .user_details_attributes_money_stats_settings_currency
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings_liveWallets",
    translations[currentLanguage]
      .user_details_attributes_money_stats_settings_liveWallets
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings_cryptoCurrency",
    translations[currentLanguage]
      .user_details_attributes_money_stats_settings_cryptoCurrency
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings_currencySymbol",
    translations[currentLanguage]
      .user_details_attributes_money_stats_settings_currencySymbol
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings_completeRequirement",
    translations[currentLanguage]
      .user_details_attributes_money_stats_settings_completeRequirement
  );
  applyLanguage(
    ".user_details_attributes_money_stats_settings_cryptoCurrencySymbol",
    translations[currentLanguage]
      .user_details_attributes_money_stats_settings_cryptoCurrencySymbol
  );
  applyLanguage(
    "user_details_mfa_new",
    translations[currentLanguage].user_details_mfa_new,
    true
  );
  /**
   * Register User Page
   */
  applyLanguage(
    "register_title",
    translations[currentLanguage].register_title,
    true
  );
  applyLanguage(
    "register_form_name",
    translations[currentLanguage].register_form_name
  );
  applyLanguage(
    "register_form_surname",
    translations[currentLanguage].register_form_surname
  );
  applyLanguage(
    "register_form_birth",
    translations[currentLanguage].register_form_birth
  );
  applyLanguage(
    "register_form_gender",
    translations[currentLanguage].register_form_gender
  );
  applyLanguage(
    "register_form_gender_choose",
    translations[currentLanguage].register_form_gender_choose
  );
  applyLanguage(
    "register_form_gender_male",
    translations[currentLanguage].register_form_gender_male
  );
  applyLanguage(
    "register_form_gender_female",
    translations[currentLanguage].register_form_gender_female
  );
  applyLanguage(
    "register_form_gender_other",
    translations[currentLanguage].register_form_gender_other
  );
  applyLanguage(
    "register_form_nationality",
    translations[currentLanguage].register_form_nationality
  );
  applyLanguage(
    "register_form_nationality_choose",
    translations[currentLanguage].register_form_nationality_choose
  );
  applyLanguage(
    "register_form_ssn",
    translations[currentLanguage].register_form_ssn
  );
  applyLanguage(
    "register_form_country_code",
    translations[currentLanguage].register_form_country_code
  );
  applyLanguage(
    "register_form_country_code_choose",
    translations[currentLanguage].register_form_country_code_choose
  );
  applyLanguage(
    "register_form_phone",
    translations[currentLanguage].register_form_phone
  );
  applyLanguage(
    "register_form_profile",
    translations[currentLanguage].register_form_profile
  );
  applyLanguage(
    "inputGroupFileAddon04",
    translations[currentLanguage].inputGroupFileAddon04
  );
  applyLanguage(
    "register_form_username",
    translations[currentLanguage].register_form_username
  );
  applyLanguage(
    "validationPassword",
    translations[currentLanguage].validationPassword,
    true
  );
  applyLanguage(
    "register_form_confirm_pass",
    translations[currentLanguage].register_form_confirm_pass
  );
  applyLanguage(
    "validationConfirmPassword",
    translations[currentLanguage].validationConfirmPassword
  );
  applyLanguage(
    "register_form_occupation",
    translations[currentLanguage].register_form_occupation
  );
  applyLanguage(
    "register_form_education",
    translations[currentLanguage].register_form_education
  );
  applyLanguage(
    "register_form_client_choose",
    translations[currentLanguage].register_form_client_choose
  );
  applyLanguage("add_role_btn", translations[currentLanguage].add_role_btn);
  applyLanguage("save_role_btn", translations[currentLanguage].save_role_btn);
  applyLanguage(
    "register_form_roles",
    translations[currentLanguage].register_form_roles
  );
  applyLanguage(
    "register_form_roles_choose",
    translations[currentLanguage].register_form_roles_choose
  );
  applyLanguage(
    "register_form_attributes",
    translations[currentLanguage].register_form_attributes
  );
  applyLanguage(
    "register_form_check",
    translations[currentLanguage].register_form_check
  );
  applyLanguage(
    "register_form_submit",
    translations[currentLanguage].register_form_submit
  );
  /**
   * Roles User Page
   */
  applyLanguage(
    "roles_page_title",
    translations[currentLanguage].roles_page_title,
    true
  );
  /**
   * Edit User Page
   */
  applyLanguage(
    "edit_user_title",
    translations[currentLanguage].edit_user_title,
    true
  );
  applyLanguage(
    "edit_user_title_btn",
    translations[currentLanguage].edit_user_title_btn,
    true
  );
  applyLanguage(
    "edit_user_button",
    translations[currentLanguage].edit_user_button
  );
  /**
   * Delete User Page
   */
  applyLanguage(
    "delete_user_text",
    translations[currentLanguage].delete_user_text,
    true
  );
  /**
   * MFA User Page
   */
  applyLanguage(
    "mfa_page_description_title",
    translations[currentLanguage].mfa_page_description_title
  );
  applyLanguage(
    "mfa_page_description_text_1",
    translations[currentLanguage].mfa_page_description_text_1,
    true
  );
  applyLanguage(
    "mfa_page_description_text_2",
    translations[currentLanguage].mfa_page_description_text_2,
    true
  );
  applyLanguage(
    "mfa_page_description_button",
    translations[currentLanguage].mfa_page_description_button
  );
  applyLanguage(
    ".mfa_page_back_button",
    translations[currentLanguage].mfa_page_back_button,
    true
  );
  applyLanguage(
    "mfa_page_setup_title",
    translations[currentLanguage].mfa_page_setup_title
  );
  applyLanguage(
    "mfa_page_setup_card_title",
    translations[currentLanguage].mfa_page_setup_card_title
  );
  applyLanguage(
    "mfa_page_setup_card_text",
    translations[currentLanguage].mfa_page_setup_card_text
  );
  applyLanguage(
    "mfa_page_setup_select_choose",
    translations[currentLanguage].mfa_page_setup_select_choose
  );
  applyLanguage(
    "mfa_page_setup_select_proceed",
    translations[currentLanguage].mfa_page_setup_select_proceed
  );
}
