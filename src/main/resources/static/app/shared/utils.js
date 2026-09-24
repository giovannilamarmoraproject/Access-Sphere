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
        "access_sphere_footer_copyright",
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

/**
 * Decodifica in modo sicuro un token JWT e ne estrae il payload JSON.
 * @param {string} token - Token JWT standard
 * @returns {object|null}
 */
function decodeJwtPayload(token) {
    if (!token || typeof token !== "string" || !token.includes(".")) return null;
    try {
        const parts = token.split(".");
        if (parts.length < 2) return null;
        let base64Url = parts[1];
        let base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        while (base64.length % 4) {
            base64 += "=";
        }
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split("")
                .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
                .join("")
        );
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

/**
 * Recupera l'utente correntemente autenticato con fallback a catena:
 * 1. Cache locale 'access_sphere_user'
 * 2. Decodifica immediata del payload JWT da 'access-token'
 * Garantisce rendering immediato a zero flicker del profilo utente nella sidebar.
 * @returns {object|null}
 */
const DEFAULT_USER_AVATAR = "https://bootdey.com/img/Content/avatar/avatar7.png";

/**
 * Arricchisce l'oggetto utente loggato con nome, cognome e foto profilo
 * estraendoli dalla cache utenti globale se presenti, garantendo visualizzazione immediata.
 * @param {object} userObj
 */
function enrichUserFromCachedUsers(userObj) {
    if (!userObj) return;
    try {
        const clientId = localStorage.getItem("Client-ID") || "ACCESS-SPHERE-TECH";
        const rawUsers =
            localStorage.getItem(clientId + "_usersData") ||
            localStorage.getItem("ACCESS-SPHERE-TECH_usersData");
        if (rawUsers) {
            const users = JSON.parse(rawUsers);
            if (Array.isArray(users)) {
                const found = users.find(
                    (u) =>
                        (userObj.username && u.username && u.username.toLowerCase() === userObj.username.toLowerCase()) ||
                        (userObj.email && u.email && u.email.toLowerCase() === userObj.email.toLowerCase())
                );
                if (found) {
                    if (found.name) userObj.name = found.name;
                    if (found.surname) userObj.surname = found.surname;
                    if (found.name && found.surname) {
                        userObj.fullName = `${found.name} ${found.surname}`;
                    } else if (found.name) {
                        userObj.fullName = found.name;
                    }
                    if (found.profilePhoto || found.profile) {
                        userObj.profilePhoto = found.profilePhoto || found.profile;
                    }
                }
            }
        }
    } catch (e) {}

    if (!userObj.profilePhoto) {
        userObj.profilePhoto = DEFAULT_USER_AVATAR;
    }
}

/**
 * Recupera l'utente correntemente autenticato con fallback a catena:
 * 1. Cache locale 'access_sphere_user'
 * 2. Decodifica immediata del payload JWT da 'access-token'
 * Garantisce rendering immediato a zero flicker del profilo utente nella sidebar.
 * @returns {object|null}
 */
function getLoggedInUser() {
    try {
        const cached = localStorage.getItem("access_sphere_user");
        if (cached) {
            const parsed = JSON.parse(cached);
            if (parsed && (parsed.fullName || parsed.name || parsed.username)) {
                if (!parsed.profilePhoto || parsed.fullName === parsed.username) {
                    enrichUserFromCachedUsers(parsed);
                    try {
                        localStorage.setItem("access_sphere_user", JSON.stringify(parsed));
                    } catch (e) {}
                }
                return parsed;
            }
        }
    } catch (e) {}

    const token =
        localStorage.getItem("access-token") ||
        localStorage.getItem("ACCESS-SPHERE-TECH_access-token") ||
        (typeof getCookie === "function" ? getCookie("access-token") : null);

    if (token) {
        const claims = decodeJwtPayload(token);
        if (claims) {
            const firstName = claims.given_name || claims.firstName || "";
            const lastName = claims.family_name || claims.lastName || "";
            let fullName = "";
            if (firstName && lastName) {
                fullName = `${firstName} ${lastName}`;
            } else if (claims.name) {
                fullName = claims.name;
            } else if (claims.sub) {
                fullName = claims.sub;
            } else {
                fullName = "Utente";
            }

            const userObj = {
                name: firstName,
                surname: lastName,
                fullName: fullName,
                username: claims.sub || claims.username || "",
                email: claims.email || "",
                roles: claims.roles || [],
                profilePhoto: claims.profilePhoto || claims.picture || claims.photo || null
            };
            enrichUserFromCachedUsers(userObj);
            try {
                localStorage.setItem("access_sphere_user", JSON.stringify(userObj));
            } catch (e) {}
            return userObj;
        }
    }

    return null;
}

/**
 * Esegue la sincronizzazione asincrona con l'endpoint del profilo (/v1/users/profile)
 * aggiornando la cache locale e riflettendo immediatamente le modifiche nel DOM.
 */
function syncLoggedInUserProfile() {
    const token =
        localStorage.getItem("access-token") ||
        localStorage.getItem("ACCESS-SPHERE-TECH_access-token") ||
        (typeof getCookie === "function" ? getCookie("access-token") : null);

    if (!token) return Promise.resolve(null);

    // 1. Arricchimento immediato da cache per zero attese
    const current = getLoggedInUser();
    if (current) {
        enrichUserFromCachedUsers(current);
        updateSidebarUserDOM(current);
    }

    // 2. Chiamata di sincronizzazione con tutti gli header di sessione
    const profileUrl = window.location.origin + "/v1/users/profile";
    const headers = {
        Authorization: `Bearer ${token}`,
        ...(typeof getSavedHeaders === "function" ? getSavedHeaders() : {})
    };
    const clientId = localStorage.getItem("Client-ID") || "ACCESS-SPHERE-TECH";
    if (!headers["Client-ID"]) {
        headers["Client-ID"] = clientId;
    }

    return fetch(profileUrl, {
        method: "GET",
        headers: headers
    })
        .then((res) => {
            if (!res.ok) {
                // Fallback trasparente su /v1/users per recuperare l'anagrafica completa
                const usersUrl = window.location.origin + "/v1/users";
                return fetch(usersUrl, { method: "GET", headers: headers })
                    .then((uRes) => (uRes.ok ? uRes.json() : null))
                    .then((uData) => {
                        if (uData && Array.isArray(uData.data)) {
                            const myUser = uData.data.find(
                                (u) =>
                                    (current && current.username && u.username && u.username.toLowerCase() === current.username.toLowerCase()) ||
                                    (current && current.email && u.email && u.email.toLowerCase() === current.email.toLowerCase())
                            );
                            if (myUser) {
                                return { data: myUser };
                            }
                        }
                        return null;
                    })
                    .catch(() => null);
            }
            return res.json();
        })
        .then((data) => {
            if (data && data.data) {
                const u = data.data;
                const firstName = u.name || u.firstName || "";
                const lastName = u.surname || u.lastName || "";
                let fullName = "";
                if (firstName && lastName) {
                    fullName = `${firstName} ${lastName}`;
                } else if (firstName) {
                    fullName = firstName;
                } else if (lastName) {
                    fullName = lastName;
                } else if (u.username) {
                    fullName = u.username;
                } else {
                    fullName = "Utente";
                }

                const photo = u.profilePhoto || u.profile || DEFAULT_USER_AVATAR;

                const userObj = {
                    name: firstName,
                    surname: lastName,
                    fullName: fullName,
                    username: u.username || "",
                    email: u.email || "",
                    roles: u.roles || [],
                    profilePhoto: photo
                };
                try {
                    localStorage.setItem("access_sphere_user", JSON.stringify(userObj));
                } catch (e) {}
                updateSidebarUserDOM(userObj);
                return userObj;
            }
            return null;
        })
        .catch(() => null);
}

/**
 * Aggiorna i selettori DOM del profilo utente per la sidebar desktop e il drawer mobile.
 * Gestisce sia avatar fotografico che fallback affidabile all'avatar standard.
 * @param {object} userObj
 */
function updateSidebarUserDOM(userObj) {
    if (!userObj) return;

    const photoUrl = userObj.profilePhoto || DEFAULT_USER_AVATAR;

    function renderAvatarContent(containerEl) {
        if (!containerEl) return;
        containerEl.innerHTML = `<img src="${photoUrl}" alt="${escapeHtmlAttr(userObj.fullName || 'User')}" class="w-full h-full object-cover rounded-full" onerror="this.onerror=null; this.src='${DEFAULT_USER_AVATAR}';" />`;
    }

    // Sidebar Desktop
    const desktopNameEl = document.getElementById("sidebar-user-fullname");
    const desktopUserEl = document.getElementById("sidebar-user-username");
    const desktopCard = document.getElementById("sidebar-user-card");
    const desktopAvatar = document.getElementById("sidebar-user-avatar");

    if (desktopNameEl) desktopNameEl.textContent = userObj.fullName || "Utente";
    if (desktopUserEl) desktopUserEl.textContent = userObj.username ? `@${userObj.username}` : (userObj.email || "");
    if (desktopCard) {
        desktopCard.title = `${userObj.fullName || "Utente"}${userObj.username ? ` (@${userObj.username})` : ""}`;
    }
    if (desktopAvatar) {
        renderAvatarContent(desktopAvatar);
    }

    // Drawer Mobile
    const mobileNameEl = document.getElementById("mobile-drawer-user-fullname");
    const mobileUserEl = document.getElementById("mobile-drawer-user-username");
    const mobileAvatar = document.getElementById("mobile-drawer-user-avatar");

    if (mobileNameEl) mobileNameEl.textContent = userObj.fullName || "Utente";
    if (mobileUserEl) mobileUserEl.textContent = userObj.username ? `@${userObj.username}` : (userObj.email || "");
    if (mobileAvatar) {
        renderAvatarContent(mobileAvatar);
    }
}

window.decodeJwtPayload = decodeJwtPayload;
window.getLoggedInUser = getLoggedInUser;
window.syncLoggedInUserProfile = syncLoggedInUserProfile;
window.updateSidebarUserDOM = updateSidebarUserDOM;

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
        window.scrollTo({top: 0, left: 0, behavior: behavior});
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
}, {passive: true});

function isPostLoginPage() {
    const p = window.location.pathname;
    return p.startsWith("/app") && !p.startsWith("/app/login");
}

function detectCurrentNavView() {
    const currentPath = window.location.pathname;
    if (currentPath.includes("/emails") || currentPath.includes("/email")) return "emails";
    if (currentPath.includes("/settings")) return "settings";
    if (currentPath.includes("/client")) return "clients";
    if (currentPath.includes("/role")) return "roles";
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
    const isRoles = viewName === "roles";
    const isEmails = viewName === "emails";
    const isSettings = viewName === "settings";

    // 1. Bottom Navigation Bar Mobile
    const bDash = document.getElementById("bottom-nav-dashboard");
    const bUsers = document.getElementById("bottom-nav-users");
    const bRoles = document.getElementById("bottom-nav-roles");
    const bClients = document.getElementById("bottom-nav-clients");
    const bEmails = document.getElementById("bottom-nav-emails");
    const bSettings = document.getElementById("bottom-nav-settings");
    if (bDash) bDash.classList.toggle("active", isOverview);
    if (bUsers) bUsers.classList.toggle("active", isUsers);
    if (bRoles) bRoles.classList.toggle("active", isRoles);
    if (bClients) bClients.classList.toggle("active", isClients);
    if (bEmails) bEmails.classList.toggle("active", isEmails);
    if (bSettings) bSettings.classList.toggle("active", isSettings);

    // 2. Mobile Navigation Drawer
    const mDash = document.getElementById("mobile-nav-dashboard");
    const mUsers = document.getElementById("mobile-nav-users");
    const mRoles = document.getElementById("mobile-nav-roles");
    const mClients = document.getElementById("mobile-nav-clients");
    const mEmails = document.getElementById("mobile-nav-emails");
    const mSettings = document.getElementById("mobile-nav-settings");
    if (mDash) mDash.classList.toggle("active", isOverview);
    if (mUsers) mUsers.classList.toggle("active", isUsers);
    if (mRoles) mRoles.classList.toggle("active", isRoles);
    if (mClients) mClients.classList.toggle("active", isClients);
    if (mEmails) mEmails.classList.toggle("active", isEmails);
    if (mSettings) mSettings.classList.toggle("active", isSettings);

    // 3. Desktop Sidebar M3
    const sDash = document.getElementById("sidebar-btn-dashboard");
    const sUsers = document.getElementById("sidebar-btn-users");
    const sRoles = document.getElementById("sidebar-btn-roles");
    const sClients = document.getElementById("sidebar-btn-clients");
    const sEmails = document.getElementById("sidebar-btn-emails");
    const sSettings = document.getElementById("sidebar-btn-settings");
    if (sDash) sDash.classList.toggle("active", isOverview);
    if (sUsers) sUsers.classList.toggle("active", isUsers);
    if (sRoles) sRoles.classList.toggle("active", isRoles);
    if (sClients) sClients.classList.toggle("active", isClients);
    if (sEmails) sEmails.classList.toggle("active", isEmails);
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
    const isRoles = currentView === "roles";
    const isClients = currentView === "clients";
    const isEmails = currentView === "emails";
    const isSettings = currentView === "settings";

    // 2. Drawer & Overlay laterale per mobile
    if (!document.getElementById("m3-mobile-nav-drawer")) {
        const currentPath = window.location.pathname;
        const isRootDashboard = currentPath === '/app' || currentPath === '/app/' || currentPath === '';
        const savedAppName = localStorage.getItem("access_sphere_app_name") || "Access Sphere";
        const savedLogoUrl = localStorage.getItem("access_sphere_logo_url") || "/img/logo-minimal.svg";
        const currentUser = getLoggedInUser();

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

      <!-- Scheda Profilo Utente Mobile (Icona, Nome e Cognome) -->
      <div class="m3-mobile-user-card" id="mobile-drawer-user-card">
        <div class="sidebar-user-avatar" id="mobile-drawer-user-avatar">
          <img src="${escapeHtmlAttr(currentUser && currentUser.profilePhoto ? currentUser.profilePhoto : 'https://bootdey.com/img/Content/avatar/avatar7.png')}" alt="${escapeHtmlAttr(currentUser ? currentUser.fullName : 'Utente')}" class="w-full h-full object-cover rounded-full" onerror="this.onerror=null; this.src='https://bootdey.com/img/Content/avatar/avatar7.png';" />
        </div>
        <div class="sidebar-user-info">
          <span class="sidebar-user-name" id="mobile-drawer-user-fullname">${escapeHtmlAttr(currentUser ? currentUser.fullName : "Utente")}</span>
          <span class="sidebar-user-role" id="mobile-drawer-user-username">${escapeHtmlAttr(currentUser && currentUser.username ? `@${currentUser.username}` : "")}</span>
        </div>
      </div>

      <!-- Navigazione Drawer suddivisa nelle stesse categorie desktop -->
      <div class="mobile-drawer-sections-container flex-1 overflow-y-auto pr-1">

        <!-- Categoria: Panoramica -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_main">Panoramica</span>
          </div>
          <a href="/app" id="mobile-nav-dashboard" onclick="return navigateToSection('/app', 'dashboard');" class="m3-mobile-nav-link ${isOverview ? 'active' : ''}">
            <i class="fa-solid fa-gauge-high"></i>
            <span data-i18n="nav_overview">Panoramica</span>
          </a>
        </div>

        <!-- Categoria: Identità & Utenti -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_identity">Identità & Utenti</span>
          </div>
          <a href="/app/users" id="mobile-nav-users" onclick="return navigateToSection('/app/users', 'users');" class="m3-mobile-nav-link ${isUsers ? 'active' : ''}">
            <i class="fa-solid fa-users"></i>
            <span data-i18n="nav_users">Utenti</span>
          </a>
        </div>

        <!-- Categoria: Ruoli & Permessi -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_roles">Ruoli & Permessi</span>
          </div>
          <a href="/app/role" id="mobile-nav-roles" onclick="return navigateToSection('/app/role', 'roles');" class="m3-mobile-nav-link ${isRoles ? 'active' : ''}">
            <i class="fa-solid fa-shield-halved"></i>
            <span data-i18n="nav_roles">Ruoli & Permessi</span>
          </a>
        </div>

        <!-- Categoria: Applicazioni & Client -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_apps">Applicazioni & Client</span>
          </div>
          <a href="/app/clients" id="mobile-nav-clients" onclick="return navigateToSection('/app/clients', 'clients');" class="m3-mobile-nav-link ${isClients ? 'active' : ''}">
            <i class="fa-solid fa-key"></i>
            <span data-i18n="nav_clients">Client OAuth 2.0</span>
          </a>
        </div>

        <!-- Categoria: Comunicazioni -->
        <div class="mb-4">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_comms">Comunicazioni</span>
          </div>
          <a href="/app/emails" id="mobile-nav-emails" onclick="return navigateToSection('/app/emails', 'emails');" class="m3-mobile-nav-link ${isEmails ? 'active' : ''}">
            <i class="fa-solid fa-envelope"></i>
            <span data-i18n="nav_emails">Template Email</span>
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
          <a href="/webjars/swagger-ui/index.html" target="_blank" rel="noopener noreferrer" class="m3-mobile-nav-link">
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
          <a href="/app/role" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
            <i class="fa-solid fa-shield-plus"></i>
            <span data-i18n="roles_btn_new">Nuovo Ruolo</span>
          </a>
        </div>
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
        syncLoggedInUserProfile();
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
      <a href="/app/role" id="bottom-nav-roles" class="m3-bottom-nav-item ${isRoles ? 'active' : ''}" onclick="return navigateToSection('/app/role', 'roles');">
        <div class="m3-bottom-nav-pill">
          <i class="fa-solid fa-shield-halved"></i>
        </div>
        <span class="m3-bottom-nav-label" data-i18n="nav_roles">Ruoli</span>
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

// Nasconde la bottom nav mobile all'apertura di modali Bootstrap per evitare sovrapposizioni con footer/pulsanti
document.addEventListener("show.bs.modal", function () {
    const nav = document.getElementById("m3-mobile-bottom-nav");
    if (nav) nav.classList.add("hidden-by-modal");
});
document.addEventListener("hidden.bs.modal", function () {
    if (!document.querySelector(".modal.show")) {
        const nav = document.getElementById("m3-mobile-bottom-nav");
        if (nav) nav.classList.remove("hidden-by-modal");
    }
});

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


