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
          "img/shape.png",
          "img/person.png",
          "img/Access Sphere Transparent 512x512.png",
          "app/i18n/errorCode.json",
          "app/i18n/translations.js",
          "app/i18n/translations.json",
          "app/login/index.html",
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
  const url = new URL(event.request.url);

  // Se la richiesta è per l'v1, non la intercettiamo
  if (url.pathname.startsWith("/v1/")) {
    console.log("Bypassing Service Worker for:", event.request.url);
    return;
  }

  //event.respondWith(
  //  caches.match(event.request).then((response) => {
  //    return (
  //      response ||
  //      fetch(event.request).catch((error) => {
  //        console.error("Fetch fallito per:", event.request.url, error);
  //        return new Response(
  //          `<h1>Offline</h1><p>Impossibile caricare la risorsa: ${event.request.url}</p>`,
  //          { headers: { "Content-Type": "text/html" } }
  //        );
  //      })
  //    );
  //  })
  //);
  event.respondWith(
    caches.match(event.request).then((response) => {
      return (
        response ||
        fetch(event.request).catch((error) => {
          console.error("Fetch fallito per:", event.request.url, error);

          // Costruzione del contenuto HTML
          const htmlContent = `
            <html>
              <head>
                <style>
                  @import url('https://fonts.googleapis.com/css?family=Gilda+Display');
                  html { background: radial-gradient(#000, #111); color: white; overflow: hidden; height: 100%; user-select: none; }
                  .static { width: 100%; height: 100%; position: relative; margin: 0; padding: 0; top: -100px; opacity: 0.05; z-index: 230; user-select: none; }
                  .error { text-align: center; font-family: 'Gilda Display', serif; font-size: 95px; font-style: italic; animation: noise 2s linear infinite; }
                  .error:before { content: '500'; font-family: 'Gilda Display', serif; font-size: 100px; color: red; animation: noise-2 .2s linear infinite; }
                  .info { text-align: center; font-family: 'Gilda Display', serif; font-size: 15px; font-style: italic; animation: noise-3 1s linear infinite; }
                  @keyframes noise-2 { 0%, 20%, 40%, 60%, 70%, 90% { opacity: 0; } 10% { opacity: .1; } 50% { opacity: .5; left: 6px; } 100% { opacity: .6; left: -2px; } }
                  @keyframes noise-3 { 0%, 3%, 5%, 42%, 44%, 100% { opacity: 1; transform: scaleY(1); } 4.3% { opacity: 1; transform: scaleY(4); } 43% { opacity: 1; transform: scaleX(10) rotate(60deg); } }
                  @keyframes noise { 0%, 3%, 5%, 42%, 44%, 100% { opacity: 1; transform: scaleY(1); } 4.3% { opacity: 1; transform: scaleY(1.7); } 43% { opacity: 1; transform: scaleX(1.5); } }
                </style>
              </head>
              <body>
                <div class="error">500</div>
                <br /><br />
                <span class="info">Server Offline ${event.request.url}</span>
                <img src="http://images2.layoutsparks.com/1/160030/too-much-tv-static.gif" class="static" />
              </body>
            </html>
          `;

          // JSON di errore
          //const jsonResponse = {
          //  dateTime: new Date().toISOString(),
          //  url: event.request.url,
          //  error: {
          //    errorCode: "ERR_SERVER_500",
          //    exception: "SERVER_OFFLINE",
          //    status: "INTERNAL_SERVER_ERROR",
          //    message: "The server is currently offline",
          //  },
          //};

          // Creiamo la risposta HTML e JSON
          return new Response(htmlContent, {
            headers: { "Content-Type": "text/html" },
          });
        })
      );
    })
  );
});
