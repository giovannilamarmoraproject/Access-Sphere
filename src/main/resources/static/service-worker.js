self.addEventListener("install", (event) => {
  console.log("📱 Service Worker installato.");
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
        console.error("❌ Errore durante il caching dei file:", error);
      })
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cache) => {
          if (cache !== "v1") {
            console.log("🗑️ Eliminazione della vecchia cache:", cache);
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
    console.log("🚧 Bypassing Service Worker for:", event.request.url);
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
          console.error("❌ Fetch fallito per:", event.request.url, error);

          // Costruzione del contenuto HTML
          const htmlContent = `
            <!DOCTYPE html>
            <html lang="en">
              <head>
                <meta charset="utf-8" />
                <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

                <title>500 Server Offline</title>

                <!-- Google font -->
                <link
                  href="https://fonts.googleapis.com/css?family=Montserrat:700,900"
                  rel="stylesheet"
                />

                <!-- Custom stlylesheet -->
                <style>
                  * {
                    -webkit-box-sizing: border-box;
                    box-sizing: border-box;
                  }

                  body {
                    padding: 0;
                    margin: 0;
                  }

                  #notfound {
                    position: relative;
                    height: 100vh;
                    background: #030005;
                  }

                  #notfound .notfound {
                    position: absolute;
                    left: 50%;
                    top: 50%;
                    -webkit-transform: translate(-50%, -50%);
                    -ms-transform: translate(-50%, -50%);
                    transform: translate(-50%, -50%);
                  }

                  .notfound {
                    max-width: 767px;
                    width: 100%;
                    line-height: 1.4;
                    text-align: center;
                  }

                  .notfound .notfound-404 {
                    position: relative;
                    height: 180px;
                    margin-bottom: 20px;
                    z-index: -1;
                  }

                  .notfound .notfound-404 h1 {
                    font-family: "Montserrat", sans-serif;
                    position: absolute;
                    left: 50%;
                    top: 50%;
                    -webkit-transform: translate(-50%, -50%);
                    -ms-transform: translate(-50%, -50%);
                    transform: translate(-50%, -50%);
                    font-size: 224px;
                    font-weight: 900;
                    margin-top: 0px;
                    margin-bottom: 0px;
                    margin-left: -12px;
                    color: #030005;
                    text-transform: uppercase;
                    text-shadow: -1px -1px 0px #8400ff, 1px 1px 0px #ff005a;
                    letter-spacing: -20px;
                  }

                  .notfound .notfound-404 h2 {
                    font-family: "Montserrat", sans-serif;
                    position: absolute;
                    left: 0;
                    right: 0;
                    top: 110px;
                    font-size: 42px;
                    font-weight: 700;
                    color: #fff;
                    text-transform: uppercase;
                    text-shadow: 0px 2px 0px #8400ff;
                    letter-spacing: 13px;
                    margin: 0;
                  }

                  .notfound a {
                    font-family: "Montserrat", sans-serif;
                    display: inline-block;
                    text-transform: uppercase;
                    color: #ff005a;
                    text-decoration: none;
                    border: 2px solid;
                    background: transparent;
                    padding: 10px 40px;
                    font-size: 14px;
                    font-weight: 700;
                    -webkit-transition: 0.2s all;
                    transition: 0.2s all;
                  }

                  .notfound a:hover {
                    color: #8400ff;
                  }

                  @media only screen and (max-width: 767px) {
                    .notfound .notfound-404 h2 {
                      font-size: 24px;
                    }
                  }

                  @media only screen and (max-width: 480px) {
                    .notfound .notfound-404 h1 {
                      font-size: 182px;
                    }
                  }
                </style>

                <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
                <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
                <!--[if lt IE 9]>
                  <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
                  <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
                <![endif]-->

                <meta name="robots" content="noindex, follow" />
              </head>

              <body>
                <div id="notfound">
                  <div class="notfound">
                    <div class="notfound-404">
                      <h1>500</h1>
                      <h2>Server Offline</h2>
                    </div>
                    <p style="color: #fff; font-family: 'Montserrat', sans-serif">
                      Not possible to access to ${event.request.url}
                    </p>
                    <a style="cursor: pointer" onclick="reload()">Reload</a>
                    <script>
                      function reload() {
                        location.reload();
                      }
                    </script>
                  </div>
                </div>
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
