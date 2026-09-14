
function extractRedirectUris(val) {
  if (!val) return [];
  if (Array.isArray(val)) {
    return val.flatMap(item => extractRedirectUris(item)).filter(Boolean);
  }
  if (typeof val === "object") {
    const entries = Object.values(val);
    return entries.flatMap(item => extractRedirectUris(item)).filter(Boolean);
  }
  if (typeof val === "string") {
    const trimmed = val.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        return extractRedirectUris(JSON.parse(trimmed));
      } catch(e) {}
    }
    return trimmed.split(/[\s,]+/).map(s => s.trim()).filter(Boolean);
  }
  return [String(val).trim()];
}

function renderRedirectUrisBadges(val) {
  const uris = extractRedirectUris(val);
  if (!uris || uris.length === 0) {
    return '<span class="text-gray-500 text-xs italic">Nessuno</span>';
  }
  return `
    <div class="flex flex-col gap-1.5 py-1 min-w-[240px] max-w-[480px]">
      ${uris.map(u => `
        <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg m3-uri-badge text-xs font-mono group transition-all">
          <i class="fa-solid fa-link text-[10px] shrink-0"></i>
          <span class="break-all select-all leading-relaxed" title="${u}">${u}</span>
        </div>
      `).join('')}
    </div>
  `;
}

function formatRedirectUris(val) {
  const uris = extractRedirectUris(val);
  return uris.join(" ");
}

function formatScopes(scopes) {
  if (!scopes) return "";
  if (Array.isArray(scopes)) return scopes.join(", ");
  if (typeof scopes === "object") return Object.values(scopes).join(", ");
  return String(scopes);
}

function formatWebhooks(webhooks) {
  if (!webhooks) return "";
  if (Array.isArray(webhooks)) {
    return webhooks.map(w => typeof w === "object" ? (w.url || w.endpoint || JSON.stringify(w)) : w).join(", ");
  }
  if (typeof webhooks === "object") return JSON.stringify(webhooks);
  return String(webhooks);
}

let cachedClients = [];
let isEditMode = false;

// getClients is now triggered conditionally by switchDashboardView("clients")

function refreshClients() {
  console.log("Refreshing clients...");
  const icon = document.getElementById("refresh-icon");
  if(icon) icon.classList.add("fa-spin");
  getClients().finally(() => {
    if(icon) setTimeout(() => icon.classList.remove("fa-spin"), 500);
  });
}

function getClients() {
  const cfg = getConfig();
  const token = getCookieOrStorage(cfg.access_token);
  const url = cfg.client_id_url || (window.location.origin + "/v1/clients");

  return GET(url, token)
    .then(async (res) => {
      const data = await res.json();
      if (data.error != null) {
        if (typeof isUnauthorizedError === "function" && isUnauthorizedError(data.error)) {
          if (typeof logout === "function") logout();
          return;
        }
        sweetalert("error", (typeof t === "function" ? t("swal_client_load_error_title", "Errore Caricamento Client") : "Errore Caricamento Client"), data.error.message || (typeof t === "function" ? t("swal_client_load_error_desc", "Impossibile recuperare i client") : "Impossibile recuperare i client"));
      } else {
        cachedClients = data.data || [];
        try {
          localStorage.setItem(cfg.client_id + "_clientsData", JSON.stringify(cachedClients));
          localStorage.setItem(cfg.client_id + "_clients", JSON.stringify(cachedClients));
        } catch(e) {}
        displayClientsTable(cachedClients);
        updateClientKpis(cachedClients);
      }
    })
    .catch((err) => {
      console.error("Error fetching clients:", err);
      sweetalert("error", (typeof t === "function" ? t("swal_network_error_title", "Errore di Rete") : "Errore di Rete"), (typeof t === "function" ? t("swal_network_error_desc", "Impossibile contattare il server per la lista dei client.") : "Impossibile contattare il server per la lista dei client."));
    });
}

function loadCachedClients() {
  try {
    const cfg = getConfig();
    const stored = localStorage.getItem(cfg.client_id + "_clientsData") ||
      localStorage.getItem(cfg.client_id + "_clients") ||
      localStorage.getItem("ACCESS-SPHERE-TECH_clientsData") ||
      localStorage.getItem("ACCESS-SPHERE-TECH_clients");
    if (stored) {
      const parsed = JSON.parse(stored);
      if (Array.isArray(parsed) && parsed.length > 0) {
        cachedClients = parsed;
        displayClientsTable(parsed);
        updateClientKpis(parsed);
        return true;
      }
    }
  } catch (e) {}
  return false;
}

function updateClientKpis(clients) {
  if (!Array.isArray(clients)) return;
  const total = clients.length;
  const mfa = clients.filter(c => c && (c.mfaEnabled === true || c.mfaEnabled === "true" || c.mfa_enabled === true || c.mfa_enabled === "true")).length;
  const jwe = clients.filter(c => c && c.tokenType === "JWE").length;

  const totalEl = document.getElementById("stat-total-clients");
  const mfaEl = document.getElementById("stat-mfa-clients");
  const jweEl = document.getElementById("stat-jwe-clients");

  if(totalEl) totalEl.innerText = total;
  if(mfaEl) mfaEl.innerText = mfa;
  if(jweEl) jweEl.innerText = jwe;

  // Sincronizza in tempo reale anche la card riepilogativa nella Dashboard
  const recapTotalEl = document.getElementById("stat-recap-clients");
  const recapMfaEl = document.getElementById("stat-recap-mfa-clients");
  if (recapTotalEl) recapTotalEl.innerText = total;
  if (recapMfaEl) recapMfaEl.innerText = mfa;
}

function displayClientsTable(clients) {
  if ($.fn.DataTable.isDataTable('#clients-table')) {
    $('#clients-table').DataTable().destroy();
  }

  const tbody = document.getElementById("clients-data");
  tbody.innerHTML = "";

  clients.forEach((c) => {
    const scopesStr = formatScopes(c.scopes);
    const scopesBadge = scopesStr ? `<span class="badge text-bg-secondary text-[10px]">${scopesStr}</span>` : '<span class="text-gray-500 text-xs">Standard</span>';
    const redirectPreview = renderRedirectUrisBadges(c.redirect_uri);
    const mfaBadge = c.mfaEnabled 
      ? `<span class="badge text-bg-success text-[10px]"><i class="fa-solid fa-shield-check mr-1"></i>${typeof t === 'function' ? t('user_detail_active', 'ATTIVO') : 'ATTIVO'}</span>` 
      : `<span class="badge text-bg-secondary text-[10px]">${typeof t === 'function' ? t('user_detail_disabled_f', 'DISATTIVATO') : 'DISATTIVATO'}</span>`;

    const tr = document.createElement("tr");
    tr.className = "cursor-pointer hover:bg-purple-500/10 transition-colors";
    tr.onclick = (e) => {
      if (e.target.closest("button") || e.target.closest("a") || e.target.closest("input")) return;
      window.location.href = '/app/clients/details/' + encodeURIComponent(c.clientId);
    };
    tr.innerHTML = `
      <td>
        <div class="font-bold text-white text-sm flex items-center gap-2">
          <i class="fa-solid fa-cube text-purple-400"></i>
          <span class="cursor-pointer text-purple-300" onclick="event.stopPropagation(); window.location.href = '/app/clients/details/' + encodeURIComponent('${c.clientId}');">${c.clientId || "N/A"}</span>
        </div>
        <div class="text-[11px] text-gray-400 font-mono">${c.externalClientId || ""}</div>
      </td>
      <td>
        <span class="badge ${c.accessType === 'ONLINE' ? 'text-bg-primary' : 'text-bg-dark'} text-[10px]">
          ${c.accessType || "ONLINE"}
        </span>
      </td>
      <td>
        <span class="badge ${c.tokenType === 'JWE' ? 'text-bg-info text-dark' : 'text-bg-purple'} text-[10px]">
          ${c.tokenType || "JWT"}
        </span>
      </td>
      <td>${redirectPreview}</td>
      <td>${mfaBadge}</td>
      <td class="text-center">
        <a href="/app/clients/edit/${encodeURIComponent(c.clientId)}" class="m3-action-btn m3-action-edit" title="${typeof t === 'function' ? t('client_edit_page_title', 'Modifica Client') : 'Modifica Client'}">
          <i class="fa-solid fa-pen-to-square text-xs"></i>
        </a>
        <button onclick="deleteClientAction('${c.clientId}')" class="m3-action-btn m3-action-delete" title="${typeof t === 'function' ? t('client_detail_delete_btn', 'Elimina Client') : 'Elimina Client'}">
          <i class="fa-solid fa-trash text-xs"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  const clientDt = $('#clients-table').DataTable({
    pageLength: 10,
    responsive: true,
    language: {
      search: typeof t === "function" ? t("dt_search_clients", "Cerca client:") : "Cerca client:",
      lengthMenu: typeof t === "function" ? t("dt_length_clients", "Mostra _MENU_ client") : "Mostra _MENU_ client",
      info: typeof t === "function" ? t("dt_info_clients", "Visualizzati _START_ a _END_ di _TOTAL_ client") : "Visualizzati _START_ a _END_ di _TOTAL_ client",
      infoEmpty: typeof t === "function" ? t("dt_info_empty", "Nessun dato presente") : "Nessun dato presente",
      zeroRecords: typeof t === "function" ? t("dt_zero_records", "Nessun risultato trovato") : "Nessun risultato trovato",
      paginate: {
        first: typeof t === "function" ? t("dt_first", "Primo") : "Primo",
        last: typeof t === "function" ? t("dt_last", "Ultimo") : "Ultimo",
        next: typeof t === "function" ? t("dt_next", "Succ.") : "Succ.",
        previous: typeof t === "function" ? t("dt_prev", "Prec.") : "Prec."
      }
    }
  });

  function styleClientPagination() {
    $('.dt-paging-button').each(function() {
      this.style.setProperty('color', '#FFFFFF', 'important');
      this.style.setProperty('-webkit-text-fill-color', '#FFFFFF', 'important');
      this.style.setProperty('opacity', '1', 'important');
    });
  }

  clientDt.on('draw', styleClientPagination);
  styleClientPagination();
}

function openClientModal(client = null) {
  isEditMode = !!client;
  document.getElementById("modal-action-title").innerText = isEditMode
    ? (typeof t === "function" ? t("client_modal_edit_title", "Modifica Client OAuth2") : "Modifica Client OAuth2")
    : (typeof t === "function" ? t("client_modal_register_title", "Registra Nuovo Client OAuth2") : "Registra Nuovo Client OAuth2");
  
  const idInput = document.getElementById("form-clientId");
  idInput.value = client ? client.clientId || "" : "";
  idInput.disabled = isEditMode; // ClientID acts as primary key
  
  document.getElementById("form-clientSecret").value = client ? client.clientSecret || "" : "";
  document.getElementById("form-accessType").value = client && client.accessType ? client.accessType : "ONLINE";
  document.getElementById("form-tokenType").value = client && client.tokenType ? client.tokenType : "JWT";
  document.getElementById("form-authType").value = client && client.authType ? client.authType : "BEARER";
  document.getElementById("form-redirectUri").value = client ? formatRedirectUris(client.redirect_uri) : "";
  document.getElementById("form-scopes").value = client ? formatScopes(client.scopes) : "";
  document.getElementById("form-webhooks").value = client ? formatWebhooks(client.webhooks) : "";
  document.getElementById("form-jwtExpiration").value = client && client.jwtExpiration ? client.jwtExpiration : 3600;
  document.getElementById("form-mfaEnabled").checked = client ? !!client.mfaEnabled : false;

  const modal = new bootstrap.Modal(document.getElementById("clientModal"));
  modal.show();
}

function generateSecret() {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_~";
  let secret = "sec_";
  for(let i = 0; i < 32; i++) {
    secret += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  document.getElementById("form-clientSecret").value = secret;
}

function editClient(clientId) {
  const client = cachedClients.find(c => c.clientId === clientId);
  if(client) {
    openClientModal(client);
  }
}

function saveClient() {
  const clientId = document.getElementById("form-clientId").value.trim();
  if(!clientId) {
    return sweetalert("warning", typeof t === "function" ? t("swal_warning_title", "Attenzione") : "Attenzione", typeof t === "function" ? t("swal_client_missing_id", "Il Client ID è obbligatorio.") : "Il Client ID è obbligatorio.");
  }

  const redirectInput = document.getElementById("form-redirectUri").value.trim();
  const scopesInput = document.getElementById("form-scopes").value.trim();
  const webhooksInput = document.getElementById("form-webhooks").value.trim();

  const payload = {
    clientId: clientId,
    clientSecret: document.getElementById("form-clientSecret").value.trim(),
    accessType: document.getElementById("form-accessType").value,
    tokenType: document.getElementById("form-tokenType").value,
    authType: document.getElementById("form-authType").value,
    redirect_uri: redirectInput ? { "redirect_uri": redirectInput } : null,
    scopes: scopesInput ? scopesInput.split(",").map(s => s.trim()).filter(Boolean) : null,
    webhooks: webhooksInput ? webhooksInput.split(",").map(w => ({ url: w.trim() })) : null,
    jwtExpiration: parseInt(document.getElementById("form-jwtExpiration").value) || 3600,
    mfaEnabled: document.getElementById("form-mfaEnabled").checked
  };

  const cfg = getConfig();
  const token = getCookieOrStorage(cfg.access_token);
  const url = window.location.origin + "/v1/client";
  const method = isEditMode ? "PUT" : "POST";

  fetch(url, {
    method: method,
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + token
    },
    body: JSON.stringify(payload)
  })
  .then(async (res) => {
    if(res.ok) {
      const modalEl = document.getElementById("clientModal");
      const modal = bootstrap.Modal.getInstance(modalEl);
      if(modal) modal.hide();
      sweetalert("success", typeof t === "function" ? t("swal_success_title", "Operazione Riuscita") : "Operazione Riuscita", isEditMode ? (typeof t === "function" ? t("swal_client_updated_success", "Client aggiornato con successo!") : "Client aggiornato con successo!") : (typeof t === "function" ? t("swal_client_created_success", "Nuovo client registrato!") : "Nuovo client registrato!"));
      getClients();
    } else {
      const err = await res.json().catch(() => ({}));
      sweetalert("error", typeof t === "function" ? t("swal_save_error_title", "Errore Salvataggio") : "Errore Salvataggio", err.message || (typeof t === "function" ? t("swal_save_error_desc", "Impossibile salvare il client sul database.") : "Impossibile salvare il client sul database."));
    }
  })
  .catch(err => {
    console.error("Save client error:", err);
    sweetalert("error", typeof t === "function" ? t("swal_network_error_title", "Errore di Rete") : "Errore di Rete", typeof t === "function" ? t("swal_network_error_desc", "Connessione fallita.") : "Connessione fallita.");
  });
}

function deleteClientAction(clientId) {
  const confirmText = (typeof t === "function" ? t("swal_delete_client_desc", `Vuoi davvero eliminare il client ${clientId}? Questa azione è irreversibile!`) : `Vuoi davvero eliminare il client ${clientId}? Questa azione è irreversibile!`).replace("#CLIENT_ID#", clientId);
  Swal.fire({
    title: (typeof t === "function" ? t("swal_delete_client_title", "Sei sicuro?") : "Sei sicuro?"),
    text: confirmText,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: (typeof t === "function" ? t("btn_delete_confirm", "Sì, elimina!") : "Sì, elimina!"),
    cancelButtonText: (typeof t === "function" ? t("btn_cancel", "Annulla") : "Annulla")
  }).then((result) => {
    if (result.isConfirmed) {
      const cfg = getConfig();
      const token = getCookieOrStorage(cfg.access_token);
      const url = window.location.origin + `/v1/client/${encodeURIComponent(clientId)}`;

      fetch(url, {
        method: "DELETE",
        headers: {
          "Authorization": "Bearer " + token
        }
      })
      .then(async (res) => {
        if(res.ok) {
          const successMsg = (typeof t === "function" ? t("swal_delete_client_success", `Il client ${clientId} è stato rimosso.`) : `Il client ${clientId} è stato rimosso.`).replace("#CLIENT_ID#", clientId);
          sweetalert("success", (typeof t === "function" ? t("swal_deleted_title", "Eliminato!") : "Eliminato!"), successMsg);
          getClients();
        } else {
          sweetalert("error", (typeof t === "function" ? t("swal_error_title", "Errore") : "Errore"), (typeof t === "function" ? t("swal_delete_client_error", "Impossibile eliminare il client dal database.") : "Impossibile eliminare il client dal database."));
        }
      })
      .catch(err => {
        console.error("Delete client error:", err);
        sweetalert("error", (typeof t === "function" ? t("swal_error_title", "Errore") : "Errore"), (typeof t === "function" ? t("swal_connection_error", "Errore di connessione durante l'eliminazione.") : "Errore di connessione durante l'eliminazione."));
      });
    }
  });
}


function viewClientDetails(clientId) {
  const client = cachedClients.find(c => c.clientId === clientId);
  if (!client) return;

  const detailModalEl = document.getElementById("clientDetailModal");
  if (!detailModalEl) return;

  document.getElementById("detail-clientId").innerText = client.clientId || "N/D";
  document.getElementById("detail-extClientId").innerText = client.externalClientId || "Nessuno";
  document.getElementById("detail-accessType").innerText = client.accessType || "ONLINE";
  document.getElementById("detail-tokenType").innerText = client.tokenType || "JWT";
  document.getElementById("detail-authType").innerText = client.authType || "BEARER";
  document.getElementById("detail-clientSecret").value = client.clientSecret || "";
  document.getElementById("detail-scopes").innerText = formatScopes(client.scopes) || "openid, profile, email";
  document.getElementById("detail-redirectUri").innerHTML = renderRedirectUrisBadges(client.redirect_uri);
  document.getElementById("detail-webhooks").innerText = formatWebhooks(client.webhooks) || "Nessun webhook";
  document.getElementById("detail-jwtExpiration").innerText = (client.jwtExpiration || 3600) + " secondi";
  document.getElementById("detail-mfaStatus").innerHTML = client.mfaEnabled 
    ? '<span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">MFA OBBLIGATORIA</span>' 
    : '<span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-500/20 text-gray-400 border border-gray-500/30">NON ATTIVA</span>';

  // Store current client id on edit button inside detail modal
  const editBtn = document.getElementById("detail-editBtn");
  if (editBtn) {
    editBtn.onclick = () => {
      const modal = bootstrap.Modal.getInstance(detailModalEl);
      if (modal) modal.hide();
      setTimeout(() => openClientModal(client), 300);
    };
  }

  const modal = new bootstrap.Modal(detailModalEl);
  modal.show();
}

function copySecret(elementId) {
  const input = document.getElementById(elementId);
  if (input) {
    navigator.clipboard.writeText(input.value).then(() => {
      sweetalert("info", typeof t === "function" ? t("swal_copied_title", "Copiato!") : "Copiato!", typeof t === "function" ? t("swal_secret_copied_desc", "Client Secret copiato negli appunti.") : "Client Secret copiato negli appunti.");
    });
  }
}

// Re-render client table when language is changed dynamically
window.addEventListener("languageChanged", () => {
  if (cachedClients && cachedClients.length > 0) {
    displayClientsTable(cachedClients);
  }
});

  if (typeof clientDt !== 'undefined') {
    clientDt.on('draw', function() {
      $('.dt-paging-button').each(function() {
        this.style.setProperty('color', '#FFFFFF', 'important');
        this.style.setProperty('-webkit-text-fill-color', '#FFFFFF', 'important');
        this.style.setProperty('opacity', '1', 'important');
      });
    });
  }
