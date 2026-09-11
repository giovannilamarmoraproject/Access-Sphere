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
      <div class="p-6 text-center text-purple-300 flex items-center justify-center gap-3" style="background: rgba(26, 20, 42, 0.6); border: 1px solid rgba(208, 188, 255, 0.12); border-radius: 20px;">
        <i class="fa-solid fa-sliders text-purple-400 text-lg"></i>
        <span class="text-xs font-medium">Nessun attributo personalizzato presente per questo account.</span>
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
          valueDisplay = `<span class="px-3 py-1 font-mono font-bold text-xs" style="background: rgba(168, 85, 247, 0.2); color: #EADDFF; border: 1px solid rgba(208, 188, 255, 0.25); border-radius: 9999px;">${subVal}</span>`;
        } else if (subVal === "€" || subVal === "$") {
          valueDisplay = `<span class="px-3 py-1 font-bold font-mono text-sm" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 9999px;">${subVal}</span>`;
        } else if (subVal === "ACTIVE" || subVal === "COMPLETED" || subVal === "TRUE" || subVal === true) {
          valueDisplay = `<span class="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-semibold" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 9999px;"><i class="fa-solid fa-circle-check text-[10px]"></i> ${subVal}</span>`;
        } else if (subVal === "INACTIVE" || subVal === "PENDING" || subVal === false) {
          valueDisplay = `<span class="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-semibold" style="background: rgba(234, 179, 8, 0.2); color: #FDE047; border: 1px solid rgba(234, 179, 8, 0.3); border-radius: 9999px;"><i class="fa-solid fa-clock text-[10px]"></i> ${subVal}</span>`;
        } else {
          valueDisplay = `<span class="font-mono text-white text-xs font-medium">${subVal}</span>`;
        }

        subItemsHtml += `
          <div class="p-3.5 flex flex-col justify-between" style="background: rgba(14, 11, 20, 0.6); border: 1px solid rgba(208, 188, 255, 0.12); border-radius: 16px;">
            <span class="text-[11px] text-purple-300/80 uppercase font-semibold tracking-wider mb-1">${subLabel}</span>
            <div class="mt-1">${valueDisplay}</div>
          </div>
        `;
      }

      sectionsHtml += `
        <div class="p-5 space-y-4" style="background: rgba(26, 20, 42, 0.7); border: 1px solid rgba(208, 188, 255, 0.15); border-radius: 20px;">
          <div class="flex items-center justify-between pb-3" style="border-bottom: 1px solid rgba(208, 188, 255, 0.12);">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 flex items-center justify-center text-purple-300" style="background: rgba(168, 85, 247, 0.2); border: 1px solid rgba(208, 188, 255, 0.25); border-radius: 12px;">
                <i class="fa-solid fa-layer-group text-xs"></i>
              </div>
              <div>
                <span class="text-sm font-bold text-white block">${formattedTitle}</span>
                <span class="text-[10px] font-mono text-purple-300">${key}</span>
              </div>
            </div>
            <span class="px-3 py-1 font-mono text-[10px]" style="background: rgba(208, 188, 255, 0.15); color: #EADDFF; border: 1px solid rgba(208, 188, 255, 0.2); border-radius: 9999px;">Oggetto</span>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
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
          <div class="p-5 space-y-3" style="background: rgba(26, 20, 42, 0.7); border: 1px solid rgba(208, 188, 255, 0.15); border-radius: 20px;">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <i class="fa-solid fa-key text-purple-400 text-xs"></i>
                <span class="text-xs font-bold text-white uppercase">${formattedTitle}</span>
                <span class="text-[10px] font-mono text-purple-300">(${key})</span>
              </div>
              <span class="px-3 py-1 font-mono text-[10px]" style="background: rgba(245, 158, 11, 0.15); color: #FCD34D; border: 1px solid rgba(245, 158, 11, 0.25); border-radius: 9999px;">Token / Secret</span>
            </div>
            <div class="flex items-center gap-2 mt-2">
              <input type="password" id="${inputId}" readonly value="${safeVal}" class="form-control font-mono text-xs flex-1" style="background: rgba(14, 11, 20, 0.85); border: 1px solid rgba(208, 188, 255, 0.2); color: #fff; border-radius: 9999px; padding: 10px 18px;" />
              <button type="button" class="flex-shrink-0" onclick="togglePasswordVisibility('${inputId}', this)" title="Mostra / Nascondi" style="width: 38px; height: 38px; border-radius: 9999px; background: rgba(208, 188, 255, 0.12); border: 1px solid rgba(208, 188, 255, 0.25); color: #FFFFFF; display: inline-flex; align-items: center; justify-content: center; cursor: pointer;">
                <i class="fa-solid fa-eye text-xs"></i>
              </button>
              <button type="button" class="flex-shrink-0" onclick="copyTokenValue('${inputId}')" title="Copia negli Appunti" style="width: 38px; height: 38px; border-radius: 9999px; background: rgba(208, 188, 255, 0.12); border: 1px solid rgba(208, 188, 255, 0.25); color: #FFFFFF; display: inline-flex; align-items: center; justify-content: center; cursor: pointer;">
                <i class="fa-solid fa-copy text-xs"></i>
              </button>
            </div>
          </div>
        `;
      } else {
        sectionsHtml += `
          <div class="p-4 flex items-center justify-between" style="background: rgba(26, 20, 42, 0.7); border: 1px solid rgba(208, 188, 255, 0.15); border-radius: 20px;">
            <div>
              <span class="text-xs font-bold text-white uppercase block">${formattedTitle}</span>
              <span class="text-[10px] font-mono text-purple-300">${key}</span>
            </div>
            <span class="font-mono text-white text-xs font-semibold px-3 py-1" style="background: rgba(208, 188, 255, 0.15); border: 1px solid rgba(208, 188, 255, 0.25); border-radius: 9999px;">${value}</span>
          </div>
        `;
      }
    }
    // Number, Boolean, or Array
    else {
      const formattedTitle = key.replace(/_/g, " ").replace(/-/g, " ").toUpperCase();
      sectionsHtml += `
        <div class="p-4 flex items-center justify-between" style="background: rgba(26, 20, 42, 0.7); border: 1px solid rgba(208, 188, 255, 0.15); border-radius: 20px;">
          <div>
            <span class="text-xs font-bold text-white uppercase block">${formattedTitle}</span>
            <span class="text-[10px] font-mono text-purple-300">${key}</span>
          </div>
          <span class="font-mono text-white text-xs font-semibold px-3 py-1" style="background: rgba(208, 188, 255, 0.15); border: 1px solid rgba(208, 188, 255, 0.25); border-radius: 9999px;">${JSON.stringify(value)}</span>
        </div>
      `;
    }
  }

  return `
    <div class="space-y-4">
      <div class="flex items-center justify-between pb-2">
        <span class="text-xs text-purple-300 font-semibold flex items-center gap-2">
          <i class="fa-solid fa-check-double text-emerald-400"></i> ${Object.keys(attributes).length} Proprietà Configurate
        </span>
        <button type="button" onclick="toggleRawJsonView()" class="transition-all flex items-center gap-2 cursor-pointer" style="background: rgba(208, 188, 255, 0.12); color: #FFFFFF !important; border: 1px solid rgba(208, 188, 255, 0.25); border-radius: 9999px; padding: 7px 18px; font-size: 0.78rem; font-weight: 600;">
          <i class="fa-solid fa-code text-xs"></i> <span id="toggle-json-label" style="color: #FFFFFF !important;">Mostra JSON Grezzo</span>
        </button>
      </div>
      
      <div class="space-y-4">
        ${sectionsHtml}
      </div>

      <div id="raw-json-container" style="display: none;" class="mt-4">
        <pre class="text-xs font-mono text-purple-200 p-4 overflow-x-auto" style="background: rgba(14, 11, 20, 0.85); border: 1px solid rgba(208, 188, 255, 0.15); border-radius: 20px;">${rawJson}</pre>
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
    if (label) label.innerText = "Nascondi JSON Grezzo";
  } else {
    container.style.display = "none";
    if (label) label.innerText = "Mostra JSON Grezzo";
  }
}

function displayUserData(user) {
  const container = document.getElementById("user-data");
  if (!container) return;
  container.innerHTML = "";

  if (!user) {
    container.innerHTML = `<div class="m3-card p-8 text-center text-gray-400">Nessun dato utente disponibile.</div>`;
    return;
  }

  const photo = getOrDefault(user.profilePhoto, "https://bootdey.com/img/Content/avatar/avatar7.png");
  const isBlocked = !!user.blocked;
  const isConfirmed = user.confirmed !== false;
  const statusBadge = isBlocked 
    ? '<span class="px-3.5 py-1 text-xs font-semibold" style="background: rgba(239, 68, 68, 0.2); color: #FCA5A5; border: 1px solid rgba(239, 68, 68, 0.35); border-radius: 9999px;">BLOCCATO</span>' 
    : '<span class="px-3.5 py-1 text-xs font-semibold" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.35); border-radius: 9999px;">ATTIVO</span>';

  let rolesHtml = '<span class="text-xs text-gray-400">Nessun ruolo assegnato</span>';
  if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
    rolesHtml = user.roles.map(r => `
      <span class="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-mono font-semibold mr-2 mb-2 shadow-sm" style="background: rgba(168, 85, 247, 0.18); color: #EADDFF; border: 1px solid rgba(208, 188, 255, 0.25); border-radius: 9999px;">
        <i class="fa-solid fa-shield-halved text-purple-400 text-[11px]"></i>
        ${r}
      </span>
    `).join("");
  }

  const attributesHtml = renderCustomAttributesData(user.attributes);

  let mfaStatus = '<span class="text-xs text-gray-400">Disattivata</span>';
  if (user.mfaSettings && user.mfaSettings.mfaEnabled) {
    mfaStatus = '<span class="px-3 py-1 text-xs font-semibold" style="background: rgba(16, 185, 129, 0.2); color: #6EE7B7; border: 1px solid rgba(16, 185, 129, 0.35); border-radius: 9999px;">ATTIVA</span>';
  }

  container.innerHTML = `
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 w-full">
      
      <!-- Colonna Sinistra: Profilo Sintetico (M3 Card) -->
      <div class="m3-card p-8 flex flex-col items-center text-center h-fit">
        <div class="relative mb-6">
          <img
            src="${photo}"
            alt="${user.username}"
            class="w-36 h-36 rounded-full object-cover shadow-2xl"
            style="border: 4px solid rgba(208, 188, 255, 0.25);"
          />
          <span class="absolute bottom-2 right-2 w-5 h-5 rounded-full ${isBlocked ? 'bg-red-500' : 'bg-emerald-400'}" style="border: 2px solid #171324;" title="${isBlocked ? 'Bloccato' : 'Attivo'}"></span>
        </div>

        <h3 class="text-2xl font-bold text-white mb-1">${user.name || ""} ${user.surname || ""}</h3>
        <p class="text-purple-300 text-sm font-mono mb-3">@${user.username || ""}</p>
        <div class="mb-6">${statusBadge}</div>

        <!-- Box UUID ed Email SENZA BORDO BIANCO -->
        <div class="w-full p-4 text-left text-xs text-gray-300 space-y-3 mb-6" style="background: rgba(26, 20, 42, 0.7); border: 1px solid rgba(208, 188, 255, 0.15); border-radius: 20px;">
          <div>
            <span class="block text-[11px] uppercase font-semibold mb-0.5" style="color: #C4B5FD;">UUID Utente</span>
            <span class="font-mono text-purple-200 text-xs break-all select-all block leading-relaxed">${user.identifier || "N/D"}</span>
          </div>
          <div style="border-top: 1px solid rgba(208, 188, 255, 0.12); padding-top: 10px;">
            <span class="block text-[11px] uppercase font-semibold mb-0.5" style="color: #C4B5FD;">Email</span>
            <span class="text-white text-xs break-all select-all block leading-relaxed">${user.email || "N/D"}</span>
          </div>
          <div class="flex items-center justify-between" style="border-top: 1px solid rgba(208, 188, 255, 0.12); padding-top: 10px;">
            <span class="text-[11px] uppercase font-semibold" style="color: #C4B5FD;">Account Confermato</span>
            <span class="${isConfirmed ? 'text-emerald-300' : 'text-yellow-300'} font-semibold text-xs">${isConfirmed ? 'Sì' : 'In attesa'}</span>
          </div>
        </div>

        <!-- Bottoni Azione M3: Tutti Arrotondati (Pill), Testo Bianco, Nessun Bordo Bianco -->
        <div class="space-y-3 w-full">
          <a href="/app/users/edit/${user.identifier}" class="m3-btn-primary w-full justify-center py-3 text-xs font-bold text-decoration-none flex items-center gap-2 shadow-lg" style="border-radius: 9999px !important; color: #FFFFFF !important;">
            <i class="fa-solid fa-user-pen text-sm"></i>
            <span style="color: #FFFFFF !important;">Modifica Profilo</span>
          </a>
          
          <div class="grid grid-cols-2 gap-2.5 w-full">
            <a href="/app/users/roles/${user.identifier}" class="py-2.5 px-3 text-xs font-semibold text-center text-decoration-none flex items-center justify-center gap-1.5 shadow-sm transition-all" style="background: rgba(168, 85, 247, 0.18); color: #FFFFFF !important; border: 1px solid rgba(208, 188, 255, 0.3); border-radius: 9999px !important;">
              <i class="fa-solid fa-shield-halved text-xs" style="color: #D0BCFF;"></i>
              <span style="color: #FFFFFF !important;">Ruoli</span>
            </a>
            
            <button onclick="deleteUser('${user.identifier}','${user.username}')" class="py-2.5 px-3 text-xs font-semibold text-center cursor-pointer flex items-center justify-center gap-1.5 shadow-sm transition-all" style="background: rgba(239, 68, 68, 0.15); color: #FFFFFF !important; border: 1px solid rgba(239, 68, 68, 0.35); border-radius: 9999px !important;">
              <i class="fa-solid fa-trash text-xs" style="color: #F87171;"></i>
              <span style="color: #FFFFFF !important;">Elimina</span>
            </button>
          </div>
        </div>

      </div>

      <!-- Colonna Destra: Dettagli Anagrafici Completi -->
      <div class="lg:col-span-2 space-y-8">
        
        <div class="m3-card p-8">
          <h4 class="text-xl font-bold text-white mb-6 flex items-center gap-2.5 pb-4" style="border-bottom: 1px solid rgba(208, 188, 255, 0.15);">
            <i class="fa-solid fa-id-card text-purple-400"></i> Informazioni Anagrafiche Complete
          </h4>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Nome</span>
              <div class="text-white font-medium text-base mt-1">${user.name || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Cognome</span>
              <div class="text-white font-medium text-base mt-1">${user.surname || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Username</span>
              <div class="text-purple-300 font-medium text-base mt-1 font-mono">@${user.username || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Indirizzo Email</span>
              <div class="text-white font-medium text-base mt-1">${user.email || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Telefono</span>
              <div class="text-white font-medium text-base mt-1 font-mono">${user.phoneNumber || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Data di Nascita</span>
              <div class="text-white font-medium text-base mt-1">${user.birthDate || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Genere</span>
              <div class="text-white font-medium text-base mt-1">${user.gender || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Nazionalità</span>
              <div class="text-white font-medium text-base mt-1">${user.nationality || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Codice Fiscale / SSN</span>
              <div class="text-white font-medium text-base mt-1 font-mono">${user.ssn || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Occupazione</span>
              <div class="text-white font-medium text-base mt-1">${user.occupation || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Istruzione</span>
              <div class="text-white font-medium text-base mt-1">${user.education || "N/D"}</div>
            </div>
            <div>
              <span class="text-xs uppercase font-semibold" style="color: #C4B5FD;">Sicurezza 2FA (MFA)</span>
              <div class="mt-1">${mfaStatus}</div>
            </div>
          </div>
        </div>

        <!-- Card Ruoli Applicativi (RBAC) -->
        <div class="m3-card p-8">
          <div class="flex items-center justify-between pb-4 mb-4" style="border-bottom: 1px solid rgba(208, 188, 255, 0.15);">
            <h4 class="text-xl font-bold text-white flex items-center gap-2.5">
              <i class="fa-solid fa-user-shield text-purple-400"></i> Ruoli Applicativi (RBAC)
            </h4>
            <a href="/app/users/roles/${user.identifier}" class="m3-btn-primary text-xs py-2 px-5 text-decoration-none shadow-md" style="border-radius: 9999px !important; color: #FFFFFF !important;">
              <i class="fa-solid fa-pen-to-square mr-1"></i> <span style="color: #FFFFFF !important;">Gestisci Ruoli</span>
            </a>
          </div>
          <div class="flex flex-wrap pt-2">
            ${rolesHtml}
          </div>
        </div>

        <!-- Card Attributi Custom Trasformati in Dati Visivi -->
        <div class="m3-card p-8">
          <h4 class="text-xl font-bold text-white mb-6 flex items-center gap-2.5 pb-4" style="border-bottom: 1px solid rgba(208, 188, 255, 0.15);">
            <i class="fa-solid fa-sliders text-purple-400"></i> Attributi Personalizzati
          </h4>
          <div>
            ${attributesHtml}
          </div>
        </div>

      </div>

    </div>
  `;
}

function deleteUser(identifier, username) {
  sweetalert(
    "warning",
    "Conferma Eliminazione",
    `Sei sicuro di voler eliminare definitivamente l'utente @${username}? L'operazione non è reversibile.`,
    true
  ).then((res) => {
    if (res.isConfirmed) {
      const url = config.users_url + "/" + identifier;
      const token = getCookieOrStorage(config.access_token);
      DELETE(url, token).then(async (data) => {
        const responseData = await data.json().catch(() => ({}));
        if (responseData.error != null) {
          sweetalert("error", "Errore Eliminazione", responseData.error.message || "Impossibile eliminare l'utente.");
        } else {
          localStorage.removeItem(config.client_id + "_usersData");
          sweetalert("success", "Utente Eliminato", `L'utente @${username} è stato rimosso.`);
          setTimeout(() => {
            window.location.href = "/app/users";
          }, 1500);
        }
      }).catch(err => {
        console.error(err);
        sweetalert("error", "Errore", "Impossibile contattare il server.");
      });
    }
  });
}
