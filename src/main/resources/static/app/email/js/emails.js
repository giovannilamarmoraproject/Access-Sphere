/**
 * Access Sphere - Email Templates Controller & Live Preview Engine
 * 
 * Questo modulo gestisce il ciclo di vita completo dei template email:
 * - Recupero, filtro e visualizzazione dei template salvati a database
 * - Editor interattivo con Live Preview reattivo in tempo reale (Desktop e Mobile)
 * - Riconoscimento ed estrazione automatica dei parametri segnaposto {{KEY}} dall'HTML
 * - Collaudo diretto con invio email di test tramite microservizio
 * - Fallback multilingua e integrazione diretta con il flusso di reset password
 */

let cachedTemplates = [];
let currentFilterLocale = "ALL";
let currentSearchQuery = "";
let currentPreviewDevice = "desktop";
let currentDetailTemplate = null;
let currentDetailDevice = "desktop";
let editorDebounceTimer = null;

// =========================================================================
// 1. Inizializzazione e Caricamento Dati
// =========================================================================

document.addEventListener("DOMContentLoaded", () => {
  initClock();
  checkAuthAndLoad();
});

function initClock() {
  const clockEl = document.getElementById("live-clock");
  if (!clockEl) return;
  const update = () => {
    const now = new Date();
    clockEl.textContent = now.toLocaleTimeString();
  };
  update();
  setInterval(update, 1000);
}

function checkAuthAndLoad() {
  const cfg = typeof getConfig === "function" ? getConfig() : null;
  const tokenKey = cfg ? cfg.access_token : "access-token";
  const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);

  if (!token) {
    console.warn("Nessun token di accesso trovato. Reindirizzamento al login...");
    const loginUrl = cfg && cfg.login_url ? cfg.login_url : "/app/login";
    window.location.href = loginUrl;
    return;
  }

  loadEmailTemplates();
}

/**
 * Carica l'elenco dei template email dal backend REST: GET /v1/email-templates
 * Si interconnette con EmailTemplateController e aggiorna le metriche KPI della Bento Grid.
 */
function loadEmailTemplates() {
  const refreshIcon = document.getElementById("refresh-icon");
  if (refreshIcon) refreshIcon.classList.add("fa-spin");

  const cfg = typeof getConfig === "function" ? getConfig() : null;
  const tokenKey = cfg ? cfg.access_token : "access-token";
  const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);
  const url = cfg && cfg.email_templates_url ? cfg.email_templates_url : (window.location.origin + "/v1/email-templates");

  GET(url, token)
    .then(async (res) => {
      const data = await res.json();
      if (data.error != null) {
        if (typeof isUnauthorizedError === "function" && isUnauthorizedError(data.error)) {
          if (typeof logout === "function") logout();
          return;
        }
        sweetalert("error", "Errore Caricamento", data.error.message || "Impossibile recuperare i template email");
      } else {
        cachedTemplates = data.data || [];
        updateKpiMetrics(cachedTemplates);
        renderTemplatesList(cachedTemplates);
      }
    })
    .catch((err) => {
      console.error("Errore durante il fetch dei template email:", err);
      sweetalert("error", "Errore di Rete", "Impossibile contattare il server per i template email.");
    })
    .finally(() => {
      if (refreshIcon) setTimeout(() => refreshIcon.classList.remove("fa-spin"), 400);
    });
}

/**
 * Aggiorna i contatori KPI della Bento Grid
 */
function updateKpiMetrics(templates) {
  // 1. Template Totali
  const totalEl = document.getElementById("stat-total-templates");
  if (totalEl) totalEl.textContent = templates.length;

  // 2. Lingue distinte attive
  const locales = new Set();
  let totalParamsCount = 0;
  let hasResetPwd = false;

  templates.forEach((t) => {
    if (t.locale) locales.add(t.locale);
    if (t.params && typeof t.params === "object") {
      totalParamsCount += Object.keys(t.params).length;
    }
    if (t.identifier === "ACCESS_SPHERE_CODE_RESET_PASSWORD") {
      hasResetPwd = true;
    }
  });

  const localesEl = document.getElementById("stat-total-locales");
  if (localesEl) localesEl.textContent = locales.size > 0 ? locales.size : "Globale";

  // 3. Parametri totali censiti
  const paramsEl = document.getElementById("stat-total-params");
  if (paramsEl) paramsEl.textContent = totalParamsCount;

  // 4. Stato Reset Password
  const resetStatusEl = document.getElementById("stat-reset-pwd-status");
  if (resetStatusEl) {
    if (hasResetPwd) {
      resetStatusEl.innerHTML = `<i class="fa-solid fa-circle-check text-emerald-400"></i> Attivo nel DB`;
    } else {
      resetStatusEl.innerHTML = `<i class="fa-solid fa-triangle-exclamation text-amber-400"></i> Non Configurato`;
    }
  }
}

// =========================================================================
// 2. Rendering Elenco Template & Filtri
// =========================================================================

function handleSearch(query) {
  currentSearchQuery = (query || "").trim().toLowerCase();
  renderTemplatesList(cachedTemplates);
}

function handleLocaleFilter(locale) {
  currentFilterLocale = locale;
  renderTemplatesList(cachedTemplates);
}

function getFilteredTemplates(templates) {
  return templates.filter((t) => {
    // Filtro per locale
    if (currentFilterLocale !== "ALL") {
      if (currentFilterLocale === "GENERIC") {
        if (t.locale && t.locale.trim() !== "") return false;
      } else {
        if (!t.locale || !t.locale.toLowerCase().startsWith(currentFilterLocale.toLowerCase())) {
          return false;
        }
      }
    }

    // Filtro per testo di ricerca
    if (currentSearchQuery) {
      const idMatch = (t.identifier || "").toLowerCase().includes(currentSearchQuery);
      const nameMatch = (t.name || "").toLowerCase().includes(currentSearchQuery);
      const subjMatch = (t.subject || "").toLowerCase().includes(currentSearchQuery);
      const descMatch = (t.description || "").toLowerCase().includes(currentSearchQuery);
      return idMatch || nameMatch || subjMatch || descMatch;
    }

    return true;
  });
}

/**
 * Renderizza l'elenco dei template sotto forma di schede M3 bento-styled interattive
 */
function renderTemplatesList(templates) {
  const container = document.getElementById("email-templates-container");
  if (!container) return;

  const filtered = getFilteredTemplates(templates);

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="text-center py-16 px-4">
        <div class="w-16 h-16 rounded-2xl theme-icon-box flex items-center justify-center text-2xl mx-auto mb-4">
          <i class="fa-solid fa-envelope-open"></i>
        </div>
        <h3 class="text-lg font-bold text-white mb-1">Nessun Template Email Trovato</h3>
        <p class="text-gray-400 text-sm max-w-md mx-auto mb-5">
          ${templates.length === 0 ? "Non è ancora presente alcun template registrato nel database. Creane uno nuovo per iniziare." : "Nessun template corrisponde ai filtri di ricerca applicati."}
        </p>
        <button onclick="openCreateTemplateModal()" class="m3-btn-primary">
          <i class="fa-solid fa-plus mr-1.5"></i> Crea Nuovo Template
        </button>
      </div>
    `;
    return;
  }

  let html = `<div class="grid grid-cols-1 gap-4">`;

  filtered.forEach((item) => {
    const localeBadge = getLocaleBadgeHtml(item.locale);
    const paramsMap = item.params || {};
    const paramKeys = Object.keys(paramsMap);

    const paramsPillsHtml = paramKeys.length > 0
      ? paramKeys.slice(0, 6).map(k => `<span class="param-pill" title="Valore di default: ${escapeHtml(String(paramsMap[k]))}">{{${escapeHtml(k)}}}</span>`).join(" ") + (paramKeys.length > 6 ? ` <span class="text-xs text-gray-400">+${paramKeys.length - 6} altri</span>` : "")
      : `<span class="text-gray-500 text-xs italic">Nessun parametro</span>`;

    const lastUpdated = item.updateDate ? new Date(item.updateDate).toLocaleString() : (item.creationDate ? new Date(item.creationDate).toLocaleString() : "N/D");

    html += `
      <div class="p-5 rounded-2xl theme-surface-box hover:brightness-125 transition-all cursor-pointer flex flex-col lg:flex-row lg:items-center justify-between gap-5 group" onclick="openEmailDetailModal(${item.id})">
        
        <!-- Info Principali Template -->
        <div class="space-y-2 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-mono text-xs font-bold theme-badge px-2.5 py-1 rounded-lg">
              ${escapeHtml(item.identifier || "SENZA_ID")}
            </span>
            ${localeBadge}
            <span class="text-xs text-gray-400 flex items-center gap-1 ml-auto lg:ml-2">
              <i class="fa-solid fa-clock-rotate-left text-[11px]"></i> ${lastUpdated}
            </span>
          </div>

          <div>
            <h4 class="text-base font-bold text-white mb-0.5 group-hover:opacity-90 transition-colors">${escapeHtml(item.name || "Template Senza Nome")}</h4>
            <div class="text-xs text-gray-300 flex items-center gap-1.5">
              <i class="fa-solid fa-heading theme-text-accent text-[10px]"></i>
              <strong>Oggetto:</strong> ${escapeHtml(item.subject || "--")}
            </div>
            ${item.description ? `<p class="text-xs text-gray-400 mt-1 mb-0">${escapeHtml(item.description)}</p>` : ""}
          </div>

          <div class="pt-1 flex flex-wrap items-center gap-1.5">
            <span class="text-[11px] text-gray-400 uppercase font-semibold mr-1">Parametri:</span>
            ${paramsPillsHtml}
          </div>
        </div>

        <!-- Pulsanti Azione Rapida -->
        <div class="flex flex-wrap items-center gap-2 shrink-0" onclick="event.stopPropagation()">
          <button type="button" onclick="event.stopPropagation(); openEmailDetailModal(${item.id})" class="m3-btn-secondary text-xs py-2 px-3" title="Visualizza Dettaglio">
            <i class="fa-solid fa-eye text-emerald-400 mr-1.5"></i> Dettaglio
          </button>
          <button type="button" onclick="event.stopPropagation(); openTestEmailModal(${item.id})" class="m3-btn-secondary text-xs py-2 px-3" title="Invia Email di Prova">
            <i class="fa-solid fa-paper-plane text-blue-400 mr-1.5"></i> Invia Test
          </button>
          <button type="button" onclick="event.stopPropagation(); openEditTemplateModal(${item.id})" class="m3-btn-secondary text-xs py-2 px-3" title="Modifica Template">
            <i class="fa-solid fa-pen-to-square text-amber-400 mr-1.5"></i> Modifica
          </button>
          <button type="button" onclick="event.stopPropagation(); confirmDeleteTemplate(${item.id}, '${escapeHtml(item.identifier)}')" class="m3-icon-btn m3-icon-btn-danger text-xs" title="Elimina Template">
            <i class="fa-solid fa-trash-can"></i>
          </button>
        </div>

      </div>
    `;
  });

  html += `</div>`;
  container.innerHTML = html;
}

function getLocaleBadgeHtml(locale) {
  if (!locale || locale.trim() === "") {
    return `<span class="locale-badge locale-badge-generic"><i class="fa-solid fa-globe text-[10px]"></i> Globale / Default</span>`;
  }
  const l = locale.toLowerCase();
  if (l.startsWith("it")) {
    return `<span class="locale-badge locale-badge-it">🇮🇹 ${escapeHtml(locale)}</span>`;
  }
  if (l.startsWith("en")) {
    return `<span class="locale-badge locale-badge-en">🇬🇧 ${escapeHtml(locale)}</span>`;
  }
  if (l.startsWith("fr")) {
    return `<span class="locale-badge locale-badge-en">🇫🇷 ${escapeHtml(locale)}</span>`;
  }
  if (l.startsWith("es")) {
    return `<span class="locale-badge locale-badge-generic">🇪🇸 ${escapeHtml(locale)}</span>`;
  }
  if (l.startsWith("de")) {
    return `<span class="locale-badge locale-badge-generic">🇩🇪 ${escapeHtml(locale)}</span>`;
  }
  return `<span class="locale-badge locale-badge-generic"><i class="fa-solid fa-language text-[10px]"></i> ${escapeHtml(locale)}</span>`;
}

// =========================================================================
// 3. Gestione Parametri Dinamici nell'Editor
// =========================================================================

/**
 * Aggiunge una riga chiave-valore per la configurazione dei parametri dinamici
 */
function addParamRow(key = "", val = "") {
  const container = document.getElementById("editor-params-list");
  if (!container) return;

  const rowId = "param-row-" + Math.random().toString(36).substr(2, 9);
  const rowHtml = `
    <div id="${rowId}" class="flex items-center gap-2">
      <div class="relative w-2/5">
        <span class="absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-accent font-mono text-xs font-bold pointer-events-none select-none z-10">{{</span>
        <input
          type="text"
          class="form-control m3-form-input rounded-xl w-full py-1.5 text-xs font-mono param-key-input uppercase"
          placeholder="CHIAVE"
          value="${escapeHtml(key)}"
          oninput="handleParamChange()"
        />
        <span class="absolute right-3.5 top-1/2 -translate-y-1/2 theme-text-accent font-mono text-xs font-bold pointer-events-none select-none z-10">}}</span>
      </div>
      <div class="w-3/5">
        <input
          type="text"
          class="form-control m3-form-input rounded-xl w-full py-1.5 px-3 text-xs param-val-input"
          placeholder="Valore di default / esempio per l'anteprima"
          value="${escapeHtml(val)}"
          oninput="handleParamChange()"
        />
      </div>
      <button type="button" onclick="removeParamRow('${rowId}')" class="m3-icon-btn m3-icon-btn-danger text-xs p-1.5 shrink-0" title="Rimuovi">
        <i class="fa-solid fa-xmark"></i>
      </button>
    </div>
  `;

  container.insertAdjacentHTML("beforeend", rowHtml);
}

function removeParamRow(rowId) {
  const row = document.getElementById(rowId);
  if (row) {
    row.remove();
    handleParamChange();
  }
}

function handleParamChange() {
  updateLivePreview();
}

/**
 * Scansiona l'HTML e rileva automaticamente tutti i segnaposto {{KEY}} presenti,
 * aggiungendoli alla lista dei parametri se non ancora configurati.
 */
function autoExtractParamsFromHtml() {
  const html = document.getElementById("editor-template-html").value || "";
  const regex = /\{\{([A-Za-z0-9_-]+)\}\}/g;
  const foundKeys = new Set();
  let match;

  while ((match = regex.exec(html)) !== null) {
    if (match[1]) foundKeys.add(match[1].trim());
  }

  if (foundKeys.size === 0) {
    sweetalert("info", "Nessun parametro trovato", "Non sono stati trovati segnaposto nel formato {{KEY}} all'interno dell'HTML.");
    return;
  }

  // Recupera parametri già esistenti nelle righe
  const currentKeys = new Set();
  document.querySelectorAll("#editor-params-list .param-key-input").forEach((input) => {
    if (input.value.trim()) currentKeys.add(input.value.trim());
  });

  let addedCount = 0;
  foundKeys.forEach((k) => {
    if (!currentKeys.has(k)) {
      // Valore di default intelligente basato sul nome chiave
      let defaultVal = "Esempio " + k;
      if (k.includes("URL")) defaultVal = "https://accesssphere.io";
      if (k.includes("CODE")) defaultVal = "123456";
      if (k.includes("NAME")) defaultVal = "Mario Rossi";
      if (k.includes("EMAIL")) defaultVal = "mario.rossi@example.com";
      if (k.includes("SITE")) defaultVal = "Access Sphere";

      addParamRow(k, defaultVal);
      addedCount++;
    }
  });

  if (addedCount > 0) {
    sweetalert("success", "Parametri Rilevati", `Aggiunti automaticamente ${addedCount} nuovi parametri trovati nell'HTML.`);
    updateLivePreview();
  } else {
    sweetalert("info", "Parametri già presenti", "Tutti i parametri {{KEY}} presenti nell'HTML sono già configurati.");
  }
}

/**
 * Serializza le righe dei parametri in un oggetto chiave-valore JSON
 */
function collectEditorParams() {
  const params = {};
  const rows = document.querySelectorAll("#editor-params-list > div");
  rows.forEach((row) => {
    const keyInput = row.querySelector(".param-key-input");
    const valInput = row.querySelector(".param-val-input");
    if (keyInput && valInput) {
      const k = keyInput.value.trim();
      const v = valInput.value;
      if (k) params[k] = v;
    }
  });
  return params;
}

// =========================================================================
// 4. Live Preview Engine (Iframe isolato in tempo reale)
// =========================================================================

function handleHtmlInput(value) {
  clearTimeout(editorDebounceTimer);
  editorDebounceTimer = setTimeout(() => {
    updateLivePreview();
  }, 100);
}

function setPreviewDevice(device) {
  currentPreviewDevice = device;
  const iframe = document.getElementById("live-preview-iframe");
  const btnDesktop = document.getElementById("btn-preview-desktop");
  const btnMobile = document.getElementById("btn-preview-mobile");

  if (!iframe || !btnDesktop || !btnMobile) return;

  if (device === "mobile") {
    iframe.className = "preview-device-mobile";
    btnMobile.classList.add("active");
    btnDesktop.classList.remove("active");
  } else {
    iframe.className = "preview-device-desktop";
    btnDesktop.classList.add("active");
    btnMobile.classList.remove("active");
  }
}

/**
 * Genera il blocco CSS per la barra di scorrimento iniettata dinamicamente negli iframe di anteprima,
 * sincronizzata con i colori e le variabili della palette del tema attualmente selezionato.
 *
 * @param {string} id Identificativo univoco del tag style
 * @returns {string} Tag <style> contenente le regole CSS per le scrollbar coerenti con il tema
 */
function getThemeInjectedScrollbarCss(id = "as-injected-scrollbar") {
  const rootStyles = getComputedStyle(document.documentElement);
  const accentRgb = rootStyles.getPropertyValue("--theme-accent-rgb").trim() || "208, 188, 255";
  const surface = rootStyles.getPropertyValue("--md-surface").trim() || "#0E0B16";
  return `
    <style id="${id}">
      ::-webkit-scrollbar { width: 8px; height: 8px; }
      ::-webkit-scrollbar-track { background: ${surface} !important; }
      ::-webkit-scrollbar-thumb { background: rgba(${accentRgb}, 0.35) !important; border-radius: 9999px !important; border: 2px solid ${surface} !important; }
      ::-webkit-scrollbar-thumb:hover { background: rgba(${accentRgb}, 0.6) !important; }
      ::-webkit-scrollbar-button { display: none !important; width: 0 !important; height: 0 !important; }
      * { scrollbar-width: thin; scrollbar-color: rgba(${accentRgb}, 0.35) ${surface} !important; }
      html, body { scrollbar-color: rgba(${accentRgb}, 0.35) ${surface} !important; }
    </style>
  `;
}

/**
 * Renderizza l'anteprima HTML dentro l'iframe isolato applicando opzionalmente
 * la sostituzione dei parametri {{KEY}} e iniettando la scrollbar coerente con il tema.
 */
function updateLivePreview() {
  const iframe = document.getElementById("live-preview-iframe");
  if (!iframe) return;

  let rawHtml = document.getElementById("editor-template-html").value || "";
  const injectParams = document.getElementById("preview-inject-params") ? document.getElementById("preview-inject-params").checked : true;

  if (injectParams) {
    const params = collectEditorParams();
    Object.keys(params).forEach((key) => {
      const regex = new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, "g");
      rawHtml = rawHtml.replace(regex, params[key]);
    });
  }

  // Se l'HTML è vuoto, mostra un placeholder grafico di attesa
  if (!rawHtml.trim()) {
    rawHtml = `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="utf-8">
        </head>
        <body style="margin:0; padding:60px 20px; font-family:sans-serif; text-align:center; background:#0f1026; color:#94a3b8;">
          <div style="font-size:36px; margin-bottom:12px;">✉️</div>
          <h2 style="color:#ffffff; margin:0 0 8px 0; font-size:18px;">Nessun Contenuto HTML</h2>
          <p style="font-size:13px; max-width:320px; margin:0 auto; line-height:1.5;">Inizia a digitare il codice HTML del template nella colonna a sinistra per vederlo renderizzato in anteprima istantanea.</p>
        </body>
      </html>
    `;
  }

  // Inietta lo stile della scrollbar del tema attivo all'interno del documento renderizzato
  const scrollbarCss = getThemeInjectedScrollbarCss("as-injected-scrollbar");

  if (rawHtml.includes("</head>")) {
    rawHtml = rawHtml.replace("</head>", scrollbarCss + "</head>");
  } else {
    rawHtml = scrollbarCss + rawHtml;
  }

  const doc = iframe.contentDocument || (iframe.contentWindow ? iframe.contentWindow.document : null);
  if (doc) {
    doc.open();
    doc.write(rawHtml);
    doc.close();
  }
}

// =========================================================================
// 5. Creazione, Modifica e Salvataggio Template
// =========================================================================

function openCreateTemplateModal() {
  document.getElementById("editor-template-id").value = "";
  document.getElementById("editor-identifier").value = "";
  document.getElementById("editor-identifier").readOnly = false;
  document.getElementById("editor-name").value = "";
  document.getElementById("editor-subject").value = "";
  document.getElementById("editor-locale").value = "";
  document.getElementById("editor-description").value = "";
  
  // Svuota parametri e aggiungi parametri predefiniti consigliati
  document.getElementById("editor-params-list").innerHTML = "";
  addParamRow("SITE_URL", "https://accesssphere.io");
  addParamRow("NAME", "Mario Rossi");

  // Template HTML di partenza ben strutturato
  const defaultHtml = `<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <title>Comunicazione Access Sphere</title>
    <style>
      body { margin: 0; padding: 0; background: #0c0d1e; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
      .container { max-width: 600px; margin: 40px auto; background: #16172e; border-radius: 16px; border: 1px solid rgba(168, 85, 247, 0.2); overflow: hidden; color: #ffffff; }
      .header { padding: 32px; text-align: center; background: linear-gradient(135deg, #1f143a 0%, #111226 100%); }
      .content { padding: 32px; font-size: 15px; line-height: 1.6; color: #cbd5e1; }
      .cta-button { display: inline-block; padding: 12px 28px; background: #a855f7; color: #ffffff !important; text-decoration: none; border-radius: 50px; font-weight: bold; margin-top: 20px; }
      .footer { padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid rgba(255, 255, 255, 0.08); }
    </style>
  </head>
  <body>
    <div class="container">
      <div class="header">
        <h1 style="margin:0; font-size: 24px; color: #f8fafc;">Access Sphere</h1>
      </div>
      <div class="content">
        <h2 style="color: #ffffff; margin-top: 0;">Ciao {{NAME}},</h2>
        <p>Questo è un messaggio inviato dalla piattaforma sicura Access Sphere.</p>
        <p>Puoi personalizzare questo testo e aggiungere tutti i parametri necessari per il tuo microservizio.</p>
        <div style="text-align: center;">
          <a href="{{SITE_URL}}" class="cta-button">Accedi al Servizio</a>
        </div>
      </div>
      <div class="footer">
        &copy; Access Sphere - Tutti i diritti riservati.
      </div>
    </div>
  </body>
</html>`;

  document.getElementById("editor-template-html").value = defaultHtml;
  document.getElementById("emailEditorModalLabel").textContent = "Nuovo Template Email";

  const modal = new bootstrap.Modal(document.getElementById("emailEditorModal"));
  modal.show();

  setTimeout(() => {
    updateLivePreview();
  }, 300);
}

function openEditTemplateModal(id) {
  const template = cachedTemplates.find((t) => t.id === id);
  if (!template) {
    sweetalert("error", "Errore", "Template non trovato in memoria locale.");
    return;
  }

  document.getElementById("editor-template-id").value = template.id || "";
  document.getElementById("editor-identifier").value = template.identifier || "";
  document.getElementById("editor-identifier").readOnly = false;
  document.getElementById("editor-name").value = template.name || "";
  document.getElementById("editor-subject").value = template.subject || "";
  document.getElementById("editor-locale").value = template.locale || "";
  document.getElementById("editor-description").value = template.description || "";
  document.getElementById("editor-template-html").value = template.template || "";

  // Popola parametri
  const paramsContainer = document.getElementById("editor-params-list");
  paramsContainer.innerHTML = "";
  const paramsMap = template.params || {};
  const keys = Object.keys(paramsMap);

  if (keys.length > 0) {
    keys.forEach((k) => {
      addParamRow(k, paramsMap[k]);
    });
  } else {
    // Prova ad estrarre automaticamente dall'HTML se vuoto
    const regex = /\{\{([A-Za-z0-9_-]+)\}\}/g;
    let match;
    const detected = new Set();
    while ((match = regex.exec(template.template || "")) !== null) {
      if (match[1]) detected.add(match[1].trim());
    }
    detected.forEach(k => addParamRow(k, ""));
  }

  document.getElementById("emailEditorModalLabel").textContent = `Modifica: ${template.name || template.identifier}`;

  const modal = new bootstrap.Modal(document.getElementById("emailEditorModal"));
  modal.show();

  setTimeout(() => {
    updateLivePreview();
  }, 300);
}

/**
 * Salva il template tramite POST o PUT a /v1/email-templates
 */
function saveTemplateForm() {
  const idVal = document.getElementById("editor-template-id").value;
  const isEdit = Boolean(idVal);

  const identifier = document.getElementById("editor-identifier").value.trim();
  const name = document.getElementById("editor-name").value.trim();
  const subject = document.getElementById("editor-subject").value.trim();
  const locale = document.getElementById("editor-locale").value.trim();
  const description = document.getElementById("editor-description").value.trim();
  const templateHtml = document.getElementById("editor-template-html").value;
  const params = collectEditorParams();

  if (!identifier) {
    sweetalert("warning", "Campo Obbligatorio", "L'identificatore univoco del template è obbligatorio.");
    return;
  }
  if (!name) {
    sweetalert("warning", "Campo Obbligatorio", "Il nome del template è obbligatorio.");
    return;
  }
  if (!subject) {
    sweetalert("warning", "Campo Obbligatorio", "L'oggetto dell'email è obbligatorio.");
    return;
  }
  if (!templateHtml || !templateHtml.trim()) {
    sweetalert("warning", "Campo Obbligatorio", "Il markup HTML del template non può essere vuoto.");
    return;
  }

  const payload = {
    identifier: identifier,
    name: name,
    subject: subject,
    locale: locale || null,
    description: description || null,
    template: templateHtml,
    params: params
  };

  if (isEdit) {
    payload.id = parseInt(idVal, 10);
  }

  const cfg = typeof getConfig === "function" ? getConfig() : null;
  const tokenKey = cfg ? cfg.access_token : "access-token";
  const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);
  const url = cfg && cfg.email_templates_url ? cfg.email_templates_url : (window.location.origin + "/v1/email-templates");

  const methodFn = isEdit ? PUT : POST;

  methodFn(url, token, payload)
    .then(async (res) => {
      const data = await res.json();
      if (data.error != null) {
        sweetalert("error", "Errore Salvataggio", data.error.message || "Impossibile salvare il template.");
      } else {
        sweetalert("success", "Template Salvato", `Il template [${identifier}] è stato memorizzato con successo nel database.`);
        const modalEl = document.getElementById("emailEditorModal");
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();
        loadEmailTemplates();
      }
    })
    .catch((err) => {
      console.error("Errore salvataggio template:", err);
      sweetalert("error", "Errore", "Si è verificato un errore durante la chiamata di salvataggio.");
    });
}

// =========================================================================
// 6. Collaudo e Invio Email di Test
// =========================================================================

function openTestEmailModal(id) {
  const template = cachedTemplates.find((t) => t.id === id);
  if (!template) {
    sweetalert("error", "Errore", "Template non trovato per il test.");
    return;
  }

  document.getElementById("test-template-id").value = template.id || "";
  document.getElementById("test-template-identifier").value = template.identifier || "";
  document.getElementById("test-template-locale").value = template.locale || "";

  const displayName = (template.name && template.name !== template.identifier)
    ? `${template.name} (${template.identifier})`
    : (template.name || template.identifier || "--");
  document.getElementById("test-modal-template-name").textContent = displayName;
  document.getElementById("test-modal-locale-badge").innerHTML = getLocaleBadgeHtml(template.locale);

  document.getElementById("test-email-subject").value = template.subject || "";
  document.getElementById("test-email-to").value = "";

  // Genera campi per ciascun parametro definito
  const paramsContainer = document.getElementById("test-email-params-container");
  paramsContainer.innerHTML = "";

  const paramsMap = template.params || {};
  const keys = Object.keys(paramsMap);

  if (keys.length === 0) {
    paramsContainer.innerHTML = `<p class="text-xs text-gray-400 mb-0 italic col-span-2">Nessun parametro dinamico richiesto da questo template.</p>`;
  } else {
    keys.forEach((k) => {
      const val = paramsMap[k] || "";
      const fieldId = "test-param-" + k;
      const html = `
        <div class="space-y-1">
          <label class="block text-xs font-mono theme-text-accent font-semibold mb-1 flex items-center gap-1.5">
            <i class="fa-solid fa-code text-[10px] opacity-70"></i> {{${escapeHtml(k)}}}
          </label>
          <input
            type="text"
            id="${fieldId}"
            data-param-key="${escapeHtml(k)}"
            class="form-control m3-form-input test-param-input rounded-xl w-full text-xs py-2 px-3"
            value="${escapeHtml(String(val))}"
            placeholder="Valore per ${escapeHtml(k)}"
            required
          />
        </div>
      `;
      paramsContainer.insertAdjacentHTML("beforeend", html);
    });
  }

  const modal = new bootstrap.Modal(document.getElementById("emailTestSendModal"));
  modal.show();
}

/**
 * Esegue l'invio dell'email di test tramite POST /v1/email-templates/send-test
 */
function executeSendTestEmail(e) {
  e.preventDefault();

  const to = document.getElementById("test-email-to").value.trim();
  const subject = document.getElementById("test-email-subject").value.trim();
  const templateId = document.getElementById("test-template-id").value;
  const identifier = document.getElementById("test-template-identifier").value;
  const locale = document.getElementById("test-template-locale").value;

  if (!to) {
    sweetalert("warning", "Attenzione", "Inserisci l'indirizzo email del destinatario.");
    return;
  }

  // Raccogli valori dei parametri compilati dall'utente
  const params = {};
  document.querySelectorAll(".test-param-input").forEach((input) => {
    const key = input.getAttribute("data-param-key");
    if (key) params[key] = input.value;
  });

  const payload = {
    to: to,
    subject: subject,
    templateId: templateId ? parseInt(templateId, 10) : null,
    identifier: identifier,
    locale: locale || null,
    params: params
  };

  const btn = document.getElementById("btn-submit-test-email");
  const originalBtnHtml = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin mr-1.5"></i> Invio in corso...`;

  const cfg = typeof getConfig === "function" ? getConfig() : null;
  const tokenKey = cfg ? cfg.access_token : "access-token";
  const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);
  const url = cfg && cfg.email_templates_test_url ? cfg.email_templates_test_url : (window.location.origin + "/v1/email-templates/send-test");

  POST(url, token, payload)
    .then(async (res) => {
      const data = await res.json();
      if (data.error != null) {
        sweetalert("error", "Errore Invio", data.error.message || "Impossibile inviare l'email di test.");
      } else {
        sweetalert("success", "Email Inviata con Successo", `L'email di prova per il template [${identifier}] è stata inoltrata a ${to}.`);
        const modalEl = document.getElementById("emailTestSendModal");
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();
      }
    })
    .catch((err) => {
      console.error("Errore invio email test:", err);
      sweetalert("error", "Errore Invio", "Si è verificato un errore durante l'invio dell'email di test.");
    })
    .finally(() => {
      btn.disabled = false;
      btn.innerHTML = originalBtnHtml;
    });
}

// =========================================================================
// 7. Modale Dettaglio Email, Anteprima Avanzata e Cancellazione
// =========================================================================

/**
 * Apre la schermata di dettaglio avanzata per un template email.
 * Si attiva al clic sulla riga del template o sul pulsante "Dettaglio".
 * Mostra le proprietà censite, i parametri dinamici con i valori di fallback,
 * e il rendering live interattivo (Desktop/Mobile).
 * 
 * Interconnessioni:
 * - Riceve l'id del template selezionato da renderTemplatesList()
 * - Permette di scatenare direttamente openTestEmailModal() o openEditTemplateModal()
 * 
 * @param {number} id - Identificativo primario del template nel database
 */
function openEmailDetailModal(id) {
  const template = cachedTemplates.find((t) => t.id === id);
  if (!template) {
    sweetalert("warning", "Template non trovato", "Impossibile reperire i dettagli del template richiesto.");
    return;
  }

  currentDetailTemplate = template;
  currentDetailDevice = "desktop";

  // 1. Titolo, Identificatore e Locale Badge
  const nameEl = document.getElementById("detail-modal-name");
  if (nameEl) nameEl.textContent = template.name || "Template Email";

  const idBadgeEl = document.getElementById("detail-modal-identifier-badge");
  if (idBadgeEl) idBadgeEl.textContent = template.identifier || "SENZA_ID";

  const localeBadgeEl = document.getElementById("detail-modal-locale-badge");
  if (localeBadgeEl) localeBadgeEl.innerHTML = getLocaleBadgeHtml(template.locale);

  // 2. Data di creazione/aggiornamento
  const lastUpdated = template.updateDate ? new Date(template.updateDate).toLocaleString() : (template.creationDate ? new Date(template.creationDate).toLocaleString() : "N/D");
  const updatedEl = document.getElementById("detail-modal-updated");
  if (updatedEl) updatedEl.textContent = `Ultimo aggiornamento: ${lastUpdated}`;

  // 3. Oggetto e Descrizione del Template
  const subjEl = document.getElementById("detail-modal-subject");
  if (subjEl) {
    subjEl.innerHTML = `<i class="fa-solid fa-heading theme-text-accent text-xs"></i> <span>${escapeHtml(template.subject || "Nessun oggetto configurato")}</span>`;
  }

  const descEl = document.getElementById("detail-modal-desc");
  if (descEl) {
    descEl.textContent = template.description || "Nessuna descrizione specificata per questo template.";
  }

  // 4. Parametri Dinamici censiti con layout dettagliato chiave => default
  const paramsContainer = document.getElementById("detail-modal-params-list");
  const paramsCountEl = document.getElementById("detail-params-count");
  const identSubEl = document.getElementById("detail-modal-ident-sub");
  const localeSubEl = document.getElementById("detail-modal-locale-sub");

  if (identSubEl) identSubEl.textContent = template.identifier || "--";
  if (localeSubEl) localeSubEl.innerHTML = getLocaleBadgeHtml(template.locale);

  if (paramsContainer) {
    paramsContainer.innerHTML = "";
    const params = template.params || {};
    const keys = Object.keys(params);
    if (paramsCountEl) paramsCountEl.textContent = keys.length;

    if (keys.length === 0) {
      paramsContainer.innerHTML = `<p class="text-xs text-gray-500 italic mb-0">Nessun segnaposto configurato per questo template.</p>`;
    } else {
        const row = `
          <div class="flex items-center justify-between p-3 rounded-xl bg-white/[0.03] border border-white/[0.08] hover:bg-white/[0.05] transition-all text-xs">
            <span class="font-mono theme-text-accent font-semibold flex items-center gap-1.5">
              <i class="fa-solid fa-code text-[10px] opacity-70"></i>
              <span>{{${escapeHtml(key)}}}</span>
            </span>
            <span class="font-mono text-[11px] text-gray-300 bg-black/40 px-2.5 py-1 rounded-lg border border-white/10 max-w-[200px] truncate" title="${escapeHtml(String(val || 'Nessun fallback'))}">
              ${escapeHtml(String(val !== undefined && val !== null && val !== "" ? val : "Nessun fallback"))}
            </span>
          </div>
        `;
        paramsContainer.insertAdjacentHTML("beforeend", row);
      });
    }
  }

  // 5. Interconnessione pulsanti azione rapida dell'header del dettaglio
  const btnTest = document.getElementById("btn-detail-test-send");
  if (btnTest) {
    btnTest.onclick = () => {
      const modalEl = document.getElementById("emailDetailModal");
      const modalInst = bootstrap.Modal.getInstance(modalEl);
      if (modalInst) modalInst.hide();
      openTestEmailModal(template.id);
    };
  }

  const btnEdit = document.getElementById("btn-detail-edit");
  if (btnEdit) {
    btnEdit.onclick = () => {
      const modalEl = document.getElementById("emailDetailModal");
      const modalInst = bootstrap.Modal.getInstance(modalEl);
      if (modalInst) modalInst.hide();
      openEditTemplateModal(template.id);
    };
  }

  // 6. Reset viewport e rendering dell'anteprima
  setDetailDevice("desktop");
  updateDetailPreview();

  // 7. Visualizzazione Modale Bootstrap a tutto schermo
  const modalEl = document.getElementById("emailDetailModal");
  if (modalEl) {
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
  }
}

/**
 * Aggiorna il rendering HTML all'interno dell'iframe isolato del modale di dettaglio.
 * Sostituisce i segnaposto dinamici {{KEY}} se la spunta è attiva e inietta la scrollbar coerente con il tema.
 */
function updateDetailPreview() {
  if (!currentDetailTemplate) return;

  const injectParamsCheckbox = document.getElementById("detail-inject-params");
  const injectParams = injectParamsCheckbox ? injectParamsCheckbox.checked : true;
  let renderedHtml = currentDetailTemplate.template || "";

  if (injectParams) {
    const params = currentDetailTemplate.params || {};
    Object.keys(params).forEach((key) => {
      const regex = new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, "g");
      renderedHtml = renderedHtml.replace(regex, params[key]);
    });
  }

  // Inietta lo stile della scrollbar del tema attivo all'interno del documento renderizzato
  const scrollbarCss = getThemeInjectedScrollbarCss("as-injected-detail-scrollbar");

  if (renderedHtml.includes("</head>")) {
    renderedHtml = renderedHtml.replace("</head>", scrollbarCss + "</head>");
  } else {
    renderedHtml = scrollbarCss + renderedHtml;
  }

  const iframe = document.getElementById("detail-preview-iframe");
  if (!iframe) return;

  const doc = iframe.contentDocument || (iframe.contentWindow ? iframe.contentWindow.document : null);
  if (doc) {
    doc.open();
    doc.write(renderedHtml);
    doc.close();
  }
}

/**
 * Commuta la risoluzione del simulatore viewport nel modale di dettaglio (Desktop 100% vs Mobile 375px).
 * @param {'desktop' | 'mobile'} device 
 */
function setDetailDevice(device) {
  currentDetailDevice = device;
  const iframe = document.getElementById("detail-preview-iframe");
  const btnDesktop = document.getElementById("btn-detail-desktop");
  const btnMobile = document.getElementById("btn-detail-mobile");

  if (!iframe || !btnDesktop || !btnMobile) return;

  if (device === "mobile") {
    iframe.className = "preview-device-mobile";
    btnMobile.classList.add("active");
    btnDesktop.classList.remove("active");
  } else {
    iframe.className = "preview-device-desktop";
    btnDesktop.classList.add("active");
    btnMobile.classList.remove("active");
  }
}

/**
 * Wrapper di retrocompatibilità: apre la visualizzazione dettagliata del template
 * @param {number} id 
 */
function openQuickPreview(id) {
  openEmailDetailModal(id);
}

function confirmDeleteTemplate(id, identifier) {
  Swal.fire({
    title: "Sei sicuro?",
    text: `L'eliminazione del template [${identifier}] è irreversibile. Procedere?`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Sì, elimina",
    cancelButtonText: "Annulla",
    confirmButtonColor: "#d33",
    cancelButtonColor: "#6c757d",
    customClass: {
      popup: "m3-swal-popup"
    }
  }).then((result) => {
    if (result.isConfirmed) {
      deleteTemplateRequest(id, identifier);
    }
  });
}

function deleteTemplateRequest(id, identifier) {
  const cfg = typeof getConfig === "function" ? getConfig() : null;
  const tokenKey = cfg ? cfg.access_token : "access-token";
  const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);
  const url = (cfg && cfg.email_templates_url ? cfg.email_templates_url : (window.location.origin + "/v1/email-templates")) + "/" + id;

  DELETE(url, token)
    .then(async (res) => {
      const data = await res.json();
      if (data.error != null) {
        sweetalert("error", "Errore Eliminazione", data.error.message || "Impossibile eliminare il template.");
      } else {
        sweetalert("success", "Template Eliminato", `Il template [${identifier}] è stato rimosso dal database.`);
        loadEmailTemplates();
      }
    })
    .catch((err) => {
      console.error("Errore eliminazione template:", err);
      sweetalert("error", "Errore", "Si è verificato un errore durante l'eliminazione del template.");
    });
}

// =========================================================================
// 8. Utility Helper
// =========================================================================

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
