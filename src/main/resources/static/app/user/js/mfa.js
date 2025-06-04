function showOTPLabelSelect() {
  const otp = document.getElementById("mfa-config-section");
  if (otp) otp.style.display = "block";
  const description = document.getElementById("mfa-description-card");
  if (description) description.style.display = "none";
}

function goBack() {
  const urlParams = window.location.href;
  const identifier = urlParams.split("mfa/")[1];
  window.location.href = "/app/users/details/" + identifier;
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
      const mfaLogos = document.getElementById("mfa-logos");
      if (mfaLogos) {
        mfaLogos.style.marginTop = "8%";
        mfaLogos.style.marginBottom = "5%";
      }
    }
  });
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
