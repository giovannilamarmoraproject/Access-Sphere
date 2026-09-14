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

    // 1. App Name in Document Title
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
    }

    // 2. Logo
    if (settings.logoUrl) {
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
