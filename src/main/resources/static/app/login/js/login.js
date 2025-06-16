//const config = getConfig();

document.addEventListener("DOMContentLoaded", function () {
  disableLoader();
  cleanStorageAndCookies();
  const loginForm = document.getElementById("loginForm");
  const verifyOTPForm = document.getElementById("otp-form");

  // Event listener per il form
  if (loginForm) {
    //cleanStorageAndCookies();
    loginForm.addEventListener("submit", async function (event) {
      event.preventDefault(); // Previene il comportamento predefinito di invio del form
      await login();
    });
  }
  // Event listener per il form
  if (verifyOTPForm) {
    //cleanStorageAndCookies();
    verifyOTPForm.addEventListener("submit", async function (event) {
      event.preventDefault(); // Previene il comportamento predefinito di invio del form
      await verifyOTP();
    });
  }
});

async function login() {
  enableLoader();
  const currentUrl = new URL(window.location.href);
  const clientId = currentUrl.searchParams.get("client_id") || config.client_id;
  const redirectUri =
    currentUrl.searchParams.get("redirect_uri") || config.redirect_uri;

  // Chiama la funzione login con i dati raccolti
  await doLogin(clientId, redirectUri);
}

async function doLogin(clientId, redirectUri) {
  if (isLoggingIn()) {
    console.log("🔓 Already logging in...");
    disableLoader();
    return;
  }
  loggingIn();

  console.log("🚀 Executing Login...");
  const email = document.getElementById("emailInput").value;
  const password = document.getElementById("passwordInput").value;
  const encode = btoa(email + ":" + password);

  const url = new URL(config.token_url);
  url.searchParams.set("grant_type", "password");
  if (clientId) url.searchParams.set("client_id", clientId);
  if (redirectUri) url.searchParams.set("redirect_uri", redirectUri);

  try {
    const token = await POST(url, encode, null, "Basic ");
    const responseData = await token.json();

    if (responseData.error) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      checkMFAAndSetupOTP(responseData, clientId);
      const redirect_uri = token.headers.get("location");
      if (redirect_uri) {
        window.location.href = `${redirect_uri}?client-id=${clientId}&access-token=${
          responseData.data.token.access_token
        }&session-id=${token.headers.get("Session-ID")}`;
      }
    }
  } catch (err) {
    console.error("❌ Errore nel login:", err);
    throw err;
  } finally {
    loggingOut(); // Reset dello stato per permettere nuovi login
  }
}

async function verifyOTP() {
  enableLoader();
  console.log("🚀 Executing Verify OTP...");
  const url = new URL(config.verify_otp_url);

  const rememberDeviceValue = document.getElementById("rememberDevice");
  const rememberDevice = rememberDeviceValue.checked;
  const otp = enableVerifyBtn();
  const client_id = localStorage.getItem("Client-ID");
  const token = localStorage.getItem(client_id + "_temp_token");
  const mfaMethod = localStorage.getItem(client_id + "_mfa_methods");
  const currentUrl = new URL(window.location.href);
  const redirectUri =
    currentUrl.searchParams.get("redirect_uri") || config.redirect_uri;
  const body = {
    mfaMethod: mfaMethod,
    otp: otp,
    redirectUri: redirectUri,
    rememberDevice: rememberDevice,
  };
  try {
    const verifyOtp = await POST(url, token, body);
    const response = await verifyOtp.json();

    if (response.error) {
      const error = getErrorCode(response.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(verifyOtp.headers);
      const redirect_uri = verifyOtp.headers.get("location");
      if (redirect_uri) {
        window.location.href = `${redirect_uri}?client-id=${client_id}&access-token=${
          response.data.token.access_token
        }&session-id=${verifyOtp.headers.get("Session-ID")}`;
      }
    }
  } catch (err) {
    console.error("❌ Errore nel login:", err);
    throw err;
  } finally {
    loggingOut(); // Reset dello stato per permettere nuovi login
  }
}

function doGoogleLogin() {
  enableLoader();
  const currentUrl = new URL(window.location.href);
  //const clientId = currentUrl.searchParams.get("client_id") || config.client_id;
  //const redirectUri =
  //  currentUrl.searchParams.get("redirect_uri") || config.redirect_uri;
  const clientId = currentUrl.searchParams.get("client_id");
  const redirectUri = currentUrl.searchParams.get("redirect_uri");

  if (!clientId) {
    disableLoader();
    return sweetalert(
      "error",
      currentTranslations.googleLogin_error_title,
      currentTranslations.googleLogin_error_text
    );
  }

  const url = new URL(window.location.origin + "/v1/oAuth/2.0/authorize");
  url.searchParams.set("access_type", "online");
  url.searchParams.set(
    "scope",
    "openid https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
  );
  if (clientId) url.searchParams.set("client_id", clientId);
  if (redirectUri) url.searchParams.set("redirect_uri", redirectUri);
  url.searchParams.set("type", "google");
  url.searchParams.set("response_type", "code");

  window.location.href = url.toString();
  disableLoader();
}

function checkMFAAndSetupOTP(response, client_id) {
  // Verifica che esista il token MFA
  if (response?.data?.token?.mfa_methods) {
    // Salvo dati temporanei
    localStorage.setItem("Client-ID", client_id);
    localStorage.setItem(
      client_id + "_temp_token",
      response.data.token.temp_token.access_token
    );

    const methods = response.data.token.mfa_methods;

    const identifier = response.data.token.identifier;
    localStorage.setItem(client_id + "_identifier", identifier);

    const loginMethod = document.getElementById("show-login-method");
    const otpButton = document.getElementById("otp_verification_code_button");

    if (!loginMethod || !otpButton) return; // elementi non trovati

    // Se ci sono più di un metodo → mostra i radio
    if (methods.length > 1) {
      showSelectOTPMethod(); // Mostra la sezione di scelta OTP
      loginMethod.innerHTML = ""; // pulisco eventuali elementi precedenti

      methods.forEach((method, idx) => {
        loginMethod.insertAdjacentHTML(
          "beforeend",
          `<label
             style="border-color: rgb(64 71 79 / var(--tw-border-opacity, 1)) !important;"
             class="flex items-center gap-4 rounded-xl border border-solid border-[#40474f] p-[15px] flex-row-reverse clickable"
           >
             <input
               type="radio"
               class="h-5 w-5 accent-white border-2 bg-[#2c3035] border-[#40474f] text-transparent
                      checked:border-white checked:bg-[image:--radio-dot-svg]
                      focus:outline-none focus:ring-0 checked:focus:border-white"
               name="mfa_method"
               value="${method}"
               ${idx === 0 ? "checked" : ""}
               required
             />
             <div class="flex grow flex-col">
               <p class="text-white text-sm font-medium leading-normal">
                 ${method}
               </p>
               <p class="text-[#a2aab3] text-sm font-normal leading-normal">
                 ${OTPType(method)}
               </p>
             </div>
           </label>`
        );
      });

      /* ---------- GESTIONE SELEZIONE RADIO & BOTTONE ---------- */

      // Funzione che abilita/disabilita il bottone e salva il metodo scelto
      const updateButtonState = () => {
        const selected = loginMethod.querySelector(
          'input[name="mfa_method"]:checked'
        );
        otpButton.disabled = !selected; // abilita se c’è qualcosa di selezionato
        if (selected) {
          localStorage.setItem(client_id + "_mfa_methods", selected.value);
        }
      };

      // Imposta lo stato iniziale (il primo è già "checked")
      updateButtonState();

      // Delego l’ascolto al container: un solo listener per tutti i radio
      loginMethod.addEventListener("change", updateButtonState);

      // Quando l’utente preme il bottone, apro la pagina OTP col metodo selezionato
      otpButton.addEventListener("click", () => {
        const selected = loginMethod.querySelector(
          'input[name="mfa_method"]:checked'
        );
        const mfaMethod = localStorage.getItem(client_id + "_mfa_methods");
        const temp_token = localStorage.getItem(client_id + "_temp_token");
        if (selected && mfaMethod == "EMAIL") {
          enableLoader();
          const body = {
            locale: translations.currentLanguage,
            identifier: identifier,
            type: mfaMethod,
          };
          const challengeMFA = config.challenge_mfa_url;
          POST(challengeMFA, temp_token, body).then(async (data) => {
            const responseData = await data.json();
            console.log(responseData);
          });
          disableLoader();
        }
        showOTPPage(selected.value);
      });
    } else {
      /* -------- Se c’è un solo metodo MFA: salto direttamente alla pagina OTP -------- */
      const singleMethod = methods[0];
      localStorage.setItem(client_id + "_mfa_methods", singleMethod);
      showOTPPage(singleMethod);
    }
  }
}

function checkMFAAndSetupOTPNew(response, client_id) {
  if (response.data.token && response.data.token.mfa_methods) {
    localStorage.setItem("Client-ID", client_id);
    localStorage.setItem(
      client_id + "_temp_token",
      response.data.token.temp_token.access_token
    );

    showSelectOTPMethod();

    if (response.data.token.mfa_methods.length > 1) {
      const loginMethod = document.getElementById("show-login-method");
      if (loginMethod) {
        const firstElement = response.data.token.mfa_methods[0];
        response.data.token.mfa_methods.forEach((element) => {
          loginMethod.innerHTML += `<label
                style="
                  border-color: rgb(
                    64 71 79 / var(--tw-border-opacity, 1)
                  ) !important;
                "
                class="flex items-center gap-4 rounded-xl border border-solid border-[#40474f] p-[15px] flex-row-reverse clickable"
              >
                <input
                  type="radio"
                  class="h-5 w-5 border-2 border-[#40474f] bg-transparent text-transparent checked:border-white checked:bg-[image:--radio-dot-svg] focus:outline-none focus:ring-0 focus:ring-offset-0 checked:focus:border-white"
                  ${element == firstElement ? "checked" : ""}
                  value="${element}"
                  name="e4d946d7-bb41-4aac-9923-be1af497aa07"
                  required
                />
                <div class="flex grow flex-col">
                  <p class="text-white text-sm font-medium leading-normal">
                    ${element}
                  </p>
                  <p class="text-[#a2aab3] text-sm font-normal leading-normal">
                    ${OTPType(element)}
                  </p>
                </div>
              </label>`;
        });
        localStorage.setItem(
          client_id + "_mfa_methods",
          response.data.token.temp_token.mfa_methods
        );
      }
    } else {
      showOTPPage(response.data.token.mfa_methods[0]);
    }
  }
}

function showOTPPage(mfa_methods) {
  showOTP();
  const client_id = localStorage.getItem("Client-ID");
  localStorage.setItem(client_id + "_mfa_methods", mfa_methods);
}

function enableVerifyBtn() {
  const digits = [
    document.getElementById("otp-1").value,
    document.getElementById("otp-2").value,
    document.getElementById("otp-3").value,
    document.getElementById("otp-4").value,
    document.getElementById("otp-5").value,
    document.getElementById("otp-6").value,
  ];

  const verifyBtn = document.getElementById("verifyOTP");

  const allValid = digits.every((d) => /^\d$/.test(d)); // ogni campo ha 1 cifra

  if (verifyBtn) {
    verifyBtn.disabled = !allValid;
  }

  return allValid ? digits.join("") : undefined;
}

//function enableVerifyBtn() {
//  const otp1 = document.getElementById("otp-1").value;
//  const otp2 = document.getElementById("otp-2").value;
//  const otp3 = document.getElementById("otp-3").value;
//  const otp4 = document.getElementById("otp-4").value;
//  const otp5 = document.getElementById("otp-5").value;
//  const otp6 = document.getElementById("otp-6").value;
//  const verifyBtn = document.getElementById("verifyOTP");
//  console.log("OTP ", otp1, otp2, otp3, otp4, otp5, otp6);
//  if (otp1 && otp2 && otp3 && otp4 && otp5 && otp6) {
//    if (verifyBtn) verifyBtn.removeAttribute("disabled");
//    return otp1 + otp2 + otp3 + otp4 + otp5 + otp6;
//  } else {
//    if (verifyBtn) verifyBtn.setAttribute("disabled");
//  }
//}
