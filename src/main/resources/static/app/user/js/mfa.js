function showMethodOTPSelect() {
  const otp = document.getElementById("mfa-select-method-card");
  if (otp) otp.style.display = "block";

  const otpMethods = getOTP();
  const selectOtpMethod = document.getElementById("show-select-method");
  Object.entries(otpMethods).forEach(([key, value]) => {
    const firstElement = Object.keys(otpMethods)[0];
    selectOtpMethod.innerHTML += `<label
                    style="
                      border-color: rgb(
                        64 71 79 / var(--tw-border-opacity, 1)
                      ) !important;
                      padding: 15px 15px 0px 15px;
                    "
                    class="flex items-center gap-4 rounded-xl border border-solid border-[#40474f] flex-row-reverse clickable"
                  >
                    <input
                      type="radio"
                      class="float-end h-5 w-5 accent-white border-2 bg-[#2c3035] border-[#40474f] text-transparent checked:border-white checked:bg-[image:--radio-dot-svg] focus:outline-none focus:ring-0 checked:focus:border-white"
                      name="mfa_method"
                      ${key == firstElement ? "checked" : ""}
                      value="${key}"
                      required=""
                    />
                    <div class="flex grow flex-col">
                      <p class="text-white text-sm font-medium leading-normal">
                        ${key}
                      </p>
                      <p
                        class="text-[#a2aab3] text-sm font-normal leading-normal"
                      >
                        ${OTPType(key)}
                      </p>
                    </div> </label
                  >`;
  });

  const description = document.getElementById("mfa-description-card");
  if (description) description.style.display = "none";

  /* ---------- GESTIONE SELEZIONE RADIO & BOTTONE ---------- */

  // Funzione che abilita/disabilita il bottone e salva il metodo scelto
  const updateButtonState = () => {
    const selected = selectOtpMethod.querySelector(
      'input[name="mfa_method"]:checked'
    );
    const confirmSelectBtn = document.getElementById(
      "mfa_page_select_method_button"
    );
    const client_id = localStorage.getItem("Client-ID");
    confirmSelectBtn.disabled = !selected; // abilita se c’è qualcosa di selezionato
    if (selected) {
      localStorage.setItem(client_id + "_mfa_methods", selected.value);
    }
  };

  // Imposta lo stato iniziale (il primo è già "checked")
  updateButtonState();

  // Delego l’ascolto al container: un solo listener per tutti i radio
  selectOtpMethod.addEventListener("change", updateButtonState);
}

function showOTPLabelSelect() {
  const client_id = localStorage.getItem("Client-ID");
  const mfaMethod = localStorage.getItem(client_id + "_mfa_methods");

  const otp = document.getElementById("mfa-config-section");
  if (otp) otp.style.display = "block";
  const otpConfig = document.getElementById("mfa-select-method-card");
  if (otpConfig) otpConfig.style.display = "none";
  const description = document.getElementById("mfa-description-card");
  if (description) description.style.display = "none";

  if (mfaMethod && mfaMethod == "EMAIL") {
    const setupCard = document.getElementById("mfa_page_setup_card");
    if (setupCard) setupCard.style.display = "none";
    const confirmCard = document.getElementById("mfa_page_confirm_card");
    if (confirmCard) confirmCard.style.display = "block";
  }
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
