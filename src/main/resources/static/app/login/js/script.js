/*
 * -----------------------------
 * Login Form Validation
 * -----------------------------
 */
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

function togglePassword() {
  const input = document.getElementById("passwordInput");
  const icon = document.getElementById("eyeIcon");

  const isPassword = input.type === "password";
  input.type = isPassword ? "text" : "password";

  // Cambia l'icona
  if (isPassword) {
    icon.classList.remove("fa-eye");
    icon.classList.add("fa-eye-slash");
    icon.style.marginRight = "-2.5px";
  } else {
    icon.classList.remove("fa-eye-slash");
    icon.classList.add("fa-eye");
    icon.style.marginRight = "0px";
  }
}

function togglePassword(inputId) {
  const input = document.getElementById(inputId);
  const icon = input.parentElement.querySelector(".eyeIcon");

  const isPassword = input.type === "password";
  input.type = isPassword ? "text" : "password";

  // Cambia l'icona
  if (isPassword) {
    icon.classList.remove("fa-eye");
    icon.classList.add("fa-eye-slash");
    icon.style.marginRight = "-2.5px";
  } else {
    icon.classList.remove("fa-eye-slash");
    icon.classList.add("fa-eye");
    icon.style.marginRight = "0px";
  }
}

function registerPopup() {
  return sweetalert(
    "error",
    currentTranslations.login_page_sign_up_block_title,
    currentTranslations.login_page_sign_up_block_text,
    true
  );
}
/*
 * -----------------------------
 * END Login Form Validation
 * -----------------------------
 */
let currentPage = "login_section";

function navigate(toId, backwards = false, backpage = "login_section") {
  if (toId === currentPage) return;

  const from = document.getElementById(currentPage);
  const to = document.getElementById(toId);

  const outCls = backwards ? "page--toRight" : "page--toLeft";

  // Rimuovo classi e pulisco eventuali stili inline
  to.classList.remove("page--current", "page--toLeft", "page--toRight");
  to.style.transform = ""; // reset

  const pages = document.querySelectorAll(".page");
  pages.forEach((page) => {
    if (page.id === toId) {
      page.style.transform = backwards
        ? "translateX(-100%)"
        : "translateX(100%)";
    } else if (page.id === backpage) page.style.transform = "translateX(-100%)";
  });

  // Forzo il reflow (per far "registrare" il nuovo stile)
  void to.offsetWidth;

  // Faccio partire l'animazione
  from.classList.add(outCls);
  from.classList.remove("page--current");
  to.classList.add("page--current");

  // Rimuovo lo stile inline per permettere al CSS di prendere il controllo
  to.style.transform = "";

  // Pulizia al termine della transizione
  from.addEventListener("transitionend", function handler() {
    from.classList.remove(outCls);
    from.removeEventListener("transitionend", handler);
  });

  currentPage = toId;
}

function showForgotPassword() {
  navigate("forgot_section");
  const form = document.querySelector("#forgot_password_form");
  form.classList.remove("fade-in");
  void form.offsetWidth; // forza reflow
  form.classList.add("fade-in");
}

function showLogin() {
  navigate("login_section", true);
  const form = document.querySelector("#loginForm");
  form.classList.remove("fade-in");
  void form.offsetWidth; // forza reflow
  form.classList.add("fade-in");
}

function showResetPassword() {
  navigate("reset_section");
  const form = document.querySelector("#reset_password_form");
  form.classList.remove("fade-in");
  void form.offsetWidth; // forza reflow
  form.classList.add("fade-in");
}

function showSelectOTPMethod() {
  navigate("select_method_section");
  const form = document.querySelector("#select_otp_form");
  form.classList.remove("fade-in");
  void form.offsetWidth; // forza reflow
  form.classList.add("fade-in");
}

function showOTP() {
  navigate("otp_section");
  const form = document.querySelector("#otp_form");
  form.classList.remove("fade-in");
  void form.offsetWidth; // forza reflow
  form.classList.add("fade-in");
}

function OTPType(otp) {
  const otpName = {
    TOTP: "Google Authenticator, Microsoft Authenticator...",
    EMAIL: "marco.rossi@email.com",
  };
  return otpName[otp] || "Unknown OTP type";
}

/*
 * --------------------------------------------------------------
 * OTP Section
 * --------------------------------------------------------------
 */
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("otp-form");
  const inputs = [...form.querySelectorAll("input[type=number]")];
  const submit = form.querySelector("button[type=submit]");

  const handleKeyDown = (e) => {
    // Consenti copia/incolla: Ctrl+V / Cmd+V / Shift+Insert
    if ((e.ctrlKey || e.metaKey) && (e.key === "v" || e.key === "V")) return;
    if (e.shiftKey && e.key === "Insert") return;

    // Consenti solo numeri e alcuni tasti di controllo
    const allowed =
      /^[0-9]$/.test(e.key) ||
      ["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab"].includes(e.key);

    if (!allowed) {
      e.preventDefault();
    }

    // Gestione Backspace/Delete per tornare indietro
    if (e.key === "Backspace" || e.key === "Delete") {
      const index = inputs.indexOf(e.target);
      if (index > 0) {
        inputs[index - 1].value = "";
        inputs[index - 1].focus();
      }
    }
  };

  //const handleKeyDown = (e) => {
  //  if (
  //    !/^[0-9]{1}$/.test(e.key) &&
  //    e.key !== "Backspace" &&
  //    e.key !== "Delete" &&
  //    e.key !== "Tab" &&
  //    !e.metaKey
  //  ) {
  //    e.preventDefault();
  //  }
  //
  //  if (e.key === "Delete" || e.key === "Backspace") {
  //    const index = inputs.indexOf(e.target);
  //    if (index > 0) {
  //      inputs[index - 1].value = "";
  //      inputs[index - 1].focus();
  //    }
  //  }
  //};

  const handleInput = (e) => {
    const { target } = e;
    const index = inputs.indexOf(target);

    // forza solo una cifra
    target.value = target.value.replace(/\D/g, "").slice(0, 1);

    if (target.value && index < inputs.length - 1) {
      inputs[index + 1].focus();
    } else if (target.value && index === inputs.length - 1) {
      submit.focus();
    }

    // Triggera eventualmente enableVerifyBtn
    if (typeof enableVerifyBtn === "function") {
      enableVerifyBtn();
    }
  };

  //const handleInput = (e) => {
  //  const { target } = e;
  //  const index = inputs.indexOf(target);
  //  if (target.value) {
  //    if (index < inputs.length - 1) {
  //      inputs[index + 1].focus();
  //    } else {
  //      submit.focus();
  //    }
  //  }
  //};

  const handleFocus = (e) => {
    e.target.select();
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const text = e.clipboardData.getData("text").replace(/\D/g, ""); // solo numeri
    if (!text) return;

    const digits = text.slice(0, inputs.length).split(""); // taglia se troppo lungo

    digits.forEach((digit, index) => {
      inputs[index].value = digit;
    });

    // Sposta il focus sull'input successivo all'ultimo riempito
    const nextIndex =
      digits.length < inputs.length ? digits.length : inputs.length - 1;
    inputs[nextIndex].focus();

    // Trigger manuale su enableVerifyBtn se presente
    if (typeof enableVerifyBtn === "function") {
      enableVerifyBtn();
    }
  };

  //const handlePaste = (e) => {
  //  e.preventDefault();
  //  const text = e.clipboardData.getData("text");
  //  if (!new RegExp(`^[0-9]{${inputs.length}}$`).test(text)) {
  //    return;
  //  }
  //  const digits = text.split("");
  //  inputs.forEach((input, index) => (input.value = digits[index]));
  //  submit.focus();
  //};

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
