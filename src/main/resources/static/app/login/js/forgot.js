$(document).ready(function () {
  let $resetPassCode = $("#resetPasswordCode");
  // Ascolta l'evento di input sulla password
  $resetPassCode.on("input", function () {
    validatePasswords();
  });
  let $passwordInput = $("#resetPasswordInput");
  // Ascolta l'evento di input sulla password
  $passwordInput.on("input", function () {
    validatePasswords();
  });
  let $passwordConfirmInput = $("#repeatPasswordInput");
  $passwordConfirmInput.on("input", function () {
    validatePasswords();
  });
});

/** Check Email Input */
document.addEventListener("DOMContentLoaded", () => {
  // Seleziona gli elementi di input e bottone
  const emailInput = document.getElementById("forgotEmailInput");
  const resetButton = document.getElementById("resetButton");

  // Funzione per verificare se entrambi i campi sono pieni
  function checkInputs() {
    const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$/;

    resetButton.disabled =
      !emailInput.value.trim() || !emailReg.test(emailInput.value.trim());
  }

  // Assegna l'evento di input a entrambi i campi
  emailInput.addEventListener("input", checkInputs);

  // Disabilita inizialmente il bottone
  checkInputs();
});

function forgotPassword() {
  const email = document.getElementById("forgotEmailInput").value;
  const browserLanguage = navigator.language;
  const body = {
    templateId: "ACCESS_SPHERE_CODE_RESET_PASSWORD",
    email: email,
  };
  const forgotUrl = config.forgot_password_url + "?locale=" + browserLanguage;
  POST(forgotUrl, null, body).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      console.log(responseData);
      fetchHeader(data.headers);
      return sweetalert(
        "success",
        currentTranslations.forgot_response_title,
        currentTranslations.forgot_response_text.replace("#EMAIL#", email)
      ).then((result) => {
        /* Read more about isConfirmed, isDenied below */
        if (result.isConfirmed) {
          showResetPassword();
        }
      });
    }
  });
}

function validatePasswords() {
  // Seleziona l'input della password e il messaggio di feedback
  let $passwordInput = $("#resetPasswordInput");
  let $passwordFeedback = $("#validationPassword");
  let $passwordConfirmInput = $("#repeatPasswordInput");
  let $passwordConfirmFeedback = $("#validationConfirmPassword");

  let $resetPassCode = $("#resetPasswordCode");

  // La regex per la password
  const passwordRegex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.,])[A-Za-z\d@$!%*?&.,]{8,20}$/;

  // Funzione per verificare la password
  function validatePassword() {
    let password = $passwordInput.val();

    // Controlla se la password rispetta la regex
    if (passwordRegex.test(password)) {
      // Se la password è valida, rimuove il feedback di errore e la classe 'is-invalid'
      $passwordInput.removeClass("is-invalid");
      $passwordFeedback.hide(); // Nasconde il messaggio di errore
    } else {
      // Se la password non è valida, aggiunge la classe 'is-invalid' e mostra il messaggio di errore
      $passwordInput.addClass("is-invalid");
      $passwordFeedback.show(); // Mostra il messaggio di errore
    }

    let passwordConfirm = $passwordConfirmInput.val();
    if (password != passwordConfirm) {
      if (passwordConfirm != "") {
        $passwordConfirmInput.addClass("is-invalid");
        $passwordConfirmFeedback.show(); // Mostra il messaggio di errore
      }
    } else {
      $passwordConfirmInput.removeClass("is-invalid");
      $passwordConfirmFeedback.hide(); // Mostra il messaggio di errore
      if ($resetPassCode.val())
        $("#changePasswordButton").prop("disabled", false);
    }
  }

  validatePassword();
}

function resetPassword() {
  const resetCode = document.getElementById("resetPasswordCode").value;
  const password = document.getElementById("resetPasswordInput").value;
  const confirmPassword = document.getElementById("repeatPasswordInput").value;
  if (password != confirmPassword)
    return sweetalert(
      "error",
      currentTranslations.changePasswordValidationTitle,
      currentTranslations.changePasswordValidationText
    );
  const body = {
    token: resetCode,
    password: encodeBase64(password),
  };
  const forgotUrl = config.reset_password_url;
  POST(forgotUrl, null, body).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      console.log(responseData);
      fetchHeader(data.headers);
      return sweetalert(
        "success",
        currentTranslations.reset_password_response_title,
        currentTranslations.reset_password_response_text
      ).then((result) => {
        /* Read more about isConfirmed, isDenied below */
        if (result.isConfirmed) {
          window.location.reload();
        }
      });
    }
  });
}
