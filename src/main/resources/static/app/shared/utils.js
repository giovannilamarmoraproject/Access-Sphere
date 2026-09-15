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
    "access_sphere_logo_url",
    "access_sphere_sidebar_collapsed",
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

function isPostLoginPage() {
  const p = window.location.pathname;
  return p.startsWith("/app") && !p.startsWith("/app/login");
}

function detectCurrentNavView() {
  const currentPath = window.location.pathname;
  if (currentPath.includes("/settings")) return "settings";
  if (currentPath.includes("/client")) return "clients";
  if (currentPath.includes("/user")) return "users";
  return "dashboard";
}

/**
 * Sincronizza lo stato attivo (active) in tempo reale su:
 * 1. Bottom Navigation Bar Mobile (M3 Expressive)
 * 2. Mobile Drawer
 * 3. Desktop Sidebar M3
 */
function syncMobileNavState(viewName) {
  const isOverview = viewName === "dashboard" || viewName === "overview";
  const isUsers = viewName === "users";
  const isClients = viewName === "clients";
  const isSettings = viewName === "settings";

  // 1. Bottom Navigation Bar Mobile
  const bDash = document.getElementById("bottom-nav-dashboard");
  const bUsers = document.getElementById("bottom-nav-users");
  const bClients = document.getElementById("bottom-nav-clients");
  const bSettings = document.getElementById("bottom-nav-settings");
  if (bDash) bDash.classList.toggle("active", isOverview);
  if (bUsers) bUsers.classList.toggle("active", isUsers);
  if (bClients) bClients.classList.toggle("active", isClients);
  if (bSettings) bSettings.classList.toggle("active", isSettings);

  // 2. Mobile Navigation Drawer
  const mDash = document.getElementById("mobile-nav-dashboard");
  const mUsers = document.getElementById("mobile-nav-users");
  const mClients = document.getElementById("mobile-nav-clients");
  const mSettings = document.getElementById("mobile-nav-settings");
  if (mDash) mDash.classList.toggle("active", isOverview);
  if (mUsers) mUsers.classList.toggle("active", isUsers);
  if (mClients) mClients.classList.toggle("active", isClients);
  if (mSettings) mSettings.classList.toggle("active", isSettings);

  // 3. Desktop Sidebar M3
  const sDash = document.getElementById("sidebar-btn-dashboard");
  const sUsers = document.getElementById("sidebar-btn-users");
  const sClients = document.getElementById("sidebar-btn-clients");
  const sSettings = document.getElementById("sidebar-btn-settings");
  if (sDash) sDash.classList.toggle("active", isOverview);
  if (sUsers) sUsers.classList.toggle("active", isUsers);
  if (sClients) sClients.classList.toggle("active", isClients);
  if (sSettings) sSettings.classList.toggle("active", isSettings);
}

function navigateToSection(targetPath, viewName) {
  closeMobileNavDrawer();
  scrollToTop("instant");
  if (viewName) {
    syncMobileNavState(viewName);
  }
  if (typeof switchDashboardView === "function" && viewName) {
    const targetElementId = "view-" + (viewName === "dashboard" ? "dashboard" : viewName);
    if (document.getElementById(targetElementId)) {
      switchDashboardView(viewName, true);
      return false;
    }
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
  if (!isPostLoginPage()) return;

  const headerContainer = document.querySelector(".glass-header .header-container");

  // 1. Hamburger button sul lato sinistro dell'header
  if (headerContainer && !document.getElementById("mobile-menu-toggle-btn")) {
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

  const currentView = detectCurrentNavView();
  const isOverview = currentView === "dashboard";
  const isUsers = currentView === "users";
  const isClients = currentView === "clients";
  const isSettings = currentView === "settings";

  // 2. Drawer & Overlay laterale per mobile
  if (!document.getElementById("m3-mobile-nav-drawer")) {
    const currentPath = window.location.pathname;
    const isRootDashboard = currentPath === '/app' || currentPath === '/app/' || currentPath === '';
    const savedAppName = localStorage.getItem("access_sphere_app_name") || "Access Sphere";
    const savedLogoUrl = localStorage.getItem("access_sphere_logo_url") || "/img/logo-minimal.svg";

    const overlay = document.createElement("div");
    overlay.id = "m3-mobile-nav-overlay";
    overlay.className = "m3-mobile-drawer-overlay";
    overlay.onclick = closeMobileNavDrawer;
    document.body.appendChild(overlay);

    const drawer = document.createElement("aside");
    drawer.id = "m3-mobile-nav-drawer";
    drawer.className = "m3-mobile-drawer";
    drawer.innerHTML = `
      <div class="flex items-center justify-between pb-3 mb-3 border-b border-[rgba(var(--theme-accent-rgb,208,188,255),0.18)]">
        <a href="/app" class="flex items-center gap-3 text-decoration-none" onclick="return navigateToSection('/app', 'dashboard');">
          <img id="mobile-drawer-brand-logo" src="${savedLogoUrl}" alt="${savedAppName}" class="w-8 h-8 app-brand-logo object-contain" />
          <div class="flex flex-col">
            <span class="text-base font-bold text-white tracking-tight flex items-center gap-1.5">
              <span id="mobile-drawer-brand-name" class="app-brand-name">${savedAppName}</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded-full font-mono m3-badge-brand">Console</span>
            </span>
          </div>
        </a>
        <button type="button" class="m3-icon-btn" onclick="closeMobileNavDrawer()" title="Chiudi menu">
          <i class="fa-solid fa-xmark text-sm"></i>
        </button>
      </div>

      <!-- Navigazione Drawer suddivisa nelle stesse categorie desktop -->
      <div class="mobile-drawer-sections-container flex-1 overflow-y-auto pr-1">

        <!-- Categoria: Principale -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_main">Principale</span>
          </div>
          <a href="/app" id="mobile-nav-dashboard" onclick="return navigateToSection('/app', 'dashboard');" class="m3-mobile-nav-link ${isOverview ? 'active' : ''}">
            <i class="fa-solid fa-gauge-high"></i>
            <span data-i18n="nav_overview">Panoramica</span>
          </a>
        </div>

        <!-- Categoria: Identità & Accessi (IAM) -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_iam">Identità & Accessi</span>
          </div>
          <a href="/app/users" id="mobile-nav-users" onclick="return navigateToSection('/app/users', 'users');" class="m3-mobile-nav-link ${isUsers ? 'active' : ''}">
            <i class="fa-solid fa-users"></i>
            <span data-i18n="nav_users">Utenti</span>
          </a>
          <a href="/app/clients" id="mobile-nav-clients" onclick="return navigateToSection('/app/clients', 'clients');" class="m3-mobile-nav-link ${isClients ? 'active' : ''}">
            <i class="fa-solid fa-key"></i>
            <span data-i18n="nav_clients">Client OAuth 2.0</span>
          </a>
        </div>

        <!-- Categoria: Sistema & Risorse -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_system">Sistema & Risorse</span>
          </div>
          <a href="/app/settings" id="mobile-nav-settings" onclick="return navigateToSection('/app/settings', 'settings');" class="m3-mobile-nav-link ${isSettings ? 'active' : ''}">
            <i class="fa-solid fa-gear"></i>
            <span data-i18n="nav_settings">Impostazioni</span>
          </a>
          <a href="https://github.com/giovannilamarmora/Access-Sphere" target="_blank" rel="noopener noreferrer" class="m3-mobile-nav-link">
            <i class="fa-solid fa-book"></i>
            <span data-i18n="nav_docs">API Docs</span>
          </a>
        </div>

        <!-- Categoria: Strumenti & Azioni -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_tools">Strumenti & Azioni</span>
          </div>
          <button type="button" onclick="triggerMobileRefresh()" class="m3-mobile-nav-link w-full text-left bg-transparent border-0 cursor-pointer">
            <i class="fa-solid fa-arrows-rotate"></i>
            <span data-i18n="btn_refresh">Aggiorna Dati</span>
          </button>
          ${!isRootDashboard ? `
          <button type="button" onclick="triggerMobileBack()" class="m3-mobile-nav-link w-full text-left bg-transparent border-0 cursor-pointer">
            <i class="fa-solid fa-arrow-left"></i>
            <span data-i18n="btn_back">Torna Indietro</span>
          </button>
          ` : ''}
          <a href="/app/users/register" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
            <i class="fa-solid fa-user-plus"></i>
            <span data-i18n="btn_new_user">Nuovo Utente</span>
          </a>
          <a href="/app/clients/register" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
            <i class="fa-solid fa-plus"></i>
            <span data-i18n="btn_new_client">Nuovo Client</span>
          </a>
        </div>

      </div>

      <!-- Footer Menu: Info & Logout -->
      <div class="mt-auto pt-3 border-t border-[rgba(var(--theme-accent-rgb,208,188,255),0.18)] space-y-2">
        <button type="button" onclick="getVersion(); closeMobileNavDrawer();" class="m3-mobile-nav-link w-full text-left bg-transparent border-0 cursor-pointer">
          <i class="fa-solid fa-circle-info"></i>
          <span data-i18n="btn_version_info">Info Versione</span>
        </button>
        <button type="button" onclick="logout()" class="m3-mobile-nav-link m3-mobile-logout-btn w-full text-left cursor-pointer">
          <i class="fa-solid fa-arrow-right-from-bracket"></i>
          <span data-i18n="btn_logout">Logout</span>
        </button>
      </div>
    `;
    document.body.appendChild(drawer);
  }

  // 3. Material 3 Expressive Bottom Navigation Bar (Mobile <992px)
  if (!document.getElementById("m3-mobile-bottom-nav")) {
    const bottomNav = document.createElement("nav");
    bottomNav.id = "m3-mobile-bottom-nav";
    bottomNav.className = "m3-mobile-bottom-nav";
    bottomNav.setAttribute("aria-label", "Navigazione Principale");
    bottomNav.innerHTML = `
      <a href="/app" id="bottom-nav-dashboard" class="m3-bottom-nav-item ${isOverview ? 'active' : ''}" onclick="return navigateToSection('/app', 'dashboard');">
        <div class="m3-bottom-nav-pill">
          <i class="fa-solid fa-gauge-high"></i>
        </div>
        <span class="m3-bottom-nav-label" data-i18n="nav_overview">Panoramica</span>
      </a>
      <a href="/app/users" id="bottom-nav-users" class="m3-bottom-nav-item ${isUsers ? 'active' : ''}" onclick="return navigateToSection('/app/users', 'users');">
        <div class="m3-bottom-nav-pill">
          <i class="fa-solid fa-users"></i>
        </div>
        <span class="m3-bottom-nav-label" data-i18n="nav_users">Utenti</span>
      </a>
      <a href="/app/clients" id="bottom-nav-clients" class="m3-bottom-nav-item ${isClients ? 'active' : ''}" onclick="return navigateToSection('/app/clients', 'clients');">
        <div class="m3-bottom-nav-pill">
          <i class="fa-solid fa-key"></i>
        </div>
        <span class="m3-bottom-nav-label" data-i18n="nav_clients_short">Client</span>
      </a>
      <a href="/app/settings" id="bottom-nav-settings" class="m3-bottom-nav-item ${isSettings ? 'active' : ''}" onclick="return navigateToSection('/app/settings', 'settings');">
        <div class="m3-bottom-nav-pill">
          <i class="fa-solid fa-gear"></i>
        </div>
        <span class="m3-bottom-nav-label" data-i18n="nav_settings">Impostazioni</span>
      </a>
    `;
    document.body.appendChild(bottomNav);
    document.body.classList.add("has-bottom-nav");
  }

  syncMobileNavState(currentView);

  // Traduzione dinamica elementi iniettati
  if (typeof translateDOM === "function") {
    const d = document.getElementById("m3-mobile-nav-drawer");
    const b = document.getElementById("m3-mobile-bottom-nav");
    if (d) translateDOM(d);
    if (b) translateDOM(b);
  }
}

// Global exports
window.isPostLoginPage = isPostLoginPage;
window.detectCurrentNavView = detectCurrentNavView;
window.syncMobileNavState = syncMobileNavState;
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


