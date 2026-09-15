/**
 * Access Sphere - Unified Application Settings & Theme Management
 */

const AppSettings = (function () {
  const THEME_KEY = "access_sphere_theme";
  const SETTINGS_KEY = "access_sphere_settings";
  const DEFAULT_THEME = "cosmic-purple";

  // 1. Instant Synchronous Initialization (Zero flicker)
  function initEarlyTheme() {
    try {
      let savedTheme = localStorage.getItem(THEME_KEY);
      if (!savedTheme) {
        const cachedRaw = localStorage.getItem(SETTINGS_KEY);
        if (cachedRaw) {
          try {
            const s = JSON.parse(cachedRaw);
            if (s && s.activeTheme) savedTheme = s.activeTheme;
          } catch (err) {}
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
          const s = JSON.parse(cachedRaw);
          if (s && s.faviconUrl && document.head) {
            let iconLink = document.querySelector("link[rel*='icon']");
            if (!iconLink) {
              iconLink = document.createElement("link");
              iconLink.rel = "icon";
              document.head.appendChild(iconLink);
            }
            iconLink.href = s.faviconUrl;
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
        } catch (err) {}
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
      window.dispatchEvent(new CustomEvent("themeChanged", { detail: { theme: themeName } }));
    }
  }

  function applyBrandingToDOM(settings) {
    if (!settings) return;

    // 1. App Name in Document Title and Brand Elements
    if (settings.appName) {
      localStorage.setItem("access_sphere_app_name", settings.appName);
      // Update any element with class app-brand-name
      document.querySelectorAll(".app-brand-name").forEach((el) => {
        el.textContent = settings.appName;
      });
      const loginTitle = document.getElementById("login_page_title");
      if (loginTitle) {
        loginTitle.textContent = settings.appName;
      }
      const loginTabTitle = document.getElementById("login_page_tab_title");
      if (loginTabTitle) {
        loginTabTitle.textContent = `${settings.appName} - Login`;
      }
      const mobileDrawerName = document.getElementById("mobile-drawer-brand-name");
      if (mobileDrawerName) {
        mobileDrawerName.textContent = settings.appName;
      }
    }

    // 2. Logo
    if (settings.logoUrl) {
      localStorage.setItem("access_sphere_logo_url", settings.logoUrl);
      // Aggiorna tutti i logo del brand evitando di sovrascrivere l'anteprima favicon
      document.querySelectorAll(".app-brand-logo, .login-logo-glow, .mobile-logo-badge img, .app-brand-icon").forEach((img) => {
        if (img.id !== "favicon-preview-img") {
          img.src = settings.logoUrl;
        }
      });
      const logoPreview = document.getElementById("logo-preview-img");
      if (logoPreview) {
        logoPreview.src = settings.logoUrl;
      }
      const mobileDrawerLogo = document.getElementById("mobile-drawer-brand-logo");
      if (mobileDrawerLogo) {
        mobileDrawerLogo.src = settings.logoUrl;
      }
    }

    // 3. Favicon (forza ricaricamento nel tab del browser ricreando il tag link)
    if (settings.faviconUrl) {
      document.querySelectorAll("link[rel*='icon'], link[rel='apple-touch-icon']").forEach((el) => el.remove());
      const iconLink = document.createElement("link");
      iconLink.rel = "icon";
      iconLink.href = settings.faviconUrl;
      document.head.appendChild(iconLink);

      const appleLink = document.createElement("link");
      appleLink.rel = "apple-touch-icon";
      appleLink.href = settings.faviconUrl;
      document.head.appendChild(appleLink);

      const favPreview = document.getElementById("favicon-preview-img");
      if (favPreview) {
        favPreview.src = settings.faviconUrl;
      }
    }

    // 4. Footer Copyright
    if (settings.footerCopyright) {
      document.querySelectorAll(".app-footer-copyright, .footer_copyright_text").forEach((el) => {
        el.innerHTML = settings.footerCopyright;
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
      const isSettings = currentPath.includes("/settings");

      sidebar.innerHTML = `
        <!-- Contenitore Categorie di Navigazione -->
        <div class="sidebar-sections-container">

          <!-- Categoria: Principale -->
          <div class="sidebar-category">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_main">Principale</span>
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

          <!-- Categoria: Identità & Accessi (IAM) -->
          <div class="sidebar-category">
            <div class="sidebar-category-header">
              <span class="sidebar-category-label" data-i18n="sidebar_cat_iam">Identità & Accessi</span>
            </div>
            <div class="sidebar-category-divider"></div>
            <ul class="sidebar-nav-list">
              <li>
                <a href="/app/users" id="sidebar-btn-users" class="sidebar-nav-item ${isUsers ? 'active' : ''}" title="Utenti" onclick="if(typeof switchDashboardView === 'function' && document.getElementById('view-users')){switchDashboardView('users'); return false;}">
                  <i class="fa-solid fa-users"></i>
                  <span class="sidebar-label" data-i18n="nav_users">Utenti</span>
                </a>
              </li>
              <li>
                <a href="/app/clients" id="sidebar-btn-clients" class="sidebar-nav-item ${isClients ? 'active' : ''}" title="Client OAuth2" onclick="if(typeof switchDashboardView === 'function' && document.getElementById('view-clients')){switchDashboardView('clients'); return false;}">
                  <i class="fa-solid fa-key"></i>
                  <span class="sidebar-label" data-i18n="nav_clients">Client OAuth2</span>
                </a>
              </li>
            </ul>
          </div>

          <!-- Categoria: Sistema & Risorse -->
          <div class="sidebar-category">
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
                <a href="https://github.com/giovannilamarmora/Access-Sphere" target="_blank" class="sidebar-nav-item" title="API Docs">
                  <i class="fa-solid fa-book"></i>
                  <span class="sidebar-label" data-i18n="nav_docs">API Docs</span>
                </a>
              </li>
            </ul>
          </div>

        </div>

        <!-- Sezione Inferiore: Pulsante Collassa / Espandi Sidebar -->
        <div class="sidebar-footer-box">
          <button type="button" id="sidebar-collapse-btn" onclick="AppSettings.toggleSidebarCollapse()" class="sidebar-collapse-btn" title="Riduci/Espandi barra">
            <i id="sidebar-collapse-icon" class="fa-solid ${isCollapsed ? 'fa-angles-right' : 'fa-angles-left'}"></i>
            <span class="sidebar-label text-[11px]" id="sidebar-collapse-text">${isCollapsed ? (typeof t === "function" ? t("sidebar_expand", "Espandi") : "Espandi") : (typeof t === "function" ? t("sidebar_collapse", "Riduci") : "Riduci")}</span>
          </button>
        </div>
      `;

      document.body.prepend(sidebar);
      if (typeof translateDOM === "function") {
        translateDOM(sidebar);
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
    if (p.includes("/roles")) return "Ruoli & Permessi";
    if (p.includes("/mfa")) return "Sicurezza MFA";
    if (p.includes("/details") || p.endsWith("/user") || p.endsWith("/client")) return "Dettaglio";
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

    if (settings.loginMode === "CUSTOM_BACKGROUND" && settings.loginBgUrl) {
      // 1. Custom Background Mode - Override any CSS gradient backgrounds
      if (leftPanel) {
        leftPanel.style.setProperty("background-image", `url("${settings.loginBgUrl}")`, "important");
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
        const opacity = (settings.loginBgOpacity !== undefined ? settings.loginBgOpacity : 50) / 100;
        overlay.style.backgroundColor = `rgba(14, 11, 22, ${opacity})`;

        // 3. Hide showcase content and ambient glow orbs completely so background is clean
        if (showcaseContent) {
          showcaseContent.style.display = "none";
        }
        document.querySelectorAll(".ambient-glow-1, .ambient-glow-2").forEach(el => el.style.display = "none");
      }

      if (mobileBanner) {
        mobileBanner.style.setProperty("background-image", `url("${settings.loginBgUrl}")`, "important");
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
    if (settings.loginHeroTitle) {
      const heroTitle = document.getElementById("login-hero-title");
      if (heroTitle) heroTitle.innerHTML = settings.loginHeroTitle;
    }
    if (settings.loginHeroSubtitle) {
      const heroDesc = document.getElementById("login-hero-desc");
      if (heroDesc) heroDesc.innerHTML = settings.loginHeroSubtitle;
    }

    // Gestione visibilità pulsante "Torna alla Home"
    const homeBtns = document.querySelectorAll(".showcase-home-btn, .showcase-mobile-home-btn");
    homeBtns.forEach((btn) => {
      btn.style.display = settings.hideHomeButton ? "none" : "";
    });
  }

  function getAuthToken() {
    if (typeof getCookieOrStorage === "function") {
      const cfg = typeof getConfig === "function" ? getConfig() : null;
      const key = cfg && cfg.access_token ? cfg.access_token : "access-token";
      const tok = getCookieOrStorage(key);
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
      const res = await fetch("/v1/app/settings/public");
      if (!res.ok) return;
      const data = await res.json();
      if (data && data.data) {
        const s = data.data;
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
    } catch (e) {
      console.warn("Could not load public settings:", e);
    }
  }

  async function loadAdminSettings(token) {
    if (!token) {
      token = getAuthToken();
    }
    const headers = {
      Authorization: "Bearer " + token,
      Accept: "application/json",
    };
    if (typeof getSavedHeaders === "function") {
      Object.assign(headers, getSavedHeaders());
    }
    const res = await fetch("/v1/app/settings", {
      headers: headers,
    });
    if (!res.ok) {
      console.warn("Could not load admin settings:", res.status);
      return null;
    }
    const json = await res.json().catch(() => ({}));
    return json.data;
  }

  async function saveAdminSettings(settingsData, token) {
    if (!token) {
      token = getAuthToken();
    }
    const headers = {
      "Content-Type": "application/json",
      Authorization: "Bearer " + token,
    };
    if (typeof getSavedHeaders === "function") {
      Object.assign(headers, getSavedHeaders());
    }
    const res = await fetch("/v1/app/settings", {
      method: "PUT",
      headers: headers,
      body: JSON.stringify(settingsData),
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) {
      console.error("Save settings failed:", res.status, json);
      throw new Error((json && (json.message || json.exception)) || `HTTP ${res.status}`);
    }
    if (json.data) {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(json.data));
      if (json.data.activeTheme) {
        applyTheme(json.data.activeTheme);
      }
      if (json.data.defaultLanguage && json.data.defaultLanguage !== "auto") {
        if (typeof switchLanguage === "function") {
          switchLanguage(json.data.defaultLanguage);
        }
      }
      applyBrandingToDOM(json.data);
    }
    return json;
  }

  async function exportBackup(token) {
    if (!token) {
      token = getAuthToken();
    }
    const headers = {
      Authorization: "Bearer " + token,
      Accept: "application/json",
    };
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
    const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: "application/json" });
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
      Authorization: "Bearer " + token,
    };
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
            let { width, height } = img;

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
