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
          "img/favicon-32x32.png",
          "img/favicon-16x16.png",
          "img/apple-touch-icon.png",
          "img/logo-minimal.svg",
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
          "app/shared/shared/animations/animation.css",
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

          // Costruzione del contenuto HTML Material Expressive 3
          const htmlContent = `
            <!DOCTYPE html>
            <html lang="it">
              <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Server Offline - Access Sphere</title>
                
                <!-- Favicon (SVG vector + ICO fallback) -->
                <link rel="icon" type="image/svg+xml" href="/img/logo-minimal.svg" />
                <link rel="alternate icon" type="image/x-icon" href="/favicon.ico" />
                
                <!-- Google Fonts & FontAwesome -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,slnt,wdth,wght,ROND@6..144,-10..0,25..151,1..1000,100&display=swap" rel="stylesheet">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" />

                <style>
                  * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                  }
                  body {
                    font-family: 'Google Sans Flex', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background-color: #0E0B16;
                    color: #E6E1E5;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                    position: relative;
                    overflow-x: hidden;
                  }
                  .ambient-glow {
                    position: fixed;
                    border-radius: 50%;
                    filter: blur(100px);
                    pointer-events: none;
                    z-index: 0;
                  }
                  .glow-1 {
                    top: 10%;
                    left: 20%;
                    width: 360px;
                    height: 360px;
                    background: rgba(168, 85, 247, 0.16);
                  }
                  .glow-2 {
                    bottom: 15%;
                    right: 20%;
                    width: 400px;
                    height: 400px;
                    background: rgba(103, 80, 164, 0.18);
                  }
                  .m3-offline-card {
                    position: relative;
                    z-index: 1;
                    width: 100%;
                    max-width: 520px;
                    background: rgba(24, 19, 38, 0.85);
                    backdrop-filter: blur(28px);
                    -webkit-backdrop-filter: blur(28px);
                    border: 1px solid rgba(208, 188, 255, 0.2);
                    border-radius: 32px;
                    padding: 42px 32px;
                    text-align: center;
                    box-shadow: 0 24px 64px -12px rgba(0, 0, 0, 0.65), 0 0 32px rgba(168, 85, 247, 0.15);
                    animation: fadeIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
                  }
                  @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(16px); }
                    to { opacity: 1; transform: translateY(0); }
                  }
                  .logo-box {
                    margin: 0 auto 20px;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    width: 88px;
                    height: 88px;
                    border-radius: 26px;
                    background: rgba(208, 188, 255, 0.06);
                    border: 1.5px solid rgba(208, 188, 255, 0.25);
                    box-shadow: 0 0 28px rgba(168, 85, 247, 0.35);
                  }
                  .logo-box img {
                    width: 56px;
                    height: 56px;
                    filter: drop-shadow(0 4px 14px rgba(168, 85, 247, 0.7));
                  }
                  .status-badge {
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    padding: 6px 16px;
                    border-radius: 9999px;
                    background: rgba(239, 68, 68, 0.12);
                    border: 1px solid rgba(239, 68, 68, 0.35);
                    color: #FCA5A5;
                    font-size: 0.75rem;
                    font-weight: 700;
                    letter-spacing: 0.05em;
                    text-transform: uppercase;
                    margin-bottom: 16px;
                  }
                  .status-dot {
                    width: 8px;
                    height: 8px;
                    border-radius: 50%;
                    background-color: #EF4444;
                    box-shadow: 0 0 8px #EF4444;
                    animation: pulse 2s infinite;
                  }
                  @keyframes pulse {
                    0%, 100% { opacity: 1; transform: scale(1); }
                    50% { opacity: 0.4; transform: scale(0.85); }
                  }
                  .title {
                    font-size: 1.65rem;
                    font-weight: 800;
                    color: #FFFFFF;
                    letter-spacing: -0.02em;
                    margin-bottom: 10px;
                  }
                  .subtitle {
                    font-size: 0.88rem;
                    line-height: 1.55;
                    color: rgba(230, 225, 229, 0.75);
                    margin-bottom: 22px;
                  }
                  .resource-card {
                    background: #1C152B;
                    border: 1px solid rgba(208, 188, 255, 0.18);
                    border-radius: 16px;
                    padding: 12px 16px;
                    margin-bottom: 26px;
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    text-align: left;
                  }
                  .resource-card i {
                    color: #D0BCFF;
                    font-size: 0.9rem;
                    flex-shrink: 0;
                  }
                  .resource-url {
                    font-family: monospace;
                    font-size: 0.75rem;
                    color: #D0BCFF;
                    word-break: break-all;
                    line-height: 1.4;
                  }
                  .actions-container {
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                  }
                  @media (min-width: 480px) {
                    .actions-container {
                      flex-direction: row;
                      justify-content: center;
                    }
                  }
                  .m3-btn-primary {
                    background: linear-gradient(135deg, #A855F7 0%, #6750A4 100%);
                    color: #FFFFFF;
                    border: none;
                    border-radius: 9999px;
                    padding: 12px 26px;
                    font-size: 0.88rem;
                    font-weight: 600;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;
                    cursor: pointer;
                    text-decoration: none;
                    box-shadow: 0 4px 16px rgba(168, 85, 247, 0.35);
                    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
                  }
                  .m3-btn-primary:hover {
                    background: linear-gradient(135deg, #B56BF9 0%, #7961BC 100%);
                    transform: translateY(-2px);
                    box-shadow: 0 6px 22px rgba(168, 85, 247, 0.5);
                  }
                  .m3-btn-outline {
                    background: rgba(255, 255, 255, 0.05);
                    color: #E6E1E5;
                    border: 1.5px solid rgba(208, 188, 255, 0.28);
                    border-radius: 9999px;
                    padding: 12px 24px;
                    font-size: 0.88rem;
                    font-weight: 600;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;
                    cursor: pointer;
                    text-decoration: none;
                    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
                  }
                  .m3-btn-outline:hover {
                    background: rgba(208, 188, 255, 0.14);
                    border-color: rgba(208, 188, 255, 0.6);
                    color: #FFFFFF;
                    transform: translateY(-2px);
                  }
                  .footer-note {
                    margin-top: 22px;
                    font-size: 0.72rem;
                    color: rgba(208, 188, 255, 0.45);
                  }
                </style>
              </head>
              <body>
                <div class="ambient-glow glow-1"></div>
                <div class="ambient-glow glow-2"></div>
                
                <div class="m3-offline-card">
                  <div class="logo-box">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" fill="none" style="width: 58px; height: 58px;">
                      <defs>
                        <linearGradient id="swM3Grad" x1="10%" y1="10%" x2="90%" y2="90%">
                          <stop offset="0%" stop-color="#D0BCFF" />
                          <stop offset="50%" stop-color="#6750A4" />
                          <stop offset="100%" stop-color="#4F378B" />
                        </linearGradient>
                        <linearGradient id="swAccentGrad" x1="0%" y1="100%" x2="100%" y2="0%">
                          <stop offset="0%" stop-color="#381E72" />
                          <stop offset="100%" stop-color="#A855F7" />
                        </linearGradient>
                        <filter id="swSubtleGlow" x="-20%" y="-20%" width="140%" height="140%">
                          <feGaussianBlur stdDeviation="3" result="blur" />
                          <feComposite in="SourceGraphic" in2="blur" operator="over" />
                        </filter>
                      </defs>
                      <circle cx="50" cy="50" r="42" stroke="url(#swM3Grad)" stroke-width="3" stroke-dasharray="190 70" stroke-linecap="round" filter="url(#swSubtleGlow)"/>
                      <ellipse cx="50" cy="50" rx="38" ry="18" stroke="url(#swAccentGrad)" stroke-width="2.5" transform="rotate(-30 50 50)" stroke-dasharray="160 40" stroke-linecap="round"/>
                      <ellipse cx="50" cy="50" rx="38" ry="18" stroke="url(#swM3Grad)" stroke-width="2" transform="rotate(45 50 50)" stroke-dasharray="120 60" opacity="0.6" stroke-linecap="round"/>
                      <circle cx="50" cy="50" r="14" fill="url(#swM3Grad)" />
                      <circle cx="50" cy="50" r="14" stroke="#FFFFFF" stroke-width="1.5" opacity="0.4" />
                      <circle cx="50" cy="47" r="3.5" fill="#FFFFFF" />
                      <path d="M47.8 49 L52.2 49 L53.2 55.5 L46.8 55.5 Z" fill="#FFFFFF" />
                    </svg>
                  </div>
                  
                  <div>
                    <div class="status-badge">
                      <span class="status-dot"></span> Server Non Raggiungibile
                    </div>
                  </div>
                  
                  <h1 class="title">Connessione Interrotta</h1>
                  <p class="subtitle">
                    Impossibile stabilire una connessione sicura con il server o il dispositivo è attualmente offline. Verifica la tua connessione e riprova.
                  </p>
                  
                  <div class="resource-card">
                    <i class="fa-solid fa-link-slash"></i>
                    <span class="resource-url">\${event.request.url}</span>
                  </div>
                  
                  <div class="actions-container">
                    <button onclick="location.reload()" class="m3-btn-primary">
                      <i class="fa-solid fa-rotate-right"></i> Riprova
                    </button>
                    <a href="/" class="m3-btn-outline">
                      <i class="fa-solid fa-house"></i> Vai alla Home
                    </a>
                  </div>

                  <div class="footer-note">
                    Access Sphere Platform • Security & Identity
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
