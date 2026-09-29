/**
 * =========================================================================================
 * Access-Sphere: Modulo Catalogo Ruoli & Permessi Granulari (/v1/roles)
 * =========================================================================================
 * Questo modulo gestisce il catalogo centralizzato dei ruoli applicativi e delle relative
 * autorizzazioni granulari per tutti i Client OAuth 2.0 registrati sulla piattaforma.
 *
 * Flusso dei dati e interconnessioni architetturali:
 * 1. Inizializzazione: all'avvio (`$(document).ready`), carica la lista dei client OAuth2
 *    (`loadClients`) e il catalogo completo dei ruoli definiti (`loadRoles`).
 * 2. Visualizzazione Bento M3: calcola le metriche KPI (totale ruoli, client configurati,
 *    assegnazioni complessive) e renderizza le schede dei ruoli nel grid reattivo.
 * 3. Filtri e Ricerca Dinamica: supporta ricerca full-text istantanea in memoria per nome ruolo,
 *    descrizione, client ID o permesso specifico, con filtri a tendina per Client e Tipo.
 * 4. Creazione & Modifica Modale: modale unificato per gestire nome ruolo, tipologia
 *    (USER, ADMIN, MANAGER, OPERATOR, TECHNICAL, AUDITOR, GUEST, SUPER_ADMIN), descrizione,
 *    toggle wildcard (*) e permessi granulari standard IAM o personalizzati aggiunti a chip.
 * 5. Cancellazione con Controllo Impatto Utenti: prima dell'eliminazione, interroga
 *    `GET /v1/roles/{role}/users?clientId=...` per mostrare l'elenco nominativo degli utenti
 *    impattati, avvisando che l'azione revocherà a cascata il ruolo da tutti loro.
 * 6. Protezione Ultimo Ruolo Obbligatorio: se per un client è configurato un solo ruolo, ne
 *    impedisce la cancellazione per garantire che ogni client mantenga almeno un ruolo attivo.
 * =========================================================================================
 */

/** Stato locale del catalogo ruoli in memoria */
let cachedRoles = [];

/** Lista dei client OAuth 2.0 registrati */
let cachedClients = [];

/** Array dei permessi personalizzati correntemente aggiunti nel modale */
let customPermissions = [];

/** Modalità del modale aperto: 'create' | 'edit' */
let currentModalMode = "create";

/** Riferimento al ruolo in corso di modifica */
let editingRoleData = null;

$(document).ready(function () {
    if (typeof AccessControl !== "undefined" && !AccessControl.canViewRoles()) {
        console.warn("AccessControl: Permission denied for viewing roles.");
        sweetalert("warning", typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato", typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei permessi necessari per accedere a questa sezione.") : "Non disponi dei permessi necessari per accedere a questa sezione.");
        setTimeout(() => {
            window.location.href = "/app";
        }, 1500);
        return;
    }

    // Inizializzazione dati
    loadClients();
    loadRoles();

    // Reset listener alla chiusura del modale
    const roleModalEl = document.getElementById("roleModal");
    if (roleModalEl) {
        roleModalEl.addEventListener("hidden.bs.modal", function () {
            resetModalForm();
        });
    }
});

/**
 * Recupera il bearer token di autenticazione per le chiamate REST.
 * Ispeziona configurazioni globali e storage locale.
 */
function getBearerToken() {
    if (typeof getCookieOrStorage === "function") {
        const cfg = typeof getConfig === "function" ? getConfig() : null;
        const key = cfg && cfg.access_token ? cfg.access_token : "access-token";
        const tok = getCookieOrStorage(key);
        if (tok) return tok;
    }
    const clientId = localStorage.getItem("Client-ID") || "ACCESS-SPHERE-AUTH";
    return (
        localStorage.getItem(`${clientId}_access-token`) ||
        localStorage.getItem("ACCESS-SPHERE-AUTH_access-token") ||
        localStorage.getItem("access-token") ||
        (typeof getCookie === "function" ? getCookie("access-token") : null) ||
        sessionStorage.getItem("access-token") ||
        ""
    );
}

/**
 * Recupera la lista dei Client OAuth2 registrati e popola i selettori client
 * della toolbar di filtro e del modale.
 */
function loadClients() {
    const token = getBearerToken();
    const url = window.location.origin + "/v1/clients";

    GET(url, token)
        .then(async (res) => {
            const data = await res.json();
            if (data && data.data && Array.isArray(data.data)) {
                cachedClients = data.data;
                populateClientSelectors(data.data);
            }
        })
        .catch((err) => {
            console.warn("loadClients: Impossibile recuperare i client OAuth2", err);
        });
}

/**
 * Popola il menu a tendina dei filtri e la tendina di selezione client nel modale.
 * @param {Array} clients - Array di oggetti ClientDTO
 */
function populateClientSelectors(clients) {
    const filterSelect = document.getElementById("filter-client-select");
    const modalSelect = document.getElementById("modalRoleClient");

    if (filterSelect) {
        // Mantieni la prima opzione "Tutti i Client OAuth2"
        filterSelect.innerHTML = `<option value="" class="bg-gray-900 text-white" data-i18n="roles_filter_all_clients">${typeof t === "function" ? t("roles_filter_all_clients", "Tutti i Client OAuth2") : "Tutti i Client OAuth2"}</option>`;
        clients.forEach((c) => {
            const opt = document.createElement("option");
            opt.value = c.clientId;
            opt.className = "bg-gray-900 text-white";
            opt.textContent = `${c.clientName || c.clientId} (${c.clientId})`;
            filterSelect.appendChild(opt);
        });
    }

    if (modalSelect) {
        modalSelect.innerHTML = `<option value="" disabled selected class="bg-gray-900 text-white">Seleziona un Client OAuth2...</option>`;
        clients.forEach((c) => {
            const opt = document.createElement("option");
            opt.value = c.clientId;
            opt.className = "bg-gray-900 text-white";
            opt.textContent = `${c.clientName || c.clientId} (${c.clientId})`;
            modalSelect.appendChild(opt);
        });
    }
}

/**
 * Recupera l'intero catalogo dei ruoli definiti da GET /v1/roles
 * arricchito con conteggio e dettagli degli utenti assegnati.
 * @param {string} [clientId] - Filtro opzionale per client
 */
function loadRoles(clientId) {
    const token = getBearerToken();
    let url = window.location.origin + "/v1/roles";
    if (clientId) {
        url += `?clientId=${encodeURIComponent(clientId)}`;
    }

    if (typeof enableLoader === "function") enableLoader();

    GET(url, token)
        .then(async (res) => {
            const data = await res.json();
            if (data.error != null) {
                if (typeof isUnauthorizedError === "function" && isUnauthorizedError(data.error)) {
                    if (typeof logout === "function") logout();
                    return;
                }
                const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
                sweetalert("error", errTitle, data.error.message || "Errore durante il caricamento dei ruoli");
                return;
            }

            cachedRoles = (data && data.data && Array.isArray(data.data)) ? data.data : [];
            updateKpis(cachedRoles);
            handleRoleFilterChange();
        })
        .catch((err) => {
            console.error("loadRoles error:", err);
            const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
            sweetalert("error", errTitle, "Impossibile recuperare il catalogo dei ruoli.");
        })
        .finally(() => {
            if (typeof disableLoader === "function") disableLoader();
        });
}

/**
 * Ricarica manualmente il catalogo attivando l'effetto rotazione sull'icona refresh.
 */
function refreshRolesCatalog() {
    const icon = document.getElementById("refresh-icon");
    if (icon) icon.classList.add("fa-spin");

    loadClients();
    const filterClient = document.getElementById("filter-client-select") ? document.getElementById("filter-client-select").value : null;

    loadRoles(filterClient);
    setTimeout(() => {
        if (icon) icon.classList.remove("fa-spin");
    }, 600);
}

/**
 * Calcola e visualizza i KPI riepilogativi in tempo reale.
 * Invariante Dati Dinamici (Rule 4.2): nessun data-i18n applicato su questi elementi.
 * @param {Array} roles - Catalogo dei ruoli correntemente caricato
 */
function updateKpis(roles) {
    const kpiRoles = document.getElementById("kpi-total-roles");
    const kpiClients = document.getElementById("kpi-total-clients");
    const kpiAssignments = document.getElementById("kpi-total-assignments");

    if (kpiRoles) {
        kpiRoles.textContent = roles.length;
    }

    if (kpiClients) {
        const uniqueClients = new Set(roles.map((r) => r.clientId).filter(Boolean));
        kpiClients.textContent = uniqueClients.size;
    }

    if (kpiAssignments) {
        const totalAssignments = roles.reduce((sum, r) => sum + (r.assignedUsersCount || 0), 0);
        kpiAssignments.textContent = totalAssignments;
    }
}

/**
 * Filtra in tempo reale l'elenco dei ruoli memorizzati nella cache locale
 * in base al testo di ricerca, al client e alla tipologia selezionati.
 */
function handleRoleFilterChange() {
    const searchInput = document.getElementById("role-search-input");
    const clientSelect = document.getElementById("filter-client-select");
    const typeSelect = document.getElementById("filter-type-select");

    const query = searchInput ? searchInput.value.trim().toLowerCase() : "";
    const selectedClient = clientSelect ? clientSelect.value : "";
    const selectedType = typeSelect ? typeSelect.value : "";

    const filtered = cachedRoles.filter((r) => {
        const matchClient = !selectedClient || r.clientId === selectedClient;
        const matchType = !selectedType || (r.type && r.type.toUpperCase() === selectedType.toUpperCase());

        let matchQuery = true;
        if (query) {
            const roleName = (r.role || "").toLowerCase();
            const desc = (r.description || "").toLowerCase();
            const client = (r.clientId || "").toLowerCase();
            const perms = Array.isArray(r.permissions) ? r.permissions.join(" ").toLowerCase() : "";
            matchQuery = roleName.includes(query) || desc.includes(query) || client.includes(query) || perms.includes(query);
        }

        return matchClient && matchType && matchQuery;
    });

    renderRolesGrid(filtered);
}

/**
 * Renderizza le schede dei ruoli applicativi nel grid M3 Bento Card.
 * @param {Array} roles - Elenco dei ruoli da visualizzare
 */
function renderRolesGrid(roles) {
    const grid = document.getElementById("roles-grid");
    const emptyState = document.getElementById("roles-empty-state");

    if (!grid) return;

    if (!roles || roles.length === 0) {
        grid.style.display = "none";
        if (emptyState) emptyState.style.display = "flex";
        return;
    }

    grid.style.display = "grid";
    if (emptyState) emptyState.style.display = "none";

    let html = "";

    roles.forEach((r) => {
        const type = (r.type || "USER").toUpperCase();
        const badgeClass = getRoleTypeBadgeClass(type);

        const permissions = Array.isArray(r.permissions) ? r.permissions : [];
        const isWildcard = permissions.includes("*");
        const assignedCount = r.assignedUsersCount || 0;

        // Verifica se questo è l'unico ruolo configurato per il Client (obbligatorio, non cancellabile)
        const clientRolesCount = cachedRoles.filter((cr) => cr.clientId === r.clientId).length;
        const isMandatoryRole = clientRolesCount <= 1;

        // Generazione chips dei permessi (massimo 4 con indicatore per gli eccedenti)
        let permsHtml = "";
        if (isWildcard) {
            permsHtml = `<span class="permission-chip permission-chip-wildcard"><i class="fa-solid fa-asterisk text-xs"></i> * (Accesso Totale)</span>`;
        } else if (permissions.length > 0) {
            const visible = permissions.slice(0, 4);
            const extra = permissions.length - 4;
            permsHtml = visible.map((p) => `<span class="permission-chip">${escapeHtml(p)}</span>`).join("");
            if (extra > 0) {
                permsHtml += `<span class="permission-chip text-gray-400 border-dashed" title="${escapeHtml(permissions.slice(4).join(', '))}">+${extra} altri</span>`;
            }
        } else {
            permsHtml = `<span class="text-xs text-gray-500 italic" data-i18n="roles_card_no_permissions">Nessun permesso esplicito</span>`;
        }

        // Serializzazione sicura per il passaggio alla modale di modifica
        const safeRoleData = encodeURIComponent(JSON.stringify(r));

        html += `
        <div class="m3-bento-card p-4 sm:p-5 flex flex-col justify-between transition-all duration-300 hover:border-purple-400/40">

            <!-- Card Header: Client & Tipologia -->
            <div>
                <div class="flex items-center justify-between gap-2 mb-3">
                    <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/5 border border-white/10 text-gray-300 text-xs font-mono font-medium truncate max-w-[55%] sm:max-w-[200px]"
                         title="Client ID: ${escapeHtml(r.clientId)}">
                        <i class="fa-solid fa-cubes text-purple-400 text-xs shrink-0"></i>
                        <span class="truncate">${escapeHtml(r.clientId)}</span>
                    </div>
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold shrink-0 ${badgeClass}">
                        ${escapeHtml(type)}
                    </span>
                </div>

                <!-- Nome Ruolo -->
                <div class="flex items-center gap-2 mb-2">
                    <div class="w-8 h-8 rounded-lg bg-purple-500/15 flex items-center justify-center text-purple-300 shrink-0">
                        <i class="fa-solid fa-shield-halved text-sm"></i>
                    </div>
                    <h4 class="text-base font-bold text-white font-mono tracking-tight mb-0 truncate" title="${escapeHtml(r.role)}">
                        ${escapeHtml(r.role)}
                    </h4>
                </div>

                <!-- Descrizione Ruolo -->
                <p class="text-xs text-gray-400 leading-relaxed mb-4 line-clamp-2 min-h-[32px]">
                    ${r.description ? escapeHtml(r.description) : '<span class="text-gray-500 italic">Nessuna descrizione specificata.</span>'}
                </p>

                <!-- Box Permessi -->
                <div class="mb-4">
                    <span class="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block mb-1.5" data-i18n="roles_card_permissions_label">
                        Permessi:
                    </span>
                    <div class="flex flex-wrap gap-1.5 items-center">
                        ${permsHtml}
                    </div>
                </div>
            </div>

            <!-- Card Footer: Conteggio Assegnazioni & Azioni -->
            <div class="pt-3 border-t border-white/10 flex items-center justify-between gap-2">
                <div class="inline-flex items-center gap-1.5 text-xs text-gray-300" title="Utenti a cui è attualmente associato questo ruolo">
                    <i class="fa-solid fa-users text-purple-400"></i>
                    <span class="font-bold text-white">${assignedCount}</span>
                    <span class="text-gray-400" data-i18n="roles_card_users_assigned">Utenti</span>
                </div>

                ${(typeof AccessControl === "undefined" || AccessControl.canManageRoles()) ? `
                <div class="flex items-center gap-1.5">
                    <!-- Modifica Ruolo -->
                    <button type="button" onclick="openRoleModal('edit', decodeURIComponent('${safeRoleData}'))"
                            class="m3-icon-btn" title="Modifica Ruolo" data-i18n-title="btn_edit">
                        <i class="fa-solid fa-pen-to-square text-xs"></i>
                    </button>

                    <!-- Elimina Ruolo / Blocco Ultimo Ruolo Obbligatorio -->
                    ${isMandatoryRole ? `
                    <button type="button" onclick="showLastRoleLockedAlert()"
                            class="m3-icon-btn border-amber-500/30 text-amber-300 opacity-60 hover:opacity-100"
                            title="Ruolo Obbligatorio (Ultimo rimanente per questo Client)" data-i18n-title="swal_role_last_locked_title">
                        <i class="fa-solid fa-lock text-xs"></i>
                    </button>
                    ` : `
                    <button type="button" onclick="deleteRoleConfirm('${escapeHtml(r.clientId)}', '${escapeHtml(r.role)}', '${escapeHtml(type)}')"
                            class="m3-icon-btn m3-icon-btn-danger" title="Elimina Ruolo" data-i18n-title="btn_delete">
                        <i class="fa-solid fa-trash-can text-xs"></i>
                    </button>
                    `}
                </div>
                ` : `<span class="text-xs text-gray-500 font-mono">-</span>`}
            </div>

        </div>
        `;
    });

    grid.innerHTML = html;

    // Traduzione etichette dinamiche
    if (typeof translateDOM === "function") {
        translateDOM(grid);
    }

    if (typeof AccessControl !== "undefined" && typeof AccessControl.applyUIPermissions === "function") {
        AccessControl.applyUIPermissions(grid);
    }
}

/**
 * Gestisce l'apertura del modale di creazione o modifica ruolo.
 * @param {string} mode - 'create' | 'edit'
 * @param {string|object} [roleData] - Dati del ruolo in caso di modifica
 */
function openRoleModal(mode, roleData) {
    if (typeof AccessControl !== "undefined" && !AccessControl.canManageRoles()) {
        sweetalert("warning", typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato", typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei permessi necessari per questa operazione.") : "Non disponi dei permessi necessari per questa operazione.");
        return;
    }
    currentModalMode = mode;
    resetModalForm();

    const titleEl = document.getElementById("roleModalLabel");
    const iconEl = document.getElementById("roleModalIcon");
    const clientSelect = document.getElementById("modalRoleClient");
    const nameInput = document.getElementById("modalRoleName");
    const descTextarea = document.getElementById("modalRoleDescription");

    if (mode === "create") {
        if (titleEl) titleEl.textContent = typeof t === "function" ? t("roles_modal_create_title", "Nuovo Ruolo Applicativo") : "Nuovo Ruolo Applicativo";
        if (iconEl) iconEl.className = "fa-solid fa-shield-plus text-base";
        if (clientSelect) {
            clientSelect.disabled = false;
            // Pre-seleziona il client eventualmente filtrato nella toolbar
            const currentFilter = document.getElementById("filter-client-select") ? document.getElementById("filter-client-select").value : "";
            if (currentFilter) clientSelect.value = currentFilter;
        }
        if (nameInput) {
            nameInput.readOnly = false;
        }
        // Tipo predefinito: USER
        const radioUser = document.getElementById("typeRadioUSER");
        if (radioUser) radioUser.checked = true;

    } else if (mode === "edit") {
        if (typeof roleData === "string") {
            try {
                roleData = JSON.parse(roleData);
            } catch (e) {
                console.error("openRoleModal: parse roleData error", e);
            }
        }
        editingRoleData = roleData;

        if (titleEl) titleEl.textContent = typeof t === "function" ? t("roles_modal_edit_title", "Modifica Ruolo Applicativo") : "Modifica Ruolo Applicativo";
        if (iconEl) iconEl.className = "fa-solid fa-shield-halved text-base";

        if (clientSelect && roleData) {
            clientSelect.value = roleData.clientId;
            clientSelect.disabled = true; // Il client di appartenenza è immutabile in modifica
        }

        if (nameInput && roleData) {
            nameInput.value = roleData.role;
        }

        if (descTextarea && roleData) {
            descTextarea.value = roleData.description || "";
        }

        // Selezione del radio button corrispondente
        if (roleData && roleData.type) {
            const targetRadio = document.getElementById(`typeRadio${roleData.type.toUpperCase()}`);
            if (targetRadio) targetRadio.checked = true;
        }

        // Popolazione permessi
        if (roleData && Array.isArray(roleData.permissions)) {
            if (roleData.permissions.includes("*")) {
                const wildCheck = document.getElementById("modalPermissionWildcard");
                if (wildCheck) {
                    wildCheck.checked = true;
                    handleWildcardToggle(true);
                }
            } else {
                const standardKeys = ["users:read", "users:write", "users:delete", "clients:read", "clients:write", "roles:manage", "settings:read", "settings:write"];
                roleData.permissions.forEach((p) => {
                    if (!p) return;
                    const key = String(p).trim().toLowerCase();
                    const normalizedKey = key === "user:read" ? "users:read"
                        : key === "user:write" ? "users:write"
                        : key === "user:delete" ? "users:delete"
                        : key === "client:read" ? "clients:read"
                        : key === "client:write" ? "clients:write"
                        : key;
                    if (standardKeys.includes(normalizedKey)) {
                        const cb = document.querySelector(`.iam-perm-checkbox[value="${normalizedKey}"]`);
                        if (cb) cb.checked = true;
                    } else {
                        customPermissions.push(p);
                    }
                });
                renderCustomPermissionsList();
            }
        }
    }

    updateLivePreview();

    const modalEl = document.getElementById("roleModal");
    if (modalEl) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }
}

/**
 * Resetta tutti i campi del modulo del modale.
 */
function resetModalForm() {
    editingRoleData = null;
    customPermissions = [];

    const clientSelect = document.getElementById("modalRoleClient");
    if (clientSelect) {
        clientSelect.disabled = false;
        clientSelect.selectedIndex = 0;
    }

    const nameInput = document.getElementById("modalRoleName");
    if (nameInput) {
        nameInput.value = "";
        nameInput.readOnly = false;
    }

    const descTextarea = document.getElementById("modalRoleDescription");
    if (descTextarea) descTextarea.value = "";

    const wildCheck = document.getElementById("modalPermissionWildcard");
    if (wildCheck) wildCheck.checked = false;

    handleWildcardToggle(false);

    document.querySelectorAll(".iam-perm-checkbox").forEach((cb) => {
        cb.checked = false;
    });

    const radioUser = document.getElementById("typeRadioUSER");
    if (radioUser) radioUser.checked = true;

    const customInput = document.getElementById("customPermissionInput");
    if (customInput) customInput.value = "";

    renderCustomPermissionsList();
    updateLivePreview();
}

/**
 * Restituisce la classe CSS corretta per il badge del tipo ruolo.
 * Supporta gli 8 ruoli aziendali standard (USER, ADMIN, MANAGER, OPERATOR, TECHNICAL, AUDITOR, GUEST, SUPER_ADMIN).
 * @param {string} type - Tipo ruolo
 * @returns {string} Classe CSS associata
 */
function getRoleTypeBadgeClass(type) {
    const t = (type || "USER").toUpperCase();
    switch (t) {
        case "ADMIN":
            return "role-badge-admin";
        case "SUPER_ADMIN":
            return "role-badge-superadmin";
        case "MANAGER":
            return "role-badge-manager";
        case "OPERATOR":
            return "role-badge-operator";
        case "TECHNICAL":
            return "role-badge-technical";
        case "AUDITOR":
            return "role-badge-auditor";
        case "GUEST":
            return "role-badge-guest";
        case "USER":
        default:
            return "role-badge-user";
    }
}

/**
 * Aggiorna l'anteprima live della scheda ruolo all'interno del modale fullscreen
 * riflettendo in tempo reale nome, client, tipo, descrizione e permessi selezionati.
 */
function updateLivePreview() {
    const nameInput = document.getElementById("modalRoleName");
    const clientSelect = document.getElementById("modalRoleClient");
    const descTextarea = document.getElementById("modalRoleDescription");
    const checkedRadio = document.querySelector('input[name="modalRoleTypeRadio"]:checked');
    const isWildcard = document.getElementById("modalPermissionWildcard") ? document.getElementById("modalPermissionWildcard").checked : false;

    const pName = document.getElementById("previewRoleName");
    const pClient = document.getElementById("previewRoleClient");
    const pType = document.getElementById("previewRoleType");
    const pDesc = document.getElementById("previewRoleDesc");
    const pPerms = document.getElementById("previewRolePerms");

    if (pName) {
        const val = nameInput && nameInput.value.trim() ? nameInput.value.trim() : "NOME_RUOLO";
        pName.textContent = val;
    }

    if (pClient) {
        const cVal = clientSelect && clientSelect.value ? clientSelect.value : "N/D";
        pClient.textContent = `Client: ${cVal}`;
    }

    const typeVal = checkedRadio ? checkedRadio.value : "USER";
    if (pType) {
        pType.textContent = typeVal;
        pType.className = `px-2 py-0.5 rounded-md font-mono text-[10px] font-bold ${getRoleTypeBadgeClass(typeVal)}`;
    }

    if (pDesc) {
        const dVal = descTextarea && descTextarea.value.trim() ? descTextarea.value.trim() : "Nessuna descrizione inserita";
        pDesc.textContent = dVal;
    }

    if (pPerms) {
        if (isWildcard) {
            pPerms.innerHTML = `<span class="permission-chip permission-chip-wildcard"><i class="fa-solid fa-asterisk text-[9px] mr-1"></i> * (Tutti i Permessi)</span>`;
        } else {
            const standardChecked = Array.from(document.querySelectorAll(".iam-perm-checkbox:checked")).map((cb) => cb.value);
            const allPerms = [...standardChecked, ...customPermissions];
            if (allPerms.length === 0) {
                pPerms.innerHTML = `<span class="text-[11px] text-gray-500 italic">Nessun permesso assegnato</span>`;
            } else {
                pPerms.innerHTML = allPerms.map((p) => `<span class="permission-chip font-mono text-[10px]">${escapeHtml(p)}</span>`).join("");
            }
        }
    }
}

/**
 * Gestisce l'abilitazione/disabilitazione dei permessi standard quando
 * viene selezionato o deselezionato il toggle wildcard (*).
 * @param {boolean} isChecked - Se true, la wildcard include già tutti i permessi
 */
function handleWildcardToggle(isChecked) {
    const standardBox = document.getElementById("standardPermissionsBox");
    if (standardBox) {
        standardBox.style.opacity = isChecked ? "0.4" : "1";
        standardBox.style.pointerEvents = isChecked ? "none" : "auto";
    }
}

/**
 * Aggiunge un nuovo permesso personalizzato all'elenco dei chip.
 */
function addCustomPermission() {
    const input = document.getElementById("customPermissionInput");
    if (!input) return;

    let val = input.value.trim().toLowerCase();
    if (!val) return;

    // Rimuovi spazi interni sostituendoli con underscore
    val = val.replace(/\s+/g, "_");

    if (!customPermissions.includes(val)) {
        customPermissions.push(val);
        renderCustomPermissionsList();
        updateLivePreview();
    }

    input.value = "";
    input.focus();
}

/**
 * Rimuove un permesso personalizzato dall'array tramite indice.
 * @param {number} index - Indice nell'array customPermissions
 */
function removeCustomPermission(index) {
    if (index >= 0 && index < customPermissions.length) {
        customPermissions.splice(index, 1);
        renderCustomPermissionsList();
        updateLivePreview();
    }
}

/**
 * Renderizza i chip dei permessi personalizzati nel contenitore modale.
 */
function renderCustomPermissionsList() {
    const container = document.getElementById("customPermissionsList");
    if (!container) return;

    if (customPermissions.length === 0) {
        container.innerHTML = `<span class="text-xs text-gray-500 italic p-1">Nessun permesso personalizzato inserito.</span>`;
        return;
    }

    container.innerHTML = customPermissions
        .map(
            (p, idx) => `
        <span class="permission-chip">
            <span>${escapeHtml(p)}</span>
            <span class="custom-chip-remove" onclick="removeCustomPermission(${idx})" title="Rimuovi">&times;</span>
        </span>
    `
        )
        .join("");
}

/**
 * Valida ed invia i dati per la creazione o l'aggiornamento del ruolo applicativo.
 */
function saveRole() {
    const clientSelect = document.getElementById("modalRoleClient");
    const nameInput = document.getElementById("modalRoleName");
    const descTextarea = document.getElementById("modalRoleDescription");
    const checkedRadio = document.querySelector('input[name="modalRoleTypeRadio"]:checked');

    const clientId = clientSelect ? clientSelect.value : "";
    const roleName = nameInput ? nameInput.value.trim().toUpperCase() : "";
    const type = checkedRadio ? checkedRadio.value : "USER";
    const description = descTextarea ? descTextarea.value.trim() : "";

    // Validazione campi obbligatori
    if (!clientId || !roleName || !type) {
        const title = typeof t === "function" ? t("validation_error_title", "Campi Obbligatori") : "Campi Obbligatori";
        const msg = typeof t === "function" ? t("roles_swal_validation_error", "Compila tutti i campi obbligatori (Client, Nome Ruolo e Tipologia).") : "Compila tutti i campi obbligatori (Client, Nome Ruolo e Tipologia).";
        sweetalert("warning", title, msg);
        return;
    }

    // Costruzione lista permessi
    const wildCheck = document.getElementById("modalPermissionWildcard");
    let permissions = [];

    if (wildCheck && wildCheck.checked) {
        permissions = ["*"];
    } else {
        document.querySelectorAll(".iam-perm-checkbox:checked").forEach((cb) => {
            permissions.push(cb.value);
        });
        customPermissions.forEach((p) => {
            if (!permissions.includes(p)) {
                permissions.push(p);
            }
        });
    }

    const payload = {
        clientId: clientId,
        role: roleName,
        type: type,
        description: description,
        permissions: permissions
    };

    const token = getBearerToken();
    const btnSave = document.getElementById("btnSaveRole");
    if (btnSave) {
        btnSave.disabled = true;
        btnSave.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Salvataggio...`;
    }

    let promise;
    if (currentModalMode === "create") {
        const url = window.location.origin + "/v1/roles";
        promise = POST(url, token, payload);
    } else {
        // Modalità Edit: PUT /v1/roles/{role}?clientId=...
        const targetRole = editingRoleData ? editingRoleData.role : roleName;
        const targetClient = editingRoleData ? editingRoleData.clientId : clientId;
        const url = `${window.location.origin}/v1/roles/${encodeURIComponent(targetRole)}?clientId=${encodeURIComponent(targetClient)}`;
        promise = PUT(url, token, payload);
    }

    promise
        .then(async (res) => {
            const data = await res.json();
            if (data.error != null) {
                const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
                sweetalert("error", errTitle, data.error.message || "Errore durante il salvataggio del ruolo");
                return;
            }

            // Chiusura modale
            const modalEl = document.getElementById("roleModal");
            if (modalEl) {
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
            }

            const successTitle = typeof t === "function" ? t("roles_swal_save_success_title", "Ruolo Salvato") : "Ruolo Salvato";
            const successDesc = typeof t === "function" ? t("roles_swal_save_success_desc", "La definizione del ruolo applicativo è stata salvata con successo.") : "La definizione del ruolo applicativo è stata salvata con successo.";

            sweetalert("success", successTitle, successDesc).then(() => {
                loadRoles(clientId);
            });
        })
        .catch((err) => {
            console.error("saveRole error:", err);
            const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
            sweetalert("error", errTitle, "Si è verificato un errore imprevisto.");
        })
        .finally(() => {
            if (btnSave) {
                btnSave.disabled = false;
                btnSave.innerHTML = `<i class="fa-solid fa-floppy-disk"></i> <span data-i18n="btn_save_role">${typeof t === "function" ? t("btn_save_role", "Salva Ruolo") : "Salva Ruolo"}</span>`;
            }
        });
}

/**
 * Notifica l'utente dell'impossibilità di eliminare l'ultimo ruolo configurato per un client.
 */
function showLastRoleLockedAlert() {
    const lockedTitle = typeof t === "function" ? t("swal_role_last_locked_title", "Ruolo Obbligatorio") : "Ruolo Obbligatorio";
    const lockedDesc = typeof t === "function" ? t("swal_role_last_locked_desc", "Ogni Client OAuth2 deve possedere almeno un ruolo configurato. Non è possibile eliminare l'ultimo ruolo rimanente per questo client.") : "Ogni Client OAuth2 deve possedere almeno un ruolo configurato. Non è possibile eliminare l'ultimo ruolo rimanente per questo client.";
    sweetalert("warning", lockedTitle, lockedDesc);
}

/**
 * Flusso di cancellazione avanzato con SweetAlert2:
 * 1. Verifica se il ruolo è l'ultimo rimasto per il Client (obbligatorio, non eliminabile).
 * 2. Interroga GET /v1/roles/{role}/users?clientId=... per recuperare gli utenti assegnati.
 * 3. Mostra la lista dettagliata degli utenti impattati chiedendo conferma esplicita
 *    per la revoca a cascata e cancellazione dal client.
 * 4. Invia DELETE /v1/roles/{role}?clientId=... e sincronizza la vista.
 *
 * @param {string} clientId - Identificativo del Client OAuth2
 * @param {string} role - Nome del ruolo da eliminare
 * @param {string} [roleType] - Tipologia del ruolo
 */
function deleteRoleConfirm(clientId, role, roleType) {
    if (typeof AccessControl !== "undefined" && !AccessControl.canManageRoles()) {
        sweetalert("warning", typeof t === "function" ? t("swal_permission_denied_title", "Accesso Negato") : "Accesso Negato", typeof t === "function" ? t("swal_permission_denied_desc", "Non disponi dei permessi necessari per questa operazione.") : "Non disponi dei permessi necessari per questa operazione.");
        return;
    }

    // 1. Protezione Ultimo Ruolo Obbligatorio del Client
    const clientRoles = cachedRoles.filter((cr) => cr.clientId === clientId);
    if (clientRoles.length <= 1) {
        showLastRoleLockedAlert();
        return;
    }

    const token = getBearerToken();
    const customSwal = typeof getM3SwalMixin === "function" ? getM3SwalMixin() : Swal;

    // 2. Modale di caricamento per la verifica degli utenti associati
    customSwal.fire({
        title: typeof t === "function" ? t("roles_swal_checking_users", "Verifica utenti associati...") : "Verifica utenti associati...",
        text: "Attendere la scansione delle assegnazioni di sicurezza attive",
        color: "#FFFFFF",
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    const checkUsersUrl = `${window.location.origin}/v1/roles/${encodeURIComponent(role)}/users?clientId=${encodeURIComponent(clientId)}`;

    GET(checkUsersUrl, token)
        .then(async (res) => {
            const data = await res.json();
            const users = (data && data.data && Array.isArray(data.data)) ? data.data : [];

            // 3. Costruzione del dialogo di conferma
            if (users.length > 0) {
                // Impatto elevato: utenti assegnati presenti
                let rawDesc = typeof t === "function" ? t("roles_swal_delete_impact_desc", "Questo ruolo è attualmente assegnato a <strong>#COUNT# utenti</strong> sul client <strong>#CLIENT#</strong>. Se procedi, il ruolo verrà eliminato dal Client e <strong>revocato automaticamente</strong> da tutti gli utenti elencati di seguito:") : "Questo ruolo è attualmente assegnato a <strong>#COUNT# utenti</strong> sul client <strong>#CLIENT#</strong>. Se procedi, il ruolo verrà eliminato dal Client e <strong>revocato automaticamente</strong> da tutti gli utenti elencati di seguito:";
                rawDesc = rawDesc.replace("#COUNT#", users.length).replace("#CLIENT#", escapeHtml(clientId));

                const usersListHtml = users.map((u) => {
                    const fullName = `${u.name || ''} ${u.surname || ''}`.trim() || u.username;
                    return `
                        <div class="flex items-center justify-between p-2.5 rounded-xl bg-white/5 border border-white/10 text-xs">
                            <div class="flex items-center gap-2.5 min-w-0">
                                <div class="w-7 h-7 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-300 shrink-0">
                                    <i class="fa-solid fa-user text-xs"></i>
                                </div>
                                <div class="truncate text-left">
                                    <div class="font-bold text-white truncate">${escapeHtml(fullName)}</div>
                                    <div class="text-[11px] text-gray-400 font-mono">@${escapeHtml(u.username)}</div>
                                </div>
                            </div>
                            <span class="text-[11px] text-gray-400 font-mono truncate max-w-[150px] shrink-0 ml-2">
                                ${escapeHtml(u.email || 'N/D')}
                            </span>
                        </div>
                    `;
                }).join("");

                const impactDialogHtml = `
                    <div class="text-left space-y-3">
                        <p class="text-xs text-rose-300 leading-relaxed">${rawDesc}</p>
                        <div class="max-h-56 overflow-y-auto space-y-1.5 p-1 rounded-xl bg-black/40 border border-white/10">
                            ${usersListHtml}
                        </div>
                    </div>
                `;

                return customSwal.fire({
                    icon: "warning",
                    title: typeof t === "function" ? t("roles_swal_delete_impact_title", "Attenzione: Ruolo Assegnato!") : "Attenzione: Ruolo Assegnato!",
                    html: impactDialogHtml,
                    showCancelButton: true,
                    confirmButtonText: `<i class="fa-solid fa-trash-can mr-1"></i> ${typeof t === "function" ? t("roles_swal_delete_confirm_btn", "Sì, revoca ed elimina") : "Sì, revoca ed elimina"}`,
                    cancelButtonText: typeof t === "function" ? t("btn_cancel", "Annulla") : "Annulla",
                    customClass: {
                        confirmButton: "m3-btn-danger",
                        cancelButton: "m3-btn-secondary"
                    },
                    buttonsStyling: false
                });

            } else {
                // Nessun utente assegnato: cancellazione sicura
                let safeDesc = typeof t === "function" ? t("roles_swal_delete_safe_desc", "Nessun utente è attualmente assegnato a questo ruolo sul client #CLIENT#. Vuoi procedere con l'eliminazione definitiva?") : "Nessun utente è attualmente assegnato a questo ruolo sul client #CLIENT#. Vuoi procedere con l'eliminazione definitiva?";
                safeDesc = safeDesc.replace("#CLIENT#", escapeHtml(clientId));

                let safeTitle = typeof t === "function" ? t("roles_swal_delete_confirm_title", "Eliminare il Ruolo #ROLE#?") : "Eliminare il Ruolo #ROLE#?";
                safeTitle = safeTitle.replace("#ROLE#", escapeHtml(role));

                return customSwal.fire({
                    icon: "warning",
                    title: safeTitle,
                    text: safeDesc,
                    showCancelButton: true,
                    confirmButtonText: typeof t === "function" ? t("btn_delete", "Elimina") : "Elimina",
                    cancelButtonText: typeof t === "function" ? t("btn_cancel", "Annulla") : "Annulla",
                    customClass: {
                        confirmButton: "m3-btn-danger",
                        cancelButton: "m3-btn-secondary"
                    },
                    buttonsStyling: false
                });
            }
        })
        .then((result) => {
            if (result && result.isConfirmed) {
                // Esecuzione chiamata HTTP DELETE
                const deleteUrl = `${window.location.origin}/v1/roles/${encodeURIComponent(role)}?clientId=${encodeURIComponent(clientId)}`;

                if (typeof enableLoader === "function") enableLoader();

                DELETE(deleteUrl, token)
                    .then(async (delRes) => {
                        const delData = await delRes.json();
                        if (delData.error != null) {
                            const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
                            sweetalert("error", errTitle, delData.error.message || "Errore durante l'eliminazione del ruolo");
                            return;
                        }

                        const succTitle = typeof t === "function" ? t("roles_swal_delete_success_title", "Ruolo Eliminato") : "Ruolo Eliminato";
                        const succDesc = typeof t === "function" ? t("roles_swal_delete_success_desc", "Il ruolo è stato eliminato con successo e revocato da tutti gli utenti associati.") : "Il ruolo è stato eliminato con successo e revocato da tutti gli utenti associati.";

                        sweetalert("success", succTitle, succDesc).then(() => {
                            const currentFilter = document.getElementById("filter-client-select") ? document.getElementById("filter-client-select").value : null;
                            loadRoles(currentFilter);
                        });
                    })
                    .catch((err) => {
                        console.error("deleteRole error:", err);
                        const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
                        sweetalert("error", errTitle, "Si è verificato un errore durante l'eliminazione.");
                    })
                    .finally(() => {
                        if (typeof disableLoader === "function") disableLoader();
                    });
            }
        })
        .catch((err) => {
            console.error("deleteRoleConfirm checking users error:", err);
            const errTitle = typeof t === "function" ? t("error_title", "Errore") : "Errore";
            sweetalert("error", errTitle, "Impossibile verificare gli utenti associati al ruolo.");
        });
}

/**
 * Funzione helper per l'escaping sicuro di markup HTML.
 * @param {string} str - Stringa grezza da sanificare
 * @returns {string} Stringa sanificata sicura per l'inserimento nel DOM
 */
function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
