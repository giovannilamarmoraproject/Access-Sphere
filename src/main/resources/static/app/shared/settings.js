/**
 * Access Sphere - Unified Application Settings & Theme Management
 */

const AppSettings = (function () {
    const THEME_KEY = "access_sphere_theme";
    const SETTINGS_KEY = "access_sphere_settings";
    const DEFAULT_THEME = "cosmic-purple";

    /**
     * Normalizza l'oggetto impostazioni supportando le mappe categorizzate
     * (brandingSettings, loginSettings, policySettings) e le impostazioni base di sistema.
     * Rimuove proprietà piatte ridondanti e restituisce esclusivamente la struttura a mappe pulita.
     */
    function normalizeSettings(s) {
        if (!s || typeof s !== "object") return {};
        const branding = s.brandingSettings || {};
        const login = s.loginSettings || {};
        const policy = s.policySettings || {};

        // Estrazione valori con priorità alle mappe di categoria e fallback a eventuali campi legacy o valori predefiniti
        const appName = branding.appName !== undefined && branding.appName !== null && branding.appName !== ""
            ? branding.appName
            : (s.appName !== undefined && s.appName !== null && s.appName !== "" ? s.appName : (branding.app_name || "Access Sphere"));
        const appTagline = branding.appTagline !== undefined && branding.appTagline !== null
            ? branding.appTagline
            : (s.appTagline !== undefined && s.appTagline !== null ? s.appTagline : (branding.app_tagline || "Gestione Identità & Accessi"));
        const logoUrl = branding.logoUrl !== undefined && branding.logoUrl !== null
            ? branding.logoUrl
            : (s.logoUrl !== undefined && s.logoUrl !== null ? s.logoUrl : (branding.logo_url || "/img/logo-minimal.svg"));
        const faviconUrl = branding.faviconUrl !== undefined && branding.faviconUrl !== null
            ? branding.faviconUrl
            : (s.faviconUrl !== undefined && s.faviconUrl !== null ? s.faviconUrl : (branding.favicon_url || "/img/logo-minimal.svg"));
        const footerCopyright = branding.footerCopyright !== undefined && branding.footerCopyright !== null
            ? branding.footerCopyright
            : (s.footerCopyright !== undefined && s.footerCopyright !== null ? s.footerCopyright : (branding.footer_copyright || "© 2026 Access Sphere"));
        const supportEmail = branding.supportEmail !== undefined && branding.supportEmail !== null
            ? branding.supportEmail
            : (s.supportEmail !== undefined && s.supportEmail !== null ? s.supportEmail : (branding.support_email || "support@accesssphere.io"));

        const loginMode = login.loginMode !== undefined && login.loginMode !== null
            ? login.loginMode
            : (s.loginMode !== undefined && s.loginMode !== null ? s.loginMode : (login.login_mode || "SHOWCASE"));
        const loginBgUrl = login.loginBgUrl !== undefined && login.loginBgUrl !== null
            ? login.loginBgUrl
            : (s.loginBgUrl !== undefined && s.loginBgUrl !== null ? s.loginBgUrl : (login.login_bg_url || null));
        const loginBgOpacity = login.loginBgOpacity !== undefined && login.loginBgOpacity !== null
            ? login.loginBgOpacity
            : (s.loginBgOpacity !== undefined && s.loginBgOpacity !== null ? s.loginBgOpacity : (login.login_bg_opacity !== undefined ? login.login_bg_opacity : 50));
        const loginHeroTitle = login.loginHeroTitle !== undefined && login.loginHeroTitle !== null
            ? login.loginHeroTitle
            : (s.loginHeroTitle !== undefined && s.loginHeroTitle !== null ? s.loginHeroTitle : (login.login_hero_title || "Autenticazione Sicura & Modulare"));
        const loginHeroSubtitle = login.loginHeroSubtitle !== undefined && login.loginHeroSubtitle !== null
            ? login.loginHeroSubtitle
            : (s.loginHeroSubtitle !== undefined && s.loginHeroSubtitle !== null ? s.loginHeroSubtitle : (login.login_hero_subtitle || ""));
        const hideHomeButton = login.hideHomeButton !== undefined && login.hideHomeButton !== null
            ? login.hideHomeButton
            : (s.hideHomeButton !== undefined && s.hideHomeButton !== null ? s.hideHomeButton : (login.hide_home_button !== undefined ? login.hide_home_button : false));

        const privacyPolicyUrl = policy.privacyPolicyUrl !== undefined && policy.privacyPolicyUrl !== null
            ? policy.privacyPolicyUrl
            : (s.privacyPolicyUrl !== undefined && s.privacyPolicyUrl !== null ? s.privacyPolicyUrl : (policy.privacy_policy_url || "/privacy-policy"));
        const cookiePolicyUrl = policy.cookiePolicyUrl !== undefined && policy.cookiePolicyUrl !== null
            ? policy.cookiePolicyUrl
            : (s.cookiePolicyUrl !== undefined && s.cookiePolicyUrl !== null ? s.cookiePolicyUrl : (policy.cookie_policy_url || "/cookie-policy"));

        const activeTheme = s.activeTheme || localStorage.getItem(THEME_KEY) || DEFAULT_THEME;
        const defaultLanguage = s.defaultLanguage || "auto";
        const navigationLayout = s.navigationLayout || "HEADER";

        const normalized = {
            activeTheme,
            defaultLanguage,
            navigationLayout,
            brandingSettings: {
                appName,
                appTagline,
                logoUrl,
                faviconUrl,
                footerCopyright,
                supportEmail
            },
            loginSettings: {
                loginMode,
                loginBgUrl,
                loginBgOpacity,
                loginHeroTitle,
                loginHeroSubtitle,
                hideHomeButton
            },
            policySettings: {
                privacyPolicyUrl,
                cookiePolicyUrl
            }
        };

        if (s.id !== undefined && s.id !== null) {
            normalized.id = s.id;
        }

        return normalized;
    }

    // 1. Instant Synchronous Initialization (Zero flicker)
    function initEarlyTheme() {
        try {
            let savedTheme = localStorage.getItem(THEME_KEY);
            if (!savedTheme) {
                const cachedRaw = localStorage.getItem(SETTINGS_KEY);
                if (cachedRaw) {
                    try {
                        const raw = JSON.parse(cachedRaw);
                        const s = normalizeSettings(raw);
                        if (s && s.activeTheme) savedTheme = s.activeTheme;
                    } catch (err) {
                    }
                }
            }
            if (!savedTheme) savedTheme = DEFAULT_THEME;

            const currentTheme = document.documentElement.getAttribute("data-theme");
            if (currentTheme !== savedTheme) {
                document.documentElement.setAttribute("data-theme", savedTheme);
            }

            const cachedRaw = localStorage.getItem(SETTINGS_KEY);
            if (cachedRaw) {
                try {
                    const raw = JSON.parse(cachedRaw);
                    const s = normalizeSettings(raw);
                    const branding = (s && s.brandingSettings) || {};
                    const favicon = branding.faviconUrl || (s && s.faviconUrl);
                    if (s && favicon && document.head) {
                        let iconLink = document.querySelector("link[rel*='icon']");
                        if (!iconLink) {
                            iconLink = document.createElement("link");
                            iconLink.rel = "icon";
                            document.head.appendChild(iconLink);
                        }
                        iconLink.href = favicon;
                    }
                    if (s && s.navigationLayout === "SIDEBAR" && isPostLoginPage()) {
                        document.documentElement.classList.add("layout-sidebar");
                        if (localStorage.getItem("access_sphere_sidebar_collapsed") === "true") {
                            document.documentElement.classList.add("sidebar-is-collapsed");
                        }
                    }
                    if (document.readyState === "loading") {
                        document.addEventListener("DOMContentLoaded", () => applyBrandingToDOM(s));
                    } else {
                        applyBrandingToDOM(s);
                    }
                } catch (err) {
                }
            }
        } catch (e) {
            console.warn("Early theme init error:", e);
        }
    }

    // Execute immediately upon script load
    initEarlyTheme();

    function applyTheme(themeName) {
        if (!themeName) themeName = DEFAULT_THEME;
        const currentTheme = document.documentElement.getAttribute("data-theme");
        localStorage.setItem(THEME_KEY, themeName);
        if (currentTheme !== themeName) {
            document.documentElement.setAttribute("data-theme", themeName);
            window.dispatchEvent(new CustomEvent("themeChanged", {detail: {theme: themeName}}));
        }
    }

    function applyBrandingToDOM(settings) {
        if (!settings) return;
        settings = normalizeSettings(settings);
        const branding = settings.brandingSettings || {};
        const appName = branding.appName;
        const logoUrl = branding.logoUrl;
        const faviconUrl = branding.faviconUrl;
        const footerCopyright = branding.footerCopyright;

        // 1. App Name in Document Title and Brand Elements
        if (appName) {
            localStorage.setItem("access_sphere_app_name", appName);
            // Update any element with class app-brand-name
            document.querySelectorAll(".app-brand-name").forEach((el) => {
                el.textContent = appName;
            });
            const loginTitle = document.getElementById("login_page_title");
            if (loginTitle) {
                loginTitle.textContent = appName;
            }
            const loginTabTitle = document.getElementById("login_page_tab_title");
            if (loginTabTitle) {
                loginTabTitle.textContent = `${appName} - Login`;
            }
            const mobileDrawerName = document.getElementById("mobile-drawer-brand-name");
            if (mobileDrawerName) {
                mobileDrawerName.textContent = appName;
            }
        }

        // 2. Logo
        if (logoUrl) {
            localStorage.setItem("access_sphere_logo_url", logoUrl);
            // Aggiorna tutti i logo del brand evitando di sovrascrivere l'anteprima favicon
            document.querySelectorAll(".app-brand-logo, .login-logo-glow, .mobile-logo-badge img, .app-brand-icon").forEach((img) => {
                if (img.id !== "favicon-preview-img") {
                    img.src = logoUrl;
                }
            });
            const logoPreview = document.getElementById("logo-preview-img");
            if (logoPreview) {
                logoPreview.src = logoUrl;
            }
            const mobileDrawerLogo = document.getElementById("mobile-drawer-brand-logo");
            if (mobileDrawerLogo) {
                mobileDrawerLogo.src = logoUrl;
            }
        }

        // 3. Favicon (forza ricaricamento nel tab del browser ricreando il tag link)
        if (faviconUrl) {
            document.querySelectorAll("link[rel*='icon'], link[rel='apple-touch-icon']").forEach((el) => el.remove());
            const iconLink = document.createElement("link");
            iconLink.rel = "icon";
            iconLink.href = faviconUrl;
            document.head.appendChild(iconLink);

            const appleLink = document.createElement("link");
            appleLink.rel = "apple-touch-icon";
            appleLink.href = faviconUrl;
            document.head.appendChild(appleLink);

            const favPreview = document.getElementById("favicon-preview-img");
            if (favPreview) {
                favPreview.src = faviconUrl;
            }
        }

        // 4. Footer Copyright
        const currentYear = new Date().getFullYear();
        let copyrightText = null;
        if (footerCopyright && footerCopyright.trim()) {
            localStorage.setItem("access_sphere_footer_copyright", footerCopyright);
            copyrightText = footerCopyright.replace("#YEAR#", currentYear).replace(/202[0-9]/g, currentYear);
        } else if (appName && appName.trim()) {
            copyrightText = `&copy; ${currentYear} ${appName.trim()} - Tutti i diritti riservati`;
        }
        if (copyrightText) {
            document.querySelectorAll(".app-footer-copyright, .footer_copyright_text").forEach((el) => {
                el.innerHTML = copyrightText;
            });
        }

        // 5. Login Page Background / Showcase Mode & Home Button
        applyLoginCustomization(settings);

        // 6. Navigation Layout (Header vs Sidebar M3 Card)
        applyNavigationLayout(settings);
    }

    /**
     * Verifica se la pagina corrente è una pagina interna autenticata
     * (esclude landing page '/', '/index.html', '/cookie-policy', '/privacy-policy' e '/app/login')
     */
    function isPostLoginPage() {
        const p = window.location.pathname;
        return p.startsWith("/app") && !p.startsWith("/app/login");
    }

    /**
     * Alterna la modalità della sidebar tra espansa (260px con testo) e ridotta (76px solo icone)
     */
    function toggleSidebarCollapse() {
        const sidebar = document.getElementById("m3-app-sidebar");
        if (!sidebar) return;
        const isNowCollapsed = !sidebar.classList.contains("collapsed");
        if (isNowCollapsed) {
            sidebar.classList.add("collapsed");
            document.body.classList.add("sidebar-is-collapsed");
            document.documentElement.classList.add("sidebar-is-collapsed");
        } else {
            sidebar.classList.remove("collapsed");
            document.body.classList.remove("sidebar-is-collapsed");
            document.documentElement.classList.remove("sidebar-is-collapsed");
        }
        localStorage.setItem("access_sphere_sidebar_collapsed", isNowCollapsed ? "true" : "false");

        const icon = document.getElementById("sidebar-collapse-icon");
        const text = document.getElementById("sidebar-collapse-text");
        if (icon) icon.className = `fa-solid ${isNowCollapsed ? "fa-angles-right" : "fa-angles-left"}`;
        if (text) text.textContent = isNowCollapsed ? (typeof t === "function" ? t("sidebar_expand", "Espandi") : "Espandi") : (typeof t === "function" ? t("sidebar_collapse", "Riduci") : "Riduci");
    }

    /**
     * Genera e inietta la Sidebar a scheda (M3 Bento Card style)
     * con border-radius 26px, effetto vetro e gap minimo.
     * Contiene esclusivamente le opzioni di navigazione ("le opzioni solo sulla sidebar")
     * e in basso il pulsante per collassare/espandere la barra.
     * Il brand e i pulsanti d'azione (refresh, versione, nuovo utente/client, logout)
     * rimangono nel vecchio header in alto.
     */
    function mountM3Sidebar(settings) {
        if (!isPostLoginPage()) return;
        let sidebar = document.getElementById("m3-app-sidebar");
        const isCollapsed = localStorage.getItem("access_sphere_sidebar_collapsed") === "true";
        document.body.classList.add("layout-sidebar");
        if (isCollapsed) {
            document.body.classList.add("sidebar-is-collapsed");
        } else {
            document.body.classList.remove("sidebar-is-collapsed");
        }

        if (!sidebar) {
            sidebar = document.createElement("aside");
            sidebar.id = "m3-app-sidebar";
            sidebar.className = `m3-app-sidebar ${isCollapsed ? "collapsed" : ""}`;

            const currentPath = window.location.pathname;
            const isOverview = currentPath === "/app" || currentPath === "/app/" || currentPath === "/app/dashboard";
            const isUsers = currentPath.includes("/user") && !currentPath.includes("/clients");
            const isClients = currentPath.includes("/client");
            const isRoles = currentPath === "/app/role" || currentPath === "/app/roles" || currentPath.startsWith("/app/role");
            const isEmails = currentPath.includes("/email");
            const isSettings = currentPath.includes("/settings");
            const currentUser = typeof getLoggedInUser === "function" ? getLoggedInUser() : null;
            const esc = typeof escapeHtmlAttr === "function" ? escapeHtmlAttr : (s => s ? String(s).replace(/"/g, '&quot;') : '');
            const userFullName = currentUser && currentUser.fullName ? currentUser.fullName : "Utente";
            const userHandle = currentUser && currentUser.username ? `@${currentUser.username}` : (currentUser && currentUser.email ? currentUser.email : "");
            const userPhoto = currentUser && currentUser.profilePhoto ? currentUser.profilePhoto : 'https://bootdey.com/img/Content/avatar/avatar7.png';

            sidebar.innerHTML = `
        <!-- Scheda Profilo Utente Desktop (Icona, Nome e Cognome) -->
        <div class="sidebar-user-card" id="sidebar-user-card" title="${esc(userFullName)}${userHandle ? ` (${esc(userHandle)})` : ''}">
          <div class="sidebar-user-avatar" id="sidebar-user-avatar">
            <img src="${esc(userPhoto)}" alt="${esc(userFullName)}" class="w-full h-full object-cover rounded-full" onerror="this.onerror=null; this.src='https://bootdey.com/img/Content/avatar/avatar7.png';" />
          </div>
          <div class="sidebar-user-info">
            <span class="sidebar-user-name" id="sidebar-user-fullname">${esc(userFullName)}</span>
            <span class="sidebar-user-role" id="sidebar-user-username">${esc(userHandle)}</span>
          </div>
        </div>

        <!-- Contenitore Categorie di Navigazione -->
        <div class="sidebar-sections-container">

          <!-- Categoria: Panoramica -->
          <div class="sidebar-category" id="sidebar-cat-main">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_main">Panoramica</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app" id="sidebar-btn-dashboard" class="sidebar-nav-item ${isOverview ? 'active' : ''}" title="Panoramica" onclick="if(typeof switchDashboardView === 'function' && document.getElementById('view-dashboard')){switchDashboardView('dashboard'); return false;}">
                  <i class="fa-solid fa-gauge-high"></i>
                  <span class="sidebar-label" data-i18n="nav_overview">Panoramica</span>
                </a>
              </li>
            </ul>
          </div>

          <!-- Categoria: Identità & Utenti -->
          <div class="sidebar-category" id="sidebar-cat-users">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_identity">Identità & Utenti</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app/users" id="sidebar-btn-users" class="sidebar-nav-item ${isUsers ? 'active' : ''}" title="Utenti" onclick="if(typeof switchDashboardView === 'function' && document.getElementById('view-users')){switchDashboardView('users'); return false;}">
                  <i class="fa-solid fa-users"></i>
                  <span class="sidebar-label" data-i18n="nav_users">Utenti</span>
                </a>
              </li>
            </ul>
          </div>

          <!-- Categoria: Ruoli & Permessi -->
          <div class="sidebar-category" id="sidebar-cat-roles">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_roles">Ruoli & Permessi</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app/role" id="sidebar-btn-roles" class="sidebar-nav-item ${isRoles ? 'active' : ''}" title="Ruoli & Permessi">
                  <i class="fa-solid fa-shield-halved"></i>
                  <span class="sidebar-label" data-i18n="nav_roles">Ruoli & Permessi</span>
                </a>
              </li>
            </ul>
          </div>

          <!-- Categoria: Applicazioni & Client -->
          <div class="sidebar-category" id="sidebar-cat-clients">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_apps">Applicazioni & Client</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app/clients" id="sidebar-btn-clients" class="sidebar-nav-item ${isClients ? 'active' : ''}" title="Client OAuth2" onclick="if(typeof switchDashboardView === 'function' && document.getElementById('view-clients')){switchDashboardView('clients'); return false;}">
                  <i class="fa-solid fa-key"></i>
                  <span class="sidebar-label" data-i18n="nav_clients">Client OAuth2</span>
                </a>
              </li>
            </ul>
          </div>

          <!-- Categoria: Comunicazioni -->
          <div class="sidebar-category" id="sidebar-cat-emails">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_comms">Comunicazioni</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app/emails" id="sidebar-btn-emails" class="sidebar-nav-item ${isEmails ? 'active' : ''}" title="Template Email">
                  <i class="fa-solid fa-envelope"></i>
                  <span class="sidebar-label" data-i18n="nav_emails">Template Email</span>
                </a>
              </li>
            </ul>
          </div>

          <!-- Categoria: Sistema & Risorse -->
          <div class="sidebar-category" id="sidebar-cat-system">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_system">Sistema & Risorse</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app/settings" id="sidebar-btn-settings" class="sidebar-nav-item ${isSettings ? 'active' : ''}" title="Impostazioni" onclick="if(typeof switchDashboardView === 'function' && document.getElementById('view-settings')){switchDashboardView('settings'); return false;}">
                  <i class="fa-solid fa-gear"></i>
                  <span class="sidebar-label" data-i18n="nav_settings">Impostazioni</span>
                </a>
              </li>
              <li>
                <a href="/webjars/swagger-ui/index.html" target="_blank" class="sidebar-nav-item" title="API Docs">
                  <i class="fa-solid fa-book"></i>
                  <span class="sidebar-label" data-i18n="nav_docs">API Docs</span>
                </a>
              </li>
            </ul>
          </div>

        </div>
        <!-- Sezione Inferiore: Impostazioni & Pulsante Collassa / Espandi Sidebar -->
        <div class="sidebar-footer-box">
          <button type="button" id="sidebar-collapse-btn" onclick="AppSettings.toggleSidebarCollapse()" class="sidebar-collapse-btn" title="Riduci/Espandi barra">
            <i id="sidebar-collapse-icon" class="fa-solid ${isCollapsed ? 'fa-angles-right' : 'fa-angles-left'}"></i>
            <span class="sidebar-label text-[11px]" id="sidebar-collapse-text">${isCollapsed ? (typeof t === "function" ? t("sidebar_expand", "Espandi") : "Espandi") : (typeof t === "function" ? t("sidebar_collapse", "Riduci") : "Riduci")}</span>
          </button>
        </div>
      `;

            document.body.prepend(sidebar);
            if (typeof syncLoggedInUserProfile === "function") {
                syncLoggedInUserProfile();
            }
            if (typeof translateDOM === "function") {
                translateDOM(sidebar);
            }
            if (typeof AccessControl !== "undefined" && typeof AccessControl.applyUIPermissions === "function") {
                AccessControl.applyUIPermissions(sidebar);
            }
        }
    }

    /**
     * Determina il titolo della pagina o sezione corrente
     */
    function detectCurrentPageTitle() {
        const p = window.location.pathname;
        if (p.includes("/register")) return "Registrazione";
        if (p.includes("/edit")) return "Modifica Profilo";
        if (p.includes("/role")) return "Ruoli & Permessi";
        if (p.includes("/mfa")) return "Sicurezza MFA";
        if (p.includes("/details") || p.endsWith("/user") || p.endsWith("/client")) return "Dettaglio";
        if (p.includes("/emails") || p.includes("/email")) return "Template Email";
        if (p.includes("/client")) return "Client OAuth2";
        if (p.includes("/user")) return "Gestione Utenti";
        if (p.includes("/settings")) return "Impostazioni";
        return "Panoramica";
    }

    /**
     * Rimuove eventuale elemento breadcrumb nell'header:
     * il vecchio header rimane intatto con brand a sinistra e controlli a destra.
     */
    function updateHeaderBreadcrumb(pageTitle) {
        const existing = document.getElementById("header-breadcrumb-box");
        if (existing) existing.remove();
    }

    /**
     * Applica il layout di navigazione richiesto (HEADER vs SIDEBAR)
     */
    function applyNavigationLayout(settings) {
        if (!settings) return;
        const navLayout = settings.navigationLayout || "HEADER";
        if (navLayout === "SIDEBAR" && isPostLoginPage()) {
            document.documentElement.classList.add("layout-sidebar");
            if (document.body) document.body.classList.add("layout-sidebar");
            mountM3Sidebar(settings);
            updateHeaderBreadcrumb();
        } else {
            document.documentElement.classList.remove("layout-sidebar");
            document.documentElement.classList.remove("sidebar-is-collapsed");
            if (document.body) {
                document.body.classList.remove("layout-sidebar");
                document.body.classList.remove("sidebar-is-collapsed");
            }
            const existing = document.getElementById("m3-app-sidebar");
            if (existing) existing.remove();
            const breadcrumb = document.getElementById("header-breadcrumb-box");
            if (breadcrumb) breadcrumb.remove();
        }
    }

    function applyLoginCustomization(settings) {
        const leftPanel = document.querySelector(".left-background");
        const mobileBanner = document.querySelector(".page-banner");
        const showcaseContent = document.querySelector(".showcase-content-box");
        let overlay = document.getElementById("login-bg-overlay");

        if (!leftPanel && !mobileBanner) return;

        const login = (settings && settings.loginSettings) || {};
        const loginMode = login.loginMode || settings.loginMode || "SHOWCASE";
        const loginBgUrl = login.loginBgUrl !== undefined ? login.loginBgUrl : settings.loginBgUrl;
        const loginBgOpacity = login.loginBgOpacity !== undefined ? login.loginBgOpacity : (settings.loginBgOpacity !== undefined ? settings.loginBgOpacity : 50);
        const loginHeroTitle = login.loginHeroTitle !== undefined ? login.loginHeroTitle : settings.loginHeroTitle;
        const loginHeroSubtitle = login.loginHeroSubtitle !== undefined ? login.loginHeroSubtitle : settings.loginHeroSubtitle;
        const hideHomeButton = login.hideHomeButton !== undefined ? login.hideHomeButton : settings.hideHomeButton;

        if (loginMode === "CUSTOM_BACKGROUND" && loginBgUrl) {
            // 1. Custom Background Mode - Override any CSS gradient backgrounds
            if (leftPanel) {
                leftPanel.style.setProperty("background-image", `url("${loginBgUrl}")`, "important");
                leftPanel.style.setProperty("background-size", "cover", "important");
                leftPanel.style.setProperty("background-position", "center", "important");
                leftPanel.style.setProperty("background-repeat", "no-repeat", "important");
                leftPanel.style.position = "relative";

                // 2. Darkening overlay for contrast
                if (!overlay) {
                    overlay = document.createElement("div");
                    overlay.id = "login-bg-overlay";
                    overlay.style.position = "absolute";
                    overlay.style.inset = "0";
                    overlay.style.zIndex = "1";
                    overlay.style.pointerEvents = "none";
                    leftPanel.insertBefore(overlay, leftPanel.firstChild);
                }
                const opacity = loginBgOpacity / 100;
                overlay.style.backgroundColor = `rgba(14, 11, 22, ${opacity})`;

                // 3. Hide showcase content and ambient glow orbs completely so background is clean
                if (showcaseContent) {
                    showcaseContent.style.display = "none";
                }
                document.querySelectorAll(".ambient-glow-1, .ambient-glow-2").forEach(el => el.style.display = "none");
            }

            if (mobileBanner) {
                mobileBanner.style.setProperty("background-image", `url("${loginBgUrl}")`, "important");
                mobileBanner.style.setProperty("background-size", "cover", "important");
                mobileBanner.style.setProperty("background-position", "center", "important");
            }
        } else {
            // Default Showcase Mode
            if (leftPanel) {
                leftPanel.style.removeProperty("background-image");
                leftPanel.style.removeProperty("background-size");
                leftPanel.style.removeProperty("background-position");
                leftPanel.style.removeProperty("background-repeat");
                leftPanel.style.removeProperty("background");
                if (overlay) overlay.remove();
                if (showcaseContent) {
                    showcaseContent.style.display = "";
                    showcaseContent.style.opacity = "1";
                }
                document.querySelectorAll(".ambient-glow-1, .ambient-glow-2").forEach(el => el.style.display = "");
            }
            if (mobileBanner) {
                mobileBanner.style.removeProperty("background-image");
                mobileBanner.style.removeProperty("background-size");
                mobileBanner.style.removeProperty("background-position");
            }
        }

        // Custom Hero Title & Subtitle if provided
        if (loginHeroTitle) {
            const heroTitle = document.getElementById("login-hero-title");
            if (heroTitle) heroTitle.innerHTML = loginHeroTitle;
        }
        if (loginHeroSubtitle) {
            const heroDesc = document.getElementById("login-hero-desc");
            if (heroDesc) heroDesc.innerHTML = loginHeroSubtitle;
        }

        // Gestione visibilità pulsante "Torna alla Home"
        const homeBtns = document.querySelectorAll(".showcase-home-btn, .showcase-mobile-home-btn");
        homeBtns.forEach((btn) => {
            btn.style.display = hideHomeButton ? "none" : "";
        });
    }

    function getAuthToken() {
        if (typeof getCookieOrStorage === "function") {
            const cfg = typeof getConfig === "function" ? getConfig() : null;
            const key = cfg && cfg.access_token ? cfg.access_token : "access-token";
            const tok = getCookieOrStorage(key);
            if (tok) return tok;
        }
        if (typeof authConfig !== "undefined" && authConfig && authConfig.access_token_name) {
            const tok = localStorage.getItem(authConfig.access_token_name);
            if (tok) return tok;
        }
        const clientId = localStorage.getItem("Client-ID") || "ACCESS-SPHERE-TECH";
        return (
            localStorage.getItem(`${clientId}_access-token`) ||
            localStorage.getItem("ACCESS-SPHERE-TECH_access-token") ||
            localStorage.getItem("access-token") ||
            (typeof getCookie === "function" ? getCookie("access-token") : null) ||
            sessionStorage.getItem("access-token") ||
            ""
        );
    }

    async function loadPublicSettings() {
        try {
            const res = await fetch("/v1/app/settings");
            if (!res.ok) return null;
            const data = await res.json().catch(() => ({}));
            if (data && data.data) {
                const s = normalizeSettings(data.data);
                localStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
                const activeTheme = s.activeTheme || localStorage.getItem(THEME_KEY);
                if (activeTheme) {
                    applyTheme(activeTheme);
                }
                const savedLang = localStorage.getItem("access_sphere_language") || localStorage.getItem("app_language");
                if (s.defaultLanguage && s.defaultLanguage !== "auto" && !savedLang) {
                    if (typeof switchLanguage === "function") {
                        switchLanguage(s.defaultLanguage);
                    }
                }
                applyBrandingToDOM(s);
                return s;
            }
            return null;
        } catch (e) {
            console.warn("Could not load public settings:", e);
            return null;
        }
    }

    async function loadAdminSettings(token) {
        if (!token) {
            token = getAuthToken();
        }
        const headers = {
            Accept: "application/json",
        };
        if (token) {
            const cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            if (cleanToken) {
                headers["Authorization"] = "Bearer " + cleanToken;
            }
        }
        if (typeof getSavedHeaders === "function") {
            Object.assign(headers, getSavedHeaders());
        }
        try {
            const res = await fetch("/v1/app/settings", {
                headers: headers,
            });
            if (!res.ok) {
                console.warn("Could not load admin settings:", res.status);
                return null;
            }
            const json = await res.json().catch(() => ({}));
            return json.data ? normalizeSettings(json.data) : null;
        } catch (err) {
            console.warn("Network error loading admin settings:", err);
            return null;
        }
    }

    async function saveAdminSettings(settingsData, token) {
        if (!token) {
            token = getAuthToken();
        }
        const normalizedPayload = normalizeSettings(settingsData);
        const headers = {
            "Content-Type": "application/json",
        };
        if (token) {
            const cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            if (cleanToken) {
                headers["Authorization"] = "Bearer " + cleanToken;
            }
        }
        if (typeof getSavedHeaders === "function") {
            Object.assign(headers, getSavedHeaders());
        }
        console.log("📤 Sending AppSettings PUT request to /v1/app/settings...", normalizedPayload);
        const res = await fetch("/v1/app/settings", {
            method: "PUT",
            headers: headers,
            body: JSON.stringify(normalizedPayload),
        });
        const json = await res.json().catch(() => ({}));
        if (!res.ok) {
            console.error("❌ Save settings failed:", res.status, json);
            throw new Error((json && (json.message || json.exception)) || `HTTP ${res.status}`);
        }
        console.log("📥 AppSettings saved successfully response:", json);
        if (json.data) {
            const s = normalizeSettings(json.data);
            localStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
            if (s.activeTheme) {
                applyTheme(s.activeTheme);
            }
            if (s.defaultLanguage && s.defaultLanguage !== "auto") {
                if (typeof switchLanguage === "function") {
                    switchLanguage(s.defaultLanguage);
                }
            }
            applyBrandingToDOM(s);
            return s;
        }
        return json;
    }

    async function exportBackup(token) {
        if (!token) {
            token = getAuthToken();
        }
        const headers = {
            Accept: "application/json",
        };
        if (token) {
            const cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            if (cleanToken) {
                headers["Authorization"] = "Bearer " + cleanToken;
            }
        }
        if (typeof getSavedHeaders === "function") {
            Object.assign(headers, getSavedHeaders());
        }
        const res = await fetch("/v1/app/settings/backup", {
            headers: headers,
        });
        const json = await res.json().catch(() => ({}));
        if (!res.ok) {
            throw new Error(json.message || "Export error");
        }
        const backupData = json.data;
        const blob = new Blob([JSON.stringify(backupData, null, 2)], {type: "application/json"});
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        const dateStr = new Date().toISOString().slice(0, 10);
        a.href = url;
        a.download = `access-sphere-backup-${dateStr}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        return backupData;
    }

    async function restoreBackup(backupJsonData, token) {
        if (!token) {
            token = getAuthToken();
        }
        const headers = {
            "Content-Type": "application/json",
        };
        if (token) {
            const cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            if (cleanToken) {
                headers["Authorization"] = "Bearer " + cleanToken;
            }
        }
        if (typeof getSavedHeaders === "function") {
            Object.assign(headers, getSavedHeaders());
        }
        const res = await fetch("/v1/app/settings/restore", {
            method: "POST",
            headers: headers,
            body: JSON.stringify(backupJsonData),
        });
        const json = await res.json().catch(() => ({}));
        if (!res.ok) {
            throw new Error(json.message || "Restore error");
        }
        // Refresh public settings after restore
        await loadPublicSettings();
        return json;
    }

    function fileToBase64(file, options = {}) {
        return new Promise((resolve, reject) => {
            if (!file) return resolve("");

            // Se non è un'immagine o è un formato vettoriale SVG, leggiamo direttamente come Data URL
            if (!file.type || !file.type.startsWith("image/") || file.type === "image/svg+xml") {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result);
                reader.onerror = (err) => reject(err);
                reader.readAsDataURL(file);
                return;
            }

            // Dimensioni massime e qualità per prevenire payload eccessivi
            const maxWidth = options.maxWidth || 1200;
            const maxHeight = options.maxHeight || 1200;
            const quality = options.quality !== undefined ? options.quality : 0.88;

            const reader = new FileReader();
            reader.onerror = (err) => reject(err);
            reader.onload = (e) => {
                const img = new Image();
                img.onerror = () => resolve(e.target.result);
                img.onload = () => {
                    try {
                        let {width, height} = img;

                        // Se l'immagine è già di dimensioni contenute e pesa meno di 350KB, manteniamo i byte originali
                        if (width <= maxWidth && height <= maxHeight && file.size < 350 * 1024) {
                            return resolve(e.target.result);
                        }

                        if (width > maxWidth || height > maxHeight) {
                            const ratio = Math.min(maxWidth / width, maxHeight / height);
                            width = Math.round(width * ratio);
                            height = Math.round(height * ratio);
                        }

                        const canvas = document.createElement("canvas");
                        canvas.width = width;
                        canvas.height = height;
                        const ctx = canvas.getContext("2d");
                        ctx.drawImage(img, 0, 0, width, height);

                        // Manteniamo PNG se ha trasparenza o è leggero, altrimenti JPEG compresso
                        const outputType = (file.type === "image/png" && file.size < 800 * 1024) ? "image/png" : "image/jpeg";
                        const dataUrl = canvas.toDataURL(outputType, quality);
                        resolve(dataUrl);
                    } catch (canvasErr) {
                        console.warn("Canvas image optimization fallback:", canvasErr);
                        resolve(e.target.result);
                    }
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        });
    }

    return {
        applyTheme,
        applyBrandingToDOM,
        applyNavigationLayout,
        mountM3Sidebar,
        toggleSidebarCollapse,
        updateHeaderBreadcrumb,
        isPostLoginPage,
        normalizeSettings,
        loadPublicSettings,
        loadAdminSettings,
        saveAdminSettings,
        exportBackup,
        restoreBackup,
        fileToBase64,
    };
})();

// Auto-fetch public settings when page finishes loading
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => AppSettings.loadPublicSettings());
} else {
    AppSettings.loadPublicSettings();
}
