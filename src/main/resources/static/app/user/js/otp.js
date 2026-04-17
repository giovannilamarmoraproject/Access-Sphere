const totpLabel = {
  GOOGLE_AUTHENTICATION: "google-authenticator",
  MICROSOFT_AUTHENTICATOR: "microsoft-authenticator",
  AUTHY: "authy",
  LASTPASS_AUTHENTICATOR: "lastpass-authenticator",
  DUO_MOBILE: "duo-mobile",
  FREE_OTP: "free-otp",
  AEGIS: "aegis",
  AND_OTP: "and-otp",
  ONE_PASSWORD: "1password",
  BIT_WARDEN: "bitwarden",
  KEEPASS: "keepass",
  EN_PASS: "enpass",
  DASH_LANE: "dashlane",
};

document.addEventListener("DOMContentLoaded", function () {
  const otpMethod = document.getElementById("mfa_label");
  if (otpMethod) {
    Object.values(totpLabel).forEach((label) => {
      const formattedLabel = label
        .replace("-", " ")
        .replace(/\b\w/g, (char) => char.toUpperCase());

      otpMethod.innerHTML += `<option value="${label}">${formattedLabel}</option>`;
    });
  }
});

/*
 * --------------------------------------------------------------
 * OTP Section
 * --------------------------------------------------------------
 */
function enableVerifyBtn() {
  const digits = [
    document.getElementById("otp-1").value,
    document.getElementById("otp-2").value,
    document.getElementById("otp-3").value,
    document.getElementById("otp-4").value,
    document.getElementById("otp-5").value,
    document.getElementById("otp-6").value,
  ];

  const verifyBtn = document.getElementById("mfa_page_setup_confirm_proceed");

  const allValid = digits.every((d) => /^\d$/.test(d)); // ogni campo ha 1 cifra

  if (verifyBtn) {
    verifyBtn.disabled = !allValid;
  }

  return allValid ? digits.join("") : undefined;
}

/*
 * --------------------------------------------------------------
 * OTP Section
 * --------------------------------------------------------------
 */
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("mfa-config-form");
  const inputs = [...form.querySelectorAll("input[type=text]")];
  const submit = form.querySelector("button[type=submit]");

  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && (e.key === "v" || e.key === "V")) return;
    if (e.shiftKey && e.key === "Insert") return;

    const allowed =
      /^[0-9]$/.test(e.key) ||
      ["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab"].includes(e.key);

    if (!allowed) {
      e.preventDefault();
      return;
    }

    const index = inputs.indexOf(e.target);

    if (e.key === "Backspace" || e.key === "Delete") {
      // Se l’input ha un valore, lo cancello
      if (e.target.value !== "") {
        e.target.value = "";
      } else if (index > 0) {
        // Se è già vuoto, passo al precedente e lo svuoto
        inputs[index - 1].value = "";
        inputs[index - 1].focus();
      }

      // 🔄 Aggiorna lo stato del bottone
      if (typeof enableVerifyBtn === "function") {
        enableVerifyBtn();
      }

      e.preventDefault();
    }
  };

  //const handleKeyDown = (e) => {
  //  // Consenti copia/incolla: Ctrl+V / Cmd+V / Shift+Insert
  //  if ((e.ctrlKey || e.metaKey) && (e.key === "v" || e.key === "V")) return;
  //  if (e.shiftKey && e.key === "Insert") return;
  //
  //  // Consenti solo numeri e alcuni tasti di controllo
  //  const allowed =
  //    /^[0-9]$/.test(e.key) ||
  //    ["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab"].includes(e.key);
  //
  //  if (!allowed) {
  //    e.preventDefault();
  //  }
  //
  //  // Gestione Backspace/Delete per tornare indietro
  //  if (e.key === "Backspace" || e.key === "Delete") {
  //    const index = inputs.indexOf(e.target);
  //    if (index > 0) {
  //      inputs[index - 1].value = "";
  //      inputs[index - 1].focus();
  //    }
  //  }
  //};

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
