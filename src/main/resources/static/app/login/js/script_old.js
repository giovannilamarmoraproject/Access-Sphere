function showForgotPassword() {
  document.getElementById("login").style.display = "none";
  document.getElementById("forgot").style.display = "block";
  document.getElementById("reset").style.display = "none";
  document.getElementById("otp-page").style.display = "none";
  document.getElementById("website_url").style.display = "block";
  document.getElementById("title").style.textAlign = "left";
}

function showLogin() {
  document.getElementById("forgot").style.display = "none";
  document.getElementById("login").style.display = "block";
  document.getElementById("reset").style.display = "none";
  document.getElementById("otp-page").style.display = "none";
  document.getElementById("website_url").style.display = "block";
  document.getElementById("title").style.textAlign = "left";
}

function showResetPassword() {
  document.getElementById("login").style.display = "none";
  document.getElementById("forgot").style.display = "none";
  document.getElementById("reset").style.display = "block";
  document.getElementById("otp-page").style.display = "none";
  document.getElementById("website_url").style.display = "block";
  document.getElementById("title").style.textAlign = "left";
}

function showOTP() {
  document.getElementById("login").style.display = "none";
  document.getElementById("forgot").style.display = "none";
  document.getElementById("reset").style.display = "none";
  document.getElementById("website_url").style.display = "none";
  document.getElementById("title").style.textAlign = "center";
  document.getElementById("otp-page").style.display = "block";
}

document.addEventListener("DOMContentLoaded", () => {
  // Seleziona gli elementi di input e bottone
  const emailInput = document.getElementById("emailInput");
  const passwordInput = document.getElementById("passwordInput");
  const submitButton = document.getElementById("loginButton");

  // Funzione per verificare se entrambi i campi sono pieni
  function checkInputs() {
    submitButton.disabled =
      !emailInput.value.trim() || !passwordInput.value.trim();
  }

  // Assegna l'evento di input a entrambi i campi
  emailInput.addEventListener("input", checkInputs);
  passwordInput.addEventListener("input", checkInputs);

  // Disabilita inizialmente il bottone
  checkInputs();
});

/*
 * --------------------------------------------------------------
 * OTP Section
 * --------------------------------------------------------------
 */
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("otp-form");
  const inputs = [...form.querySelectorAll("input[type=text]")];
  const submit = form.querySelector("button[type=submit]");

  const handleKeyDown = (e) => {
    if (
      !/^[0-9]{1}$/.test(e.key) &&
      e.key !== "Backspace" &&
      e.key !== "Delete" &&
      e.key !== "Tab" &&
      !e.metaKey
    ) {
      e.preventDefault();
    }

    if (e.key === "Delete" || e.key === "Backspace") {
      const index = inputs.indexOf(e.target);
      if (index > 0) {
        inputs[index - 1].value = "";
        inputs[index - 1].focus();
      }
    }
  };

  const handleInput = (e) => {
    const { target } = e;
    const index = inputs.indexOf(target);
    if (target.value) {
      if (index < inputs.length - 1) {
        inputs[index + 1].focus();
      } else {
        submit.focus();
      }
    }
  };

  const handleFocus = (e) => {
    e.target.select();
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const text = e.clipboardData.getData("text");
    if (!new RegExp(`^[0-9]{${inputs.length}}$`).test(text)) {
      return;
    }
    const digits = text.split("");
    inputs.forEach((input, index) => (input.value = digits[index]));
    submit.focus();
  };

  inputs.forEach((input) => {
    input.addEventListener("input", handleInput);
    input.addEventListener("keydown", handleKeyDown);
    input.addEventListener("focus", handleFocus);
    input.addEventListener("paste", handlePaste);
  });
});
/*
 * --------------------------------------------------------------
 * END OTP Section
 * --------------------------------------------------------------
 */
