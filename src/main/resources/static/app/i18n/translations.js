let translations = {};
let errorCode = {};
let currentLanguage = "en"; // Default language

let currentTranslations = {};

async function loadTranslations() {
  try {
    const response = await fetch("/app/i18n/translations.json");
    translations = await response.json();
    detectLanguage();
    currentTranslations = translations[currentLanguage];
    applyTranslations();
  } catch (error) {
    console.error("Error loading translations:", error);
  }
}

async function loadErrorCode() {
  try {
    const response = await fetch("/app/i18n/errorCode.json");
    const error_translations = await response.json();
    detectLanguage();
    errorCode = error_translations[currentLanguage];
  } catch (error) {
    console.error("Error loading error code:", error);
  }
}

function getErrorCode(error) {
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

function detectLanguage() {
  const browserLanguage = navigator.language.slice(0, 2); // Get first 2 chars (e.g., "en", "it")
  currentLanguage = translations[browserLanguage] ? browserLanguage : "en";
}

function applyLanguageOld(id, text, innerHTML = false) {
  const data = document.getElementById(id);
  if (data && !innerHTML) data.textContent = text;
  else if (data && innerHTML) data.innerHTML = text;
}

function applyLanguage(selector, text, innerHTML = false) {
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

document.addEventListener("DOMContentLoaded", loadTranslations);
document.addEventListener("DOMContentLoaded", loadErrorCode);

/*
 *----------------------------------------------------------
 * i18n
 *----------------------------------------------------------
 */
function applyTranslations() {
  applyLanguage("pageTitle", translations[currentLanguage].pageTitle);
  applyLanguage("title", translations[currentLanguage].title);
  applyLanguage("loginTitle", translations[currentLanguage].loginTitle, true);
  applyLanguage("emailLabel", translations[currentLanguage].emailLabel);
  applyLanguage("passwordLabel", translations[currentLanguage].passwordLabel);
  applyLanguage("forgotPassword", translations[currentLanguage].forgotPassword);
  applyLanguage("loginButton", translations[currentLanguage].loginButton);
  applyLanguage("googleLogin", translations[currentLanguage].googleLogin);
  applyLanguage("forgotTitle", translations[currentLanguage].forgotTitle);
  applyLanguage(
    "forgotEmailLabel",
    translations[currentLanguage].forgotEmailLabel
  );
  applyLanguage("backToLogin", translations[currentLanguage].backToLogin);
  applyLanguage("resetButton", translations[currentLanguage].resetButton);
  applyLanguage("resetTitle", translations[currentLanguage].resetTitle);
  applyLanguage(
    "resetPasswordLabel",
    translations[currentLanguage].resetPasswordLabel
  );
  applyLanguage(
    "resetPasswordCodeLabel",
    translations[currentLanguage].resetPasswordCodeLabel
  );
  applyLanguage(
    "repeatPasswordLabel",
    translations[currentLanguage].repeatPasswordLabel
  );
  applyLanguage(
    "changePasswordButton",
    translations[currentLanguage].changePasswordButton
  );
  applyLanguage(
    "footerText",
    translations[currentLanguage].footerText.replace(
      "#YEAR#",
      new Date().getFullYear()
    ),
    true
  );
  /**
   * Menu
   */
  applyLanguage("menu_user", translations[currentLanguage].menu_user);
  applyLanguage("menu_register", translations[currentLanguage].menu_register);
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
    "user_details_roles",
    translations[currentLanguage].user_details_roles
  );
  applyLanguage(
    "user_details_attributes",
    translations[currentLanguage].user_details_attributes,
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
}
