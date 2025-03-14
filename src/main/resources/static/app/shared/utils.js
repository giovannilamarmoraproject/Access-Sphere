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
