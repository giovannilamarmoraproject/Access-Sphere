const config = typeof getConfig === "function" ? getConfig() : {};

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";").shift();
}

function getCookieOrStorage(data) {
  const client_id = localStorage.getItem("Client-ID") || "";
  const configClientId = config?.client_id || "";

  return (
    localStorage.getItem(client_id ? client_id + "_" + data : null) ||
    localStorage.getItem(configClientId ? configClientId + "_" + data : null) ||
    localStorage.getItem(data) ||
    getCookie(data)
  );
}

function getSavedHeaders() {
  const headers = {};

  const parentId = getCookieOrStorage("Parent-ID");
  const redirectUri = getCookieOrStorage("redirect-uri");
  const sessionId = getCookieOrStorage("Session-ID");
  const spanId = getCookieOrStorage("Span-ID");
  const traceId = getCookieOrStorage("Trace-ID");
  const deviceToken = getCookieOrStorage("Device-Token");

  if (parentId) headers["Parent-ID"] = parentId;
  if (redirectUri) headers["redirect-uri"] = redirectUri;
  if (sessionId) headers["Session-ID"] = sessionId;
  if (spanId) headers["Span-ID"] = spanId;
  if (traceId) headers["Trace-ID"] = traceId;
  if (deviceToken) headers["Device-Token"] = deviceToken;

  return headers;
}

function fetchHeader(headers) {
  const parentId = headers.get("Parent-ID");
  const redirectUri = headers.get("redirect-uri");
  const sessionId = headers.get("Session-ID");
  const spanId = headers.get("Span-ID");
  const traceId = headers.get("Trace-ID");
  const deviceToken = headers.get("Device-Token");

  // Save the headers in localStorage
  if (parentId) localStorage.setItem("Parent-ID", parentId);
  if (redirectUri) localStorage.setItem("redirect-uri", redirectUri);
  if (sessionId) localStorage.setItem("Session-ID", sessionId);
  if (spanId) localStorage.setItem("Span-ID", spanId);
  if (traceId) localStorage.setItem("Trace-ID", traceId);
  if (deviceToken) localStorage.setItem("Device-Token", deviceToken);
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

function escapeHtmlAttr(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
window.escapeHtmlAttr = escapeHtmlAttr;

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
  console.log("🧹 Cleaning Storage and Cookies (preserving settings & preferences)...");
  const preservedKeys = [
    "access_sphere_theme",
    "access_sphere_settings",
    "access_sphere_language",
    "app_language",
    "access_sphere_app_name",
  ];
  const preserved = {};
  preservedKeys.forEach((key) => {
    const val = localStorage.getItem(key);
    if (val !== null) preserved[key] = val;
  });

  localStorage.clear();

  Object.keys(preserved).forEach((key) => {
    localStorage.setItem(key, preserved[key]);
  });
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

function isLoggingIn() {
  var logged = localStorage.getItem("ACCESS-SPHERE_logged_in");
  if (logged) logged = JSON.parse(logged);
  return logged;
}

function loggingIn() {
  localStorage.setItem("ACCESS-SPHERE_logged_in", true);
}

function loggingOut() {
  localStorage.setItem("ACCESS-SPHERE_logged_in", false);
}

const formatDateIntl = (inputDate) => {
  const date = new Date(inputDate);

  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false, // se vuoi orario in formato 24h
  }).format(date);
};

function togglePasswordVisibility(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const icon = btn.querySelector("i");
  if (input.type === "password") {
    input.type = "text";
    if (icon) {
      icon.className = "fa-solid fa-eye-slash text-xs";
    }
  } else {
    input.type = "password";
    if (icon) {
      icon.className = "fa-solid fa-eye text-xs";
    }
  }
}

/* -------------------------------------------
   MOBILE NAVIGATION DRAWER INITIALIZER
   ------------------------------------------- */
function toggleMobileNavDrawer() {
  const drawer = document.getElementById("m3-mobile-nav-drawer");
  const overlay = document.getElementById("m3-mobile-nav-overlay");
  if (!drawer || !overlay) return;

  const isOpen = drawer.classList.contains("active");
  if (isOpen) {
    closeMobileNavDrawer();
  } else {
    drawer.classList.add("active");
    overlay.classList.add("active");
    document.body.style.overflow = "hidden";
  }
}

function closeMobileNavDrawer() {
  const drawer = document.getElementById("m3-mobile-nav-drawer");
  const overlay = document.getElementById("m3-mobile-nav-overlay");
  if (drawer) drawer.classList.remove("active");
  if (overlay) overlay.classList.remove("active");
  document.body.style.overflow = "";
}

function triggerMobileRefresh() {
  closeMobileNavDrawer();
  const refreshBtn = document.getElementById("btn-refresh-data");
  if (refreshBtn) {
    refreshBtn.click();
    return;
  }
  if (typeof handleCurrentRefresh === "function") {
    handleCurrentRefresh();
  } else if (typeof refreshUsers === "function") {
    refreshUsers();
  } else if (typeof refreshClients === "function") {
    refreshClients();
  } else {
    window.location.reload();
  }
}

// ----------------------------------------------------
// SCROLL MANAGEMENT (Always start from top on section/page change)
// ----------------------------------------------------
function scrollToTop(behavior = "instant") {
  try {
    window.scrollTo({ top: 0, left: 0, behavior: behavior });
  } catch (e) {
    window.scrollTo(0, 0);
  }
  if (document.documentElement) document.documentElement.scrollTop = 0;
  if (document.body) document.body.scrollTop = 0;
}
window.scrollToTop = scrollToTop;

// Disable automatic browser scroll restoration so navigating across pages/sections always starts at top
if ("scrollRestoration" in history) {
  history.scrollRestoration = "manual";
}

// Reset scroll on initial script evaluation
scrollToTop("instant");

// Reset scroll on DOM ready, window load, pageshow (back/forward cache), and popstate
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => scrollToTop("instant"));
} else {
  scrollToTop("instant");
}
window.addEventListener("pageshow", () => {
  scrollToTop("instant");
});
window.addEventListener("load", () => {
  scrollToTop("instant");
});
window.addEventListener("popstate", () => {
  scrollToTop("instant");
});
window.addEventListener("beforeunload", () => {
  scrollToTop("instant");
});

// Intercept link clicks to reset scroll immediately before navigation
document.addEventListener("click", (e) => {
  const link = e.target.closest("a[href]");
  if (link) {
    const href = link.getAttribute("href");
    if (href && !href.startsWith("#") && !href.startsWith("javascript:") && !link.target) {
      scrollToTop("instant");
    }
  }
}, { passive: true });

function navigateToSection(targetPath, viewName) {
  closeMobileNavDrawer();
  scrollToTop("instant");
  if (typeof switchDashboardView === "function" && viewName) {
    switchDashboardView(viewName, true);
    return false;
  }
  window.location.href = targetPath;
  return false;
}

function triggerMobileBack() {
  closeMobileNavDrawer();
  scrollToTop("instant");
  if (typeof goBack === "function") {
    goBack();
  } else if (window.history.length > 1) {
    window.history.back();
  } else {
    window.location.href = "/app";
  }
}

function initMobileNavigation() {
  const headerContainer = document.querySelector(".glass-header .header-container");
  if (!headerContainer) return;

  // 1. Add hamburger button on the left if not present
  if (!document.getElementById("mobile-menu-toggle-btn")) {
    const toggleBtn = document.createElement("button");
    toggleBtn.id = "mobile-menu-toggle-btn";
    toggleBtn.type = "button";
    toggleBtn.className = "m3-icon-btn m3-mobile-menu-btn";
    toggleBtn.title = "Menu di Navigazione";
    toggleBtn.setAttribute("aria-label", "Menu di Navigazione");
    toggleBtn.onclick = toggleMobileNavDrawer;
    toggleBtn.innerHTML = '<i class="fa-solid fa-bars text-sm"></i>';
    headerContainer.prepend(toggleBtn);
  }

  // 2. Add Drawer & Overlay if not present
  if (!document.getElementById("m3-mobile-nav-drawer")) {
    const currentPath = window.location.pathname;
    const isRootDashboard = currentPath === '/app' || currentPath === '/app/' || currentPath === '';

    const overlay = document.createElement("div");
    overlay.id = "m3-mobile-nav-overlay";
    overlay.className = "m3-mobile-drawer-overlay";
    overlay.onclick = closeMobileNavDrawer;
    document.body.appendChild(overlay);

    const drawer = document.createElement("aside");
    drawer.id = "m3-mobile-nav-drawer";
    drawer.className = "m3-mobile-drawer";
    drawer.innerHTML = `
      <div class="flex items-center justify-between pb-4 mb-4 border-b border-purple-500/20">
        <a href="/app" class="flex items-center gap-3 text-decoration-none" onclick="return navigateToSection('/app', 'dashboard');">
          <img src="/img/logo-minimal.svg" alt="Access Sphere" class="w-8 h-8" />
          <div class="flex flex-col">
            <span class="text-base font-bold text-white tracking-tight flex items-center gap-1.5">
              Access Sphere
              <span class="text-[9px] px-1.5 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-mono border border-purple-500/30">Console</span>
            </span>
          </div>
        </a>
        <button type="button" class="m3-icon-btn" onclick="closeMobileNavDrawer()" title="Chiudi menu">
          <i class="fa-solid fa-xmark text-sm"></i>
        </button>
      </div>

      <!-- Navigazione Principale -->
      <div class="mb-5">
        <span class="text-[10px] uppercase font-bold text-purple-300/60 tracking-wider px-3 mb-2 block">Menu Principale</span>
        <a href="/app" onclick="return navigateToSection('/app', 'dashboard');" class="m3-mobile-nav-link ${currentPath === '/app' || currentPath === '/app/' ? 'active' : ''}">
          <i class="fa-solid fa-gauge-high"></i>
          <span>Panoramica</span>
        </a>
        <a href="/app/users" onclick="return navigateToSection('/app/users', 'users');" class="m3-mobile-nav-link ${currentPath.includes('/app/user') ? 'active' : ''}">
          <i class="fa-solid fa-users"></i>
          <span>Utenti</span>
        </a>
        <a href="/app/clients" onclick="return navigateToSection('/app/clients', 'clients');" class="m3-mobile-nav-link ${currentPath.includes('/app/client') ? 'active' : ''}">
          <i class="fa-solid fa-key"></i>
          <span>Client OAuth 2.0</span>
        </a>
        <a href="https://github.com/giovannilamarmora/Access-Sphere" target="_blank" rel="noopener noreferrer" class="m3-mobile-nav-link">
          <i class="fa-solid fa-book"></i>
          <span>API Docs</span>
        </a>
      </div>

      <!-- Azioni & Strumenti Rapidi -->
      <div class="mb-5">
        <span class="text-[10px] uppercase font-bold text-purple-300/60 tracking-wider px-3 mb-2 block">Strumenti & Azioni</span>
        <button type="button" onclick="triggerMobileRefresh()" class="m3-mobile-nav-link w-full text-left bg-transparent border-0 cursor-pointer">
          <i class="fa-solid fa-arrows-rotate text-purple-400"></i>
          <span>Aggiorna Dati</span>
        </button>
        ${!isRootDashboard ? `
        <button type="button" onclick="triggerMobileBack()" class="m3-mobile-nav-link w-full text-left bg-transparent border-0 cursor-pointer">
          <i class="fa-solid fa-arrow-left text-purple-300"></i>
          <span>Torna Indietro</span>
        </button>
        ` : ''}
        <a href="/app/users/register" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
          <i class="fa-solid fa-user-plus text-purple-400"></i>
          <span>Nuovo Utente</span>
        </a>
        <a href="/app/clients/register" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
          <i class="fa-solid fa-plus text-indigo-400"></i>
          <span>Nuovo Client</span>
        </a>
      </div>

      <!-- Footer Menu: Info & Logout -->
      <div class="mt-auto pt-4 border-t border-purple-500/20 space-y-2">
        <button type="button" onclick="getVersion(); closeMobileNavDrawer();" class="m3-mobile-nav-link w-full text-left bg-transparent border-0 cursor-pointer">
          <i class="fa-solid fa-circle-info text-purple-300"></i>
          <span>Info Versione</span>
        </button>
        <button type="button" onclick="logout()" class="m3-mobile-nav-link w-full text-left bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/25 cursor-pointer">
          <i class="fa-solid fa-arrow-right-from-bracket text-red-400"></i>
          <span>Logout</span>
        </button>
      </div>
    `;
    document.body.appendChild(drawer);
  }
}

// Global exports
window.scrollToTop = scrollToTop;
window.navigateToSection = navigateToSection;
window.toggleMobileNavDrawer = toggleMobileNavDrawer;
window.closeMobileNavDrawer = closeMobileNavDrawer;
window.triggerMobileRefresh = triggerMobileRefresh;
window.triggerMobileBack = triggerMobileBack;
window.initMobileNavigation = initMobileNavigation;

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initMobileNavigation);
} else {
  initMobileNavigation();
}


