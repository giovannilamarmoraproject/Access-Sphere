const CACHE_NAME = "access-sphere-v3";

const CORE_ASSETS = [
  "/",
  "/index.html",
  "/css/m3-dashboard.css",
  "/css/m3-themes.css",
  "/app/shared/loader/loader.css",
  "/app/shared/loader/loader.js",
  "/app/shared/sweetalert.css",
  "/app/shared/sweetalert.js",
  "/app/shared/config.js",
  "/app/shared/utils.js",
  "/app/shared/settings.js",
  "/app/i18n/translations.js",
  "/app/i18n/errorCode.json",
  "/app/i18n/languages/it.json",
  "/app/i18n/languages/en.json",
  "/img/logo-minimal.svg",
  "/favicon.ico"
];

self.addEventListener("install", (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) =>
      Promise.allSettled(CORE_ASSETS.map((url) => cache.add(url)))
    )
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== CACHE_NAME)
          .map((key) => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);
  // Never intercept API calls or dynamic backend endpoints
  if (url.pathname.startsWith("/v1/") || url.pathname.startsWith("/webjars/")) {
    return;
  }

  // HTML document navigations: Network-First with fast cache fallback
  if (req.mode === "navigate" || (req.headers.get("accept") || "").includes("text/html")) {
    event.respondWith(
      fetch(req)
        .then((networkRes) => {
          if (networkRes && networkRes.ok && url.origin === self.location.origin) {
            const clone = networkRes.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(req, clone));
          }
          return networkRes;
        })
        .catch(() => caches.match(req))
    );
    return;
  }

  // Static assets (CSS, JS, JSON, SVG, Fonts, Images): Stale-While-Revalidate for instant loading
  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.open(CACHE_NAME).then((cache) =>
        cache.match(req).then((cachedRes) => {
          const fetchPromise = fetch(req)
            .then((networkRes) => {
              if (networkRes && networkRes.ok) {
                cache.put(req, networkRes.clone());
              }
              return networkRes;
            })
            .catch(() => cachedRes);
          return cachedRes || fetchPromise;
        })
      )
    );
  }
});
