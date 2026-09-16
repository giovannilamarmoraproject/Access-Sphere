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
let cachedClients = [];
let currentFilterCategory = "ALL";
let currentFilterClient = "ALL";
let currentFilterLocale = "ALL";
let currentSearchQuery = "";
let currentPreviewDevice = "desktop";
let currentDetailTemplate = null;
let currentDetailDevice = "desktop";
let editorDebounceTimer = null;
let duplicateCheckTimer = null;

/**
 * Mappatura semantica e metadati per le categorie di template censite nel sistema.
 * Definisce icone, badge di stile, identificatori tecnici consigliati e parametri standard.
 */
const EMAIL_CATEGORIES = {
    RESET_PASSWORD: {
        code: "RESET_PASSWORD",
        identifier: "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95",
        icon: "fa-key",
        colorClass: "text-purple-400",
        badgeBg: "bg-purple-500/10 border-purple-500/30 text-purple-300",
        activeInCode: true,
        defaultName: "Reimpostazione Password",
        defaultSubject: "Reimposta la tua password",
        defaultParams: {
            SITE_URL: "https://accesssphere.io",
            NAME: "Mario Rossi",
            RESET_CODE: "123456",
            SITE_SHORT: "AS"
        }
    },
    WELCOME_USER: {
        code: "WELCOME_USER",
        identifier: "ACCESS_SPHERE_WELCOME_USER",
        icon: "fa-user-plus",
        colorClass: "text-blue-400",
        badgeBg: "bg-blue-500/10 border-blue-500/30 text-blue-300",
        activeInCode: false,
        defaultName: "Benvenuto Utente",
        defaultSubject: "Benvenuto in Access Sphere",
        defaultParams: {
            SITE_URL: "https://accesssphere.io",
            NAME: "Mario Rossi",
            USERNAME: "mario.rossi"
        }
    },
    EMAIL_VERIFICATION: {
        code: "EMAIL_VERIFICATION",
        identifier: "ACCESS_SPHERE_EMAIL_VERIFICATION",
        icon: "fa-envelope-circle-check",
        colorClass: "text-emerald-400",
        badgeBg: "bg-emerald-500/10 border-emerald-500/30 text-emerald-300",
        activeInCode: false,
        defaultName: "Verifica Indirizzo Email",
        defaultSubject: "Verifica il tuo indirizzo email",
        defaultParams: {
            SITE_URL: "https://accesssphere.io",
            NAME: "Mario Rossi",
            VERIFY_URL: "https://accesssphere.io/verify?token=xyz",
            VERIFY_CODE: "654321"
        }
    },
    MFA_OTP: {
        code: "MFA_OTP",
        identifier: "ACCESS_SPHERE_MFA_OTP",
        icon: "fa-shield-halved",
        colorClass: "text-cyan-400",
        badgeBg: "bg-cyan-500/10 border-cyan-500/30 text-cyan-300",
        activeInCode: false,
        defaultName: "Codice OTP MFA",
        defaultSubject: "Il tuo codice di sicurezza MFA",
        defaultParams: {
            NAME: "Mario Rossi",
            OTP_CODE: "890123",
            EXPIRE_MINUTES: "10"
        }
    },
    ACCOUNT_LOCKED: {
        code: "ACCOUNT_LOCKED",
        identifier: "ACCESS_SPHERE_ACCOUNT_LOCKED",
        icon: "fa-lock",
        colorClass: "text-rose-400",
        badgeBg: "bg-rose-500/10 border-rose-500/30 text-rose-300",
        activeInCode: false,
        defaultName: "Avviso Account Bloccato",
        defaultSubject: "Avviso di sicurezza: Account temporaneamente bloccato",
        defaultParams: {
            NAME: "Mario Rossi",
            LOCK_REASON: "Troppi tentativi di accesso falliti",
            SUPPORT_URL: "https://accesssphere.io/support"
        }
    }
};

// =========================================================================
// 1. Inizializzazione e Caricamento Dati
// =========================================================================

document.addEventListener("DOMContentLoaded", () => {
    initClock();
    checkAuthAndLoad();
});

// Ascolta l'evento di cambio lingua globale per aggiornare istantaneamente la vista
window.addEventListener("languageChanged", () => {
    if (cachedTemplates && cachedTemplates.length > 0) {
        updateKpiMetrics(cachedTemplates);
        renderTemplatesList(cachedTemplates);
    }
    populateClientSelects();
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
    loadOAuthClients();
}

/**
 * Recupera l'elenco dei client OAuth2 registrati per popolare il filtro della toolbar e la select di scoping dell'editor.
 */
function loadOAuthClients() {
    const cfg = typeof getConfig === "function" ? getConfig() : null;
    const tokenKey = cfg ? cfg.access_token : "access-token";
    const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);
    const url = cfg && cfg.client_id_url ? cfg.client_id_url : (window.location.origin + "/v1/clients");

    GET(url, token)
        .then(async (res) => {
            const data = await res.json();
            if (data && !data.error && Array.isArray(data.data)) {
                cachedClients = data.data;
                populateClientSelects();
            }
        })
        .catch((err) => {
            console.warn("Impossibile caricare i client OAuth2 per i template email:", err);
        });
}

/**
 * Popola dinamicamente le select dei client (filtro nella barra strumenti ed editor)
 * Collegando il client predefinito a quello configurato all'avvio dell'app (ACCESS-SPHERE-TECH)
 */
function populateClientSelects() {
    const filterSelect = document.getElementById("client-filter-select");
    const editorSelect = document.getElementById("editor-client");
    const cfg = typeof getConfig === "function" ? getConfig() : null;
    const defaultClientId = (cfg && cfg.client_id) ? cfg.client_id : "ACCESS-SPHERE-TECH";

    if (filterSelect) {
        const currentVal = filterSelect.value || "ALL";
        const lblAll = typeof t === "function" ? t("email_filter_client_all", "Tutte le applicazioni") : "Tutte le applicazioni";
        const lblDefault = typeof t === "function" ? t("email_client_default", `${defaultClientId} (Predefinito)`) : `${defaultClientId} (Predefinito)`;
        const lblGlobal = typeof t === "function" ? t("email_client_global", "Globale (Senza Client)") : "Globale (Senza Client)";

        let filterHtml = `
            <option value="ALL" data-i18n="email_filter_client_all">${escapeHtml(lblAll)}</option>
            <option value="${escapeHtml(defaultClientId)}" data-i18n="email_client_default">${escapeHtml(lblDefault)}</option>
            <option value="GLOBAL" data-i18n="email_client_global">${escapeHtml(lblGlobal)}</option>
        `;
        cachedClients.forEach((c) => {
            const cid = c.clientId || c.id;
            if (cid && cid.toUpperCase() !== defaultClientId.toUpperCase()) {
                filterHtml += `<option value="${escapeHtml(cid)}">${escapeHtml(cid)}</option>`;
            }
        });
        filterSelect.innerHTML = filterHtml;
        filterSelect.value = currentVal;
    }

    if (editorSelect) {
        const currentVal = editorSelect.value !== undefined && editorSelect.value !== "" ? editorSelect.value : defaultClientId;
        const lblDefaultOption = typeof t === "function" ? t("email_client_default", `${defaultClientId} (Predefinito)`) : `${defaultClientId} (Predefinito)`;
        const lblNoneGlobal = typeof t === "function" ? t("email_client_none_global", "Nessun Client (Globale per tutti i client)") : "Nessun Client (Globale per tutti i client)";

        let editorHtml = `
            <option value="${escapeHtml(defaultClientId)}" data-i18n="email_client_default">${escapeHtml(lblDefaultOption)}</option>
            <option value="" data-i18n="email_client_none_global">${escapeHtml(lblNoneGlobal)}</option>
        `;
        cachedClients.forEach((c) => {
            const cid = c.clientId || c.id;
            if (cid && cid.toUpperCase() !== defaultClientId.toUpperCase()) {
                editorHtml += `<option value="${escapeHtml(cid)}">${escapeHtml(cid)}</option>`;
            }
        });
        editorSelect.innerHTML = editorHtml;
        editorSelect.value = currentVal;
    }
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
                sweetalert("error", typeof t === "function" ? t("email_swal_load_error_title", "Errore Caricamento") : "Errore Caricamento", data.error.message || (typeof t === "function" ? t("email_swal_load_error_desc", "Impossibile recuperare i template email.") : "Impossibile recuperare i template email."));
            } else {
                cachedTemplates = data.data || [];
                updateKpiMetrics(cachedTemplates);
                renderTemplatesList(cachedTemplates);
            }
        })
        .catch((err) => {
            console.error("Errore durante il fetch dei template email:", err);
            sweetalert("error", typeof t === "function" ? t("swal_network_error_title", "Errore di Rete") : "Errore di Rete", typeof t === "function" ? t("swal_network_error_desc", "Impossibile contattare il server per i template email.") : "Impossibile contattare il server per i template email.");
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
        if (t.identifier === "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95" || t.identifier === "ACCESS_SPHERE_CODE_RESET_PASSWORD" || t.category === "RESET_PASSWORD") {
            hasResetPwd = true;
        }
    });

    const localesEl = document.getElementById("stat-total-locales");
    if (localesEl) localesEl.textContent = locales.size > 0 ? locales.size : (typeof t === "function" ? t("email_kpi_global", "Globale") : "Globale");

    // 3. Parametri totali censiti
    const paramsEl = document.getElementById("stat-total-params");
    if (paramsEl) paramsEl.textContent = totalParamsCount;

    // 4. Stato Reset Password
    const resetStatusEl = document.getElementById("stat-reset-pwd-status");
    if (resetStatusEl) {
        if (hasResetPwd) {
            resetStatusEl.innerHTML = `<i class="fa-solid fa-circle-check text-emerald-400"></i> ${typeof t === "function" ? t("email_kpi_reset_active", "Attivo nel DB") : "Attivo nel DB"}`;
        } else {
            resetStatusEl.innerHTML = `<i class="fa-solid fa-triangle-exclamation text-amber-400"></i> ${typeof t === "function" ? t("email_kpi_reset_unconfigured", "Non Configurato") : "Non Configurato"}`;
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

function handleCategoryFilter(category) {
    currentFilterCategory = category || "ALL";
    renderTemplatesList(cachedTemplates);
}

function handleClientFilter(client) {
    currentFilterClient = client || "ALL";
    renderTemplatesList(cachedTemplates);
}

function getFilteredTemplates(templates) {
    return templates.filter((t) => {
        // 1. Filtro per Categoria
        if (currentFilterCategory !== "ALL") {
            const cat = (t.category || "RESET_PASSWORD").toUpperCase();
            if (cat !== currentFilterCategory) {
                return false;
            }
        }

        // 2. Filtro per Applicazione / Client Scoping
        if (currentFilterClient !== "ALL") {
            if (currentFilterClient === "GLOBAL") {
                if (t.clientId && t.clientId.trim() !== "") return false;
            } else {
                if (!t.clientId || t.clientId.toUpperCase() !== currentFilterClient.toUpperCase()) {
                    return false;
                }
            }
        }

        // 3. Filtro per Lingua / Locale
        if (currentFilterLocale !== "ALL") {
            if (currentFilterLocale === "GENERIC") {
                if (t.locale && t.locale.trim() !== "") return false;
            } else {
                if (!t.locale || !t.locale.toLowerCase().startsWith(currentFilterLocale.toLowerCase())) {
                    return false;
                }
            }
        }

        // 4. Filtro per testo di ricerca
        if (currentSearchQuery) {
            const idMatch = (t.identifier || "").toLowerCase().includes(currentSearchQuery);
            const nameMatch = (t.name || "").toLowerCase().includes(currentSearchQuery);
            const subjMatch = (t.subject || "").toLowerCase().includes(currentSearchQuery);
            const descMatch = (t.description || "").toLowerCase().includes(currentSearchQuery);
            const catMatch = (t.category || "").toLowerCase().includes(currentSearchQuery);
            const clientMatch = (t.clientId || "").toLowerCase().includes(currentSearchQuery);
            return idMatch || nameMatch || subjMatch || descMatch || catMatch || clientMatch;
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
        const emptyTitle = typeof t === "function" ? t("email_empty_title", "Nessun Template Email Trovato") : "Nessun Template Email Trovato";
        const emptyDesc = templates.length === 0
            ? (typeof t === "function" ? t("email_empty_desc_db", "Non è ancora presente alcun template registrato nel database. Creane uno nuovo per iniziare.") : "Non è ancora presente alcun template registrato nel database. Creane uno nuovo per iniziare.")
            : (typeof t === "function" ? t("email_empty_desc_filter", "Nessun template corrisponde ai filtri di ricerca applicati.") : "Nessun template corrisponde ai filtri di ricerca applicati.");
        const btnNew = typeof t === "function" ? t("btn_new_template", "Crea Nuovo Template") : "Crea Nuovo Template";

        container.innerHTML = `
      <div class="text-center py-16 px-4">
        <div class="w-16 h-16 rounded-2xl theme-icon-box flex items-center justify-center text-2xl mx-auto mb-4">
          <i class="fa-solid fa-envelope-open"></i>
        </div>
        <h3 class="text-lg font-bold text-white mb-1">${emptyTitle}</h3>
        <p class="text-gray-400 text-sm max-w-md mx-auto mb-5">
          ${emptyDesc}
        </p>
        <button onclick="openCreateTemplateModal()" class="m3-btn-primary">
          <i class="fa-solid fa-plus mr-1.5"></i> ${btnNew}
        </button>
      </div>
    `;
        return;
    }

    let html = `<div class="grid grid-cols-1 gap-4">`;

    const lblSubject = typeof t === "function" ? t("email_table_subject_label", "Oggetto:") : "Oggetto:";
    const lblParams = typeof t === "function" ? t("email_table_params_label", "Parametri:") : "Parametri:";
    const lblNoParams = typeof t === "function" ? t("email_no_params", "Nessun parametro") : "Nessun parametro";
    const lblMore = typeof t === "function" ? t("email_params_more", "altri") : "altri";
    const lblDefaultVal = typeof t === "function" ? t("email_param_default_val", "Valore di default:") : "Valore di default:";
    const lblUntitled = typeof t === "function" ? t("email_untitled", "Template Senza Nome") : "Template Senza Nome";
    const lblDetail = typeof t === "function" ? t("btn_detail", "Dettaglio") : "Dettaglio";
    const lblTestSend = typeof t === "function" ? t("btn_test_send", "Invia Test") : "Invia Test";
    const lblEdit = typeof t === "function" ? t("email_action_edit", t("btn_edit", "Modifica")) : "Modifica";
    const lblDeleteTitle = typeof t === "function" ? t("email_action_delete_title", "Elimina Template") : "Elimina Template";

    filtered.forEach((item) => {
        const categoryBadge = getCategoryBadgeHtml(item.category);
        const clientBadge = getClientBadgeHtml(item.clientId);
        const localeBadge = getLocaleBadgeHtml(item.locale);
        const statusBadge = getCategoryStatusBadgeHtml(item.category);
        const paramsMap = item.params || {};
        const paramKeys = Object.keys(paramsMap);

        const paramsPillsHtml = paramKeys.length > 0
            ? paramKeys.slice(0, 6).map(k => `<span class="param-pill truncate max-w-[160px]" title="${lblDefaultVal} ${escapeHtml(String(paramsMap[k]))}">{{${escapeHtml(k)}}}</span>`).join(" ") + (paramKeys.length > 6 ? ` <span class="text-xs text-gray-400 shrink-0">+${paramKeys.length - 6} ${lblMore}</span>` : "")
            : `<span class="text-gray-500 text-xs italic">${lblNoParams}</span>`;

        const lastUpdated = item.updateDate ? new Date(item.updateDate).toLocaleString() : (item.creationDate ? new Date(item.creationDate).toLocaleString() : "N/D");

        html += `
      <div class="p-4 sm:p-5 rounded-2xl theme-surface-box hover:brightness-125 transition-all cursor-pointer flex flex-col lg:flex-row lg:items-center justify-between gap-4 group overflow-hidden" onclick="openEmailDetailModal(${item.id})">
        
        <!-- Info Principali Template con Truncate Responsive -->
        <div class="space-y-2 flex-1 min-w-0">
          <div class="flex flex-wrap items-center gap-1.5 sm:gap-2 min-w-0">
            ${categoryBadge}
            ${clientBadge}
            <span class="font-mono text-xs font-bold theme-badge px-2.5 py-1 rounded-lg truncate max-w-[200px] sm:max-w-xs" title="${escapeHtml(item.identifier || "SENZA_ID")}">
              ${escapeHtml(item.identifier || "SENZA_ID")}
            </span>
            ${localeBadge}
            ${statusBadge}
            <span class="text-xs text-gray-400 flex items-center gap-1 ml-auto lg:ml-2 shrink-0">
              <i class="fa-solid fa-clock-rotate-left text-[11px]"></i> ${lastUpdated}
            </span>
          </div>

          <div class="min-w-0">
            <h4 class="text-base font-bold text-white mb-1 group-hover:opacity-90 transition-colors truncate" title="${escapeHtml(item.name || lblUntitled)}">${escapeHtml(item.name || lblUntitled)}</h4>
            <div class="text-xs text-gray-300 flex items-center gap-1.5 min-w-0">
              <i class="fa-solid fa-heading theme-text-accent text-[10px] shrink-0"></i>
              <strong class="shrink-0">${lblSubject}</strong> 
              <span class="truncate" title="${escapeHtml(item.subject || "--")}">${escapeHtml(item.subject || "--")}</span>
            </div>
            ${item.description ? `<p class="text-xs text-gray-400 mt-1 mb-0 line-clamp-2 break-words" title="${escapeHtml(item.description)}">${escapeHtml(item.description)}</p>` : ""}
          </div>

          <div class="pt-1 flex flex-wrap items-center gap-1.5 min-w-0">
            <span class="text-[11px] text-gray-400 uppercase font-semibold mr-1 shrink-0">${lblParams}</span>
            ${paramsPillsHtml}
          </div>
        </div>

        <!-- Pulsanti Azione Rapida -->
        <div class="flex flex-wrap items-center gap-2 shrink-0 pt-2 lg:pt-0 border-t lg:border-t-0 border-white/5" onclick="event.stopPropagation()">
          <button type="button" onclick="event.stopPropagation(); openEmailDetailModal(${item.id})" class="m3-btn-secondary text-xs py-2 px-3" title="${lblDetail}">
            <i class="fa-solid fa-eye text-emerald-400 mr-1.5"></i> ${lblDetail}
          </button>
          <button type="button" onclick="event.stopPropagation(); openTestEmailModal(${item.id})" class="m3-btn-secondary text-xs py-2 px-3" title="${lblTestSend}">
            <i class="fa-solid fa-paper-plane text-blue-400 mr-1.5"></i> ${lblTestSend}
          </button>
          <button type="button" onclick="event.stopPropagation(); openEditTemplateModal(${item.id})" class="m3-btn-secondary text-xs py-2 px-3" title="${lblEdit}">
            <i class="fa-solid fa-pen-to-square text-amber-400 mr-1.5"></i> ${lblEdit}
          </button>
          <button type="button" onclick="event.stopPropagation(); confirmDeleteTemplate(${item.id}, '${escapeHtml(item.identifier)}')" class="m3-icon-btn m3-icon-btn-danger text-xs" title="${lblDeleteTitle}">
            <i class="fa-solid fa-trash-can"></i>
          </button>
        </div>

      </div>
    `;
    });

    html += `</div>`;
    container.innerHTML = html;
}

function getCategoryBadgeHtml(catKey) {
    const cat = (catKey || "RESET_PASSWORD").toUpperCase();
    const meta = EMAIL_CATEGORIES[cat] || {
        icon: "fa-envelope",
        badgeBg: "bg-white/5 border-white/10 text-gray-300"
    };
    const translatedName = typeof t === "function" ? t("email_cat_" + cat.toLowerCase(), cat) : cat;
    return `
        <span class="px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 border ${meta.badgeBg}">
            <i class="fa-solid ${meta.icon} text-[10px]"></i>
            <span>${escapeHtml(translatedName)}</span>
        </span>
    `;
}

function getClientBadgeHtml(clientId) {
    const cfg = typeof getConfig === "function" ? getConfig() : null;
    const defaultClientId = (cfg && cfg.client_id) ? cfg.client_id : "ACCESS-SPHERE-TECH";

    if (!clientId || clientId.trim() === "") {
        const lblGlobal = typeof t === "function" ? t("email_client_badge_global", "Globale") : "Globale";
        return `<span class="px-2 py-0.5 rounded-lg text-[11px] font-medium bg-white/5 border border-white/10 text-gray-300 flex items-center gap-1" title="${typeof t === "function" ? t("email_client_global", "Globale (Senza Client)") : "Globale (Senza Client)"}"><i class="fa-solid fa-earth-americas text-[10px] opacity-70"></i> ${lblGlobal}</span>`;
    }

    if (clientId.toUpperCase() === defaultClientId.toUpperCase()) {
        return `<span class="px-2 py-0.5 rounded-lg text-[11px] font-medium bg-blue-500/10 border border-blue-500/30 text-blue-300 flex items-center gap-1" title="Client Applicativo Predefinito"><i class="fa-solid fa-cube text-[10px]"></i> ${escapeHtml(clientId)}</span>`;
    }

    return `<span class="px-2 py-0.5 rounded-lg text-[11px] font-medium bg-purple-500/10 border border-purple-500/30 text-purple-300 flex items-center gap-1"><i class="fa-solid fa-cubes text-[10px]"></i> ${escapeHtml(clientId)}</span>`;
}

function getCategoryStatusBadgeHtml(catKey) {
    const cat = (catKey || "RESET_PASSWORD").toUpperCase();
    const isActive = cat === "RESET_PASSWORD";
    if (isActive) {
        const lblActive = typeof t === "function" ? t("email_status_active_in_code", "Attivo nel codice") : "Attivo nel codice";
        return `<span class="px-2 py-0.5 rounded-md text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"><i class="fa-solid fa-check text-[9px] mr-1"></i>${lblActive}</span>`;
    } else {
        const lblPlanned = typeof t === "function" ? t("email_status_coming_soon", "Predisposto") : "Predisposto";
        return `<span class="px-2 py-0.5 rounded-md text-[10px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20"><i class="fa-solid fa-clock text-[9px] mr-1"></i>${lblPlanned}</span>`;
    }
}

function getLocaleBadgeHtml(locale) {
    if (!locale || locale.trim() === "") {
        const lblGeneric = typeof t === "function" ? t("email_locale_generic", "Globale / Default") : "Globale / Default";
        return `<span class="locale-badge locale-badge-generic"><i class="fa-solid fa-globe text-[10px]"></i> ${lblGeneric}</span>`;
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

    const keyPlaceholder = typeof t === "function" ? t("email_param_key_placeholder", "NOME_CHIAVE") : "NOME_CHIAVE";
    const valPlaceholder = typeof t === "function" ? t("email_param_val_placeholder", "Valore di default / esempio per l'anteprima") : "Valore di default / esempio per l'anteprima";
    const removeTitle = typeof t === "function" ? t("email_param_remove_title", "Rimuovi parametro") : "Rimuovi parametro";

    const rowId = "param-row-" + Math.random().toString(36).substr(2, 9);
    const rowHtml = `
    <div id="${rowId}" class="flex items-center gap-2">
      <div class="relative w-2/5">
        <span class="absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-accent font-mono text-xs font-bold pointer-events-none select-none z-10">{{</span>
        <input
          type="text"
          class="form-control m3-form-input rounded-xl w-full py-1.5 text-xs font-mono param-key-input uppercase"
          placeholder="${keyPlaceholder}"
          value="${escapeHtml(key)}"
          oninput="handleParamChange()"
        />
        <span class="absolute right-3.5 top-1/2 -translate-y-1/2 theme-text-accent font-mono text-xs font-bold pointer-events-none select-none z-10">}}</span>
      </div>
      <div class="w-3/5">
        <input
          type="text"
          class="form-control m3-form-input rounded-xl w-full py-1.5 px-3 text-xs param-val-input"
          placeholder="${valPlaceholder}"
          value="${escapeHtml(val)}"
          oninput="handleParamChange()"
        />
      </div>
      <button type="button" onclick="removeParamRow('${rowId}')" class="m3-icon-btn m3-icon-btn-danger text-xs p-1.5 shrink-0" title="${removeTitle}">
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
        sweetalert("info", typeof t === "function" ? t("email_swal_no_params_title", "Nessun Parametro") : "Nessun Parametro", typeof t === "function" ? t("email_swal_no_params_desc", "Nessun segnaposto {{CHIAVE}} trovato nel markup HTML.") : "Nessun segnaposto {{CHIAVE}} trovato nel markup HTML.");
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
        sweetalert("success", typeof t === "function" ? t("email_swal_params_detected_title", "Parametri Rilevati") : "Parametri Rilevati", typeof t === "function" ? t("email_swal_params_detected_desc", `Aggiunti automaticamente ${addedCount} nuovi parametri trovati nell'HTML.`) : `Aggiunti automaticamente ${addedCount} nuovi parametri trovati nell'HTML.`);
        updateLivePreview();
    } else {
        sweetalert("info", typeof t === "function" ? t("email_swal_params_detected_title", "Parametri Rilevati") : "Parametri Rilevati", typeof t === "function" ? t("email_swal_params_detected_desc", "Tutti i parametri {{KEY}} presenti nell'HTML sono già configurati.") : "Tutti i parametri {{KEY}} presenti nell'HTML sono già configurati.");
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

/**
 * Gestisce il cambio di categoria nell'editor:
 * - Aggiorna il badge di stato (Attivo nel codice vs Predisposto)
 * - Se in fase di creazione di un nuovo template, pre-compila nome, identificatore, oggetto e parametri consigliati
 * - Ricalcola immediatamente la verifica duplicati
 */
function handleCategoryChange(catCode) {
    const cat = (catCode || "RESET_PASSWORD").toUpperCase();
    const meta = EMAIL_CATEGORIES[cat];
    const statusBadgeEl = document.getElementById("editor-cat-status-badge");

    if (statusBadgeEl) {
        if (cat === "RESET_PASSWORD") {
            const activeText = typeof t === "function" ? t("email_status_active_in_code", "Attivo nel codice") : "Attivo nel codice";
            statusBadgeEl.className = "text-[10px] font-mono px-2 py-0.5 rounded-md bg-emerald-500/15 text-emerald-300 border border-emerald-500/30";
            statusBadgeEl.innerHTML = `<i class="fa-solid fa-circle-check text-[9px] mr-1"></i><span>${escapeHtml(activeText)}</span>`;
        } else {
            const plannedText = typeof t === "function" ? t("email_status_coming_soon", "Predisposto") : "Predisposto";
            statusBadgeEl.className = "text-[10px] font-mono px-2 py-0.5 rounded-md bg-amber-500/15 text-amber-300 border border-amber-500/30";
            statusBadgeEl.innerHTML = `<i class="fa-solid fa-clock text-[9px] mr-1"></i><span>${escapeHtml(plannedText)}</span>`;
        }
    }

    // Se stiamo creando un nuovo template (non in modalità modifica), compiliamo suggerimenti coerenti
    const isEdit = Boolean(document.getElementById("editor-template-id").value);
    if (!isEdit && meta) {
        const identInput = document.getElementById("editor-identifier");
        if (identInput && !identInput.value.trim()) {
            generateRandomTemplateUuid();
        }

        const nameInput = document.getElementById("editor-name");
        const defaultTranslatedName = typeof t === "function" ? t("email_cat_" + cat.toLowerCase(), meta.defaultName) : meta.defaultName;
        if (nameInput) {
            nameInput.value = defaultTranslatedName;
        }

        const subjInput = document.getElementById("editor-subject");
        if (subjInput) {
            subjInput.value = meta.defaultSubject;
        }

        // Popola i parametri standard di default per questa categoria
        const paramsList = document.getElementById("editor-params-list");
        if (paramsList) {
            paramsList.innerHTML = "";
            Object.keys(meta.defaultParams).forEach((k) => {
                addParamRow(k, meta.defaultParams[k]);
            });
        }
    }

    checkDuplicateConflictDebounced();
}

/**
 * Genera un identificatore UUID casuale per il template e lo inserisce nel campo editor-identifier
 * @returns {string} L'UUID generato
 */
function generateRandomTemplateUuid() {
    const uuid = (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function")
        ? crypto.randomUUID()
        : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    const identInput = document.getElementById("editor-identifier");
    if (identInput) {
        identInput.value = uuid;
    }
    return uuid;
}

/**
 * Verifica in tempo reale se esiste già un template registrato con la medesima combinazione di Categoria, Client e Lingua.
 * Mostra o nasconde il banner di avviso nell'editor.
 *
 * @returns {boolean} true se è presente una collisione duplicata, false altrimenti
 */
function checkDuplicateConflict() {
    const currentId = document.getElementById("editor-template-id") ? document.getElementById("editor-template-id").value : "";
    const categoryEl = document.getElementById("editor-category");
    const clientEl = document.getElementById("editor-client");
    const localeEl = document.getElementById("editor-locale");

    if (!categoryEl || !clientEl || !localeEl) return false;

    const category = (categoryEl.value || "RESET_PASSWORD").trim().toUpperCase();
    const clientId = (clientEl.value || "").trim().toUpperCase();
    const rawLocale = (localeEl.value || "").trim().toLowerCase();
    const locale = rawLocale === "" ? null : rawLocale;

    const warningBanner = document.getElementById("editor-duplicate-warning");

    const isDuplicate = cachedTemplates.some((t) => {
        if (currentId && String(t.id) === String(currentId)) {
            return false;
        }
        const tCat = (t.category || "RESET_PASSWORD").trim().toUpperCase();
        const tClient = (t.clientId || "").trim().toUpperCase();
        const tLocale = (t.locale || "").trim().toLowerCase() || null;

        return tCat === category && tClient === clientId && tLocale === locale;
    });

    if (warningBanner) {
        if (isDuplicate) {
            warningBanner.classList.remove("hidden");
        } else {
            warningBanner.classList.add("hidden");
        }
    }

    return isDuplicate;
}

function checkDuplicateConflictDebounced() {
    if (duplicateCheckTimer) clearTimeout(duplicateCheckTimer);
    duplicateCheckTimer = setTimeout(checkDuplicateConflict, 150);
}

function openCreateTemplateModal() {
    const cfg = typeof getConfig === "function" ? getConfig() : null;
    const defaultClientId = (cfg && cfg.client_id) ? cfg.client_id : "ACCESS-SPHERE-TECH";

    document.getElementById("editor-template-id").value = "";
    document.getElementById("editor-identifier").value = "";
    document.getElementById("editor-identifier").readOnly = false;
    document.getElementById("editor-name").value = "";
    document.getElementById("editor-subject").value = "";
    document.getElementById("editor-locale").value = "";
    document.getElementById("editor-description").value = "";

    const catSelect = document.getElementById("editor-category");
    if (catSelect) catSelect.value = "RESET_PASSWORD";

    const clientSelect = document.getElementById("editor-client");
    if (clientSelect) clientSelect.value = defaultClientId;

    const warningBanner = document.getElementById("editor-duplicate-warning");
    if (warningBanner) warningBanner.classList.add("hidden");

    handleCategoryChange("RESET_PASSWORD");
    generateRandomTemplateUuid();

    // Template HTML di partenza responsive, compatibile con i principali client email (Outlook, Gmail, Apple Mail)
    const defaultHtml = `<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="it">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Reset Password</title>
    <!--[if mso]>
    <noscript>
      <xml>
        <o:OfficeDocumentSettings>
          <o:PixelsPerInch>96</o:PixelsPerInch>
        </o:OfficeDocumentSettings>
      </xml>
    </noscript>
    <![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap");
      body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
      table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
      img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }
      body { margin: 0; padding: 0; width: 100% !important; background-color: #0c0d1e; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
      a:hover { opacity: 0.9 !important; text-decoration: none !important; }
    </style>
  </head>
  <body style="margin: 0; padding: 0; background-color: #0c0d1e; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
    <!-- 100% background wrapper table -->
    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #0c0d1e; min-width: 100%;">
      <tr>
        <td align="center" style="padding: 40px 16px;">
          <!--[if (gte mso 9)|(IE)]>
          <table align="center" border="0" cellpadding="0" cellspacing="0" width="600">
          <tr>
          <td>
          <![endif]-->
          <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; margin: 0 auto;">
            <!-- Brand Logo Header -->
            <tr>
              <td align="center" style="padding-bottom: 24px;">
                <a href="{{SITE_URL}}" target="_blank" title="Access Sphere" style="text-decoration: none; display: inline-block;">
                  <img src="https://access-sphere.giovannilamarmora.com/img/Access%20Sphere%20Transparent%20512x512.png" alt="Access Sphere Logo" width="72" height="72" style="display: block; width: 72px; height: 72px; border-radius: 50%; border: 1px solid rgba(168, 85, 247, 0.35); background-color: #16172e;" />
                </a>
              </td>
            </tr>
            <!-- Card Container -->
            <tr>
              <td align="center">
                <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #16172e; border-radius: 20px; border: 1px solid #292a4d; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35); overflow: hidden;">
                  <tr>
                    <td align="center" style="padding: 40px 32px;">
                      <!-- Salutation -->
                      <h2 style="color: #a78bfa; font-size: 16px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; margin: 0 0 12px 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                        Ciao {{NAME}},
                      </h2>
                      <!-- Main Title -->
                      <h1 style="color: #ffffff; font-size: 24px; font-weight: 700; line-height: 1.35; margin: 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                        Hai richiesto il reset della password
                      </h1>
                      <!-- Gradient Line Separator -->
                      <div style="display: inline-block; vertical-align: middle; margin: 24px auto; border-bottom: 2px solid #7c3aed; width: 60px;"></div>
                      <!-- Instructional Text -->
                      <p style="color: #cbd5e1; font-size: 15px; line-height: 24px; margin: 0 0 24px 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                        Usa il seguente codice di sicurezza per completare il ripristino della password del tuo account. Se non hai richiesto tu questa modifica, puoi ignorare questo messaggio in totale sicurezza.
                      </p>
                      <!-- Verification Code Highlight Block -->
                      <table border="0" cellpadding="0" cellspacing="0" align="center" style="margin: 20px auto;">
                        <tr>
                          <td align="center" style="background-color: #201f3d; border: 1px dashed #a855f7; border-radius: 14px; padding: 16px 32px;">
                            <span style="font-family: 'Courier New', Courier, monospace, 'Poppins', sans-serif; font-size: 32px; font-weight: 700; letter-spacing: 8px; color: #f5f3ff; display: inline-block;">
                              {{RESET_CODE}}
                            </span>
                          </td>
                        </tr>
                      </table>
                      <!-- Expiration Warning -->
                      <p style="color: #94a3b8; font-size: 13px; line-height: 20px; margin: 16px 0 0 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                        <i style="display: inline-block;">&#9201;</i> Il codice di verifica &egrave; valido per <strong>15 minuti</strong>.
                      </p>
                      <!-- CTA Button to Platform -->
                      <table border="0" cellpadding="0" cellspacing="0" align="center" style="margin-top: 32px;">
                        <tr>
                          <td align="center" style="border-radius: 50px; background-color: #7c3aed;">
                            <a href="{{SITE_URL}}" target="_blank" style="font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 14px; font-weight: 600; color: #ffffff; text-decoration: none; padding: 13px 32px; border-radius: 50px; display: inline-block; text-transform: uppercase; letter-spacing: 0.5px;">
                              Reimposta Password
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <!-- Email Footer -->
            <tr>
              <td align="center" style="padding-top: 24px;">
                <p style="font-size: 13px; color: #64748b; line-height: 18px; margin: 0 0 6px 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                  &copy; <strong>{{SITE_SHORT}}</strong> &bull; Identity &amp; Access Management
                </p>
                <p style="font-size: 12px; color: #475569; margin: 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                  Messaggio automatico di sicurezza generato da <a href="{{SITE_URL}}" target="_blank" style="color: #a78bfa; text-decoration: none;">Access Sphere</a>.
                </p>
              </td>
            </tr>
          </table>
          <!--[if (gte mso 9)|(IE)]>
          </td>
          </tr>
          </table>
          <![endif]-->
        </td>
      </tr>
    </table>
  </body>
</html>`;

    document.getElementById("editor-template-html").value = defaultHtml;
    document.getElementById("emailEditorModalLabel").textContent = typeof t === "function" ? t("modal_email_editor_new_title", "Nuovo Template Email") : "Nuovo Template Email";

    const modal = new bootstrap.Modal(document.getElementById("emailEditorModal"));
    modal.show();

    setTimeout(() => {
        updateLivePreview();
        checkDuplicateConflict();
    }, 300);
}

function openEditTemplateModal(id) {
    const template = cachedTemplates.find((t) => t.id === id);
    if (!template) {
        sweetalert("error", typeof t === "function" ? t("error", "Errore") : "Errore", typeof t === "function" ? t("email_swal_load_error_desc", "Template non trovato in memoria locale.") : "Template non trovato in memoria locale.");
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

    const catSelect = document.getElementById("editor-category");
    if (catSelect) {
        catSelect.value = (template.category || "RESET_PASSWORD").toUpperCase();
    }

    const clientSelect = document.getElementById("editor-client");
    if (clientSelect) {
        clientSelect.value = template.clientId || "";
    }

    handleCategoryChange(template.category || "RESET_PASSWORD");

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

    const editPrefix = typeof t === "function" ? t("email_action_edit", "Modifica") : "Modifica";
    const modalLabelEl = document.getElementById("emailEditorModalLabel");
    if (modalLabelEl) {
        const fullTitle = `${editPrefix}: ${template.name || template.identifier}`;
        modalLabelEl.textContent = fullTitle;
        modalLabelEl.title = fullTitle;
    }

    const modal = new bootstrap.Modal(document.getElementById("emailEditorModal"));
    modal.show();

    setTimeout(() => {
        updateLivePreview();
        checkDuplicateConflict();
    }, 300);
}

/**
 * Salva il template tramite POST o PUT a /v1/email-templates.
 * Valida i campi obbligatori, esegue il controllo preventivo dei duplicati (Category, Client, Locale)
 * e inoltra la richiesta al backend.
 */
function saveTemplateForm() {
    const idVal = document.getElementById("editor-template-id").value;
    const isEdit = Boolean(idVal);

    const category = (document.getElementById("editor-category").value || "RESET_PASSWORD").trim().toUpperCase();
    const clientId = (document.getElementById("editor-client").value || "").trim().toUpperCase();
    const identifier = document.getElementById("editor-identifier").value.trim();
    const name = document.getElementById("editor-name").value.trim();
    const subject = document.getElementById("editor-subject").value.trim();
    const locale = document.getElementById("editor-locale").value.trim();
    const description = document.getElementById("editor-description").value.trim();
    const templateHtml = document.getElementById("editor-template-html").value;
    const params = collectEditorParams();

    if (!identifier || !name || !subject || !templateHtml || !templateHtml.trim()) {
        sweetalert(
            "warning",
            typeof t === "function" ? t("warning", "Attenzione") : "Attenzione",
            typeof t === "function" ? t("email_swal_missing_fields_desc", "Compila tutti i campi obbligatori (Identificatore, Nome, Oggetto e Markup HTML).") : "Compila tutti i campi obbligatori (Identificatore, Nome, Oggetto e Markup HTML)."
        );
        return;
    }

    // Controllo preventivo duplicazione su memoria locale
    if (checkDuplicateConflict()) {
        sweetalert(
            "warning",
            typeof t === "function" ? t("email_swal_duplicate_error_title", "Template Già Esistente") : "Template Già Esistente",
            typeof t === "function" ? t("email_swal_duplicate_error_desc", "Esiste già un template registrato per questa combinazione di Categoria, Applicazione e Lingua.") : "Esiste già un template registrato per questa combinazione di Categoria, Applicazione e Lingua."
        );
        return;
    }

    const payload = {
        category: category,
        clientId: clientId || null,
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
                sweetalert(
                    "error",
                    typeof t === "function" ? t("email_swal_save_error_title", "Errore Salvataggio") : "Errore Salvataggio",
                    data.error.message || (typeof t === "function" ? t("email_swal_save_error_title", "Impossibile salvare il template.") : "Impossibile salvare il template.")
                );
            } else {
                sweetalert(
                    "success",
                    typeof t === "function" ? t("email_swal_save_success_title", "Template Salvato!") : "Template Salvato!",
                    typeof t === "function" ? t("email_swal_save_success_desc", `Il template [${identifier}] è stato memorizzato con successo nel database.`) : `Il template [${identifier}] è stato memorizzato con successo nel database.`
                );
                const modalEl = document.getElementById("emailEditorModal");
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) modalInstance.hide();
                loadEmailTemplates();
            }
        })
        .catch((err) => {
            console.error("Errore salvataggio template:", err);
            sweetalert(
                "error",
                typeof t === "function" ? t("error", "Errore") : "Errore",
                typeof t === "function" ? t("email_swal_save_error_title", "Si è verificato un errore durante la chiamata di salvataggio.") : "Si è verificato un errore durante la chiamata di salvataggio."
            );
        });
}

// =========================================================================
// 6. Collaudo e Invio Email di Test
// =========================================================================

function openTestEmailModal(id) {
    const template = cachedTemplates.find((t) => t.id === id);
    if (!template) {
        sweetalert("error", typeof t === "function" ? t("error", "Errore") : "Errore", typeof t === "function" ? t("email_swal_load_error_desc", "Template non trovato per il test.") : "Template non trovato per il test.");
        return;
    }

    document.getElementById("test-template-id").value = template.id || "";
    document.getElementById("test-template-identifier").value = template.identifier || "";
    document.getElementById("test-template-locale").value = template.locale || "";

    const displayName = (template.name && template.name !== template.identifier)
        ? `${template.name} (${template.identifier})`
        : (template.name || template.identifier || "--");
    const nameEl = document.getElementById("test-modal-template-name");
    if (nameEl) {
        nameEl.textContent = displayName;
        nameEl.title = displayName;
    }
    document.getElementById("test-modal-locale-badge").innerHTML = getLocaleBadgeHtml(template.locale);

    document.getElementById("test-email-subject").value = template.subject || "";
    document.getElementById("test-email-to").value = "";

    // Genera campi per ciascun parametro definito
    const paramsContainer = document.getElementById("test-email-params-container");
    paramsContainer.innerHTML = "";

    const paramsMap = template.params || {};
    const keys = Object.keys(paramsMap);

    if (keys.length === 0) {
        const noParamsMsg = typeof t === "function" ? t("email_test_no_params_msg", "Nessun parametro richiesto da questo template. Procedi direttamente con l'invio.") : "Nessun parametro richiesto da questo template. Procedi direttamente con l'invio.";
        paramsContainer.innerHTML = `<p class="text-xs text-gray-400 mb-0 italic col-span-2">${escapeHtml(noParamsMsg)}</p>`;
    } else {
        const valPlaceholderPrefix = typeof t === "function" ? t("email_param_default_val", "Valore di default:") : "Valore per";
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
            placeholder="${escapeHtml(valPlaceholderPrefix)} ${escapeHtml(k)}"
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
        sweetalert(
            "warning",
            typeof t === "function" ? t("warning", "Attenzione") : "Attenzione",
            typeof t === "function" ? t("email_test_recipient", "Inserisci l'indirizzo email del destinatario.") : "Inserisci l'indirizzo email del destinatario."
        );
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
    const sendingText = typeof t === "function" ? t("email_test_sending", "Invio in corso...") : "Invio in corso...";
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin mr-1.5"></i> ${escapeHtml(sendingText)}`;

    const cfg = typeof getConfig === "function" ? getConfig() : null;
    const tokenKey = cfg ? cfg.access_token : "access-token";
    const token = typeof getCookieOrStorage === "function" ? getCookieOrStorage(tokenKey) : localStorage.getItem(tokenKey);
    const url = cfg && cfg.email_templates_test_url ? cfg.email_templates_test_url : (window.location.origin + "/v1/email-templates/send-test");

    POST(url, token, payload)
        .then(async (res) => {
            const data = await res.json();
            if (data.error != null) {
                sweetalert(
                    "error",
                    typeof t === "function" ? t("email_swal_test_error_title", "Errore Invio") : "Errore Invio",
                    data.error.message || (typeof t === "function" ? t("email_swal_test_error_title", "Impossibile inviare l'email di test.") : "Impossibile inviare l'email di test.")
                );
            } else {
                sweetalert(
                    "success",
                    typeof t === "function" ? t("email_swal_test_sent_title", "Email Inviata con Successo") : "Email Inviata con Successo",
                    typeof t === "function" ? t("email_swal_test_sent_desc", `L'email di prova per il template [${identifier}] è stata inoltrata a ${to}.`) : `L'email di prova per il template [${identifier}] è stata inoltrata a ${to}.`
                );
                const modalEl = document.getElementById("emailTestSendModal");
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) modalInstance.hide();
            }
        })
        .catch((err) => {
            console.error("Errore invio email test:", err);
            sweetalert(
                "error",
                typeof t === "function" ? t("email_swal_test_error_title", "Errore Invio") : "Errore Invio",
                typeof t === "function" ? t("email_swal_test_error_title", "Si è verificato un errore durante l'invio dell'email di test.") : "Si è verificato un errore durante l'invio dell'email di test."
            );
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
        sweetalert(
            "warning",
            typeof t === "function" ? t("warning", "Attenzione") : "Attenzione",
            typeof t === "function" ? t("email_empty_desc_filter", "Impossibile reperire i dettagli del template richiesto.") : "Impossibile reperire i dettagli del template richiesto."
        );
        return;
    }

    currentDetailTemplate = template;
    currentDetailDevice = "desktop";

    // 1. Titolo, Identificatore e Locale Badge
    const nameEl = document.getElementById("detail-modal-name");
    if (nameEl) {
        const titleText = template.name || (typeof t === "function" ? t("email_untitled", "Template Senza Nome") : "Template Senza Nome");
        nameEl.textContent = titleText;
        nameEl.title = titleText;
    }

    const idBadgeEl = document.getElementById("detail-modal-identifier-badge");
    if (idBadgeEl) {
        const idText = template.identifier || "SENZA_ID";
        idBadgeEl.textContent = idText;
        idBadgeEl.title = idText;
    }

    const localeBadgeEl = document.getElementById("detail-modal-locale-badge");
    if (localeBadgeEl) localeBadgeEl.innerHTML = getLocaleBadgeHtml(template.locale);

    // 2. Data di creazione/aggiornamento
    const lastUpdated = template.updateDate ? new Date(template.updateDate).toLocaleString() : (template.creationDate ? new Date(template.creationDate).toLocaleString() : "N/D");
    const updatedEl = document.getElementById("detail-modal-updated");
    const lastUpdatedPrefix = typeof t === "function" ? t("email_detail_last_updated", "Ultimo aggiornamento:") : "Ultimo aggiornamento:";
    if (updatedEl) updatedEl.textContent = `${lastUpdatedPrefix} ${lastUpdated}`;

    // 3. Oggetto e Descrizione del Template
    const subjEl = document.getElementById("detail-modal-subject");
    if (subjEl) {
        subjEl.innerHTML = `<i class="fa-solid fa-heading theme-text-accent text-xs"></i> <span>${escapeHtml(template.subject || (typeof t === "function" ? t("email_untitled", "Nessun oggetto configurato") : "Nessun oggetto configurato"))}</span>`;
    }

    const descEl = document.getElementById("detail-modal-desc");
    if (descEl) {
        descEl.textContent = template.description || (typeof t === "function" ? t("email_untitled", "Nessuna descrizione specificata per questo template.") : "Nessuna descrizione specificata per questo template.");
    }

    // 4. Parametri Dinamici censiti con layout dettagliato chiave => default
    const paramsContainer = document.getElementById("detail-modal-params-list");
    const paramsCountEl = document.getElementById("detail-params-count");
    const catSubEl = document.getElementById("detail-modal-category-sub");
    const clientSubEl = document.getElementById("detail-modal-client-sub");
    const identSubEl = document.getElementById("detail-modal-ident-sub");
    const localeSubEl = document.getElementById("detail-modal-locale-sub");

    if (catSubEl) {
        const catKey = (template.category || "RESET_PASSWORD").toUpperCase();
        const catMeta = EMAIL_CATEGORIES[catKey] || { icon: "fa-envelope" };
        const translatedCat = typeof t === "function" ? t("email_cat_" + catKey.toLowerCase(), catKey) : catKey;
        catSubEl.innerHTML = `<i class="fa-solid ${catMeta.icon} mr-1.5 theme-text-accent"></i> <span>${escapeHtml(translatedCat)}</span>`;
    }

    if (clientSubEl) {
        const cfg = typeof getConfig === "function" ? getConfig() : null;
        const defaultClientId = (cfg && cfg.client_id) ? cfg.client_id : "ACCESS-SPHERE-TECH";

        if (template.clientId) {
            const isDefault = template.clientId.toUpperCase() === defaultClientId.toUpperCase();
            const colorClass = isDefault ? "text-blue-400" : "text-purple-400";
            const textClass = isDefault ? "text-blue-300" : "text-purple-300";
            const labelSuffix = isDefault ? ` (${typeof t === "function" ? t("email_client_default_short", "Predefinito") : "Predefinito"})` : "";
            clientSubEl.innerHTML = `<i class="fa-solid ${isDefault ? 'fa-cube' : 'fa-cubes'} ${colorClass} mr-1.5"></i> <span class="${textClass}">${escapeHtml(template.clientId)}${escapeHtml(labelSuffix)}</span>`;
        } else {
            const lblGlobal = typeof t === "function" ? t("email_client_none_global", "Nessun Client (Globale)") : "Nessun Client (Globale)";
            clientSubEl.innerHTML = `<i class="fa-solid fa-earth-americas text-gray-400 mr-1.5"></i> <span class="text-gray-300">${escapeHtml(lblGlobal)}</span>`;
        }
    }

    if (identSubEl) {
        const identVal = template.identifier || "--";
        identSubEl.textContent = identVal;
        identSubEl.title = identVal;
    }
    if (localeSubEl) localeSubEl.innerHTML = getLocaleBadgeHtml(template.locale);

    if (paramsContainer) {
        paramsContainer.innerHTML = "";
        const params = template.params || {};
        const keys = Object.keys(params);
        if (paramsCountEl) paramsCountEl.textContent = keys.length;

        if (keys.length === 0) {
            const noParamsMsg = typeof t === "function" ? t("email_detail_no_params", "Nessun segnaposto configurato per questo template.") : "Nessun segnaposto configurato per questo template.";
            paramsContainer.innerHTML = `<p class="text-xs text-gray-500 italic mb-0">${escapeHtml(noParamsMsg)}</p>`;
        } else {
            const noFallbackLabel = typeof t === "function" ? t("email_detail_no_fallback", "Nessun fallback") : "Nessun fallback";
            keys.forEach((key) => {
                const val = params[key];
                const displayVal = (val !== undefined && val !== null && val !== "") ? val : noFallbackLabel;
                const row = `
          <div class="flex items-center justify-between p-3 rounded-xl bg-white/[0.03] border border-white/[0.08] hover:bg-white/[0.05] transition-all text-xs gap-2 min-w-0">
            <span class="font-mono theme-text-accent font-semibold flex items-center gap-1.5 min-w-0 truncate">
              <i class="fa-solid fa-code text-[10px] opacity-70 shrink-0"></i>
              <span class="truncate" title="{{${escapeHtml(key)}}}">{{${escapeHtml(key)}}}</span>
            </span>
            <span class="font-mono text-[11px] text-gray-300 bg-black/40 px-2.5 py-1 rounded-lg border border-white/10 max-w-[130px] sm:max-w-[200px] truncate shrink-0" title="${escapeHtml(String(displayVal))}">
              ${escapeHtml(String(displayVal))}
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
    const titleText = typeof t === "function" ? t("email_swal_delete_confirm_title", "Eliminare il Template?") : "Eliminare il Template?";
    const descText = typeof t === "function" ? t("email_swal_delete_confirm_desc", `L'eliminazione del template [${identifier}] è irreversibile. Procedere?`) : `L'eliminazione del template [${identifier}] è irreversibile. Procedere?`;
    const confirmBtn = typeof t === "function" ? t("delete", "Sì, elimina") : "Sì, elimina";
    const cancelBtn = typeof t === "function" ? t("cancel", "Annulla") : "Annulla";

    Swal.fire({
        title: titleText,
        text: descText,
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: confirmBtn,
        cancelButtonText: cancelBtn,
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
                sweetalert(
                    "error",
                    typeof t === "function" ? t("email_swal_delete_error_title", "Errore Eliminazione") : "Errore Eliminazione",
                    data.error.message || (typeof t === "function" ? t("email_swal_delete_error_title", "Impossibile eliminare il template.") : "Impossibile eliminare il template.")
                );
            } else {
                sweetalert(
                    "success",
                    typeof t === "function" ? t("email_swal_delete_success_title", "Template Eliminato") : "Template Eliminato",
                    typeof t === "function" ? t("email_swal_delete_success_desc", `Il template [${identifier}] è stato rimosso dal database.`) : `Il template [${identifier}] è stato rimosso dal database.`
                );
                loadEmailTemplates();
            }
        })
        .catch((err) => {
            console.error("Errore eliminazione template:", err);
            sweetalert(
                "error",
                typeof t === "function" ? t("email_swal_delete_error_title", "Errore Eliminazione") : "Errore Eliminazione",
                typeof t === "function" ? t("email_swal_delete_error_title", "Si è verificato un errore durante l'eliminazione del template.") : "Si è verificato un errore durante l'eliminazione del template."
            );
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
