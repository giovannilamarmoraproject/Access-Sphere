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
  const currentUrl = new URL(window.location.href);
  //const clientId = currentUrl.searchParams.get("client_id") || config.client_id;
  //const redirectUri =
  //  currentUrl.searchParams.get("redirect_uri") || config.redirect_uri;
  const clientId = currentUrl.searchParams.get("client_id");
  const redirectUri = currentUrl.searchParams.get("redirect_uri");

  if (!clientId)
    return sweetalert(
      "error",
      currentTranslations.googleLogin_error_title,
      currentTranslations.googleLogin_error_text
    );

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
}

function checkMFAAndSetupOTP(response, client_id) {
  if (response.data.token && response.data.token.mfa_methods) {
    localStorage.setItem("Client-ID", client_id);
    localStorage.setItem(
      client_id + "_temp_token",
      response.data.token.temp_token.access_token
    );

    showOTP();

    if (response.data.token.mfa_methods.length > 1) {
      const loginMethod = document.getElementById("show-login-method");
      if (loginMethod) {
        let select = "";
        response.data.token.mfa_methods.forEach((element) => {
          select += "<option value='" + element + "'>" + element + "</option>";
        });
        loginMethod.innerHTML =
          `<div class="mt-2" style="width: 100%">
                    <h4 class="title">${currentTranslations.otp_verification_code_title}</h4>
                    <p class="text-center otp-verify">
                      ${currentTranslations.otp_verification_code_text}
                    </p>
                  </div>
                  <select
                  id="otp-selected"
                    class="form-select mx-auto mt-4"
                    style="width: fit-content; border-radius: 20px; height: 50px; padding-left: 30px; padding-right:40px"
                    aria-label="Select login method"
                  >
                    <option selected disabled>${currentTranslations.otp_verification_code_select}</option>` +
          select +
          `
                  </select>
                  <div
                style="width: 100%"
                class="otp-button"
                data-inviewport="slide-up"
              >
                <button
                  disabled
                  id="otp_verification_code_button"
                  type="button"
                  onclick="showOTPPage(null)"
                  class="w-full block bg-blue-500 hover:bg-blue-400 focus:bg-blue-400 text-white font-semibold rounded-lg px-4 py-0 xl:mt-6 md:mt-3 input-small"
                >
                ${currentTranslations.otp_verification_code_button}
                </button>
              </div>`;
        localStorage.setItem(
          client_id + "_mfa_methods",
          response.data.token.temp_token.mfa_methods
        );
        const selectOtp = document.getElementById("otp-selected");
        const button = document.getElementById("otp_verification_code_button");

        selectOtp.addEventListener("change", function () {
          if (selectOtp.value) {
            button.removeAttribute("disabled");
          } else {
            // button.setAttribute("disabled", "true");
          }
        });
      }
    } else {
      showOTPPage(response.data.token.mfa_methods[0]);
    }
  }
}

function showOTPPage(mfa_methods) {
  if (!mfa_methods) {
    mfa_methods = document.getElementById("otp-selected").value;
    const loginMethod = document.getElementById("show-login-method");
    if (loginMethod) loginMethod.style.display = "none";
  }
  const client_id = localStorage.getItem("Client-ID");
  const otp = document.getElementById("totp-method");
  const otpButton = document.getElementById("otp-button");
  if (otp) {
    otp.style.display = "block";
    localStorage.setItem(client_id + "_mfa_methods", mfa_methods);
  }
  if (otpButton) {
    otpButton.style.display = "block";
  }
}

function enableVerifyBtn() {
  const otp1 = document.getElementById("otp-1").value;
  const otp2 = document.getElementById("otp-2").value;
  const otp3 = document.getElementById("otp-3").value;
  const otp4 = document.getElementById("otp-4").value;
  const otp5 = document.getElementById("otp-5").value;
  const otp6 = document.getElementById("otp-6").value;
  if (otp1 && otp2 && otp3 && otp4 && otp5 && otp6) {
    const verifyBtn = document.getElementById("verifyOTP");
    if (verifyBtn) verifyBtn.removeAttribute("disabled");
    return otp1 + otp2 + otp3 + otp4 + otp5 + otp6;
  }
}
