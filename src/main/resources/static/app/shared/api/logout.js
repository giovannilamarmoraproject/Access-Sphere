function logout() {
  console.log("Logout process started...");
  const token = getCookieOrStorage(config.access_token);

  const logoutUrl = config.logout_url;

  fetch(logoutUrl, {
    method: "POST",
    //mode: "no-cors", // Disabilita il controllo CORS (ma la risposta sarà "opaque")
    mode: "cors",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...getSavedHeaders(),
    },
    credentials: "include",
  })
    .then((response) => {
      fetchHeader(response.headers);
      return response.json();
    })
    .finally((res) => {
      //cleanStorageAndCookies();
      window.location.href = config.login_url;
    })
    .catch((error) => {
      console.error("Logout failed.", error);
      //cleanStorageAndCookies();
      window.location.href = config.login_url;
    });
}
