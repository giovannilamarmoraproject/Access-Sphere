function showForgotPassword() {
  document.getElementById("login").style.display = "none";
  document.getElementById("forgot").style.display = "block";
  document.getElementById("reset").style.display = "none";
}

function showLogin() {
  document.getElementById("forgot").style.display = "none";
  document.getElementById("login").style.display = "block";
  document.getElementById("reset").style.display = "none";
}

function showResetPassword() {
  document.getElementById("login").style.display = "none";
  document.getElementById("forgot").style.display = "none";
  document.getElementById("reset").style.display = "block";
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
