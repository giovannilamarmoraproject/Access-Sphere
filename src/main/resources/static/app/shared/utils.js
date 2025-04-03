const config = getConfig();

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";").shift();
}

function getCookieOrStorage(data) {
  return (
    getCookie(data) ||
    localStorage.getItem(config.client_id + "_" + data) ||
    localStorage.getItem(data)
  );
}

function getSavedHeaders() {
  const headers = {};

  const parentId = getCookieOrStorage("Parent-ID");
  const redirectUri = getCookieOrStorage("redirect-uri");
  const sessionId = getCookieOrStorage("Session-ID");
  const spanId = getCookieOrStorage("Span-ID");
  const traceId = getCookieOrStorage("Trace-ID");

  if (parentId) headers["Parent-ID"] = parentId;
  if (redirectUri) headers["redirect-uri"] = redirectUri;
  if (sessionId) headers["Session-ID"] = sessionId;
  if (spanId) headers["Span-ID"] = spanId;
  if (traceId) headers["Trace-ID"] = traceId;

  return headers;
}

function fetchHeader(headers) {
  const parentId = headers.get("Parent-ID");
  const redirectUri = headers.get("redirect-uri");
  const sessionId = headers.get("Session-ID");
  const spanId = headers.get("Span-ID");
  const traceId = headers.get("Trace-ID");

  // Save the headers in localStorage
  if (parentId) localStorage.setItem("Parent-ID", parentId);
  if (redirectUri) localStorage.setItem("redirect-uri", redirectUri);
  if (sessionId) localStorage.setItem("Session-ID", sessionId);
  if (spanId) localStorage.setItem("Span-ID", spanId);
  if (traceId) localStorage.setItem("Trace-ID", traceId);
}

function getOrDefault(data, defaultData) {
  if (data) return data;
  return defaultData;
}

function encodeBase64(password) {
  // Prima codifica la stringa in UTF-8 per evitare problemi con caratteri speciali
  const encodedPassword = btoa(encodeURIComponent(password));
  return encodedPassword;
}

function isMobile() {
  return window.matchMedia("(max-width: 768px)").matches;
  //|| /Android|iPhone|iPad|iPod|Windows Phone/i.test(navigator.userAgent)
}

function isBelow(width) {
  return window.matchMedia("(max-width: " + width + "px)").matches;
}

function isBetween(minWidth, maxWidth) {
  return (
    window.matchMedia(`(min-width: ${minWidth}px)`).matches &&
    window.matchMedia(`(max-width: ${maxWidth}px)`).matches
  );
}

function refreshAnimation(id) {
  const animation = "fa-spin";
  const icon = document.getElementById(id);

  if (!icon) return;

  if (!icon.classList.contains(animation)) icon.classList.add(animation);
  else if (icon.classList.contains(animation))
    setTimeout(() => {
      icon.classList.remove(animation);
    }, 2000); // Rimuove l'animazione dopo 500ms
}

function cleanStorageAndCookies() {
  console.log("Cleaning Storage and Cookies...");
  localStorage.clear();
  deleteSelectedCookies();
}

// Funzione per cancellare tutti i cookie
function deleteAllCookies() {
  var cookies = document.cookie.split(";");

  for (var i = 0; i < cookies.length; i++) {
    var cookie = cookies[i];
    var cookieName = cookie.split("=")[0].trim();
    document.cookie =
      cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
  }
}

function deleteSelectedCookies() {
  var cookies = document.cookie.split(";");

  // Lista dei cookie da eliminare
  var cookiesToDelete = [
    "Session-ID",
    "Span-ID",
    "Parent-ID",
    "Trace-ID",
    "access-token",
    "strapi-token",
    "Redirect-Uri",
    "Registration-Token",
  ];

  for (var i = 0; i < cookies.length; i++) {
    var cookie = cookies[i].split("=")[0].trim();

    if (cookiesToDelete.includes(cookie)) {
      document.cookie =
        cookie + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    }
  }
}
