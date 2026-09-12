function isUnauthorizedError(error) {
  if (!error) return false;
  const errorCode = (error.errorCode || "").toUpperCase();
  const exception = (error.exception || "").toUpperCase();
  const status = String(error.status || "").toUpperCase();
  const message = (error.message || "").toLowerCase();

  return (
    errorCode === "ERR_OAUTH_401" ||
    errorCode === "ERR_TOKEN_401" ||
    errorCode === "ERR_USER_401" ||
    exception === "OAUTH_NOT_VALID" ||
    exception === "TOKEN_NOT_VALID" ||
    status === "UNAUTHORIZED" ||
    status === "401" ||
    message.includes("auth-token is invalid") ||
    message.includes("token is invalid") ||
    message.includes("token not valid") ||
    message.includes("not authorized") ||
    message.includes("unauthorized")
  );
}

let isAuthRedirecting = false;

function handleUnauthorizedResponse(response, url) {
  if (!response) return;

  // Se siamo già nella pagina di login o l'endpoint è di login/logout, non reindirizzare
  const pathname = window.location.pathname;
  if (pathname.includes("/login")) return;
  if (typeof url === "string" && (url.includes("/login") || url.includes("/logout"))) return;

  const triggerLogout = () => {
    if (isAuthRedirecting) return;
    isAuthRedirecting = true;
    console.warn("🔒 Invalid or expired auth token detected on", url, "- Esecuzione logout e ritorno alla pagina di accesso...");
    if (typeof logout === "function") {
      logout();
    } else {
      if (typeof cleanStorageAndCookies === "function") {
        cleanStorageAndCookies();
      } else {
        localStorage.clear();
      }
      const loginUrl = (typeof config !== "undefined" && config && config.login_url)
        ? config.login_url
        : (window.location.origin + "/app/login");
      window.location.href = loginUrl;
    }
  };

  if (response.status === 401) {
    triggerLogout();
    return;
  }

  // Verifica anche il body clonando lo stream della response
  try {
    const clone = response.clone();
    clone.json().then((body) => {
      if (body && body.error && isUnauthorizedError(body.error)) {
        triggerLogout();
      }
    }).catch(() => {});
  } catch (e) {}
}

if (typeof window !== "undefined") {
  window.isUnauthorizedError = isUnauthorizedError;
  window.handleUnauthorizedResponse = handleUnauthorizedResponse;
}

const GET = async (url, bearer) => {
  try {
    const response = await fetch(url, {
      method: "GET", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "include", // include, *same-origin, omit
      //credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      //body: JSON.stringify(data), // body data type must match "Content-Type" header
    });
    handleUnauthorizedResponse(response, url);
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const POST = async (url, bearer, data, token_type = "Bearer ") => {
  try {
    const response = await fetch(url, {
      method: "POST", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "include", // include, *same-origin, omit
      //credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer, token_type),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: getBody(data), // body data type must match "Content-Type" header
    });
    handleUnauthorizedResponse(response, url);
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const PUT = async (url, bearer, data) => {
  try {
    const response = await fetch(url, {
      method: "PUT", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "include", // include, *same-origin, omit
      //credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: getBody(data), // body data type must match "Content-Type" header
    });
    handleUnauthorizedResponse(response, url);
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const PATCH = async (url, bearer, data) => {
  try {
    const response = await fetch(url, {
      method: "PATCH", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "include", // include, *same-origin, omit
      //credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: getBody(data), // body data type must match "Content-Type" header
    });
    handleUnauthorizedResponse(response, url);
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const DELETE = async (url, bearer) => {
  try {
    const response = await fetch(url, {
      method: "DELETE", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "include", // include, *same-origin, omit
      //credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
    });
    handleUnauthorizedResponse(response, url);
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const getHeaders = (bearer, type = "Bearer ") => {
  const headers = bearer
    ? {
        "Content-Type": "application/json",
        Authorization: type + bearer,
        ...getSavedHeaders(),
        // 'Content-Type': 'application/x-www-form-urlencoded',
      }
    : {
        "Content-Type": "application/json",
        ...getSavedHeaders(),
        // 'Content-Type': 'application/x-www-form-urlencoded',
      };
  return headers;
};

const getBody = (data) => {
  const body = data ? JSON.stringify(data) : null;
  return body;
};
