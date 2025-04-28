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
  console.log(otpMethod);
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
  const otp1 = document.getElementById("otp-1").value;
  const otp2 = document.getElementById("otp-2").value;
  const otp3 = document.getElementById("otp-3").value;
  const otp4 = document.getElementById("otp-4").value;
  const otp5 = document.getElementById("otp-5").value;
  const otp6 = document.getElementById("otp-6").value;
  if (otp1 && otp2 && otp3 && otp4 && otp5 && otp6) {
    const verifyBtn = document.getElementById("mfa_page_setup_confirm_proceed");
    if (verifyBtn) verifyBtn.removeAttribute("disabled");
    return otp1 + otp2 + otp3 + otp4 + otp5 + otp6;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("mfa-config-form");
  if (!form) return;
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
