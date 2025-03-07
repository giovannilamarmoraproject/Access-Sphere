let translations = {};
let errorCode = {};
let currentLanguage = "en"; // Default language

async function loadTranslations() {
  try {
    const response = await fetch("login/translations.json");
    translations = await response.json();
    detectLanguage();
    applyTranslations();
  } catch (error) {
    console.error("Error loading translations:", error);
  }
}

function applyTranslations() {
  document.getElementById("pageTitle").textContent =
    translations[currentLanguage].pageTitle;
  document.getElementById("title").textContent =
    translations[currentLanguage].title;
  document.getElementById("loginTitle").textContent =
    translations[currentLanguage].loginTitle;
  document.getElementById("emailLabel").textContent =
    translations[currentLanguage].emailLabel;
  document.getElementById("passwordLabel").textContent =
    translations[currentLanguage].passwordLabel;
  document.getElementById("forgotPassword").textContent =
    translations[currentLanguage].forgotPassword;
  document.getElementById("loginButton").textContent =
    translations[currentLanguage].loginButton;
  document.getElementById("googleLogin").textContent =
    translations[currentLanguage].googleLogin;
  document.getElementById("forgotTitle").textContent =
    translations[currentLanguage].forgotTitle;
  document.getElementById("forgotEmailLabel").textContent =
    translations[currentLanguage].forgotEmailLabel;
  document.getElementById("backToLogin").textContent =
    translations[currentLanguage].backToLogin;
  document.getElementById("resetButton").textContent =
    translations[currentLanguage].resetButton;
  document.getElementById("resetTitle").textContent =
    translations[currentLanguage].resetTitle;
  document.getElementById("resetPasswordLabel").textContent =
    translations[currentLanguage].resetPasswordLabel;
  document.getElementById("repeatPasswordLabel").textContent =
    translations[currentLanguage].repeatPasswordLabel;
  document.getElementById("changePasswordButton").textContent =
    translations[currentLanguage].changePasswordButton;
  document.getElementById("footerText").innerHTML =
    translations[currentLanguage].footerText.replace("#YEAR#", new Date().getFullYear());
}

async function loadErrorCode() {
  try {
    const response = await fetch("login/errorCode.json");
    const error_translations = await response.json();
    detectLanguage();
    errorCode = error_translations[currentLanguage];
    console.log("Language " + currentLanguage);
    console.log(errorCode);
  } catch (error) {
    console.error("Error loading error code:", error);
  }
}

function getErrorCode(error){
    const errorData = errorCode[error.exception];
    if(errorData) {
        if (errorData.message)
            return errorData;
        else {
            return {
                "title": errorData.title,
                "message": error.message
            };
        }
    }
    return {
      "title": error.exception
    };
}

function detectLanguage() {
  const browserLanguage = navigator.language.slice(0, 2); // Get first 2 chars (e.g., "en", "it")
  currentLanguage = translations[browserLanguage] ? browserLanguage : "en";
}

document.addEventListener("DOMContentLoaded", loadTranslations);
document.addEventListener("DOMContentLoaded", loadErrorCode);
