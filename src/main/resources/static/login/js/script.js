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
