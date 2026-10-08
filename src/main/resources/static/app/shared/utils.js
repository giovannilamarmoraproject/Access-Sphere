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

/**
 * Restituisce la classe CSS corretta per il badge del tipo ruolo Material Design 3.
 * Supporta resilientemente tutti gli 8 tipi canonici definiti in AppRoleType:
 * USER, ADMIN, MANAGER, OPERATOR, TECHNICAL, AUDITOR, GUEST, SUPER_ADMIN.
 *
 * @param {string} type - Tipo ruolo (stringa, case-insensitive, supporta prefissi ROLE_ e ACCESS-SPHERE_)
 * @returns {string} Nome della classe CSS associata
 */
function getRoleTypeBadgeClass(type) {
    if (!type) return "role-badge-user";
    const t = String(type).toUpperCase().replace(/^(ROLE_|ACCESS-SPHERE_)/, "");
    switch (t) {
        case "ADMIN":
            return "role-badge-admin";
        case "SUPER_ADMIN":
        case "SUPERADMIN":
            return "role-badge-superadmin";
        case "MANAGER":
            return "role-badge-manager";
        case "OPERATOR":
            return "role-badge-operator";
        case "TECHNICAL":
        case "TECH":
            return "role-badge-technical";
        case "AUDITOR":
            return "role-badge-auditor";
        case "GUEST":
            return "role-badge-guest";
        case "USER":
        default:
            return "role-badge-user";
    }
}
window.getRoleTypeBadgeClass = getRoleTypeBadgeClass;

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
        const clientId = localStorage.getItem("Client-ID") || "ACCESS-SPHERE-AUTH";
        const rawUsers =
            localStorage.getItem(clientId + "_usersData") ||
            localStorage.getItem("ACCESS-SPHERE-AUTH_usersData");
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
    } catch (e) {
    }

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
    const token =
        localStorage.getItem("access-token") ||
        localStorage.getItem("ACCESS-SPHERE-AUTH_access-token") ||
        (typeof getCookie === "function" ? getCookie("access-token") : null);

    let tokenClaims = null;
    if (token) {
        tokenClaims = decodeJwtPayload(token);
    }

    try {
        const cached = localStorage.getItem("access_sphere_user");
        if (cached) {
            let parsed = JSON.parse(cached);
            // Verifica di coerenza: se esiste un token attivo con subject (sub), la cache utente
            // viene invalidata se non corrisponde allo username attualmente autenticato.
            if (tokenClaims && tokenClaims.sub && parsed && parsed.username &&
                tokenClaims.sub.toLowerCase() !== parsed.username.toLowerCase()) {
                parsed = null;
                localStorage.removeItem("access_sphere_user");
            }
            if (parsed && (parsed.fullName || parsed.name || parsed.username)) {
                if (!parsed.profilePhoto || parsed.fullName === parsed.username) {
                    enrichUserFromCachedUsers(parsed);
                    try {
                        localStorage.setItem("access_sphere_user", JSON.stringify(parsed));
                    } catch (e) {
                    }
                }
                return parsed;
            }
        }
    } catch (e) {
    }

    if (token) {
        const claims = tokenClaims || decodeJwtPayload(token);
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

            const isTechnical = (claims.subject_type === "TECHNICAL") ||
                (claims.sub && claims.sub.toLowerCase() === "tech_admin") ||
                (Array.isArray(claims.roles) && claims.roles.some((r) => r === "ROLE_TECH" || r === "ROLE_SUPER_ADMIN"));

            const userObj = {
                name: firstName,
                surname: lastName,
                fullName: fullName,
                username: claims.sub || claims.username || "",
                email: claims.email || "",
                roles: Array.isArray(claims.roles) ? claims.roles : (claims.roles ? [claims.roles] : []),
                permissions: Array.isArray(claims.permissions) ? claims.permissions : (claims.permissions ? [claims.permissions] : []),
                clientRoles: Array.isArray(claims.clientRoles) ? claims.clientRoles : [],
                type: claims.type || claims.subject_type || "",
                subjectType: claims.subject_type || "",
                isTechnical: isTechnical,
                profilePhoto: claims.profilePhoto || claims.picture || claims.photo || null
            };
            enrichUserFromCachedUsers(userObj);
            try {
                localStorage.setItem("access_sphere_user", JSON.stringify(userObj));
            } catch (e) {
            }
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
        localStorage.getItem("ACCESS-SPHERE-AUTH_access-token") ||
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
    const clientId = localStorage.getItem("Client-ID") || "ACCESS-SPHERE-AUTH";
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
                return fetch(usersUrl, {method: "GET", headers: headers})
                    .then((uRes) => (uRes.ok ? uRes.json() : null))
                    .then((uData) => {
                        if (uData && Array.isArray(uData.data)) {
                            const myUser = uData.data.find(
                                (u) =>
                                    (current && current.username && u.username && u.username.toLowerCase() === current.username.toLowerCase()) ||
                                    (current && current.email && u.email && u.email.toLowerCase() === current.email.toLowerCase())
                            );
                            if (myUser) {
                                return {data: myUser};
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

                // Aggrega ruoli completi (da flat roles e clientRoles)
                let roles = Array.isArray(u.roles) ? [...u.roles] : [];
                if (Array.isArray(u.clientRoles)) {
                    u.clientRoles.forEach((cr) => {
                        if (cr && cr.role) roles.push(cr.role);
                    });
                }
                roles = Array.from(new Set(roles));

                // Aggrega permessi completi (da u.permissions e clientRoles.permissions)
                let permissions = Array.isArray(u.permissions) ? [...u.permissions] : [];
                if (Array.isArray(u.clientRoles)) {
                    u.clientRoles.forEach((cr) => {
                        if (cr && Array.isArray(cr.permissions)) {
                            permissions.push(...cr.permissions);
                        }
                    });
                }
                permissions = Array.from(new Set(permissions));

                const isTechnical = (u.username && u.username.toLowerCase() === "tech_admin") ||
                    roles.some((r) => r === "ROLE_TECH" || r === "ROLE_SUPER_ADMIN") ||
                    (current && current.isTechnical === true);

                const userObj = {
                    name: firstName,
                    surname: lastName,
                    fullName: fullName,
                    username: u.username || "",
                    email: u.email || "",
                    roles: roles,
                    permissions: permissions,
                    clientRoles: u.clientRoles || [],
                    type: u.type || (current && current.type) || "",
                    subjectType: (current && current.subjectType) || (isTechnical ? "TECHNICAL" : "USER"),
                    isTechnical: isTechnical,
                    profilePhoto: photo
                };
                try {
                    localStorage.setItem("access_sphere_user", JSON.stringify(userObj));
                } catch (e) {
                }
                updateSidebarUserDOM(userObj);
                if (typeof AccessControl !== "undefined" && typeof AccessControl.applyUIPermissions === "function") {
                    AccessControl.applyUIPermissions();
                }
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
    if (typeof AccessControl !== "undefined") {
        if ((viewName === "clients" || targetPath.includes("/clients")) && !AccessControl.canViewClients()) {
            if (typeof sweetalert === "function") {
                const title = typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato";
                const desc = typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei privilegi necessari per accedere a questa sezione.") : "Non disponi dei privilegi necessari per accedere a questa sezione.";
                sweetalert("warning", title, desc);
            }
            return false;
        }
        if ((viewName === "users" || targetPath.includes("/users")) && !AccessControl.canViewUsers()) {
            if (typeof sweetalert === "function") {
                const title = typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato";
                const desc = typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei privilegi necessari per accedere a questa sezione.") : "Non disponi dei privilegi necessari per accedere a questa sezione.";
                sweetalert("warning", title, desc);
            }
            return false;
        }
        if ((viewName === "roles" || targetPath.includes("/role")) && !AccessControl.canViewRoles()) {
            if (typeof sweetalert === "function") {
                const title = typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato";
                const desc = typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei privilegi necessari per accedere a questa sezione.") : "Non disponi dei privilegi necessari per accedere a questa sezione.";
                sweetalert("warning", title, desc);
            }
            return false;
        }
        if ((viewName === "emails" || targetPath.includes("/emails")) && !AccessControl.canViewEmails()) {
            if (typeof sweetalert === "function") {
                const title = typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato";
                const desc = typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei privilegi necessari per accedere a questa sezione.") : "Non disponi dei privilegi necessari per accedere a questa sezione.";
                sweetalert("warning", title, desc);
            }
            return false;
        }
        if ((viewName === "settings" || targetPath.includes("/settings")) && !AccessControl.canViewSettings()) {
            if (typeof sweetalert === "function") {
                const title = typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato";
                const desc = typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei privilegi necessari per accedere a questa sezione.") : "Non disponi dei privilegi necessari per accedere a questa sezione.";
                sweetalert("warning", title, desc);
            }
            return false;
        }
    }
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
        <div class="mb-4" id="mobile-drawer-cat-main">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_main">Panoramica</span>
          </div>
          <a href="/app" id="mobile-nav-dashboard" onclick="return navigateToSection('/app', 'dashboard');" class="m3-mobile-nav-link ${isOverview ? 'active' : ''}">
            <i class="fa-solid fa-gauge-high"></i>
            <span data-i18n="nav_overview">Panoramica</span>
          </a>
        </div>

        <!-- Categoria: Identità & Utenti -->
        <div class="mb-4" id="mobile-drawer-cat-users">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_identity">Identità & Utenti</span>
          </div>
          <a href="/app/users" id="mobile-nav-users" onclick="return navigateToSection('/app/users', 'users');" class="m3-mobile-nav-link ${isUsers ? 'active' : ''}">
            <i class="fa-solid fa-users"></i>
            <span data-i18n="nav_users">Utenti</span>
          </a>
        </div>

        <!-- Categoria: Ruoli & Permessi -->
        <div class="mb-4" id="mobile-drawer-cat-roles">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_roles">Ruoli & Permessi</span>
          </div>
          <a href="/app/role" id="mobile-nav-roles" onclick="return navigateToSection('/app/role', 'roles');" class="m3-mobile-nav-link ${isRoles ? 'active' : ''}">
            <i class="fa-solid fa-shield-halved"></i>
            <span data-i18n="nav_roles">Ruoli & Permessi</span>
          </a>
        </div>

        <!-- Categoria: Applicazioni & Client -->
        <div class="mb-4" id="mobile-drawer-cat-clients">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_apps">Applicazioni & Client</span>
          </div>
          <a href="/app/clients" id="mobile-nav-clients" onclick="return navigateToSection('/app/clients', 'clients');" class="m3-mobile-nav-link ${isClients ? 'active' : ''}">
            <i class="fa-solid fa-key"></i>
            <span data-i18n="nav_clients">Client OAuth 2.0</span>
          </a>
        </div>

        <!-- Categoria: Comunicazioni -->
        <div class="mb-4" id="mobile-drawer-cat-emails">
          <div class="sidebar-category-header px-2 py-1">
            <span class="sidebar-category-label text-[10px]" data-i18n="sidebar_cat_comms">Comunicazioni</span>
          </div>
          <a href="/app/emails" id="mobile-nav-emails" onclick="return navigateToSection('/app/emails', 'emails');" class="m3-mobile-nav-link ${isEmails ? 'active' : ''}">
            <i class="fa-solid fa-envelope"></i>
            <span data-i18n="nav_emails">Template Email</span>
          </a>
        </div>

        <!-- Categoria: Sistema & Risorse -->
        <div class="mb-4" id="mobile-drawer-cat-system">
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
        <div class="mb-4" id="mobile-drawer-cat-tools">
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
          <a href="/app/users/register" id="mobile-tool-new-user" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
            <i class="fa-solid fa-user-plus"></i>
            <span data-i18n="btn_new_user">Nuovo Utente</span>
          </a>
          <a href="/app/clients/register" id="mobile-tool-new-client" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
            <i class="fa-solid fa-plus"></i>
            <span data-i18n="btn_new_client">Nuovo Client</span>
          </a>
          <a href="/app/role" id="mobile-tool-new-role" onclick="closeMobileNavDrawer(); scrollToTop('instant');" class="m3-mobile-nav-link">
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

/**
 * Utility per mostrare o nascondere un elemento DOM rimuovendo o aggiungendo display: none !important.
 * Rispetta le classi Tailwind esistenti (flex, block, inline-flex, ecc.) al ripristino della visibilità.
 * @param {HTMLElement|null} el
 * @param {boolean} isVisible
 */
function toggleVisibility(el, isVisible) {
    if (!el) return;
    if (isVisible) {
        el.style.removeProperty("display");
        if (el.classList.contains("force-hidden")) {
            el.classList.remove("force-hidden");
        }
    } else {
        el.style.setProperty("display", "none", "important");
    }
}

/**
 * =========================================================================================
 * AccessControl: Frontend Role-Based Access Control (RBAC) & Visibility Engine
 * =========================================================================================
 * Centralizza tutte le regole di autorizzazione, permessi granulari e visibilità condizionale
 * della UI di Access-Sphere per Header, Sidebar M3, Dashboard Hub e Drawer Mobile:
 *
 * Flusso dei dati e interconnessioni architetturali:
 * 1. isSuperAdmin(): Verifica se l'operatore è un'utenza tecnica di sistema o un Super Admin
 *    (subject_type == 'TECHNICAL', sub == 'tech_admin', ruoli ROLE_TECH / SUPER_ADMIN, o '*').
 * 2. canViewClients() / canManageClients(): Controlla i permessi per visualizzare o gestire i client.
 * 3. canViewUsers() / canManageUsers() / canDeleteUsers(): Controlla i permessi per utenti IAM.
 * 4. canViewRoles() / canManageRoles(): Controlla l'accesso al catalogo dei ruoli.
 * 5. canViewEmails() / canManageEmails(): Controlla l'accesso ai template email.
 * 6. canViewSettings() / canEditSettings() / canManageBackup(): Impostazioni e Backup & Restore.
 * 7. applyUIPermissions(root): Esegue la scansione degli elementi di navigazione e azioni nel DOM,
 *    nascondendo completamente qualsiasi percorso o pulsante per cui l'utente non dispone di permessi.
 * =========================================================================================
 */
const AccessControl = {
    getUser() {
        if (typeof getLoggedInUser === "function") {
            return getLoggedInUser();
        }
        try {
            const raw = localStorage.getItem("access_sphere_user");
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    },

    getRoles() {
        const u = this.getUser();
        if (!u) return [];
        const raw = u.roles || u.clientRoles;
        if (!Array.isArray(raw)) return [];
        return raw.map((r) => {
            if (typeof r === "object" && r !== null && r.role) {
                return String(r.role).toUpperCase();
            }
            return String(r).toUpperCase();
        });
    },

    getPermissions() {
        const u = this.getUser();
        if (!u) return [];
        const set = new Set();
        if (Array.isArray(u.permissions)) {
            u.permissions.forEach((p) => set.add(String(p).trim().toLowerCase()));
        }
        const rolesList = Array.isArray(u.roles) ? u.roles : (Array.isArray(u.clientRoles) ? u.clientRoles : []);
        rolesList.forEach((cr) => {
            if (cr && typeof cr === "object" && Array.isArray(cr.permissions)) {
                cr.permissions.forEach((p) => set.add(String(p).trim().toLowerCase()));
            }
        });
        return Array.from(set);
    },

    hasRole(...roleNames) {
        const roles = this.getRoles();
        const u = this.getUser();
        const userType = u && u.type ? String(u.type).toUpperCase() : "";
        return roleNames.some((target) => {
            const upper = String(target).toUpperCase();
            const stripped = upper.replace(/^(ROLE_|ACCESS-SPHERE_)/, "");
            return (
                roles.some((r) => {
                    const rUpper = String(r).toUpperCase();
                    const rStripped = rUpper.replace(/^(ROLE_|ACCESS-SPHERE_)/, "");
                    return rUpper === upper || rStripped === stripped;
                }) ||
                userType === stripped ||
                userType === upper
            );
        });
    },

    /**
     * Normalizza una stringa di permesso in minuscolo e formato canonico.
     * @param {string} perm
     * @returns {string}
     */
    normalizePerm(perm) {
        if (!perm) return "";
        let p = String(perm).trim().toLowerCase().replace(/_/g, ":");
        if (p === "*" || p === "all") return "*";
        return p;
    },

    /**
     * Verifica se una collezione di permessi posseduti soddisfa il permesso target richiesto.
     * Supporta wildcard "*", wildcard per risorsa ("users:*") e privilegi ":manage".
     * @param {string[]|Set<string>} grantedPerms
     * @param {string} targetPerm
     * @returns {boolean}
     */
    checkPermMatch(grantedPerms, targetPerm) {
        if (!grantedPerms || !targetPerm) return false;
        const normTarget = this.normalizePerm(targetPerm);
        if (!normTarget) return false;

        const list = Array.isArray(grantedPerms) ? grantedPerms : Array.from(grantedPerms);
        for (const granted of list) {
            if (!granted) continue;
            const normGranted = this.normalizePerm(granted);
            if (normGranted === "*") return true;
            if (normGranted === normTarget) return true;
            if (normGranted.endsWith(":*")) {
                const res = normGranted.substring(0, normGranted.length - 2);
                if (normTarget.startsWith(res + ":")) return true;
            }
            if (normGranted.endsWith(":manage")) {
                const res = normGranted.substring(0, normGranted.length - 7);
                if (normTarget.startsWith(res + ":")) return true;
            }
        }
        return false;
    },

    hasPermission(...permNames) {
        if (this.isSuperAdmin()) return true;
        const perms = this.getPermissions();
        if (perms.includes("*")) return true;
        return permNames.some((target) => this.checkPermMatch(perms, target));
    },

    isSuperAdmin() {
        const u = this.getUser();
        if (!u) return false;
        if (
            u.isTechnical === true ||
            u.subjectType === "TECHNICAL" ||
            (u.username && u.username.toLowerCase() === "tech_admin")
        ) {
            return true;
        }
        if (this.hasRole("TECH", "ROLE_TECH", "SUPER_ADMIN", "ROLE_SUPER_ADMIN", "TECHNICAL")) {
            return true;
        }
        const perms = this.getPermissions();
        return perms.includes("*");
    },

    /**
     * Recupera tutti i permessi associati a uno specifico clientId (dai ruoli in clientRoles dell'utente).
     * @param {string} clientId
     * @returns {string[]}
     */
    getClientPermissions(clientId) {
        if (!clientId) return this.getPermissions();
        if (this.isSuperAdmin()) return ["*"];
        const u = this.getUser();
        if (!u) return [];

        const targetClient = String(clientId).trim().toUpperCase();
        const set = new Set();

        // 1. Permessi espliciti globali (se presenti nell'utenza)
        if (Array.isArray(u.permissions)) {
            u.permissions.forEach((p) => set.add(String(p).trim().toLowerCase()));
        }

        // 2. Permessi derivati dai ruoli assegnati su questo specifico client
        if (Array.isArray(u.clientRoles)) {
            u.clientRoles.forEach((cr) => {
                if (cr && cr.clientId && String(cr.clientId).trim().toUpperCase() === targetClient) {
                    if (Array.isArray(cr.permissions)) {
                        cr.permissions.forEach((p) => set.add(String(p).trim().toLowerCase()));
                    }
                }
            });
        }
        return Array.from(set);
    },

    /**
     * Verifica se l'utente possiede almeno uno dei permessi specificati per il client target.
     * Supporta wildcard ('*') e normalizzazione (es. 'clients:write' / 'clients_write').
     * @param {string} clientId
     * @param {...string} permNames
     * @returns {boolean}
     */
    hasClientPermission(clientId, ...permNames) {
        if (this.isSuperAdmin()) return true;
        const perms = this.getClientPermissions(clientId);
        if (perms.includes("*")) return true;
        return permNames.some((target) => this.checkPermMatch(perms, target));
    },

    /**
     * Restituisce la lista dei clientId gestiti dall'utente (multi-tenant scoping).
     * @returns {string[]}
     */
    getManagedClients() {
        if (this.isSuperAdmin()) return ["*"];
        const u = this.getUser();
        if (!u || !Array.isArray(u.clientRoles)) return [];
        const clients = u.clientRoles
            .map((cr) => (cr && cr.clientId ? String(cr.clientId).trim().toUpperCase() : null))
            .filter(Boolean);
        return Array.from(new Set(clients));
    },

    canViewClients() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("clients:read", "clients:write", "clients:manage") ||
            this.hasRole(
                "ADMIN", "ROLE_ADMIN",
                "MANAGER", "ROLE_MANAGER",
                "OPERATOR", "ROLE_OPERATOR",
                "AUDITOR", "ROLE_AUDITOR"
            )
        );
    },

    /**
     * Verifica se l'operatore può visualizzare un determinato client.
     * @param {string} clientId
     * @returns {boolean}
     */
    canViewClient(clientId) {
        if (this.isSuperAdmin()) return true;
        if (!clientId) return this.canViewClients();
        const target = String(clientId).trim().toUpperCase();
        return this.hasClientPermission(target, "clients:read", "clients:write", "clients:manage");
    },

    /**
     * Verifica se l'operatore può creare un nuovo client.
     * @returns {boolean}
     */
    canCreateClient() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("clients:write", "clients:manage", "clients:create") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    /**
     * Verifica se l'operatore può modificare un determinato client.
     * NOTA: Il client tecnico ACCESS-SPHERE-AUTH può essere modificato unicamente dal Super Admin.
     * @param {string} clientId
     * @returns {boolean}
     */
    canEditClient(clientId) {
        if (!clientId) return this.canManageClients();
        const target = String(clientId).trim().toUpperCase();
        if (target === "ACCESS-SPHERE-AUTH") {
            return this.isSuperAdmin();
        }
        if (this.isSuperAdmin()) return true;
        return (
            this.hasClientPermission(target, "clients:write", "clients:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    /**
     * Verifica se l'operatore può eliminare un determinato client.
     * NOTA: Il client tecnico ACCESS-SPHERE-AUTH non può MAI essere eliminato da nessun utente.
     * @param {string} clientId
     * @returns {boolean}
     */
    canDeleteClient(clientId) {
        if (!clientId) return false;
        const target = String(clientId).trim().toUpperCase();
        if (target === "ACCESS-SPHERE-AUTH") {
            return false;
        }
        if (this.isSuperAdmin()) return true;
        return (
            this.hasClientPermission(target, "clients:delete", "clients:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    canManageClients() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("clients:write", "clients:delete", "clients:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    /**
     * Visualizza un alert SweetAlert2 di accesso negato e reindirizza l'utente alla schermata sicura.
     * @param {string} title
     * @param {string} text
     * @param {string} redirectUrl
     */
    showDeniedAlert(title, text, redirectUrl = "/app/clients") {
        if (typeof sweetalert === "function") {
            sweetalert("warning", title, text);
            setTimeout(() => {
                window.location.href = redirectUrl;
            }, 1800);
        } else if (typeof Swal !== "undefined") {
            Swal.fire({
                icon: "warning",
                title: title,
                text: text,
                confirmButtonColor: "var(--theme-accent, #7C4DFF)"
            }).then(() => {
                window.location.href = redirectUrl;
            });
        } else {
            alert(title + ": " + text);
            window.location.href = redirectUrl;
        }
    },

    canViewUsers() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("users:read", "users:write", "users:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "OPERATOR", "ROLE_OPERATOR", "AUDITOR", "ROLE_AUDITOR")
        );
    },

    canManageUsers() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("users:write", "users:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
        );
    },

    canDeleteUsers() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("users:delete", "users:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    /**
     * Verifica se l'operatore può bloccare o sbloccare l'accesso di un utente.
     * Privilegio concesso a Super Admin, Admin, Manager e Operator (users:lock).
     * @returns {boolean}
     */
    canLockUsers() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("users:lock", "users:write", "users:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "OPERATOR", "ROLE_OPERATOR")
        );
    },

    canViewRoles() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("roles:read", "roles:write", "roles:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
        );
    },

    /**
     * Verifica se l'operatore può definire, creare e modificare le definizioni dei ruoli applicativi
     * per i client (roles:manage). Riservato ad Admin e Super Admin.
     * @returns {boolean}
     */
    canManageRoles() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("roles:write", "roles:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    /**
     * Verifica se l'operatore può assegnare o revocare ruoli associati a uno specifico utente (users:roles).
     * Privilegio concesso ad Admin, Super Admin e Manager con delega IAM.
     * @returns {boolean}
     */
    canAssignUserRoles() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("users:roles", "roles:manage", "users:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
        );
    },

    canViewEmails() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("emails:read", "emails:write", "emails:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
        );
    },

    canManageEmails() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("emails:manage", "emails:write", "emails:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    canViewSettings() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("settings:read", "settings:write", "settings:manage") ||
            this.hasRole(
                "ADMIN", "ROLE_ADMIN",
                "MANAGER", "ROLE_MANAGER",
                "OPERATOR", "ROLE_OPERATOR",
                "AUDITOR", "ROLE_AUDITOR"
            )
        );
    },

    canEditSettings() {
        if (this.isSuperAdmin()) return true;
        return (
            this.hasPermission("settings:write", "settings:manage") ||
            this.hasRole("ADMIN", "ROLE_ADMIN")
        );
    },

    canManageBackup() {
        if (this.isSuperAdmin()) return true;
        return this.hasPermission("backup:manage");
    },

    applyUIPermissions(root = document) {
        if (!root) return;

        const canVClients = this.canViewClients();
        const canCClients = this.canCreateClient();
        const canMClients = this.canManageClients();
        const canVUsers = this.canViewUsers();
        const canMUsers = this.canManageUsers();
        const canDUsers = this.canDeleteUsers();
        const canVRoles = this.canViewRoles();
        const canMRoles = this.canManageRoles();
        const canAssignURoles = this.canAssignUserRoles();
        const canLUsers = this.canLockUsers();
        const canVEmails = this.canViewEmails();
        const canMEmails = this.canManageEmails();
        const canVSettings = this.canViewSettings();
        const canESettings = this.canEditSettings();
        const canMBackup = this.canManageBackup();

        // 1. Navigation Header Tabs, Dropdowns & Links (M3 Unified Navbar)
        toggleVisibility(root.querySelector("#nav-group-identity"), canVUsers || canVRoles || canMUsers);
        toggleVisibility(root.querySelector("#nav-sub-users"), canVUsers);
        toggleVisibility(root.querySelector("#nav-sub-new-user"), canMUsers);
        toggleVisibility(root.querySelector("#nav-sub-roles"), canVRoles);

        toggleVisibility(root.querySelector("#nav-group-apps"), canVClients || canCClients);
        toggleVisibility(root.querySelector("#nav-sub-clients"), canVClients);
        toggleVisibility(root.querySelector("#nav-sub-new-client"), canCClients);

        toggleVisibility(root.querySelector("#nav-group-system"), canVEmails || canVSettings);
        toggleVisibility(root.querySelector("#nav-sub-emails"), canVEmails);
        toggleVisibility(root.querySelector("#nav-sub-settings"), canVSettings);

        // Retrocompatibilità selettori storici
        toggleVisibility(root.querySelector("#nav-btn-clients"), canVClients);
        toggleVisibility(root.querySelector("#nav-btn-users"), canVUsers);
        toggleVisibility(root.querySelector("#nav-btn-roles"), canVRoles);
        toggleVisibility(root.querySelector("#nav-btn-emails"), canVEmails);
        toggleVisibility(root.querySelector("#nav-btn-settings"), canVSettings);

        root.querySelectorAll("nav a[href='/app/clients'], nav a[href*='/app/client'], a[href='/app/clients']").forEach((el) => {
            toggleVisibility(el, canVClients);
        });
        root.querySelectorAll("nav a[href='/app/users'], nav a[href*='/app/user'], a[href='/app/users']").forEach((el) => {
            toggleVisibility(el, canVUsers);
        });
        root.querySelectorAll("nav a[href='/app/role'], nav a[href='/app/roles'], nav a[href*='/role'], a[href='/app/role'], a[href='/app/roles'], #nav-btn-roles").forEach((el) => {
            toggleVisibility(el, canVRoles);
        });
        root.querySelectorAll("nav a[href='/app/emails'], nav a[href='/app/email'], nav a[href*='/email'], a[href='/app/emails'], #nav-btn-emails").forEach((el) => {
            toggleVisibility(el, canVEmails);
        });
        root.querySelectorAll("nav a[href='/app/settings'], nav a[href*='/settings'], a[href='/app/settings'], #nav-btn-settings").forEach((el) => {
            toggleVisibility(el, canVSettings);
        });

        // 2. Desktop Sidebar M3 Categories & Items
        toggleVisibility(root.querySelector("#sidebar-cat-clients"), canVClients);
        toggleVisibility(root.querySelector("#sidebar-cat-users"), canVUsers);
        toggleVisibility(root.querySelector("#sidebar-cat-roles"), canVRoles);
        toggleVisibility(root.querySelector("#sidebar-cat-emails"), canVEmails);
        toggleVisibility(root.querySelector("#sidebar-btn-settings"), canVSettings);

        // 3. Mobile Drawer Navigation & Bottom Navigation
        toggleVisibility(root.querySelector("#bottom-nav-clients"), canVClients);
        toggleVisibility(root.querySelector("#bottom-nav-users"), canVUsers);
        toggleVisibility(root.querySelector("#bottom-nav-roles"), canVRoles);
        toggleVisibility(root.querySelector("#bottom-nav-settings"), canVSettings);

        toggleVisibility(root.querySelector("#mobile-nav-clients"), canVClients);
        toggleVisibility(root.querySelector("#mobile-nav-users"), canVUsers);
        toggleVisibility(root.querySelector("#mobile-nav-roles"), canVRoles);
        toggleVisibility(root.querySelector("#mobile-nav-emails"), canVEmails);
        toggleVisibility(root.querySelector("#mobile-nav-settings"), canVSettings);

        toggleVisibility(root.querySelector("#mobile-drawer-cat-clients"), canVClients);
        toggleVisibility(root.querySelector("#mobile-drawer-cat-users"), canVUsers);
        toggleVisibility(root.querySelector("#mobile-drawer-cat-roles"), canVRoles);
        toggleVisibility(root.querySelector("#mobile-drawer-cat-emails"), canVEmails);
        toggleVisibility(root.querySelector("#mobile-drawer-cat-system"), canVSettings);

        toggleVisibility(root.querySelector("#mobile-tool-new-user"), canMUsers);
        toggleVisibility(root.querySelector("#mobile-tool-new-client"), canCClients);
        toggleVisibility(root.querySelector("#mobile-tool-new-role"), canMRoles);

        // 4. Dashboard Bento KPI & Hub Cards
        toggleVisibility(root.querySelector("#stat-recap-users-card, #kpi-card-users"), canVUsers);
        toggleVisibility(root.querySelector("#stat-recap-clients-card, #kpi-card-clients"), canVClients);
        toggleVisibility(root.querySelector("#hub-card-users"), canVUsers);
        toggleVisibility(root.querySelector("#hub-card-clients"), canVClients);
        toggleVisibility(root.querySelector("#quick-switch-clients"), canVClients);
        toggleVisibility(root.querySelector("#quick-switch-users"), canVUsers);

        // Sub-azioni nei KPI o Hub Card
        root.querySelectorAll("#hub-card-users a[href='/app/users/register']").forEach((el) => {
            toggleVisibility(el, canMUsers);
        });
        root.querySelectorAll("#hub-card-clients a[href='/app/clients/register']").forEach((el) => {
            toggleVisibility(el, canCClients);
        });

        // 5. In-View Create Action Buttons (pulsanti di creazione nelle sezioni e tabelle interne)
        root.querySelectorAll("a[href='/app/users/register'], a[href*='/users/register'], #btn-new-user").forEach((el) => {
            toggleVisibility(el, canMUsers);
        });
        root.querySelectorAll("a[href='/app/clients/register'], a[href*='/clients/register'], #btn-new-client, #btn-register-new-client").forEach((el) => {
            toggleVisibility(el, canCClients);
        });
        root.querySelectorAll("[onclick*='openRoleModal(\\'create\\')'], #btn-create-role, #btn-new-role, #btn-create-role-header, #btn-create-role-empty").forEach((el) => {
            toggleVisibility(el, canMRoles);
        });
        root.querySelectorAll("[onclick*='openCreateTemplateModal'], #btn-new-template, #btn-create-template-header").forEach((el) => {
            toggleVisibility(el, canMEmails);
        });

        // 7. Roles Dedicated Cards & Actions across Views
        toggleVisibility(root.querySelector("#user-detail-roles-card"), canVRoles);
        toggleVisibility(root.querySelector("#client-detail-roles-card"), canVRoles);
        toggleVisibility(root.querySelector("#sec-client-roles"), canVRoles);

        root.querySelectorAll(".m3-action-role, a[href*='/users/roles']").forEach((el) => {
            toggleVisibility(el, canVRoles && canAssignURoles);
        });
        root.querySelectorAll("[onclick*='openRoleModal(\\'edit\\']").forEach((el) => {
            toggleVisibility(el, canMRoles);
        });
        root.querySelectorAll("[onclick*='deleteRoleConfirm']").forEach((el) => {
            toggleVisibility(el, canMRoles);
        });

        // 8. User Modification, Lock/Unlock & Deletion Actions
        root.querySelectorAll("#users-table .m3-action-edit, a[href*='/users/edit'], a[href*='/app/users/edit'], #user-detail-edit-profile-btn, #user-detail-edit-attrs-btn").forEach((el) => {
            toggleVisibility(el, canMUsers);
        });
        root.querySelectorAll("#users-table .m3-action-delete, button[onclick*='deleteUser'], #user-detail-delete-btn").forEach((el) => {
            toggleVisibility(el, canDUsers);
        });
        root.querySelectorAll("[onclick*='toggleBlockUser']").forEach((el) => {
            toggleVisibility(el, canLUsers);
        });
        root.querySelectorAll("[onclick*='toggleMfaStatus'], [onclick*='finishMfaVerification'], [onclick*='deleteMfaMethod'], a[href*='/app/mfa/']").forEach((el) => {
            toggleVisibility(el, canMUsers);
        });
        root.querySelectorAll("[data-require-action='assign-roles']").forEach((el) => {
            toggleVisibility(el, canAssignURoles);
        });
        root.querySelectorAll("[data-require-action='lock-users']").forEach((el) => {
            toggleVisibility(el, canLUsers);
        });

        // 9. Client Modification & Deletion Actions
        root.querySelectorAll("#clients-table .m3-action-edit, a[href*='/clients/edit'], a[href*='/app/clients/edit'], #client-detail-edit-btn").forEach((el) => {
            toggleVisibility(el, canMClients);
        });
        root.querySelectorAll("#clients-table .m3-action-delete, button[onclick*='deleteClientAction'], #client-detail-delete-btn").forEach((el) => {
            toggleVisibility(el, canMClients);
        });

        // 10. Email Template Actions
        root.querySelectorAll("[onclick*='openEditTemplateModal'], [onclick*='deleteTemplateConfirm'], [onclick*='testSendEmail']").forEach((el) => {
            toggleVisibility(el, canMEmails);
        });

        // 11. Settings Specific Cards and Actions
        toggleVisibility(root.querySelector("#settings-card-backup"), canMBackup);
        root.querySelectorAll("[onclick*='saveAllSettings'], [onclick*='resetSettingsToDefault']").forEach((el) => {
            toggleVisibility(el, canESettings);
        });

        // 12. Attributi Dichiarativi HTML
        root.querySelectorAll("[data-require-role]").forEach((el) => {
            const reqRoles = el.getAttribute("data-require-role").split(",");
            toggleVisibility(el, this.hasRole(...reqRoles));
        });
        root.querySelectorAll("[data-require-perm]").forEach((el) => {
            const reqPerms = el.getAttribute("data-require-perm").split(",");
            toggleVisibility(el, this.hasPermission(...reqPerms));
        });
        root.querySelectorAll("[data-require-action]").forEach((el) => {
            const act = el.getAttribute("data-require-action");
            let allowed = true;
            if (act === "manage-clients") allowed = canMClients;
            else if (act === "manage-users") allowed = canMUsers;
            else if (act === "delete-users") allowed = canDUsers;
            else if (act === "manage-roles") allowed = canMRoles;
            else if (act === "manage-emails") allowed = canMEmails;
            else if (act === "manage-backup") allowed = canMBackup;
            else if (act === "edit-settings") allowed = canESettings;
            toggleVisibility(el, allowed);
        });
    }
};

/**
 * Inizializza i menu a discesa della navbar superiore (M3 Unified Navbar).
 * Supporta toggle al click, chiusura con click esterno, tasto ESC e auto-evidenziazione attiva.
 */
function initHeaderDropdowns() {
    window.closeAllNavDropdowns = function() {
        document.querySelectorAll(".nav-dropdown.is-open").forEach((dropdown) => {
            dropdown.classList.remove("is-open");
            const btn = dropdown.querySelector(".nav-dropdown-trigger");
            if (btn) btn.setAttribute("aria-expanded", "false");
        });
    };

    document.querySelectorAll(".nav-dropdown-trigger").forEach((trigger) => {
        trigger.addEventListener("click", function (e) {
            e.stopPropagation();
            const parent = this.closest(".nav-dropdown");
            if (!parent) return;
            const isOpen = parent.classList.contains("is-open");
            window.closeAllNavDropdowns();
            if (!isOpen) {
                parent.classList.add("is-open");
                this.setAttribute("aria-expanded", "true");
            }
        });
    });

    document.addEventListener("click", function (e) {
        if (!e.target.closest(".nav-dropdown")) {
            window.closeAllNavDropdowns();
        }
    });

    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape") {
            window.closeAllNavDropdowns();
        }
    });

    syncActiveNavState();
}

/**
 * Sincronizza lo stato attivo delle pillole e dei sottomenu in base alla pagina o vista corrente.
 * @param {string} [currentViewOverride] - Nome opzionale della vista SPA attiva (es. 'dashboard', 'users', 'clients', 'settings')
 */
function syncActiveNavState(currentViewOverride) {
    const path = window.location.pathname;

    document.querySelectorAll(".nav-tab-m3, .nav-dropdown-trigger, .nav-dropdown-item").forEach((el) => {
        el.classList.remove("active");
    });

    if (currentViewOverride) {
        if (currentViewOverride === "dashboard") {
            const el = document.getElementById("nav-btn-dashboard");
            if (el) el.classList.add("active");
        } else if (currentViewOverride === "users") {
            const trigger = document.querySelector("#nav-group-identity .nav-dropdown-trigger");
            if (trigger) trigger.classList.add("active");
            const item = document.getElementById("nav-sub-users");
            if (item) item.classList.add("active");
        } else if (currentViewOverride === "clients") {
            const trigger = document.querySelector("#nav-group-apps .nav-dropdown-trigger");
            if (trigger) trigger.classList.add("active");
            const item = document.getElementById("nav-sub-clients");
            if (item) item.classList.add("active");
        } else if (currentViewOverride === "settings") {
            const trigger = document.querySelector("#nav-group-system .nav-dropdown-trigger");
            if (trigger) trigger.classList.add("active");
            const item = document.getElementById("nav-sub-settings");
            if (item) item.classList.add("active");
        }
        return;
    }

    if (path === "/app" || path === "/app/dashboard" || path === "/app/home") {
        const el = document.getElementById("nav-btn-dashboard");
        if (el) el.classList.add("active");
    } else if (path.startsWith("/app/user") || path.startsWith("/app/role") || path.startsWith("/app/roles") || path.startsWith("/app/mfa")) {
        const trigger = document.querySelector("#nav-group-identity .nav-dropdown-trigger");
        if (trigger) trigger.classList.add("active");
        if (path === "/app/users" || path.startsWith("/app/users/details") || path.startsWith("/app/users/edit")) {
            const item = document.getElementById("nav-sub-users");
            if (item) item.classList.add("active");
        } else if (path === "/app/users/register") {
            const item = document.getElementById("nav-sub-new-user");
            if (item) item.classList.add("active");
        } else if (path.startsWith("/app/role") || path.startsWith("/app/roles")) {
            const item = document.getElementById("nav-sub-roles");
            if (item) item.classList.add("active");
        }
    } else if (path.startsWith("/app/client")) {
        const trigger = document.querySelector("#nav-group-apps .nav-dropdown-trigger");
        if (trigger) trigger.classList.add("active");
        if (path === "/app/clients" || path.startsWith("/app/clients/details") || path.startsWith("/app/clients/edit")) {
            const item = document.getElementById("nav-sub-clients");
            if (item) item.classList.add("active");
        } else if (path === "/app/clients/register") {
            const item = document.getElementById("nav-sub-new-client");
            if (item) item.classList.add("active");
        }
    } else if (path.startsWith("/app/email") || path.startsWith("/app/emails") || path.startsWith("/app/settings")) {
        const trigger = document.querySelector("#nav-group-system .nav-dropdown-trigger");
        if (trigger) trigger.classList.add("active");
        if (path.startsWith("/app/email") || path.startsWith("/app/emails")) {
            const item = document.getElementById("nav-sub-emails");
            if (item) item.classList.add("active");
        } else if (path.startsWith("/app/settings")) {
            const item = document.getElementById("nav-sub-settings");
            if (item) item.classList.add("active");
        }
    }
}

// Global exports
window.toggleVisibility = toggleVisibility;
window.AccessControl = AccessControl;
window.initHeaderDropdowns = initHeaderDropdowns;
window.syncActiveNavState = syncActiveNavState;

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initHeaderDropdowns();
        initMobileNavigation();
        AccessControl.applyUIPermissions();
    });
} else {
    initHeaderDropdowns();
    initMobileNavigation();
    AccessControl.applyUIPermissions();
}


