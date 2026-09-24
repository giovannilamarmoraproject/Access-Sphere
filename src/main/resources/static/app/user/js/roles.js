/**
 * =========================================================================================
 * Access-Sphere: Modulo Gestione Ruoli Utente & Permessi (RBAC Multi-Tenant Console)
 * =========================================================================================
 * Questo script gestisce l'interfaccia di assegnazione e revoca dei ruoli applicativi contestualizzati
 * per ciascun Client OAuth 2.0 registrato sulla piattaforma.
 *
 * Flusso dei dati e interconnessioni architetturali:
 * 1. Inizializzazione: all'avvio della pagina (`$(document).ready`), vengono chiamati `getUser()`
 *    e `getClient()` per recuperare in parallelo i dati dell'utente target e la lista dei client OAuth2.
 * 2. Mappatura Dati: `displayUserData(user)` estrae sia la struttura moderna `user.clientRoles`
 *    (UserClientRoleDTO) che la lista retrocompatibile `user.roles` valorizzando lo stato locale `currentClientRoles`.
 * 3. Popolazione Client & Ruoli: `displayClientData(clients)` popola la tendina dei client. Alla selezione
 *    di un client, vengono estratti i relativi `appRoles` e inseriti nella tendina dei ruoli.
 * 4. Anteprima Reattiva: al cambio di selezione in `#role_select`, viene mostrato il box `#role_preview_box`
 *    con nome del ruolo, tipologia applicativa, descrizione e lista dei permessi granulari (inclusa la wildcard '*').
 * 5. Validazione Duplicati: `validateRoleSelection()` verifica in tempo reale se la coppia (clientId, role)
 *    e gia presente in `currentClientRoles`, prevenendo duplicazioni.
 * 6. Assegnazione & Revoca: `addRole()` inserisce il nuovo ruolo contestualizzato nella lista temporanea,
 *    mentre la rimozione tramite la card aggiorna reattivamente la vista invocando `renderAssignedRoles()`.
 * 7. Persistenza Atomica: `changeRoles()` invia una chiamata HTTP PUT a `/v1/users/{identifier}/roles`
 *    fornendo un payload unificato che include sia `clientRoles` (moderno) che `roles` (retrocompatibile).
 * =========================================================================================
 */

/** Stato locale dei ruoli attualmente assegnati all'utente (array di oggetti UserClientRoleDTO) */
let currentClientRoles = [];

/** Catalogo completo dei client OAuth2 registrati, memorizzato per lookup rapidi dei ruoli applicativi */
let allClientsData = [];

/** Identificativo univoco dell'utente correntemente selezionato */
let currentTargetIdentifier = null;

$(document).ready(function () {
  // Verifica autorizzazioni per la gestione dei ruoli utente
  if (typeof AccessControl !== "undefined" && !AccessControl.canManageRoles()) {
    console.warn("AccessControl: Permesso insufficiente per gestire i ruoli utente.");
    if (typeof AccessControl.showDeniedAlert === "function") {
      AccessControl.showDeniedAlert(
        typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato",
        typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei permessi necessari per gestire i ruoli.") : "Non disponi dei permessi necessari per gestire i ruoli.",
        "/app/users"
      );
    } else {
      window.location.href = "/app/users";
    }
    return;
  }

  // Risoluzione dell'identificativo utente e caricamento anagrafica
  getUser();

  // Caricamento del catalogo dei Client OAuth2 registrati da cache o API
  const clientsJSON = localStorage.getItem(config.client_id + "_clients");
  if (clientsJSON) {
    try {
      const parsedClients = JSON.parse(clientsJSON);
      allClientsData = parsedClients;
      displayClientData(parsedClients);
    } catch (e) {
      getClient();
    }
  } else {
    getClient();
  }

  // Listener per l'anteprima reattiva dei permessi del ruolo selezionato
  $("#role_select").on("change", function () {
    handleRoleSelectionChange();
  });
});

/**
 * Effettua la richiesta HTTP GET per recuperare la lista dei client OAuth2 registrati.
 * Interconnesso con:
 * - `config.client_id_url`: endpoint REST backend per i client
 * - `displayClientData()`: funzione di rendering del menu a tendina
 */
function getClient() {
  const url = config.client_id_url;
  const token = getCookieOrStorage(config.access_token);

  GET(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      console.warn("getClient error in roles:", responseData.error);
      if (typeof isUnauthorizedError === "function" && isUnauthorizedError(responseData.error)) {
        if (typeof logout === "function") logout();
        return;
      }
    } else {
      fetchHeader(data.headers);
      allClientsData = responseData.data || [];
      localStorage.setItem(
        config.client_id + "_clients",
        JSON.stringify(allClientsData)
      );
      displayClientData(allClientsData);
    }
  }).catch((err) => {
    console.error("Errore nel recupero dei client:", err);
  });
}

/**
 * Popola il menu a tendina dei Client OAuth2 (`#client_id_select`) e aggancia il listener
 * per aggiornare dinamicamente i ruoli applicativi disponibili per il client selezionato.
 *
 * @param {Array<Object>} clients Lista dei client OAuth2
 */
function displayClientData(clients) {
  allClientsData = clients || [];
  const clientSelect = $("#client_id_select").empty().append(
    '<option selected disabled value="">' +
      (typeof t === "function" ? t("role_choose_client_opt", "Scegli un Client...") : "Scegli un Client...") +
      '</option>'
  );

  allClientsData.forEach((client) => {
    $("<option>").val(client.clientId).text(client.clientId).appendTo(clientSelect);
  });

  // Aggancio listener al cambio client per popolare i ruoli specifici del client
  $("#client_id_select").off("change").on("change", function () {
    const selectedClientId = $(this).val();
    const selectedClient = allClientsData.find((c) => c.clientId === selectedClientId);

    const roleSelect = $("#role_select").empty().append(
      '<option selected disabled value="">' +
        (typeof t === "function" ? t("role_choose_role_opt", "Scegli un Ruolo...") : "Scegli un Ruolo...") +
        '</option>'
    );

    let clientRoles = selectedClient?.appRoles || selectedClient?.app_roles;
    if (typeof clientRoles === "string") {
      try {
        clientRoles = JSON.parse(clientRoles);
      } catch (e) {
        clientRoles = [];
      }
    }

    if (Array.isArray(clientRoles) && clientRoles.length > 0) {
      clientRoles.forEach((role) => {
        const roleName = typeof role === "string" ? role : (role?.role || role?.name || role?.value);
        const roleType = (typeof role === "object" && role?.type) ? ` (${role.type})` : "";
        if (roleName) {
          $("<option>").val(roleName).text(roleName + roleType).appendTo(roleSelect);
        }
      });
      roleSelect.prop("disabled", false);
    } else {
      roleSelect.prop("disabled", true);
    }

    // Reset dell'anteprima e disabilitazione pulsante Assegna
    $("#role_preview_box").addClass("hidden");
    $("#add_role_btn").prop("disabled", true);
    validateRoleSelection();
  });
}

/**
 * Gestisce l'anteprima in tempo reale del ruolo selezionato nel box `#role_preview_box`.
 * Mostra nome del ruolo, tipologia applicativa, descrizione e badge dei permessi associati.
 */
function handleRoleSelectionChange() {
  const selectedClientId = $("#client_id_select").val();
  const selectedRoleName = $("#role_select").val();

  if (!selectedRoleName || !selectedClientId) {
    $("#role_preview_box").addClass("hidden");
    validateRoleSelection();
    return;
  }

  const selectedClient = allClientsData.find((c) => c.clientId === selectedClientId);
  let clientRoles = selectedClient?.appRoles || selectedClient?.app_roles || [];
  if (typeof clientRoles === "string") {
    try {
      clientRoles = JSON.parse(clientRoles);
    } catch (e) {
      clientRoles = [];
    }
  }

  // Lookup dell'oggetto AppRole corrispondente al ruolo selezionato
  const roleObj = clientRoles.find((r) => {
    const rName = typeof r === "string" ? r : (r?.role || r?.name || r?.value);
    return rName === selectedRoleName;
  });

  const roleType = (typeof roleObj === "object" && roleObj?.type) ? roleObj.type : "DEFAULT";
  const roleDesc = (typeof roleObj === "object" && roleObj?.description) ? roleObj.description : "";
  const rolePermissions = (typeof roleObj === "object" && Array.isArray(roleObj?.permissions)) ? roleObj.permissions : [];

  // Iniezione dati nell'anteprima
  $("#preview_client_badge").html(`<i class="fa-solid fa-cube text-[9px] mr-1"></i>${selectedClientId}`);
  $("#preview_type_badge").text(roleType);
  $("#preview_role_name").text(selectedRoleName);

  if (roleDesc && roleDesc.trim().length > 0) {
    $("#preview_role_desc").text(roleDesc).removeClass("hidden");
  } else {
    $("#preview_role_desc").text("").addClass("hidden");
  }

  // Generazione dei badge per i permessi granulari
  const permsContainer = $("#preview_role_permissions").empty();
  if (rolePermissions.includes("*")) {
    permsContainer.append(`
      <span class="px-2.5 py-1 rounded-md text-xs font-mono font-bold flex items-center gap-1.5 shadow-sm"
            style="background: rgba(16, 185, 129, 0.18); color: #34D399; border: 1px solid rgba(16, 185, 129, 0.38);">
        <i class="fa-solid fa-wand-magic-sparkles text-[10px]"></i>
        <span>${typeof t === "function" ? t("role_perm_wildcard", "Tutti i permessi (*)") : "Tutti i permessi (*)"}</span>
      </span>
    `);
  } else if (rolePermissions.length > 0) {
    rolePermissions.forEach((perm) => {
      permsContainer.append(`
        <span class="px-2 py-0.5 rounded text-[11px] font-mono font-semibold flex items-center gap-1"
              style="background: rgba(var(--theme-accent-rgb), 0.14); color: var(--theme-accent); border: 1px solid rgba(var(--theme-accent-rgb), 0.28);">
          <i class="fa-solid fa-key text-[9px] opacity-75"></i>
          <span>${perm}</span>
        </span>
      `);
    });
  } else {
    permsContainer.append(`
      <span class="text-xs text-gray-400 italic">
        ${typeof t === "function" ? t("role_no_permissions", "Nessun permesso esplicito") : "Nessun permesso esplicito"}
      </span>
    `);
  }

  $("#role_preview_box").removeClass("hidden");
  validateRoleSelection();
}

/**
 * Valida che il ruolo selezionato non sia gia stato assegnato all'utente per lo specifico Client.
 * Abilita o disabilita il pulsante `#add_role_btn` fornendo feedback visivo.
 */
function validateRoleSelection() {
  const selectedClientId = $("#client_id_select").val();
  const selectedRoleName = $("#role_select").val();

  if (!selectedClientId || !selectedRoleName) {
    $("#role_select").removeClass("is-invalid");
    $("#validationRoles").text("");
    $("#add_role_btn").prop("disabled", true);
    return;
  }

  // Verifica unicità su chiave composta (clientId, role)
  const alreadyAdded = currentClientRoles.some(
    (cr) => cr.clientId === selectedClientId && cr.role === selectedRoleName
  );

  if (alreadyAdded) {
    $("#role_select").addClass("is-invalid");
    $("#validationRoles").text(
      typeof t === "function"
        ? t("role_already_assigned", "Questo ruolo è già stato assegnato per questo client.")
        : "Questo ruolo è già stato assegnato per questo client."
    );
    $("#add_role_btn").prop("disabled", true);
  } else {
    $("#role_select").removeClass("is-invalid");
    $("#validationRoles").text("");
    $("#add_role_btn").prop("disabled", false);
  }
}

/**
 * Aggiunge il ruolo selezionato alla lista locale dei ruoli assegnati (`currentClientRoles`).
 * Interconnesso con:
 * - `handleRoleSelectionChange()`: recupera i dettagli del ruolo
 * - `renderAssignedRoles()`: rigenera le card nel DOM
 * - `validateRoleSelection()`: reimposta lo stato di validazione
 */
function addRole() {
  const selectedClientId = $("#client_id_select").val();
  const selectedRoleName = $("#role_select").val();

  if (!selectedClientId || !selectedRoleName) return;

  const selectedClient = allClientsData.find((c) => c.clientId === selectedClientId);
  let clientRoles = selectedClient?.appRoles || selectedClient?.app_roles || [];
  if (typeof clientRoles === "string") {
    try {
      clientRoles = JSON.parse(clientRoles);
    } catch (e) {
      clientRoles = [];
    }
  }

  const roleObj = clientRoles.find((r) => {
    const rName = typeof r === "string" ? r : (r?.role || r?.name || r?.value);
    return rName === selectedRoleName;
  });

  const roleType = (typeof roleObj === "object" && roleObj?.type) ? roleObj.type : "DEFAULT";
  const roleDesc = (typeof roleObj === "object" && roleObj?.description) ? roleObj.description : "";
  const rolePermissions = (typeof roleObj === "object" && Array.isArray(roleObj?.permissions)) ? roleObj.permissions : [];

  // Inserimento del nuovo ruolo contestualizzato
  currentClientRoles.push({
    clientId: selectedClientId,
    role: selectedRoleName,
    type: roleType,
    description: roleDesc,
    permissions: rolePermissions
  });

  // Rigenerazione della visualizzazione e abilitazione salvataggio
  renderAssignedRoles();
  $("#save_role_btn").prop("disabled", false);

  // Reset del campo di selezione ruolo e occultamento anteprima
  $("#role_select").val("");
  $("#role_preview_box").addClass("hidden");
  validateRoleSelection();
}

/**
 * Esegue il rendering reattivo dell'intera griglia dei ruoli assegnati all'utente in `#role_container`.
 * Mostra per ogni card:
 * - Badge identificativo del Client OAuth2
 * - Tipologia del ruolo (DEFAULT, ADMIN, ecc.)
 * - Nome del ruolo con icona scudo
 * - Descrizione testuale (se disponibile)
 * - Badge dei permessi granulari aggregati
 * - Pulsante di revoca immediata
 */
function renderAssignedRoles() {
  const container = $("#role_container").empty();

  // Aggiornamento del contatore badge nel banner utente
  const count = currentClientRoles.length;
  $("#user-roles-count-badge").html(
    `<i class="fa-solid fa-shield-halved" style="color: var(--theme-accent);"></i><span>${count} ${
      count === 1 ? "Ruolo Assegnato" : "Ruoli Assegnati"
    }</span>`
  );

  // Stato vuoto se nessun ruolo assegnato
  if (count === 0) {
    container.html(`
      <div class="col-span-full py-4 text-center">
        <p class="text-xs text-gray-400 italic mb-0">
          ${
            typeof t === "function"
              ? t(
                  "roles_empty_assigned",
                  "Nessun ruolo assegnato. Seleziona un'applicazione e un ruolo nel riquadro sottostante per assegnarlo."
                )
              : "Nessun ruolo assegnato. Seleziona un'applicazione e un ruolo nel riquadro sottostante per assegnarlo."
          }
        </p>
      </div>
    `);
    return;
  }

  // Generazione dinamica delle card dei ruoli
  currentClientRoles.forEach((cr, index) => {
    const isWildcard = cr.permissions && cr.permissions.includes("*");
    let permissionsBadgeHtml = "";

    if (isWildcard) {
      permissionsBadgeHtml = `
        <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold flex items-center gap-1 shrink-0"
              style="background: rgba(16, 185, 129, 0.15); color: #34D399; border: 1px solid rgba(16, 185, 129, 0.3);">
          <i class="fa-solid fa-wand-magic-sparkles text-[9px]"></i>
          <span>${typeof t === "function" ? t("role_perm_wildcard", "Tutti i permessi (*)") : "Tutti i permessi (*)"}</span>
        </span>
      `;
    } else if (cr.permissions && cr.permissions.length > 0) {
      permissionsBadgeHtml = cr.permissions
        .map(
          (p) => `
        <span class="px-1.5 py-0.5 rounded text-[9px] font-mono font-medium"
              style="background: rgba(var(--theme-accent-rgb), 0.12); color: var(--theme-accent); border: 1px solid rgba(var(--theme-accent-rgb), 0.22);">
          ${p}
        </span>
      `
        )
        .join("");
    } else {
      permissionsBadgeHtml = `
        <span class="text-[10px] text-gray-400 italic">
          ${typeof t === "function" ? t("role_no_permissions", "Nessun permesso esplicito") : "Nessun permesso esplicito"}
        </span>
      `;
    }

    const card = $(`
      <div class="user-role-card p-4 rounded-xl border flex flex-col justify-between gap-3 transition-all"
           style="background: rgba(var(--theme-primary-rgb), 0.12); border: 1px solid rgba(var(--theme-accent-rgb), 0.25);">
        <div>
          <!-- Header card: Client ID Badge + Tipo Ruolo + Pulsante Revoca -->
          <div class="flex items-start justify-between gap-2 mb-2">
            <div class="flex items-center gap-1.5 flex-wrap min-w-0">
              <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold truncate max-w-[170px]"
                    title="${cr.clientId}"
                    style="background: rgba(var(--theme-accent-rgb), 0.18); color: var(--theme-accent); border: 1px solid rgba(var(--theme-accent-rgb), 0.32);">
                <i class="fa-solid fa-cube text-[9px] mr-1"></i>${cr.clientId}
              </span>
              <span class="px-2 py-0.5 rounded-full text-[9px] font-mono font-semibold"
                    style="background: rgba(255, 255, 255, 0.08); color: #E5E7EB; border: 1px solid rgba(255, 255, 255, 0.15);">
                ${cr.type || "DEFAULT"}
              </span>
            </div>
            <button type="button" class="remove-role-btn m3-icon-btn m3-icon-btn-danger shrink-0"
                    title="${typeof t === "function" ? t("role_revoke_tooltip", "Revoca Ruolo") : "Revoca Ruolo"}"
                    style="width: 28px; height: 28px;">
              <i class="fa-solid fa-xmark text-xs"></i>
            </button>
          </div>

          <!-- Titolo Ruolo -->
          <div class="flex items-center gap-2 mb-1.5">
            <div class="w-6 h-6 rounded-full flex items-center justify-center shrink-0"
                 style="background: rgba(var(--theme-accent-rgb), 0.22); color: var(--theme-accent);">
              <i class="fa-solid fa-shield-halved text-[11px]"></i>
            </div>
            <span class="font-mono text-sm font-bold text-white tracking-wide truncate" title="${cr.role}">
              ${cr.role}
            </span>
          </div>

          <!-- Descrizione del Ruolo se disponibile -->
          ${
            cr.description
              ? `<p class="text-[11px] text-gray-300 mt-1 mb-2 italic line-clamp-2">${cr.description}</p>`
              : ""
          }
        </div>

        <!-- Sezione Permessi Granulari -->
        <div class="pt-2 border-t" style="border-top-color: rgba(var(--theme-accent-rgb), 0.15);">
          <div class="text-[10px] font-semibold text-gray-400 mb-1 flex items-center gap-1">
            <i class="fa-solid fa-key text-[9px]" style="color: var(--theme-accent);"></i>
            <span>${typeof t === "function" ? t("role_permissions_label", "Permessi:") : "Permessi:"}</span>
          </div>
          <div class="flex flex-wrap gap-1 items-center">
            ${permissionsBadgeHtml}
          </div>
        </div>
      </div>
    `);

    // Handler revoca ruolo
    card.find(".remove-role-btn").on("click", function () {
      currentClientRoles.splice(index, 1);
      renderAssignedRoles();
      $("#save_role_btn").prop("disabled", false);
      validateRoleSelection();
    });

    container.append(card);
  });
}

/**
 * Estrae l'identificativo utente dall'URL e recupera il profilo aggiornato da API o cache.
 * Interconnesso con:
 * - `config.users_url`: endpoint anagrafica utenti
 * - `displayUserData()`: aggiornamento banner profilo e ruoli
 */
function getUser() {
  const urlParams = window.location.href;
  let identifier = null;
  if (urlParams.includes("roles/")) {
    identifier = urlParams.split("roles/")[1].split("/")[0].split("?")[0].split("#")[0];
  } else {
    identifier = new URLSearchParams(window.location.search).get("identifier");
  }

  if (!identifier) return;
  currentTargetIdentifier = identifier;

  // Tentativo di lettura da cache per rendering immediato
  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    try {
      const user = JSON.parse(usersJSON).find((u) => u.identifier == identifier || u.username == identifier);
      if (user) {
        displayUserData(user);
      }
    } catch (e) {}
  }

  // Chiamata REST in background per ottenere sempre la lista utenti fresca con clientRoles
  const token = getCookieOrStorage(config.access_token);
  GET(config.users_url, token).then(async (res) => {
    try {
      const data = await res.json();
      if (data && data.data) {
        localStorage.setItem(config.client_id + "_usersData", JSON.stringify(data.data));
        const user = data.data.find((u) => u.identifier == identifier || u.username == identifier);
        if (user) {
          displayUserData(user);
        }
      }
    } catch (e) {
      console.warn("Impossibile aggiornare i dati utente:", e);
    }
  });
}

/**
 * Inietta le informazioni anagrafiche dell'utente nell'header banner e inizializza
 * la lista `currentClientRoles` con supporto backward-compatible al formato legacy `roles`.
 *
 * @param {Object} user Oggetto User restituito dall'API
 */
function displayUserData(user) {
  if (user) {
    $("#user-full-name").text((user.name || "") + " " + (user.surname || "") || "Utente Access Sphere");
    $("#user-username-badge").text("@" + (user.username || ""));
    $("#user-email-text").text(user.email || "");
    if (user.profilePhoto) {
      $("#user-avatar-img").attr("src", user.profilePhoto);
    }
  }

  // Popolazione dello stato currentClientRoles: priorità a clientRoles, fallback su roles legacy
  if (user && user.clientRoles && Array.isArray(user.clientRoles) && user.clientRoles.length > 0) {
    currentClientRoles = JSON.parse(JSON.stringify(user.clientRoles));
  } else if (user && user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
    // Trasformazione del set legacy flat di ruoli in elementi client-scoped con clientId di default
    currentClientRoles = user.roles.map((r) => ({
      clientId: "ACCESS_SPHERE_DEFAULT_CLIENT",
      role: r,
      type: "DEFAULT",
      description: null,
      permissions: []
    }));
  } else {
    currentClientRoles = [];
  }

  renderAssignedRoles();
}

/**
 * Invia le modifiche dei ruoli al server tramite chiamata HTTP PUT atomica a `/v1/users/{identifier}/roles`.
 * Costruisce un payload unificato che sincronizza sia la collezione moderna `clientRoles` sia
 * la lista flat `roles` per garantire la piena retrocompatibilità con i servizi legacy.
 */
function changeRoles() {
  if (typeof AccessControl !== "undefined" && !AccessControl.canManageRoles()) {
    if (typeof AccessControl.showDeniedAlert === "function") {
      AccessControl.showDeniedAlert(
        typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato",
        typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei permessi necessari per modificare i ruoli.") : "Non disponi dei permessi necessari per modificare i ruoli."
      );
    }
    return;
  }

  const identifier = currentTargetIdentifier || (
    window.location.href.includes("roles/")
      ? window.location.href.split("roles/")[1].split("/")[0].split("?")[0].split("#")[0]
      : new URLSearchParams(window.location.search).get("identifier")
  );

  if (!identifier) {
    sweetalert("error", "Errore", "Identificativo utente non valido.");
    return;
  }

  const url = config.users_url + "/" + encodeURIComponent(identifier) + "/roles";
  const token = getCookieOrStorage(config.access_token);

  // Sincronizzazione atomica sia del campo legacy 'roles' che del campo moderno 'clientRoles'
  const payload = {
    roles: Array.from(new Set(currentClientRoles.map((cr) => cr.role))),
    clientRoles: currentClientRoles.map((cr) => ({
      clientId: cr.clientId,
      role: cr.role,
      type: cr.type || "DEFAULT",
      description: cr.description || null,
      permissions: cr.permissions || []
    }))
  };

  fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + token
    },
    body: JSON.stringify(payload)
  })
    .then(async (res) => {
      if (res.ok) {
        // Pulizia della cache locale degli utenti per forzare il ricaricamento dei nuovi ruoli
        localStorage.removeItem(config.client_id + "_usersData");
        const title = typeof t === "function" ? t("roles_response_title", "Ruoli Cambiati") : "Ruoli Cambiati";
        const text =
          typeof t === "function"
            ? t("roles_response_text", "I ruoli sono stati cambiati con successo.")
            : "I ruoli sono stati cambiati con successo.";

        sweetalert("success", title, text);
        setTimeout(() => {
          window.location.href = "/app/users/details/" + encodeURIComponent(identifier);
        }, 1200);
      } else {
        const err = await res.json().catch(() => ({}));
        sweetalert("error", "Errore", err.message || "Impossibile salvare i ruoli.");
      }
    })
    .catch((err) => {
      console.error(err);
      sweetalert("error", "Errore di Connessione", "Impossibile contattare il server.");
    });
}
