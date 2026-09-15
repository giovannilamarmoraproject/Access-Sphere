function getUser() {
  const urlParams = window.location.href;
  let identifier = null;
  if (urlParams.includes("details/")) {
    identifier = urlParams.split("details/")[1].split("/")[0].split("?")[0].split("#")[0];
  } else {
    const params = new URLSearchParams(window.location.search);
    identifier = params.get("id") || params.get("identifier");
  }

  if (!identifier) {
    console.error("Identificativo utente non specificato nella URL");
    return sweetalert("error", "Errore", "Identificativo utente mancante.");
  }

  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    try {
      const users = JSON.parse(usersJSON);
      const user = users.find((u) => u.identifier == identifier || u.username == identifier);
      if (user) {
        displayUserData(user);
        return;
      }
    } catch(e) {
      console.warn("Errore parsing localStorage usersData:", e);
    }
  }

  console.log("Recupero dati utente da API...");
  const token = getCookieOrStorage(config.access_token);
  GET(config.users_url, token).then(async (res) => {
    try {
      const data = await res.json();
      if (data && data.error && typeof isUnauthorizedError === "function" && isUnauthorizedError(data.error)) {
        if (typeof logout === "function") logout();
        return;
      }
      if (data && data.data && Array.isArray(data.data)) {
        localStorage.setItem(config.client_id + "_usersData", JSON.stringify(data.data));
        const user = data.data.find((u) => u.identifier == identifier || u.username == identifier);
        if (user) {
          displayUserData(user);
        } else {
          sweetalert("error", "Utente Non Trovato", `Nessun utente trovato con ID: ${identifier}`);
        }
      } else {
        sweetalert("error", "Errore", "Impossibile recuperare i dati utente dal server.");
      }
    } catch(err) {
      console.error(err);
      sweetalert("error", "Errore", "Errore durante il parsing dei dati utente.");
    }
  }).catch(err => {
    console.error("Fetch users error:", err);
    sweetalert("error", "Errore di Connessione", "Impossibile contattare il server.");
  });
}

function renderCustomAttributesData(attributes) {
  if (!attributes || typeof attributes !== "object" || Object.keys(attributes).length === 0) {
    return `
      <div class="p-6 text-center flex items-center justify-center gap-3" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.6); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 20px; color: var(--theme-accent, #D0BCFF);">
        <i class="fa-solid fa-sliders text-lg" style="color: var(--theme-accent, #A855F7);"></i>
        <span class="text-xs font-medium">${typeof t === 'function' ? t("user_detail_no_attributes", "Nessun attributo personalizzato presente per questo account.") : "Nessun attributo personalizzato presente per questo account."}</span>
      </div>
    `;
  }

  let sectionsHtml = "";
  const rawJson = JSON.stringify(attributes, null, 2);

  for (const [key, value] of Object.entries(attributes)) {
    // Nested object (e.g. money_stats_settings)
    if (value && typeof value === "object" && !Array.isArray(value)) {
      const formattedTitle = key.replace(/_/g, " ").replace(/-/g, " ").toUpperCase();
      let subItemsHtml = "";

      for (const [subKey, subVal] of Object.entries(value)) {
        const subLabel = subKey.replace(/([A-Z])/g, " $1").replace(/_/g, " ").trim();
        let valueDisplay = "";

        if (typeof subVal === "string" && (subVal === "EUR" || subVal === "USD" || subVal.length <= 4)) {
          valueDisplay = `<span class="px-2.5 py-0.5 font-mono font-bold text-xs" style="background: rgba(var(--theme-primary-rgb, 168, 85, 247), 0.2); color: var(--theme-accent, #EADDFF); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); border-radius: 9999px;">${subVal}</span>`;
        } else if (subVal === "€" || subVal === "$") {
          valueDisplay = `<span class="px-2.5 py-0.5 font-bold font-mono text-sm" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 9999px;">${subVal}</span>`;
        } else if (subVal === "ACTIVE" || subVal === "COMPLETED" || subVal === "TRUE" || subVal === true) {
          valueDisplay = `<span class="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-semibold" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 9999px;"><i class="fa-solid fa-circle-check text-[10px]"></i> ${subVal}</span>`;
        } else if (subVal === "INACTIVE" || subVal === "PENDING" || subVal === false) {
          valueDisplay = `<span class="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-semibold" style="background: rgba(234, 179, 8, 0.2); color: #FDE047; border: 1px solid rgba(234, 179, 8, 0.3); border-radius: 9999px;"><i class="fa-solid fa-clock text-[10px]"></i> ${subVal}</span>`;
        } else {
          valueDisplay = `<span class="font-mono text-white text-xs font-medium truncate">${subVal}</span>`;
        }

        subItemsHtml += `
          <div class="p-3 flex flex-col justify-between min-w-0" style="background: rgba(var(--theme-surface-rgb, 14, 11, 20), 0.6); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.12); border-radius: 14px;">
            <span class="text-[10px] uppercase font-semibold tracking-wider truncate mb-1" style="color: var(--theme-accent, #C4B5FD);" title="${String(subLabel).replace(/"/g, '&quot;')}">${subLabel}</span>
            <div class="mt-1 flex items-center min-w-0">${valueDisplay}</div>
          </div>
        `;
      }

      sectionsHtml += `
        <div class="p-4 sm:p-5 space-y-3.5" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.7); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 18px;">
          <div class="flex items-center justify-between pb-3 gap-2" style="border-bottom: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.12);">
            <div class="flex items-center gap-2.5 min-w-0">
              <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0" style="background: rgba(var(--theme-primary-rgb, 168, 85, 247), 0.2); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); color: var(--theme-accent, #D0BCFF);">
                <i class="fa-solid fa-layer-group text-xs"></i>
              </div>
              <div class="min-w-0">
                <span class="text-xs sm:text-sm font-bold text-white block truncate uppercase">${formattedTitle}</span>
                <span class="text-[10px] font-mono block truncate" style="color: var(--theme-accent, #D0BCFF); opacity: 0.85;">${key}</span>
              </div>
            </div>
            <span class="px-2.5 py-1 font-mono text-[10px] font-semibold whitespace-nowrap flex-shrink-0 rounded-full" style="background: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); color: var(--theme-accent, #EADDFF); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.2);">
              ${typeof t === 'function' ? t("client_field_nested_obj", "Oggetto") : "Oggetto"} (${Object.keys(value).length})
            </span>
          </div>
          <div class="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
            ${subItemsHtml}
          </div>
        </div>
      `;
    }
    // String (token or regular property)
    else if (typeof value === "string") {
      const isToken = key.toLowerCase().includes("token") || value.length > 30;
      const formattedTitle = key.replace(/_/g, " ").replace(/-/g, " ").toUpperCase();
      const safeVal = value.replace(/"/g, '&quot;');
      const inputId = "token_" + Math.random().toString(36).substring(2, 9);

      if (isToken) {
        sectionsHtml += `
          <div class="p-4 sm:p-5 space-y-3" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.7); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 18px;">
            <div class="flex items-center justify-between gap-2">
              <div class="flex items-center gap-2.5 min-w-0">
                <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0" style="background: rgba(var(--theme-primary-rgb, 168, 85, 247), 0.2); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); color: var(--theme-accent, #D0BCFF);">
                  <i class="fa-solid fa-key text-xs"></i>
                </div>
                <div class="min-w-0">
                  <span class="text-xs sm:text-sm font-bold text-white block truncate uppercase">${formattedTitle}</span>
                  <span class="text-[10px] font-mono block truncate" style="color: var(--theme-accent, #D0BCFF); opacity: 0.85;">${key}</span>
                </div>
              </div>
              <span class="px-2.5 py-1 font-mono text-[10px] font-semibold whitespace-nowrap flex-shrink-0 rounded-full" style="background: rgba(245, 158, 11, 0.15); color: #FCD34D; border: 1px solid rgba(245, 158, 11, 0.25);">
                Token / Secret
              </span>
            </div>
            <div class="flex items-center gap-2 pt-1">
              <input type="password" id="${inputId}" readonly value="${safeVal}" class="form-control font-mono text-xs flex-1 min-w-0" style="background: var(--theme-input-bg, #231B34); border: 1.5px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.28); color: #fff; border-radius: 9999px; padding: 9px 16px;" />
              <button type="button" class="flex-shrink-0" onclick="togglePasswordVisibility('${inputId}', this)" title="Mostra / Nascondi" style="width: 36px; height: 36px; border-radius: 9999px; background: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.12); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); color: var(--theme-accent, #FFFFFF); display: inline-flex; align-items: center; justify-content: center; cursor: pointer;">
                <i class="fa-solid fa-eye text-xs"></i>
              </button>
              <button type="button" class="flex-shrink-0" onclick="copyTokenValue('${inputId}')" title="${typeof t === 'function' ? t("user_detail_copy_btn", "Copia") : "Copia"}" style="width: 36px; height: 36px; border-radius: 9999px; background: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.12); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); color: var(--theme-accent, #FFFFFF); display: inline-flex; align-items: center; justify-content: center; cursor: pointer;">
                <i class="fa-solid fa-copy text-xs"></i>
              </button>
            </div>
          </div>
        `;
      } else {
        sectionsHtml += `
          <div class="p-3.5 sm:p-4 flex items-center justify-between gap-3" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.7); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 18px;">
            <div class="min-w-0">
              <span class="text-xs font-bold text-white uppercase block truncate">${formattedTitle}</span>
              <span class="text-[10px] font-mono block truncate" style="color: var(--theme-accent, #D0BCFF); opacity: 0.85;">${key}</span>
            </div>
            <span class="font-mono text-white text-xs font-semibold px-3 py-1 flex-shrink-0 truncate" style="background: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); border-radius: 9999px;">${value}</span>
          </div>
        `;
      }
    }
    // Number, Boolean, or Array
    else {
      const formattedTitle = key.replace(/_/g, " ").replace(/-/g, " ").toUpperCase();
      sectionsHtml += `
        <div class="p-3.5 sm:p-4 flex items-center justify-between gap-3" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.7); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 18px;">
          <div class="min-w-0">
            <span class="text-xs font-bold text-white uppercase block truncate">${formattedTitle}</span>
            <span class="text-[10px] font-mono block truncate" style="color: var(--theme-accent, #D0BCFF); opacity: 0.85;">${key}</span>
          </div>
          <span class="font-mono text-white text-xs font-semibold px-3 py-1 flex-shrink-0 truncate" style="background: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); border-radius: 9999px;">${JSON.stringify(value)}</span>
        </div>
      `;
    }
  }

  return `
    <div class="space-y-4">
      <div class="flex items-center justify-between gap-2 pb-2">
        <span class="text-xs font-semibold flex items-center gap-1.5 truncate" style="color: var(--theme-accent, #D0BCFF);">
          <i class="fa-solid fa-check-double text-emerald-400 flex-shrink-0"></i>
          <span>${Object.keys(attributes).length} ${typeof t === 'function' ? t("user_detail_properties_count", "Proprietà") : "Proprietà"}</span>
        </span>
        <button type="button" onclick="toggleRawJsonView()" class="m3-btn-outline text-xs py-1.5 px-3 flex items-center gap-1.5 flex-shrink-0 cursor-pointer" style="border-radius: 9999px !important;">
          <i class="fa-solid fa-code text-xs"></i>
          <span id="toggle-json-label">${typeof t === 'function' ? t("user_detail_show_raw_json", "Mostra JSON Grezzo") : "Mostra JSON Grezzo"}</span>
        </button>
      </div>
      
      <div class="space-y-3">
        ${sectionsHtml}
      </div>

      <div id="raw-json-container" style="display: none;" class="mt-4">
        <div class="flex items-center justify-between pb-2 mb-1">
          <span class="text-[11px] font-mono font-semibold uppercase tracking-wider" style="color: var(--theme-accent, #D0BCFF);">
            <i class="fa-solid fa-terminal mr-1"></i> ${typeof t === 'function' ? t("user_detail_json_source", "Codice Sorgente JSON") : "Codice Sorgente JSON"}
          </span>
          <button type="button" class="m3-btn-outline text-xs py-1 px-2.5 cursor-pointer" onclick="navigator.clipboard.writeText(document.getElementById('raw-json-pre').innerText).then(() => sweetalert('success', 'Copiato', 'JSON copiato negli appunti!'))">
            <i class="fa-solid fa-copy mr-1"></i> ${typeof t === 'function' ? t("user_detail_copy_btn", "Copia") : "Copia"}
          </button>
        </div>
        <pre id="raw-json-pre" class="text-xs font-mono p-4 overflow-x-auto" style="background: rgba(var(--theme-surface-rgb, 14, 11, 20), 0.85); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 16px; color: var(--theme-accent, #D0BCFF);">${rawJson}</pre>
      </div>
    </div>
  `;
}

function togglePasswordVisibility(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const icon = btn.querySelector("i");
  if (input.type === "password") {
    input.type = "text";
    if (icon) icon.className = "fa-solid fa-eye-slash text-xs";
  } else {
    input.type = "password";
    if (icon) icon.className = "fa-solid fa-eye text-xs";
  }
}

function copyTokenValue(inputId) {
  const input = document.getElementById(inputId);
  if (!input) return;
  navigator.clipboard.writeText(input.value).then(() => {
    sweetalert("success", "Copiato", "Valore copiato negli appunti!");
  });
}

function toggleRawJsonView() {
  const container = document.getElementById("raw-json-container");
  const label = document.getElementById("toggle-json-label");
  if (!container) return;
  if (container.style.display === "none") {
    container.style.display = "block";
    if (label) label.innerText = typeof t === 'function' ? t("user_detail_hide_raw_json", "Nascondi JSON Grezzo") : "Nascondi JSON Grezzo";
  } else {
    container.style.display = "none";
    if (label) label.innerText = typeof t === 'function' ? t("user_detail_show_raw_json", "Mostra JSON Grezzo") : "Mostra JSON Grezzo";
  }
}

let currentUserData = null;

// Re-render user details when language changes dynamically
window.addEventListener("languageChanged", () => {
  if (currentUserData) {
    displayUserData(currentUserData);
  }
});

function displayUserData(user) {
  currentUserData = user;
  const container = document.getElementById("user-data");
  if (!container) return;
  container.innerHTML = "";

  if (!user) {
    container.innerHTML = `<div class="m3-card p-8 text-center text-gray-400">${typeof t === 'function' ? t("common_no_data", "Nessun dato utente disponibile.") : "Nessun dato utente disponibile."}</div>`;
    return;
  }

  const photo = getOrDefault(user.profilePhoto, "https://bootdey.com/img/Content/avatar/avatar7.png");
  const isBlocked = !!user.blocked;
  const isConfirmed = user.confirmed !== false;
  const statusBadge = isBlocked 
    ? `<span class="px-3.5 py-1 text-xs font-semibold" style="background: rgba(239, 68, 68, 0.2); color: #FCA5A5; border: 1px solid rgba(239, 68, 68, 0.35); border-radius: 9999px;">${typeof t === 'function' ? t("user_detail_blocked", "BLOCCATO") : "BLOCCATO"}</span>` 
    : `<span class="px-3.5 py-1 text-xs font-semibold" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.35); border-radius: 9999px;">${typeof t === 'function' ? t("user_detail_active", "ATTIVO") : "ATTIVO"}</span>`;

  let rolesHtml = `<span class="text-xs text-gray-400">${typeof t === 'function' ? t("user_detail_no_roles", "Nessun ruolo assegnato") : "Nessun ruolo assegnato"}</span>`;
  if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
    rolesHtml = user.roles.map(r => `
      <span class="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-mono font-semibold mr-2 mb-2 shadow-sm" style="background: rgba(var(--theme-primary-rgb, 168, 85, 247), 0.18); color: var(--theme-accent, #EADDFF); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.25); border-radius: 9999px;">
        <i class="fa-solid fa-shield-halved text-[11px]" style="color: var(--theme-accent, #A855F7);"></i>
        ${r}
      </span>
    `).join("");
  }

  const attributesHtml = renderCustomAttributesData(user.attributes);

  const isMfaActive = !!(user.mfaSettings && (user.mfaSettings.enabled === true || user.mfaSettings.mfaEnabled === true || user.mfaSettings.enabled === "true" || user.mfaSettings.mfaEnabled === "true"));
  const mfaMethods = (user.mfaSettings && Array.isArray(user.mfaSettings.mfaMethods)) ? user.mfaSettings.mfaMethods : [];
  const hasMfaMethods = mfaMethods.length > 0;

  const mfaStatus = isMfaActive 
    ? `<span class="px-3 py-1 text-xs font-semibold" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.35); border-radius: 9999px;">${typeof t === 'function' ? t("user_detail_active_f", "ATTIVA") : "ATTIVA"}</span>` 
    : `<span class="px-3 py-1 text-xs font-semibold" style="background: rgba(255, 255, 255, 0.08); color: #9CA3AF; border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 9999px;">${typeof t === 'function' ? t("user_detail_disabled_f", "DISATTIVATA") : "DISATTIVATA"}</span>`;

  let mfaMethodsHtml = "";
  if (hasMfaMethods) {
    mfaMethodsHtml = `
      <div class="space-y-3">
        ${mfaMethods.map(m => `
          <div class="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.7); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 20px;">
            <div class="flex items-center gap-3.5">
              <div class="w-11 h-11 rounded-full flex items-center justify-center shrink-0 shadow-inner" style="background: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.1); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.2); color: var(--theme-accent, #D0BCFF);">
                ${getMfaIcon(m.label)}
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-sm font-bold text-white">${formatMfaLabel(m.label)}</span>
                  <span class="text-[10px] font-mono px-2 py-0.5 rounded-full" style="background: rgba(var(--theme-primary-rgb, 168, 85, 247), 0.15); color: var(--theme-accent, #D0BCFF); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.2);">${m.type || 'TOTP'}</span>
                </div>
                <div class="text-xs text-gray-400 mt-1 flex flex-wrap items-center gap-2">
                  <span>${typeof t === 'function' ? t("user_detail_state", "Stato:") : "Stato:"}</span>
                  ${m.confirmed !== false
                    ? `<span class="text-emerald-300 font-semibold flex items-center gap-1"><i class="fa-solid fa-circle-check text-[11px]"></i> ${typeof t === 'function' ? t("user_detail_state_confirmed", "Confermato") : "Confermato"}</span>`
                    : `<span class="text-amber-300 font-semibold flex items-center gap-1"><i class="fa-solid fa-clock text-[11px]"></i> ${typeof t === 'function' ? t("user_detail_state_pending", "In attesa di verifica") : "In attesa di verifica"}</span>`}
                  ${m.creationDate ? `<span class="text-gray-500">• ${typeof t === 'function' ? t("user_detail_configured_on", "Configurato il") : "Configurato il"} ${typeof formatDateIntl === 'function' ? formatDateIntl(m.creationDate) : m.creationDate}</span>` : ''}
                </div>
              </div>
            </div>

            <div class="flex items-center justify-end gap-2.5">
              ${m.confirmed === false ? `
                <button type="button" onclick="finishMfaVerification('${user.identifier}', '${(m.label || '').replace(/'/g, "\\'")}', '${m.type || 'totp'}')" class="m3-btn-primary text-xs py-1.5 px-3.5 flex items-center gap-1.5 shadow-md cursor-pointer" style="border-radius: 9999px !important; color: #FFFFFF !important;" title="Inserisci il codice OTP per completare la verifica">
                  <i class="fa-solid fa-circle-check text-xs"></i> <span>${typeof t === 'function' ? t("user_detail_complete_verify", "Completa Verifica") : "Completa Verifica"}</span>
                </button>
              ` : ''}
              <button type="button" onclick="deleteMfaMethod('${user.identifier}', '${(m.label || '').replace(/'/g, "\\'")}')" class="m3-icon-btn m3-icon-btn-danger" title="Elimina Metodo MFA">
                <i class="fa-solid fa-trash text-xs"></i>
              </button>
            </div>
          </div>
        `).join("")}
      </div>
    `;
  } else {
    mfaMethodsHtml = `
      <div class="p-6 text-center rounded-2xl" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.5); border: 1px dashed rgba(var(--theme-accent-rgb, 208, 188, 255), 0.2);">
        <i class="fa-solid fa-shield-virus text-3xl mb-2 block" style="color: var(--theme-accent, #A855F7); opacity: 0.5;"></i>
        <p class="text-sm text-gray-300 font-medium m-0">${typeof t === 'function' ? t("user_detail_no_mfa", "Nessun metodo di autenticazione a due fattori configurato.") : "Nessun metodo di autenticazione a due fattori configurato."}</p>
        <p class="text-xs text-gray-400 mt-1 mb-4">${typeof t === 'function' ? t("user_detail_no_mfa_desc", "Aggiungi un'app di autenticazione (es. Google o Microsoft Authenticator) per rafforzare la sicurezza dell'account.") : "Aggiungi un'app di autenticazione (es. Google o Microsoft Authenticator) per rafforzare la sicurezza dell'account."}</p>
        <a href="/app/mfa/${encodeURIComponent(user.identifier)}" class="m3-btn-primary text-xs py-2 px-5 inline-flex items-center gap-2 text-decoration-none shadow-md" style="border-radius: 9999px !important; color: #FFFFFF !important;">
          <i class="fa-solid fa-qrcode"></i> <span style="color: #FFFFFF !important;">${typeof t === 'function' ? t("user_detail_setup_mfa", "Configura MFA Ora") : "Configura MFA Ora"}</span>
        </a>
      </div>
    `;
  }

  container.innerHTML = `
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 w-full">
      
      <!-- Colonna Sinistra: Profilo Sintetico (M3 Card) -->
      <div class="m3-card p-5 sm:p-8 flex flex-col items-center text-center h-fit">
        <div class="relative mb-6">
          <img
            src="${photo}"
            alt="${user.username}"
            class="w-36 h-36 rounded-full object-cover shadow-2xl"
            style="border: 4px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.35);"
          />
          <span class="absolute bottom-2 right-2 w-5 h-5 rounded-full ${isBlocked ? 'bg-red-500' : 'bg-emerald-400'}" style="border: 2px solid #171324;" title="${isBlocked ? (typeof t === 'function' ? t('user_detail_blocked', 'Bloccato') : 'Bloccato') : (typeof t === 'function' ? t('user_detail_active', 'Attivo') : 'Attivo')}"></span>
        </div>

        <h3 class="text-2xl font-bold text-white mb-1">${user.name || ""} ${user.surname || ""}</h3>
        <p class="text-sm font-mono mb-3" style="color: var(--theme-accent, #D0BCFF);">@${user.username || ""}</p>
        <div class="mb-6">${statusBadge}</div>

        <!-- Box UUID ed Email SENZA BORDO BIANCO -->
        <div class="w-full p-4 text-left text-xs text-gray-300 space-y-3 mb-6" style="background: rgba(var(--theme-surface-rgb, 26, 20, 42), 0.7); border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15); border-radius: 20px;">
          <div>
            <span class="block text-[11px] uppercase font-semibold mb-0.5" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_detail_uuid", "UUID Utente") : "UUID Utente"}</span>
            <span class="font-mono text-xs break-all select-all block leading-relaxed" style="color: var(--m3-on-surface, #FFFFFF); opacity: 0.9;">${user.identifier || "N/D"}</span>
          </div>
          <div style="border-top: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.12); padding-top: 10px;">
            <span class="block text-[11px] uppercase font-semibold mb-0.5" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_email", "Email") : "Email"}</span>
            <span class="text-white text-xs break-all select-all block leading-relaxed">${user.email || "N/D"}</span>
          </div>
          <div class="flex items-center justify-between" style="border-top: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.12); padding-top: 10px;">
            <span class="text-[11px] uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_detail_confirmed", "Account Confermato") : "Account Confermato"}</span>
            <span class="${isConfirmed ? 'text-emerald-300' : 'text-yellow-300'} font-semibold text-xs">${isConfirmed ? (typeof t === 'function' ? t("user_detail_yes", "Sì") : "Sì") : (typeof t === 'function' ? t("user_detail_waiting", "In attesa") : "In attesa")}</span>
          </div>
        </div>

        <!-- Bottoni Azione M3: Tutti Arrotondati (Pill), Testo Bianco, Stesso Font Size e Spessore identici -->
        <div class="space-y-2.5 w-full">
          <a href="/app/users/edit/${encodeURIComponent(user.identifier)}" class="m3-btn-primary m3-action-pill w-full justify-center py-2.5 px-4 text-decoration-none shadow-lg hover:brightness-110 transition-all" style="border-radius: 9999px !important; color: #FFFFFF !important; font-size: 0.88rem !important; font-weight: 600 !important;">
            <i class="fa-solid fa-user-pen text-sm"></i>
            <span style="color: #FFFFFF !important; font-size: 0.88rem !important; font-weight: 600 !important;">${typeof t === 'function' ? t("user_detail_edit_profile", "Modifica Profilo") : "Modifica Profilo"}</span>
          </a>
          
          <div class="grid grid-cols-2 gap-2.5 w-full">
            <a href="/app/users/roles/${encodeURIComponent(user.identifier)}" class="m3-action-pill py-2.5 px-3 text-center text-decoration-none shadow-sm hover:brightness-110 transition-all" style="background: rgba(var(--theme-primary-rgb, 168, 85, 247), 0.18); color: #FFFFFF !important; border: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.3); border-radius: 9999px !important; font-size: 0.88rem !important; font-weight: 600 !important;">
              <i class="fa-solid fa-shield-halved text-sm" style="color: var(--theme-accent, #D0BCFF);"></i>
              <span style="color: #FFFFFF !important; font-size: 0.88rem !important; font-weight: 600 !important;">${typeof t === 'function' ? t("user_detail_roles_btn", "Ruoli") : "Ruoli"}</span>
            </a>
            
            ${isBlocked ? `
              <button onclick="toggleBlockUser('${user.identifier}','${(user.username || '').replace(/'/g, "\\'")}', false)" class="m3-action-pill py-2.5 px-3 text-center cursor-pointer shadow-sm hover:brightness-110 transition-all" style="background: rgba(16, 185, 129, 0.18); color: #FFFFFF !important; border: 1px solid rgba(52, 211, 153, 0.35); border-radius: 9999px !important; font-size: 0.88rem !important; font-weight: 600 !important;" title="${typeof t === 'function' ? t("user_detail_unlock_btn", "Sblocca") : "Sblocca"}">
                <i class="fa-solid fa-lock-open text-sm" style="color: #6EE7B7;"></i>
                <span style="color: #FFFFFF !important; font-size: 0.88rem !important; font-weight: 600 !important;">${typeof t === 'function' ? t("user_detail_unlock_btn", "Sblocca") : "Sblocca"}</span>
              </button>
            ` : `
              <button onclick="toggleBlockUser('${user.identifier}','${(user.username || '').replace(/'/g, "\\'")}', true)" class="m3-action-pill py-2.5 px-3 text-center cursor-pointer shadow-sm hover:brightness-110 transition-all" style="background: rgba(245, 158, 11, 0.18); color: #FFFFFF !important; border: 1px solid rgba(251, 191, 36, 0.35); border-radius: 9999px !important; font-size: 0.88rem !important; font-weight: 600 !important;" title="${typeof t === 'function' ? t("user_detail_lock_btn", "Blocca") : "Blocca"}">
                <i class="fa-solid fa-lock text-sm" style="color: #FCD34D;"></i>
                <span style="color: #FFFFFF !important; font-size: 0.88rem !important; font-weight: 600 !important;">${typeof t === 'function' ? t("user_detail_lock_btn", "Blocca") : "Blocca"}</span>
              </button>
            `}
          </div>

          <button onclick="deleteUser('${user.identifier}','${(user.username || '').replace(/'/g, "\\'")}')" class="m3-action-pill w-full py-2.5 px-4 text-center cursor-pointer shadow-sm hover:brightness-110 transition-all" style="background: rgba(239, 68, 68, 0.15); color: #FFFFFF !important; border: 1px solid rgba(239, 68, 68, 0.35); border-radius: 9999px !important; font-size: 0.88rem !important; font-weight: 600 !important;" title="${typeof t === 'function' ? t("user_detail_delete_btn", "Elimina") : "Elimina"}">
            <i class="fa-solid fa-trash text-sm" style="color: #F87171;"></i>
            <span style="color: #FFFFFF !important; font-size: 0.88rem !important; font-weight: 600 !important;">${typeof t === 'function' ? t("user_detail_delete_btn", "Elimina") : "Elimina"}</span>
          </button>
        </div>

      </div>

      <!-- Colonna Destra: Dettagli Anagrafici Completi -->
      <div class="lg:col-span-2 space-y-8">
        
        <div class="m3-card p-5 sm:p-8">
          <h4 class="text-xl font-bold text-white mb-6 flex items-center gap-2.5 pb-4" style="border-bottom: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15);">
            <i class="fa-solid fa-id-card" style="color: var(--theme-accent, #A855F7);"></i> ${typeof t === 'function' ? t("user_detail_full_info", "Informazioni Anagrafiche Complete") : "Informazioni Anagrafiche Complete"}
          </h4>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_name", "Nome") : "Nome"}</span>
              <div class="text-white font-medium text-base mt-1">${user.name || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_surname", "Cognome") : "Cognome"}</span>
              <div class="text-white font-medium text-base mt-1">${user.surname || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_username", "Username") : "Username"}</span>
              <div class="font-medium text-base mt-1 font-mono" style="color: var(--theme-accent, #D0BCFF);">@${user.username || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_email", "Email") : "Email"}</span>
              <div class="text-white font-medium text-base mt-1">${user.email || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_detail_phone", "Telefono") : "Telefono"}</span>
              <div class="text-white font-medium text-base mt-1 font-mono">${user.phoneNumber || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_birthdate", "Data di Nascita") : "Data di Nascita"}</span>
              <div class="text-white font-medium text-base mt-1">${user.birthDate || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_gender", "Genere") : "Genere"}</span>
              <div class="text-white font-medium text-base mt-1">${user.gender || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_nationality", "Nazionalità") : "Nazionalità"}</span>
              <div class="text-white font-medium text-base mt-1">${user.nationality || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_ssn", "Codice Fiscale / SSN") : "Codice Fiscale / SSN"}</span>
              <div class="text-white font-medium text-base mt-1 font-mono">${user.ssn || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_occupation", "Occupazione") : "Occupazione"}</span>
              <div class="text-white font-medium text-base mt-1">${user.occupation || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_field_education", "Istruzione") : "Istruzione"}</span>
              <div class="text-white font-medium text-base mt-1">${user.education || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: var(--theme-accent, #C4B5FD);">${typeof t === 'function' ? t("user_detail_mfa_sec", "Sicurezza 2FA (MFA)") : "Sicurezza 2FA (MFA)"}</span>
              <div class="mt-1">${mfaStatus}</div>
            </div>
          </div>
        </div>

        <!-- Card Ruoli Applicativi (RBAC) -->
        <div class="m3-card p-5 sm:p-8">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-4 mb-4 gap-3" style="border-bottom: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15);">
            <h4 class="text-xl font-bold text-white flex items-center gap-2.5 m-0">
              <i class="fa-solid fa-user-shield" style="color: var(--theme-accent, #A855F7);"></i> ${typeof t === 'function' ? t("user_detail_rbac_title", "Ruoli Applicativi (RBAC)") : "Ruoli Applicativi (RBAC)"}
            </h4>
            <a href="/app/users/roles/${user.identifier}" class="m3-btn-primary text-xs py-2 px-5 text-decoration-none shadow-md w-full sm:w-auto justify-center" style="border-radius: 9999px !important; color: #FFFFFF !important;">
              <i class="fa-solid fa-pen-to-square mr-1"></i> <span style="color: #FFFFFF !important;">${typeof t === 'function' ? t("user_detail_manage_roles", "Gestisci Ruoli") : "Gestisci Ruoli"}</span>
            </a>
          </div>
          <div class="flex flex-wrap pt-2">
            ${rolesHtml}
          </div>
        </div>

        <!-- Card Autenticazione a Due Fattori (MFA) -->
        <div class="m3-card p-5 sm:p-8">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-4 mb-6 gap-3" style="border-bottom: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15);">
            <div class="flex items-center justify-between sm:justify-start gap-3 w-full sm:w-auto">
              <h4 class="text-xl font-bold text-white flex items-center gap-2.5 m-0">
                <i class="fa-solid fa-shield-halved" style="color: var(--theme-accent, #A855F7);"></i> ${typeof t === 'function' ? t("user_detail_mfa_title", "Autenticazione a Due Fattori (MFA)") : "Autenticazione a Due Fattori (MFA)"}
              </h4>
              ${isMfaActive
                ? `<span class="px-3 py-1 text-xs font-semibold rounded-full flex-shrink-0" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.35);">${typeof t === 'function' ? t("user_detail_active_f", "ATTIVA") : "ATTIVA"}</span>`
                : `<span class="px-3 py-1 text-xs font-semibold rounded-full flex-shrink-0" style="background: rgba(255, 255, 255, 0.08); color: #9CA3AF; border: 1px solid rgba(255, 255, 255, 0.15);">${typeof t === 'function' ? t("user_detail_disabled_f", "DISATTIVATA") : "DISATTIVATA"}</span>`}
            </div>
            <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 w-full sm:w-auto">
              ${isMfaActive ? `
                <button type="button" onclick="toggleMfaStatus('${user.identifier}', false)" class="m3-btn-outline m3-btn-warning-outline text-xs cursor-pointer justify-center flex items-center gap-1.5 py-2 px-3.5" title="${typeof t === 'function' ? t("user_detail_disable_mfa", "Disattiva MFA") : "Disattiva MFA"}">
                  <i class="fa-solid fa-power-off text-xs" style="color: #FCD34D !important;"></i> ${typeof t === 'function' ? t("user_detail_disable_mfa", "Disattiva MFA") : "Disattiva MFA"}
                </button>
              ` : (hasMfaMethods ? `
                <button type="button" onclick="toggleMfaStatus('${user.identifier}', true)" class="m3-btn-outline m3-btn-success-outline text-xs cursor-pointer justify-center flex items-center gap-1.5 py-2 px-3.5" title="${typeof t === 'function' ? t("user_detail_enable_mfa", "Attiva MFA") : "Attiva MFA"}">
                  <i class="fa-solid fa-toggle-on text-xs" style="color: #6EE7B7 !important;"></i> ${typeof t === 'function' ? t("user_detail_enable_mfa", "Attiva MFA") : "Attiva MFA"}
                </button>
              ` : '')}
              <a href="/app/mfa/${encodeURIComponent(user.identifier)}" class="m3-btn-primary text-xs py-2 px-4 text-decoration-none shadow-md justify-center flex items-center gap-1.5" style="border-radius: 9999px !important; color: #FFFFFF !important;">
                <i class="fa-solid fa-plus text-xs"></i> <span style="color: #FFFFFF !important;">${typeof t === 'function' ? t("user_detail_config_method", "Configura Metodo") : "Configura Metodo"}</span>
              </a>
            </div>
          </div>

          <!-- Elenco Metodi MFA Configurati -->
          ${mfaMethodsHtml}
        </div>

        <!-- Card Attributi Custom Trasformati in Dati Visivi -->
        <div class="m3-card p-5 sm:p-8">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-4 mb-5 gap-3" style="border-bottom: 1px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.15);">
            <h4 class="text-xl font-bold text-white flex items-center gap-2.5 m-0">
              <i class="fa-solid fa-sliders" style="color: var(--theme-accent, #A855F7);"></i> ${typeof t === 'function' ? t("user_detail_custom_attributes", "Attributi Personalizzati") : "Attributi Personalizzati"}
            </h4>
            <a href="/app/users/edit/${encodeURIComponent(user.identifier)}" class="m3-btn-outline text-xs w-full sm:w-auto justify-center py-2 px-3.5 text-decoration-none" style="color: var(--theme-accent, #D0BCFF) !important; border-color: rgba(var(--theme-accent-rgb, 208, 188, 255), 0.3) !important;">
              <i class="fa-solid fa-pen-to-square mr-1"></i> <span style="color: #FFFFFF !important;">${typeof t === 'function' ? t("user_detail_edit_attributes", "Modifica Attributi") : "Modifica Attributi"}</span>
            </a>
          </div>
          <div>
            ${attributesHtml}
          </div>
        </div>

      </div>

    </div>
  `;
}

function toggleBlockUser(identifier, username, block) {
  const actionTitle = block 
    ? (typeof t === 'function' ? t("swal_user_blocked_title", "Conferma Blocco") : "Conferma Blocco") 
    : (typeof t === 'function' ? t("swal_user_unblocked_title", "Conferma Sblocco") : "Conferma Sblocco");
  const actionText = block
    ? (typeof t === 'function' ? t("swal_block_user_confirm", `Sei sicuro di voler bloccare l'utente @${username}? L'utente non potrà più accedere al sistema fino al successivo sblocco.`).replace("#USERNAME#", username) : `Sei sicuro di voler bloccare l'utente @${username}? L'utente non potrà più accedere al sistema fino al successivo sblocco.`)
    : (typeof t === 'function' ? t("swal_unblock_user_confirm", `Sei sicuro di voler sbloccare l'utente @${username}? L'utente potrà nuovamente accedere alle applicazioni del sistema.`).replace("#USERNAME#", username) : `Sei sicuro di voler sbloccare l'utente @${username}? L'utente potrà nuovamente accedere alle applicazioni del sistema.`);
  const confirmBtnText = block 
    ? (typeof t === 'function' ? t("swal_block_confirm_btn", "Sì, blocca") : "Sì, blocca") 
    : (typeof t === 'function' ? t("swal_unblock_confirm_btn", "Sì, sblocca") : "Sì, sblocca");
  const cancelBtnText = (typeof t === 'function' ? t("btn_cancel", "Annulla") : "Annulla");

  const confirmPromise = typeof sweetalertConfirm === 'function'
    ? sweetalertConfirm("warning", actionTitle, actionText, confirmBtnText, cancelBtnText)
    : (typeof Swal !== 'undefined' ? Swal.fire({
        icon: "warning",
        title: actionTitle,
        text: actionText,
        showCancelButton: true,
        confirmButtonText: confirmBtnText,
        cancelButtonText: cancelBtnText
      }) : Promise.resolve({ isConfirmed: confirm(actionText) }));

  confirmPromise.then((res) => {
    if (res.isConfirmed) {
      const url = config.users_url + "/" + encodeURIComponent(identifier) + "?block=" + (block ? "true" : "false");
      const token = getCookieOrStorage(config.access_token);
      const patchFn = typeof PATCH !== 'undefined' ? PATCH : (typeof api !== 'undefined' && api.PATCH ? api.PATCH : null);

      if (!patchFn) {
        sweetalert("error", (typeof t === 'function' ? t("swal_error_title", "Errore") : "Errore"), "Funzione PATCH non disponibile.");
        return;
      }

      patchFn(url, token).then(async (data) => {
        const responseData = await data.json().catch(() => ({}));
        if (!data.ok || responseData.error != null) {
          const errMsg = responseData.error?.message || responseData.message || (block ? "Impossibile bloccare l'utente." : "Impossibile sbloccare l'utente.");
          sweetalert("error", block ? "Errore Blocco" : "Errore Sblocco", errMsg);
        } else {
          try {
            localStorage.removeItem(config.client_id + "_usersData");
            sessionStorage.removeItem("accesssphere_users_data");
          } catch (e) {}

          const successTitle = block 
            ? (typeof t === 'function' ? t("swal_user_blocked_title", "Utente Bloccato") : "Utente Bloccato") 
            : (typeof t === 'function' ? t("swal_user_unblocked_title", "Utente Sbloccato") : "Utente Sbloccato");
          const successMsg = block
            ? (typeof t === 'function' ? t("swal_user_blocked_desc", `L'utente @${username} è stato bloccato con successo.`).replace("#USERNAME#", username) : `L'utente @${username} è stato bloccato con successo.`)
            : (typeof t === 'function' ? t("swal_user_unblocked_desc", `L'utente @${username} è stato sbloccato con successo.`).replace("#USERNAME#", username) : `L'utente @${username} è stato sbloccato con successo.`);

          sweetalert("success", successTitle, successMsg).then(() => {
            window.location.reload();
          });
        }
      }).catch(err => {
        console.error("Error toggling user block status:", err);
        sweetalert("error", (typeof t === 'function' ? t("swal_error_title", "Errore") : "Errore"), (typeof t === 'function' ? t("swal_connection_error", "Errore di connessione durante l'operazione.") : "Errore di connessione durante l'operazione."));
      });
    }
  });
}

function deleteUser(identifier, username) {
  const title = (typeof t === 'function' ? t("swal_delete_user_confirm_title", "Conferma Eliminazione") : "Conferma Eliminazione");
  const text = (typeof t === 'function' ? t("swal_delete_user_confirm_desc", `Sei sicuro di voler eliminare definitivamente l'utente @${username}? L'operazione non è reversibile.`).replace("#USERNAME#", username) : `Sei sicuro di voler eliminare definitivamente l'utente @${username}? L'operazione non è reversibile.`);
  const confirmBtn = (typeof t === 'function' ? t("btn_delete_confirm", "Sì, elimina") : "Sì, elimina");
  const cancelBtn = (typeof t === 'function' ? t("btn_cancel", "Annulla") : "Annulla");

  const confirmPromise = typeof sweetalertConfirm === 'function'
    ? sweetalertConfirm("warning", title, text, confirmBtn, cancelBtn)
    : sweetalert("warning", title, text, true);

  confirmPromise.then((res) => {
    if (res.isConfirmed) {
      const url = config.users_url + "/" + encodeURIComponent(identifier);
      const token = getCookieOrStorage(config.access_token);
      DELETE(url, token).then(async (data) => {
        const responseData = await data.json().catch(() => ({}));
        if (!data.ok || responseData.error != null) {
          sweetalert("error", (typeof t === 'function' ? t("swal_error_title", "Errore Eliminazione") : "Errore Eliminazione"), responseData.error?.message || responseData.message || "Impossibile eliminare l'utente.");
        } else {
          try {
            localStorage.removeItem(config.client_id + "_usersData");
            sessionStorage.removeItem("accesssphere_users_data");
          } catch (e) {}
          const successTitle = (typeof t === 'function' ? t("swal_user_deleted_title", "Utente Eliminato") : "Utente Eliminato");
          const successDesc = (typeof t === 'function' ? t("swal_user_deleted_desc", `L'utente @${username} è stato rimosso.`).replace("#USERNAME#", username) : `L'utente @${username} è stato rimosso.`);
          sweetalert("success", successTitle, successDesc).then(() => {
            window.location.href = "/app/users";
          });
        }
      }).catch(err => {
        console.error("Error deleting user:", err);
        sweetalert("error", (typeof t === 'function' ? t("swal_error_title", "Errore") : "Errore"), (typeof t === 'function' ? t("swal_connection_error", "Errore di connessione durante l'operazione.") : "Errore di connessione durante l'operazione."));
      });
    }
  });
}

function getMfaIcon(label) {
  const l = (label || "").toLowerCase();
  if (l.includes("google")) return '<i class="fa-brands fa-google text-red-400 text-lg"></i>';
  if (l.includes("microsoft")) return '<i class="fa-brands fa-microsoft text-blue-400 text-lg"></i>';
  if (l.includes("authy")) return '<i class="fa-solid fa-key text-red-400 text-lg"></i>';
  if (l.includes("1password") || l.includes("onepassword")) return '<i class="fa-solid fa-lock text-cyan-400 text-lg"></i>';
  if (l.includes("bitwarden")) return '<i class="fa-solid fa-shield-halved text-blue-400 text-lg"></i>';
  return '<i class="fa-solid fa-mobile-screen-button text-purple-400 text-lg"></i>';
}

function formatMfaLabel(label) {
  if (!label) return "Authenticator App";
  const map = {
    "google-authenticator": "Google Authenticator",
    "microsoft-authenticator": "Microsoft Authenticator",
    "authy": "Twilio Authy",
    "lastpass-authenticator": "LastPass Authenticator",
    "duo-mobile": "Duo Mobile",
    "free-otp": "FreeOTP",
    "aegis": "Aegis Authenticator",
    "and-otp": "andOTP",
    "1password": "1Password",
    "bitwarden": "Bitwarden",
    "keepass": "KeePass",
    "enpass": "Enpass",
    "dashlane": "Dashlane"
  };
  return map[label.toLowerCase()] || label.replace(/[-_]/g, " ").replace(/\b\w/g, l => l.toUpperCase());
}

function toggleMfaStatus(identifier, enable) {
  const title = enable 
    ? (typeof t === "function" ? t("swal_enable_mfa_title", "Attiva Autenticazione a Due Fattori") : "Attiva Autenticazione a Due Fattori")
    : (typeof t === "function" ? t("swal_disable_mfa_title", "Disattiva Autenticazione a Due Fattori") : "Disattiva Autenticazione a Due Fattori");
  const text = enable
    ? (typeof t === "function" ? t("swal_enable_mfa_desc", "Sei sicuro di voler abilitare l'autenticazione a due fattori (MFA) per questo utente?") : "Sei sicuro di voler abilitare l'autenticazione a due fattori (MFA) per questo utente?")
    : (typeof t === "function" ? t("swal_disable_mfa_desc", "Sei sicuro di voler disattivare l'autenticazione a due fattori (MFA)? L'accesso sarà protetto unicamente dalla password.") : "Sei sicuro di voler disattivare l'autenticazione a due fattori (MFA)? L'accesso sarà protetto unicamente dalla password.");
  const confirmBtn = enable 
    ? (typeof t === "function" ? t("swal_enable_mfa_btn", "Sì, attiva MFA") : "Sì, attiva MFA") 
    : (typeof t === "function" ? t("swal_disable_mfa_btn", "Sì, disattiva MFA") : "Sì, disattiva MFA");
  const denyBtn = (typeof t === "function" ? t("btn_cancel", "Annulla") : "Annulla");

  const confirmPromise = typeof sweetalertConfirm === "function"
    ? sweetalertConfirm("warning", title, text, confirmBtn, denyBtn)
    : (typeof Swal !== "undefined"
        ? Swal.fire({
            icon: "warning",
            title: title,
            text: text,
            showCancelButton: true,
            confirmButtonText: confirmBtn,
            cancelButtonText: denyBtn
          })
        : Promise.resolve({ isConfirmed: confirm(text) }));

  confirmPromise.then((res) => {
    if (res.isConfirmed) {
      const url = window.location.origin + "/v1/mfa/manage";
      const token = getCookieOrStorage(config.access_token);
      const body = {
        identifier: identifier,
        action: enable ? "ENABLE" : "DISABLE"
      };

      const postFn = typeof POST !== "undefined" ? POST : (typeof api !== "undefined" && api.POST ? api.POST : null);
      if (!postFn) {
        sweetalert("error", (typeof t === "function" ? t("swal_error_title", "Errore") : "Errore"), "Funzione POST non disponibile.");
        return;
      }

      postFn(url, token, body).then(async (data) => {
        const responseData = await data.json().catch(() => ({}));
        if (!data.ok || responseData.error != null) {
          const errMsg = responseData.error?.message || responseData.message || (enable ? "Impossibile attivare l'MFA." : "Impossibile disattivare l'MFA.");
          sweetalert("error", enable ? (typeof t === "function" ? t("swal_error_title", "Errore") : "Errore Attivazione") : (typeof t === "function" ? t("swal_error_title", "Errore") : "Errore Disattivazione"), errMsg);
        } else {
          try {
            localStorage.removeItem(config.client_id + "_usersData");
            sessionStorage.removeItem("accesssphere_users_data");
          } catch (e) {}

          const successMsg = enable
            ? (typeof t === "function" ? t("swal_mfa_enabled_desc", "Autenticazione a due fattori attivata con successo.") : "Autenticazione a due fattori attivata con successo.")
            : (typeof t === "function" ? t("swal_mfa_disabled_desc", "Autenticazione a due fattori disattivata con successo.") : "Autenticazione a due fattori disattivata con successo.");

          const successTitle = enable 
            ? (typeof t === "function" ? t("swal_mfa_enabled_title", "MFA Attivata") : "MFA Attivata")
            : (typeof t === "function" ? t("swal_mfa_disabled_title", "MFA Disattivata") : "MFA Disattivata");

          sweetalert("success", successTitle, successMsg).then(() => {
            window.location.reload();
          });
        }
      }).catch(err => {
        console.error("Error toggling MFA status:", err);
        sweetalert("error", (typeof t === "function" ? t("swal_error_title", "Errore") : "Errore"), (typeof t === "function" ? t("swal_connection_error", "Errore di connessione durante l'operazione.") : "Errore di connessione durante l'operazione."));
      });
    }
  });
}

function deleteMfaMethod(identifier, label) {
  const formattedLabel = formatMfaLabel(label);
  const title = (typeof t === "function" ? t("swal_delete_mfa_title", "Elimina Metodo MFA") : "Elimina Metodo MFA");
  const text = (typeof t === "function" ? t("swal_delete_mfa_desc", `Sei sicuro di voler eliminare il metodo "${formattedLabel}"? Se non rimangono altri metodi configurati, l'MFA verrà automaticamente disattivata.`).replace("#LABEL#", formattedLabel) : `Sei sicuro di voler eliminare il metodo "${formattedLabel}"? Se non rimangono altri metodi configurati, l'MFA verrà automaticamente disattivata.`);
  const confirmBtn = (typeof t === "function" ? t("btn_delete_confirm", "Sì, elimina") : "Sì, elimina");
  const cancelBtn = (typeof t === "function" ? t("btn_cancel", "Annulla") : "Annulla");

  const confirmPromise = typeof sweetalertConfirm === "function"
    ? sweetalertConfirm("warning", title, text, confirmBtn, cancelBtn)
    : (typeof Swal !== "undefined"
        ? Swal.fire({
            icon: "warning",
            title: title,
            text: text,
            showCancelButton: true,
            confirmButtonText: confirmBtn,
            cancelButtonText: cancelBtn
          })
        : Promise.resolve({ isConfirmed: confirm("Sei sicuro di voler eliminare il metodo?") }));

  confirmPromise.then((res) => {
    if (res.isConfirmed) {
      const url = window.location.origin + "/v1/mfa/manage";
      const token = getCookieOrStorage(config.access_token);
      const body = {
        identifier: identifier,
        label: label,
        action: "DELETE"
      };

      const postFn = typeof POST !== "undefined" ? POST : (typeof api !== "undefined" && api.POST ? api.POST : null);
      if (!postFn) {
        sweetalert("error", "Errore", "Funzione POST non disponibile.");
        return;
      }

      postFn(url, token, body).then(async (data) => {
        const responseData = await data.json().catch(() => ({}));
        if (!data.ok || responseData.error != null) {
          const errMsg = responseData.error?.message || responseData.message || "Impossibile eliminare il metodo MFA.";
          sweetalert("error", "Errore Eliminazione", errMsg);
        } else {
          try {
            localStorage.removeItem(config.client_id + "_usersData");
            sessionStorage.removeItem("accesssphere_users_data");
          } catch (e) {}

          sweetalert("success", "Metodo Rimosso", `Il metodo "${formattedLabel}" è stato eliminato con successo.`).then(() => {
            window.location.reload();
          });
        }
      }).catch(err => {
        console.error("Error deleting MFA method:", err);
        sweetalert("error", "Errore", "Impossibile contattare il server.");
      });
    }
  });
}

function finishMfaVerification(identifier, label, type) {
  const formattedLabel = formatMfaLabel(label);
  const swalHtml = `
    <div style="text-align: left; color: #E2E8F0; font-size: 0.9rem; line-height: 1.5;">
      <p style="margin-bottom: 1rem;">
        Inserisci il codice temporaneo a 6 cifre generato dalla tua applicazione di autenticazione (<strong>${formattedLabel}</strong>) per completare la verifica e attivare il metodo.
      </p>
      <div style="display: flex; justify-content: center; margin: 1.25rem 0;">
        <input 
          type="text" 
          id="swal-mfa-otp" 
          maxlength="6" 
          placeholder="••••••" 
          pattern="\\d*" 
          inputmode="numeric" 
          autocomplete="one-time-code"
          style="width: 220px; text-align: center; font-family: monospace; font-size: 1.75rem; font-weight: 700; letter-spacing: 0.35em; background: var(--theme-input-bg, #1C172E); border: 1.5px solid rgba(var(--theme-accent-rgb, 208, 188, 255), 0.35); border-radius: 16px; color: #FFFFFF; padding: 10px 16px; outline: none;" 
        />
      </div>
      <p style="text-align: center; font-size: 0.75rem; color: var(--theme-accent, #A78BFA); margin-top: 0.5rem;">
        Non hai ancora scansionato il QR code? 
        <a href="/app/mfa/${encodeURIComponent(identifier)}" style="color: var(--theme-accent, #D0BCFF); text-decoration: underline; font-weight: 600;">Riconfigura da zero</a>
      </p>
    </div>
  `;

  if (typeof Swal !== "undefined") {
    Swal.fire({
      title: (typeof t === "function" ? t("swal_complete_mfa_title", "Completa Verifica MFA") : "Completa Verifica MFA"),
      html: swalHtml,
      showCancelButton: true,
      confirmButtonText: `<i class="fa-solid fa-circle-check" style="margin-right: 6px;"></i> ${typeof t === "function" ? t("swal_verify_and_activate_btn", "Verifica e Attiva") : "Verifica e Attiva"}`,
      cancelButtonText: (typeof t === "function" ? t("btn_cancel", "Annulla") : "Annulla"),
      background: "var(--theme-surface-container, #161124)",
      color: "#FFFFFF",
      confirmButtonColor: "var(--theme-primary, #7C3AED)",
      cancelButtonColor: "#4B5563",
      didOpen: () => {
        const input = document.getElementById("swal-mfa-otp");
        if (input) {
          input.focus();
          input.addEventListener("input", (e) => {
            e.target.value = e.target.value.replace(/\D/g, "").slice(0, 6);
          });
          input.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && input.value.length === 6) {
              Swal.clickConfirm();
            }
          });
        }
      },
      preConfirm: () => {
        const input = document.getElementById("swal-mfa-otp");
        const otp = input ? input.value.trim() : "";
        if (!otp || otp.length !== 6) {
          Swal.showValidationMessage("Inserisci un codice numerico valido di 6 cifre");
          return false;
        }
        return otp;
      }
    }).then((result) => {
      if (result.isConfirmed && result.value) {
        submitMfaConfirmation(identifier, label, type || "totp", result.value, formattedLabel);
      }
    });
  } else {
    const otp = prompt(`Inserisci il codice OTP a 6 cifre per ${formattedLabel}:`);
    if (otp && otp.trim().length === 6) {
      submitMfaConfirmation(identifier, label, type || "totp", otp.trim(), formattedLabel);
    }
  }
}

function submitMfaConfirmation(identifier, label, type, otp, formattedLabel) {
  const url = window.location.origin + "/v1/mfa/confirm";
  const token = getCookieOrStorage(config.access_token);
  const body = {
    identifier: identifier,
    label: label,
    type: type || "totp",
    otp: otp
  };

  const postFn = typeof POST !== "undefined" ? POST : (typeof api !== "undefined" && api.POST ? api.POST : null);
  if (!postFn) {
    sweetalert("error", "Errore", "Funzione POST non disponibile.");
    return;
  }

  postFn(url, token, body).then(async (data) => {
    const responseData = await data.json().catch(() => ({}));
    if (!data.ok || responseData.error != null) {
      const errMsg = responseData.error?.message || responseData.message || "Codice OTP errato o scaduto. Riprova.";
      sweetalert("error", "Verifica Non Riuscita", errMsg);
    } else {
      try {
        localStorage.removeItem(config.client_id + "_usersData");
        sessionStorage.removeItem("accesssphere_users_data");
      } catch (e) {}

      sweetalert("success", "Verifica Completata", `Il metodo "${formattedLabel}" è stato verificato e attivato con successo!`).then(() => {
        window.location.reload();
      });
    }
  }).catch(err => {
    console.error("Error confirming MFA:", err);
    sweetalert("error", "Errore", "Impossibile contattare il server per confermare l'MFA.");
  });
}

// Global exports
window.finishMfaVerification = finishMfaVerification;
window.deleteMfaMethod = deleteMfaMethod;
window.toggleMfaStatus = toggleMfaStatus;
window.toggleBlockUser = toggleBlockUser;
window.getUser = getUser;

