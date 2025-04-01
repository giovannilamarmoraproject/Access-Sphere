self.addEventListener("install", (event) => {
  console.log("Service Worker installato.");
  event.waitUntil(
    caches
      .open("v1")
      .then((cache) => {
        return cache.addAll([
          "/",
          "/index.html",
          "/cookie-policy.html",
          "/privacy-policy.html",
          "/favicon.ico",
          "css/styles.css",
          "js/main.js",
          "img/shape.png",
          "img/person.png",
          "img/Access Sphere Transparent 512x512.png",
          "app/i18n/errorCode.json",
          "app/i18n/translations.js",
          "app/i18n/translations.json",
          "app/login/index.html",
          "app/login/js/forgot.js",
          "app/login/js/login.js",
          "app/login/js/script.js",
          "app/login/img/Access Sphere Full.png",
          "app/login/img/Access Sphere Transparent 512x512.png",
          "app/login/css/sliding-animations.css",
          "app/login/css/style.css",
          "app/shared/css/config.js",
          "app/shared/css/utils.js",
          "app/shared/css/sweetalert.js",
          "app/shared/css/sweetalert.css",
          "app/shared/user/edit.html",
          "app/shared/user/register.html",
          "app/shared/user/roles.html",
          "app/shared/user/user.html",
          "app/shared/user/users.html",
          "app/shared/user/js/delete.js",
          "app/shared/user/js/edit.js",
          "app/shared/user/js/register.js",
          "app/shared/user/js/roles.js",
          "app/shared/user/js/user.js",
          "app/shared/user/js/users.js",
          "app/shared/user/js/validationForm.js",
          "app/shared/user/img/shape.png",
          "app/shared/user/img/Access Sphere Full.png",
          "app/shared/user/img/Access Sphere Transparent 512x512.png",
          "app/shared/user/css/style.css",
          "app/shared/user/css/form.css",
          "app/shared/user/css/animation.css",
        ]);
      })
      .catch((error) => {
        console.error("Errore durante il caching dei file:", error);
      })
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cache) => {
          if (cache !== "v1") {
            console.log("Eliminazione della vecchia cache:", cache);
            return caches.delete(cache);
          }
        })
      );
    })
  );
});

self.addEventListener("fetch", (event) => {
  event.respondWith(
    caches.match(event.request).then((response) => {
      return (
        response ||
        fetch(event.request).catch((error) => {
          console.error("Fetch fallito per:", event.request.url, error);
          // Puoi restituire una risposta alternativa in caso di errore, ad esempio una pagina offline:
          return new Response(
            `<h1>Offline</h1><p>Impossibile caricare la risorsa: ${event.request.url}</p>`,
            { headers: { "Content-Type": "text/html" } }
          );
        })
      );
    })
  );
});
