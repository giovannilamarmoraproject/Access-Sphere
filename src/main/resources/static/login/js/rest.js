document.addEventListener("DOMContentLoaded", function () {
  const loginForm = document.getElementById("loginForm");

  // Event listener per il form
  if (loginForm) {
    loginForm.addEventListener("submit", function (event) {
      event.preventDefault(); // Previene il comportamento predefinito di invio del form

      const currentUrl = new URL(window.location.href);
      console.log(currentUrl);
      const clientId = currentUrl.searchParams.get("client_id");
      const redirectUri = currentUrl.searchParams.get("redirect_uri");

      // Chiama la funzione login con i dati raccolti
      return doLogin(clientId, redirectUri);
    });
  }

  async function doLogin(clientId, redirectUri) {
    const email = document.getElementById("emailInput").value;
    const password = document.getElementById("passwordInput").value;

    let encode = btoa(email + ":" + password);

    const url = new URL(window.location.origin + "/v1/oAuth/2.0/token");
    url.searchParams.set("grant_type", "password");
    if (clientId) url.searchParams.set("client_id", clientId);
    if (redirectUri) url.searchParams.set("redirect_uri", redirectUri);

    token(url, encode).then(async (data) => {
      const responseData = await data.json();
      if (responseData.error != null)
        return sweetalert(
          "error",
          responseData.error.status,
          responseData.error.message
        );
      else {
        const redirect_uri = data.headers.get("location");
        if (redirect_uri != null)
          window.location.href =
            redirect_uri +
            "?access-token=" +
            responseData.data.token.access_token +
            "&session-id=" +
            data.headers.get("Session-ID");
      }
    });
  }

  const token = async (url, basic) => {
    try {
      const response = await fetch(url, {
        method: "POST", // *GET, POST, PUT, DELETE, etc.
        mode: "cors", // no-cors, *cors, same-origin
        cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
        credentials: "same-origin", // include, *same-origin, omit
        headers: {
          "Content-Type": "application/json",
          Authorization: "Basic " + basic,
          // 'Content-Type': 'application/x-www-form-urlencoded',
        },
        redirect: "follow", // manual, *follow, error
        referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
        //body: JSON.stringify(data), // body data type must match "Content-Type" header
      });
      return response;
    } catch (err) {
      console.error(err);
      throw new Error(`Error on login, message is ${err.message}`);
    }
  };
});

function doGoogleLogin() {
  const currentUrl = new URL(window.location.href);
  const clientId = currentUrl.searchParams.get("client_id");
  const redirectUri = currentUrl.searchParams.get("redirect_uri");

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

function sweetalert(icon, title, message) {
  const customClassSwal = Swal.mixin({
    customClass: {
      confirmButton: "rounded-pill buttonInput",
      denyButton: "rounded-pill buttonInput",
      popup: "border_round",
    },
    buttonsStyling: true,
  });

  return customClassSwal.fire({
    icon: icon,
    title: title,
    text: message,
    color: "#FFFFFF",
    background: "rgba(56, 62, 66, 0.8)",
    backdrop: "rgba(0, 0, 0, 0.5)",
    showCancelButton: false,
  });
}
