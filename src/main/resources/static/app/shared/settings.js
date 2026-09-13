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
        const s = JSON.parse(cachedRaw);
        applyBrandingToDOM(s);
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
    }

    // 2. Logo
    if (settings.logoUrl) {
      document.querySelectorAll(".app-brand-logo").forEach((img) => {
        img.src = settings.logoUrl;
      });
    }

    // 3. Favicon
    if (settings.faviconUrl) {
      let iconLink = document.querySelector("link[rel~='icon']");
      if (!iconLink) {
        iconLink = document.createElement("link");
        iconLink.rel = "icon";
        document.head.appendChild(iconLink);
      }
      iconLink.href = settings.faviconUrl;
    }

    // 4. Footer Copyright
    if (settings.footerCopyright) {
      document.querySelectorAll(".app-footer-copyright").forEach((el) => {
        el.innerHTML = settings.footerCopyright;
      });
    }

    // 5. Login Page Background / Showcase Mode
    applyLoginCustomization(settings);
  }

  function applyLoginCustomization(settings) {
    const leftPanel = document.querySelector(".left-background");
    if (!leftPanel) return;

    const showcaseContent = document.querySelector(".showcase-content-box");

    if (settings.loginMode === "CUSTOM_BACKGROUND" && settings.loginBgUrl) {
      // Custom Background Mode
      leftPanel.style.backgroundImage = `url('${settings.loginBgUrl}')`;
      leftPanel.style.backgroundSize = "cover";
      leftPanel.style.backgroundPosition = "center";
      leftPanel.style.position = "relative";

      // Apply darkening overlay
      let overlay = document.getElementById("login-bg-overlay");
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

      // Hide showcase text or keep subtle branding
      if (showcaseContent) {
        showcaseContent.style.opacity = "0.9";
        showcaseContent.style.position = "relative";
        showcaseContent.style.zIndex = "2";
      }
    } else {
      // Default Showcase Mode
      leftPanel.style.backgroundImage = "";
      const overlay = document.getElementById("login-bg-overlay");
      if (overlay) overlay.remove();
      if (showcaseContent) {
        showcaseContent.style.opacity = "1";
        showcaseContent.style.display = "";
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
  }

  async function loadPublicSettings() {
    try {
      const res = await fetch("/v1/app/settings/public");
      if (!res.ok) return;
      const data = await res.json();
      if (data && data.data) {
        localStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
        const currentLocal = localStorage.getItem(THEME_KEY);
        if (!currentLocal && s.activeTheme) {
          applyTheme(s.activeTheme);
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
      token = localStorage.getItem("access-token") || localStorage.getItem("ACCESS-SPHERE-TECH_access-token");
    }
    const res = await fetch("/v1/app/settings", {
      headers: {
        Authorization: "Bearer " + token,
        Accept: "application/json",
      },
    });
    const json = await res.json();
    return json.data;
  }

  async function saveAdminSettings(settingsData, token) {
    if (!token) {
      token = localStorage.getItem("access-token") || localStorage.getItem("ACCESS-SPHERE-TECH_access-token");
    }
    const res = await fetch("/v1/app/settings", {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + token,
      },
      body: JSON.stringify(settingsData),
    });
    const json = await res.json();
    if (res.ok && json.data) {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(json.data));
      if (json.data.activeTheme) {
        applyTheme(json.data.activeTheme);
      }
      applyBrandingToDOM(json.data);
    }
    return json;
  }

  async function exportBackup(token) {
    if (!token) {
      token = localStorage.getItem("access-token") || localStorage.getItem("ACCESS-SPHERE-TECH_access-token");
    }
    const res = await fetch("/v1/app/settings/backup", {
      headers: {
        Authorization: "Bearer " + token,
        Accept: "application/json",
      },
    });
    const json = await res.json();
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
      token = localStorage.getItem("access-token") || localStorage.getItem("ACCESS-SPHERE-TECH_access-token");
    }
    const res = await fetch("/v1/app/settings/restore", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + token,
      },
      body: JSON.stringify(backupJsonData),
    });
    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || "Restore error");
    }
    // Refresh public settings after restore
    await loadPublicSettings();
    return json;
  }

  function fileToBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = (error) => reject(error);
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
