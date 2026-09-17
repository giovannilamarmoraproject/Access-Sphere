/**
 * Access Sphere - Gestione Modifica Profilo Utente (Edit User)
 *
 * Interconnessioni logiche:
 * - Recupera la sessione e la configurazione globale tramite `config.js` (es. `config.users_url`, `config.client_id`).
 * - Carica i dati utente dalla cache o tramite l'API reattiva GET `/v1/users` (`rest.js`).
 * - Si integra con `attributes-manager.js` per sincronizzare la vista visuale/JSON degli attributi personalizzati.
 * - Valida i campi del form tramite `validateUserEditForm()` (`validationForm.js`).
 * - Invia l'aggiornamento anagrafico con `PUT /v1/users/{identifier}` via `rest.js`, allineato al DTO `User.java`.
 * - Fornisce feedback all'utente tramite `sweetalert.js` con messaggi localizzati tramite `translations.js` (`t()`).
 */

// Stato globale dell'utente attualmente caricato nel modulo di modifica
let currentLoadedUser = null;
let profileImage = null;

$(document).ready(function () {
  // Inizializza il recupero dei dati utente tramite parametro URL o query string
  getUser();

  // Gestione upload e anteprima dell'immagine profilo
  const fileInput = document.getElementById("file");
  if (fileInput) {
    fileInput.addEventListener("change", function (event) {
      const file = event.target.files[0];

      if (file) {
        const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
        if (!allowedTypes.includes(file.type)) {
          const errTitle = typeof t === "function" ? t("register_form_profile_invalid_title", "Formato non valido") : "Formato non valido";
          const errText = typeof t === "function" ? t("register_form_profile_invalid_text", "Formati consentiti: JPEG, PNG, WebP.") : "Formati consentiti: JPEG, PNG, WebP.";
          sweetalert("error", errTitle, errText);
          event.target.value = "";
          return;
        }

        const reader = new FileReader();
        reader.onload = function () {
          const base64String = reader.result;
          const prof = document.getElementById("profile");
          if (prof) {
            prof.src = base64String;
            prof.style.display = "block";
          }
          profileImage = base64String;
        };
        reader.readAsDataURL(file);
      }
    });
  }

  // Intercettazione dell'invio del form di modifica
  $(".form").on("submit", function (event) {
    event.preventDefault();

    // Validazione client-side dei campi obbligatori
    if (typeof validateUserEditForm === "function") {
      if (!validateUserEditForm()) {
        return;
      }
    }

    // Sincronizzazione forzata degli attributi personalizzati (da visuale o da codice JSON) verso la textarea nascosta
    if (typeof currentAttributesMode !== "undefined" && currentAttributesMode === "code") {
      const codeEditor = document.getElementById("attributes-json-editor");
      if (codeEditor && typeof validateJsonCodeEditor === "function") {
        validateJsonCodeEditor();
      }
    } else if (typeof syncAttributesToTextarea === "function") {
      syncAttributesToTextarea();
    }

    // Risoluzione foto profilo (nuova caricata o esistente)
    const currentProfilePhoto = $("#profile").attr("src");
    const profilePhoto = profileImage || currentProfilePhoto || (currentLoadedUser ? currentLoadedUser.profilePhoto : null);

    // Parsing degli attributi JSON personalizzati
    let parsedAttributes = null;
    const rawAttributes = $("#attributes").val() ? $("#attributes").val().trim() : "";
    if (rawAttributes && rawAttributes !== "{}") {
      try {
        parsedAttributes = JSON.parse(rawAttributes);
      } catch (e) {
        console.error("Errore nel parsing degli attributi JSON personalizzati:", e);
      }
    }

    // Risoluzione numero di telefono con prefisso internazionale
    const prefix = $("#phone_prefix").val() ? $("#phone_prefix").val().trim() : "";
    const phone = $("#phone").val() ? $("#phone").val().trim() : "";
    let phoneNumber = null;
    if (phone && prefix) {
      phoneNumber = prefix + " " + phone;
    } else if (phone) {
      phoneNumber = phone;
    }

    // Risoluzione dell'identificatore univoco (URL path, query param o stato utente caricato)
    const urlParams = window.location.href;
    let identifierFromUrl = null;
    if (urlParams.includes("edit/")) {
      identifierFromUrl = urlParams.split("edit/")[1].split("/")[0].split("?")[0].split("#")[0];
    } else {
      const params = new URLSearchParams(window.location.search);
      identifierFromUrl = params.get("id") || params.get("identifier");
    }

    const finalIdentifier = (currentLoadedUser && currentLoadedUser.identifier)
      ? currentLoadedUser.identifier
      : identifierFromUrl;

    // Estrazione e pulizia dei valori anagrafici dal form
    const nameVal = $("#name").val() ? $("#name").val().trim() : "";
    const surnameVal = $("#surname").val() ? $("#surname").val().trim() : "";
    const birthdateVal = $("#birthdate").val() ? $("#birthdate").val().trim() : null;
    const genderVal = $("#gender").val() ? $("#gender").val().trim() : null;
    const nationalityVal = $("#nationality").val() ? $("#nationality").val().trim() : null;
    const ssnVal = $("#ssn").val() ? $("#ssn").val().trim() : null;
    const emailVal = $("#email").val() ? $("#email").val().trim() : "";
    const usernameVal = $("#validationDefaultUsername").val() ? $("#validationDefaultUsername").val().trim() : "";
    const occupationVal = $("#occupation").val() ? $("#occupation").val().trim() : null;
    const educationVal = $("#education").val() ? $("#education").val().trim() : null;

    // Costruzione del payload allineato a User DTO (con alias legacy e preservazione stato)
    const userData = {
      identifier: finalIdentifier,
      name: nameVal,
      surname: surnameVal,
      email: emailVal,
      username: usernameVal,
      phoneNumber: phoneNumber,
      birthDate: birthdateVal || null,
      gender: genderVal || null,
      nationality: nationalityVal || null,
      ssn: ssnVal || null,
      occupation: occupationVal || null,
      education: educationVal || null,
      profilePhoto: profilePhoto || null,
      attributes: parsedAttributes,
      // Campi alias per massima retrocompatibilità frontend/backend
      firstName: nameVal,
      lastName: surnameVal,
      birthdate: birthdateVal || null,
      phone: phoneNumber,
      profile: profilePhoto || null,
      // Preservazione dello stato utente esistente (ruoli, MFA, blocco)
      roles: currentLoadedUser ? currentLoadedUser.roles : undefined,
      blocked: currentLoadedUser ? currentLoadedUser.blocked : undefined,
      confirmed: currentLoadedUser ? currentLoadedUser.confirmed : undefined,
      mfaSettings: currentLoadedUser ? currentLoadedUser.mfaSettings : undefined
    };

    // Invocazione API di aggiornamento
    editUser(userData, finalIdentifier);
  });
});

/**
 * Recupera le informazioni dell'utente da modificare.
 * Prova prima a recuperare dal cache locale (`localStorage`) e se mancante esegue
 * una chiamata HTTP GET `/v1/users` al backend.
 */
function getUser() {
  const urlParams = window.location.href;
  let identifier = null;
  if (urlParams.includes("edit/")) {
    identifier = urlParams.split("edit/")[1].split("/")[0].split("?")[0].split("#")[0];
  } else {
    const params = new URLSearchParams(window.location.search);
    identifier = params.get("id") || params.get("identifier");
  }

  if (!identifier) {
    console.error("Identificativo utente mancante nella richiesta");
    const errTitle = typeof t === "function" ? t("swal_error_title", "Errore") : "Errore";
    const errDesc = typeof t === "function" ? t("user_edit_error_identifier", "Identificativo utente mancante o non valido.") : "Identificativo utente mancante o non valido.";
    return sweetalert("error", errTitle, errDesc);
  }

  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    try {
      const userList = JSON.parse(usersJSON);
      const user = userList.find((u) => u.identifier == identifier || u.username == identifier);
      if (user) {
        populateUserData(user);
        return;
      }
    } catch(e) {
      console.warn("Errore lettura cache utenti locale:", e);
    }
  }

  // Fallback via API reattiva invece di reindirizzare immediatamente
  console.log("Recupero dati utente da API per identificativo:", identifier);
  const token = getCookieOrStorage(config.access_token);
  GET(config.users_url, token).then(async (res) => {
    try {
      const data = await res.json();
      if (data && data.data && Array.isArray(data.data)) {
        localStorage.setItem(config.client_id + "_usersData", JSON.stringify(data.data));
        const user = data.data.find((u) => u.identifier == identifier || u.username == identifier);
        if (user) {
          populateUserData(user);
        } else {
          sweetalert("error", "Utente non trovato", `Nessun utente trovato con ID: ${identifier}`);
        }
      }
    } catch(err) {
      console.error("Errore durante il parsing dei dati utenti:", err);
    }
  }).catch(err => {
    console.error("Errore di rete durante il recupero utente:", err);
  });
}

/**
 * Popola tutti i campi del modulo HTML con i dati anagrafici e le impostazioni dell'utente.
 * Supporta sia le nuove chiavi DTO che gli alias legacy.
 *
 * @param {Object} user DTO dell'utente da mostrare nel form
 */
function populateUserData(user) {
  currentLoadedUser = user;

  $("#name").val(user.name || user.firstName || "");
  $("#surname").val(user.surname || user.lastName || "");
  $("#birthdate").val(user.birthDate || user.birthdate || "");
  $("#gender").val(user.gender || "");
  $("#nationality").val(user.nationality || "");
  $("#ssn").val(user.ssn || "");
  $("#email").val(user.email || "");

  const phoneVal = user.phoneNumber || user.phone || "";
  if (phoneVal) {
    const match = phoneVal.match(/^\+\d+/);
    if (match) {
      $("#phone_prefix").val(match[0]);
      $("#phone").val(phoneVal.replace(/^\+\d+\s*/, ""));
    } else {
      $("#phone").val(phoneVal);
    }
  }

  $("#validationDefaultUsername").val(user.username || "");
  $("#occupation").val(user.occupation || "");
  $("#education").val(user.education || "");

  // Gestione attributi personalizzati
  if (user.attributes) {
    $("#attributes").val(JSON.stringify(user.attributes, null, 2));
    if (typeof loadAttributesIntoRepeater === "function") {
      loadAttributesIntoRepeater(user.attributes);
    }
  } else if (typeof loadAttributesIntoRepeater === "function") {
    loadAttributesIntoRepeater({});
  }

  // Foto profilo
  const photo = user.profilePhoto || user.profile;
  if (photo) {
    $("#profile").attr("src", photo).show();
  }
}

/**
 * Invia i dati modificati al backend tramite HTTP PUT verso l'endpoint REST `/v1/users/{identifier}`.
 * Gestisce l'aggiornamento atomico della cache locale e notifica l'utente con feedback visivo.
 *
 * @param {Object} userData Payload contenente tutti i campi anagrafici aggiornati
 * @param {string} identifier Identificatore univoco UUID dell'utente
 */
function editUser(userData, identifier) {
  const finalId = identifier || userData.identifier;
  if (!finalId) {
    const errTitle = typeof t === "function" ? t("swal_error_title", "Errore") : "Errore";
    const errDesc = typeof t === "function" ? t("user_edit_error_identifier", "Identificativo utente mancante o non valido.") : "Identificativo utente mancante o non valido.";
    return sweetalert("error", errTitle, errDesc);
  }

  const token = getCookieOrStorage(config.access_token);
  const targetUrl = (config.users_url || (config.baseUrl + "/v1/users")) + "/" + encodeURIComponent(finalId);

  // Disabilita il pulsante di salvataggio per prevenire submit multipli
  const $submitBtn = $("#edit_user_button");
  if ($submitBtn.length > 0) {
    $submitBtn.prop("disabled", true);
  }

  PUT(targetUrl, token, userData)
    .then(async (res) => {
      let data = {};
      try {
        data = await res.json();
      } catch (e) {}

      if (res.ok && (!data.error || data.error == null)) {
        // Invalida la cache locale degli utenti per forzare il refresh alla pagina di elenco
        localStorage.removeItem(config.client_id + "_usersData");

        const successTitle = typeof t === "function" ? t("user_edit_success_title", "Profilo Aggiornato!") : "Profilo Aggiornato!";
        const successMsg = typeof t === "function" ? t("user_edit_success_desc", "I dati del profilo utente sono stati aggiornati con successo.") : "I dati del profilo utente sono stati aggiornati con successo.";
        sweetalert("success", successTitle, successMsg);

        setTimeout(() => {
          window.location.href = "/app/users";
        }, 1200);
      } else {
        if ($submitBtn.length > 0) {
          $submitBtn.prop("disabled", false);
        }
        const errMsg = (data && data.error)
          ? (data.error.message || data.error.exceptionMessage || "Impossibile aggiornare i dati del profilo.")
          : (data && data.message ? data.message : "Impossibile aggiornare i dati del profilo.");
        const errTitle = typeof t === "function" ? t("swal_error_title", "Errore") : "Errore";
        sweetalert("error", errTitle, errMsg);
      }
    })
    .catch((err) => {
      if ($submitBtn.length > 0) {
        $submitBtn.prop("disabled", false);
      }
      console.error("Errore di connessione durante l'aggiornamento utente:", err);
      const errTitle = typeof t === "function" ? t("swal_error_title", "Errore di Rete") : "Errore di Rete";
      const errNetwork = typeof t === "function" ? t("user_edit_error_network", "Connessione fallita. Impossibile aggiornare i dati utente.") : "Connessione fallita. Impossibile aggiornare i dati utente.";
      sweetalert("error", errTitle, errNetwork);
    });
}
