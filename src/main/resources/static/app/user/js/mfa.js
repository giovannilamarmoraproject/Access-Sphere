function showOTPLabelSelect() {
  const otp = document.getElementById("mfa-config-section");
  if (otp) otp.style.display = "block";
  const description = document.getElementById("mfa-description-card");
  if (description) description.style.display = "none";
  const reInfo = document.getElementById("btn-mfa-reinfo");
  if (reInfo) reInfo.style.display = "flex";
}

function showDescriptionCard() {
  const otp = document.getElementById("mfa-config-section");
  if (otp) otp.style.display = "none";
  const description = document.getElementById("mfa-description-card");
  if (description) description.style.display = "block";
  const reInfo = document.getElementById("btn-mfa-reinfo");
  if (reInfo) reInfo.style.display = "none";
}

function goBack() {
  const urlParams = window.location.href;
  if (urlParams.includes("mfa/")) {
    const rawId = urlParams.split("mfa/")[1];
    const identifier = rawId.split("?")[0].split("#")[0];
    if (identifier) {
      window.location.href = "/app/users/details/" + identifier;
      return;
    }
  }
  window.location.href = "/app/users";
}

document.addEventListener("DOMContentLoaded", function () {
  const mfaMethod = document.getElementById("mfa-method");
  const button = document.getElementById("mfa_page_setup_select_proceed");

  mfaMethod.addEventListener("change", function () {
    if (mfaMethod.value) {
      button.removeAttribute("disabled");
    } else {
      // button.setAttribute("disabled", "true");
    }
  });
});

function setupMFA() {
  const label = document.getElementById("mfa-method").value;
  const urlParams = window.location.href;
  const identifier = urlParams.split("mfa/")[1];
  const setupMFAUrl = window.location.origin + "/v1/mfa/setup";
  const token = getCookieOrStorage(config.access_token);
  const body = {
    identifier: identifier,
    label: label,
    type: "totp",
    generateImage: true,
  };
  POST(setupMFAUrl, token, body).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      const setupCard = document.getElementById("mfa_page_setup_card");
      if (setupCard) setupCard.style.display = "none";
      const confirmCard = document.getElementById("mfa_page_confirm_card");
      if (confirmCard) confirmCard.style.display = "block";
      const qrCode = document.getElementById("mfa_page_confirm_card_image");
      if (qrCode) qrCode.src = responseData.data.base64QrCodeImage;

      // Popola la chiave segreta per la configurazione manuale
      const secretInput = document.getElementById("mfa-manual-secret");
      if (secretInput && responseData.data.secret) {
        secretInput.value = responseData.data.secret;
      }
      const accountElem = document.getElementById("mfa-manual-account");
      if (accountElem) {
        accountElem.textContent = identifier ? decodeURIComponent(identifier.split("?")[0].split("#")[0]) : "Access Sphere";
      }

      const mfaLogos = document.getElementById("mfa-logos");
      if (mfaLogos) {
        mfaLogos.style.marginTop = "8%";
        mfaLogos.style.marginBottom = "5%";
      }
    }
  });
}

function toggleManualMfaKey() {
  const container = document.getElementById("mfa-manual-key-container");
  const chevron = document.getElementById("manual-key-chevron");
  if (!container) return;
  const isHidden = container.classList.contains("hidden");
  if (isHidden) {
    container.classList.remove("hidden");
    if (chevron) chevron.classList.add("rotate-180");
  } else {
    container.classList.add("hidden");
    if (chevron) chevron.classList.remove("rotate-180");
  }
}

function copyMfaSecret() {
  const secretInput = document.getElementById("mfa-manual-secret");
  if (!secretInput || !secretInput.value) return;

  const copyBtn = document.getElementById("btn-copy-mfa-secret");
  const onCopied = () => {
    if (copyBtn) {
      copyBtn.innerHTML = '<i class="fa-solid fa-check text-green-400"></i> <span class="text-green-400 font-semibold">Copiato!</span>';
      setTimeout(() => {
        copyBtn.innerHTML = '<i class="fa-regular fa-copy text-xs"></i> <span>Copia</span>';
      }, 2500);
    }
  };

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(secretInput.value).then(onCopied).catch(() => {
      secretInput.select();
      document.execCommand("copy");
      onCopied();
    });
  } else {
    secretInput.select();
    document.execCommand("copy");
    onCopied();
  }
}

function confirmMFA() {
  const label = document.getElementById("mfa-method").value;
  const urlParams = window.location.href;
  const identifier = urlParams.split("mfa/")[1];
  const setupMFAUrl = window.location.origin + "/v1/mfa/confirm";
  const token = getCookieOrStorage(config.access_token);
  const body = {
    identifier: identifier,
    label: label,
    type: "totp",
    otp: enableVerifyBtn(),
  };
  POST(setupMFAUrl, token, body).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      return sweetalert(
        "success",
        currentTranslations.mfa_page_setup_success_title,
        currentTranslations.mfa_page_setup_success_text
      ).then((result) => {
        /* Read more about isConfirmed, isDenied below */
        if (result.isConfirmed) {
          const origin = window.location.origin;

          // Costruisci l'URL completo aggiungendo il path
          const fullUrl = `${origin}/app/users`;

          // Reindirizza l'utente al nuovo URL
          window.location.href = fullUrl;
        }
      });
    }
  });
}
