function getFieldLabel($field) {
  const id = $field.attr("id");
  if (id) {
    const $label = $(`label[for='${id}']`);
    if ($label.length > 0) {
      return $label.text().replace("*", "").trim();
    }
  }
  const placeholder = $field.attr("placeholder");
  if (placeholder) return placeholder.trim();
  const name = $field.attr("name") || id;
  return name || "Campo";
}

function attachRemoveInvalidListener($el) {
  const handler = function () {
    $el.removeClass("is-invalid");
    $el.off("input change", handler);
  };
  $el.on("input change", handler);
}

function validateUserRegistrationForm() {
  const errors = [];
  const invalidFields = [];

  const $name = $("#name");
  const $surname = $("#surname");
  const $username = $("#validationDefaultUsername");
  const $email = $("#email");
  const $password = $("#password");
  const $passwordConfirm = $("#confirm_password");
  const $clientSelect = $("#client_id_select");
  const $terms = $("#invalidCheck2");
  const $attributes = $("#attributes");

  // Nome
  if (!$name.val() || !$name.val().trim()) {
    errors.push("<b>Nome</b> è obbligatorio");
    invalidFields.push($name);
  }

  // Cognome
  if (!$surname.val() || !$surname.val().trim()) {
    errors.push("<b>Cognome</b> è obbligatorio");
    invalidFields.push($surname);
  }

  // Username
  if (!$username.val() || !$username.val().trim()) {
    errors.push("<b>Username</b> è obbligatorio");
    invalidFields.push($username);
  }

  // Email
  const emailVal = $email.val() ? $email.val().trim() : "";
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailVal) {
    errors.push("<b>Email</b> è obbligatoria");
    invalidFields.push($email);
  } else if (!emailRegex.test(emailVal)) {
    errors.push("<b>Email</b> non ha un formato valido (es. nome@dominio.com)");
    invalidFields.push($email);
  }

  // Password
  const passVal = $password.val() || "";
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.,])[A-Za-z\d@$!%*?&.,]{8,20}$/;
  if (!passVal) {
    errors.push("<b>Password</b> è obbligatoria");
    invalidFields.push($password);
    $("#password_rules_container").slideDown(150);
  } else if (!passwordRegex.test(passVal)) {
    errors.push("<b>Password</b> non conforme (richiesti 8-20 caratteri, almeno una maiuscola, una minuscola, un numero e un carattere speciale)");
    invalidFields.push($password);
    $("#password_rules_container").slideDown(150);
    $("#validationPassword").show();
  }

  // Confirm Password
  const confirmVal = $passwordConfirm.val() || "";
  if (!confirmVal) {
    errors.push("<b>Conferma Password</b> è obbligatoria");
    invalidFields.push($passwordConfirm);
  } else if (confirmVal !== passVal) {
    errors.push("Le password inserite non corrispondono");
    invalidFields.push($passwordConfirm);
    $("#validationConfirmPassword").show();
  }

  // Client ID
  if (!$clientSelect.val() || !$clientSelect.val().trim()) {
    errors.push("<b>Client ID Associato</b> è obbligatorio (seleziona un client dall'elenco)");
    invalidFields.push($clientSelect);
  }

  // Ruoli Client
  let selectedRoles = [];
  try {
    selectedRoles = JSON.parse(localStorage.getItem("selected_roles") || "[]");
  } catch(e) {}
  const hasRoleCards = $(".role-card").length > 0;
  if (!hasRoleCards && selectedRoles.length === 0) {
    errors.push("<b>Ruolo Client</b> obbligatorio: seleziona un ruolo dal menu a tendina e clicca su <b>'Aggiungi'</b>");
    invalidFields.push($("#role_select"));
  }

  // Termini e condizioni
  if (!$terms.is(":checked")) {
    errors.push("Devi accettare i <b>termini e condizioni d'uso</b> del servizio");
    invalidFields.push($terms);
  }

  // Attributi Custom JSON
  if ($attributes.val() && $attributes.val().trim()) {
    try {
      JSON.parse($attributes.val().trim());
    } catch(e) {
      errors.push("<b>Attributi Custom</b> contiene una sintassi JSON non valida");
      invalidFields.push($attributes);
    }
  }

  if (errors.length > 0) {
    invalidFields.forEach($el => {
      $el.addClass("is-invalid");
      attachRemoveInvalidListener($el);
    });

    if (invalidFields.length > 0 && invalidFields[0].length > 0) {
      invalidFields[0].focus();
      const rawEl = invalidFields[0][0];
      if (rawEl && typeof rawEl.scrollIntoView === "function") {
        rawEl.scrollIntoView({ behavior: "smooth", block: "center" });
      }
    }

    Swal.fire({
      icon: "warning",
      title: (typeof t === 'function' ? t("swal_missing_fields_title", "Campi Obbligatori Mancanti") : "Campi Obbligatori Mancanti"),
      html: `<div style="text-align: left; font-size: 13px; color: var(--theme-accent, #D0BCFF);">
        ${typeof t === 'function' ? t("swal_missing_user_fields_desc", "Per completare la registrazione dell'utente, compila o correggi i seguenti campi:") : "Per completare la registrazione dell'utente, compila o correggi i seguenti campi:"}
        <ul style="margin-top: 10px; margin-left: 15px; list-style-type: disc; color: #F87171; line-height: 1.6;">
          ${errors.map(e => `<li>${e}</li>`).join("")}
        </ul>
      </div>`,
      confirmButtonText: (typeof t === 'function' ? t("swal_understood_btn", "Ho capito") : "Ho capito")
    });
    return false;
  }

  return true;
}

function validateUserEditForm() {
  const errors = [];
  const invalidFields = [];

  const $name = $("#name");
  const $surname = $("#surname");
  const $username = $("#validationDefaultUsername");
  const $email = $("#email");
  const $terms = $("#invalidCheck2");

  if (!$name.val() || !$name.val().trim()) {
    errors.push("<b>Nome</b> è obbligatorio");
    invalidFields.push($name);
  }

  if (!$surname.val() || !$surname.val().trim()) {
    errors.push("<b>Cognome</b> è obbligatorio");
    invalidFields.push($surname);
  }

  if (!$username.val() || !$username.val().trim()) {
    errors.push("<b>Username</b> è obbligatorio");
    invalidFields.push($username);
  }

  const emailVal = $email.val() ? $email.val().trim() : "";
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailVal) {
    errors.push("<b>Email</b> è obbligatoria");
    invalidFields.push($email);
  } else if (!emailRegex.test(emailVal)) {
    errors.push("<b>Email</b> non ha un formato valido");
    invalidFields.push($email);
  }

  if ($terms.length > 0 && !$terms.is(":checked")) {
    errors.push("Devi accettare i <b>termini e condizioni d'uso</b>");
    invalidFields.push($terms);
  }

  if (errors.length > 0) {
    invalidFields.forEach($el => {
      $el.addClass("is-invalid");
      attachRemoveInvalidListener($el);
    });

    if (invalidFields.length > 0 && invalidFields[0].length > 0) {
      invalidFields[0].focus();
      const rawEl = invalidFields[0][0];
      if (rawEl && typeof rawEl.scrollIntoView === "function") {
        rawEl.scrollIntoView({ behavior: "smooth", block: "center" });
      }
    }

    Swal.fire({
      icon: "warning",
      title: (typeof t === 'function' ? t("swal_missing_fields_title", "Campi Obbligatori Mancanti") : "Campi Obbligatori Mancanti"),
      html: `<div style="text-align: left; font-size: 13px; color: var(--theme-accent, #D0BCFF);">
        ${typeof t === 'function' ? t("swal_missing_user_edit_fields_desc", "Per salvare le modifiche, compila o correggi i seguenti campi obbligatori:") : "Per salvare le modifiche, compila o correggi i seguenti campi obbligatori:"}
        <ul style="margin-top: 10px; margin-left: 15px; list-style-type: disc; color: #F87171; line-height: 1.6;">
          ${errors.map(e => `<li>${e}</li>`).join("")}
        </ul>
      </div>`,
      confirmButtonText: (typeof t === 'function' ? t("swal_understood_btn", "Ho capito") : "Ho capito")
    });
    return false;
  }

  return true;
}

/**
 * Live Password Validation & Strength Meter
 */
function initLivePasswordValidation() {
  const $password = $("#password");
  const $confirm = $("#confirm_password");
  const $rulesContainer = $("#password_rules_container");
  const $strengthContainer = $("#password_strength_container");
  const $strengthBar = $("#password_strength_bar");
  const $strengthText = $("#password_strength_text");
  const $rulesCounter = $("#rules_counter");
  const $confirmFeedback = $("#confirm_password_feedback");
  const $confirmIcon = $("#confirm_icon");
  const $confirmText = $("#confirm_text");
  const $disallowedWarn = $("#rule_disallowed");

  if (!$password.length) return;

  function updateRule(ruleId, isPassed) {
    const $el = $(`#${ruleId}`);
    if (!$el.length) return isPassed ? 1 : 0;
    const $icon = $el.find("i");
    if (isPassed) {
      $icon
        .removeClass("fa-circle text-gray-500 text-[7px]")
        .addClass("fa-circle-check text-emerald-400 text-xs");
      $el
        .removeClass("text-gray-400")
        .addClass("text-emerald-300 font-medium");
    } else {
      $icon
        .removeClass("fa-circle-check text-emerald-400 text-xs")
        .addClass("fa-circle text-gray-500 text-[7px]");
      $el
        .removeClass("text-emerald-300 font-medium")
        .addClass("text-gray-400");
    }
    return isPassed ? 1 : 0;
  }

  function evaluatePassword() {
    const pass = $password.val() || "";

    if (pass.length === 0) {
      $strengthContainer.slideUp(150);
      $rulesContainer.slideUp(150);
      $password.removeClass("is-valid is-invalid");
      $("#validationPassword").hide();
      evaluateConfirm();
      return;
    }

    $strengthContainer.slideDown(150);
    $rulesContainer.slideDown(150);

    const isLengthValid = pass.length >= 8 && pass.length <= 20;
    const hasUpper = /[A-Z]/.test(pass);
    const hasLower = /[a-z]/.test(pass);
    const hasNumber = /\d/.test(pass);
    const hasSpecial = /[@$!%*?&.,]/.test(pass);
    const hasOnlyAllowed = /^[A-Za-z\d@$!%*?&.,]*$/.test(pass);

    let score = 0;
    score += updateRule("rule_length", isLengthValid);
    score += updateRule("rule_uppercase", hasUpper);
    score += updateRule("rule_lowercase", hasLower);
    score += updateRule("rule_number", hasNumber);
    score += updateRule("rule_special", hasSpecial);

    if ($rulesCounter.length) {
      $rulesCounter.text(`${score}/5`);
    }

    // Caratteri non ammessi
    if (!hasOnlyAllowed) {
      $disallowedWarn.show();
    } else {
      $disallowedWarn.hide();
    }

    // Calcolo robustezza grafica
    if (score <= 2) {
      $strengthBar.css({ width: `${Math.max(score * 20, 15)}%`, backgroundColor: "#EF4444" });
      $strengthText.text("Debole").css("color", "#F87171");
    } else if (score === 3 || score === 4) {
      $strengthBar.css({ width: `${score * 20}%`, backgroundColor: "#F59E0B" });
      $strengthText.text("Media").css("color", "#FBBF24");
    } else if (score === 5 && hasOnlyAllowed) {
      $strengthBar.css({ width: "100%", backgroundColor: "#10B981" });
      $strengthText.text("Forte & Conforme ✓").css("color", "#34D399");
    } else {
      $strengthBar.css({ width: "90%", backgroundColor: "#F59E0B" });
      $strengthText.text("Carattere non ammesso").css("color", "#F87171");
    }

    // Se completamente valida e conforme
    if (score === 5 && hasOnlyAllowed) {
      $password.removeClass("is-invalid").addClass("is-valid");
      $("#validationPassword").hide();
    } else {
      $password.removeClass("is-valid");
    }

    evaluateConfirm();
  }

  function evaluateConfirm() {
    const pass = $password.val() || "";
    const confirm = $confirm.val() || "";

    if (confirm.length === 0) {
      $confirmFeedback.slideUp(150);
      $confirm.removeClass("is-valid is-invalid");
      $("#validationConfirmPassword").hide();
      return;
    }

    $confirmFeedback.slideDown(150);

    if (pass === confirm && pass.length > 0) {
      $confirmIcon
        .removeClass("fa-circle-xmark text-red-400 text-xs fa-circle text-gray-500 text-[8px]")
        .addClass("fa-circle-check text-emerald-400 text-xs");
      $confirmText
        .removeClass("text-red-400")
        .addClass("text-emerald-300 font-medium")
        .text("Le password corrispondono");
      $confirm.removeClass("is-invalid").addClass("is-valid");
      $("#validationConfirmPassword").hide();
    } else {
      $confirmIcon
        .removeClass("fa-circle-check text-emerald-400 text-xs fa-circle text-gray-500 text-[8px]")
        .addClass("fa-circle-xmark text-red-400 text-xs");
      $confirmText
        .removeClass("text-emerald-300 font-medium")
        .addClass("text-red-400")
        .text("Le password non corrispondono");
      $confirm.removeClass("is-valid");
      if (confirm.length >= pass.length && pass.length > 0) {
        $confirm.addClass("is-invalid");
      }
    }
  }

  $password.on("focus", function () {
    $rulesContainer.slideDown(150);
    if ($(this).val().length > 0) {
      $strengthContainer.slideDown(150);
    }
  });

  $password.on("blur", function () {
    if (!$(this).val()) {
      $rulesContainer.slideUp(150);
      $strengthContainer.slideUp(150);
    }
  });

  $password.on("input", evaluatePassword);
  $confirm.on("input", evaluateConfirm);
}

$(document).ready(function () {
  const registration = document.getElementById("registration_form");
  const edit = document.getElementById("edit_form");

  // Keep submit buttons enabled and reactive at all times
  $("button[type='submit']").prop("disabled", false);

  if (registration) {
    // Inizializza validazione live della password
    initLivePasswordValidation();

    // Remove is-invalid upon selection
    $("#client_id_select").on("change", function () {
      if ($(this).val()) $(this).removeClass("is-invalid");
    });
    $("#role_select").on("change", function () {
      if ($(this).val()) $(this).removeClass("is-invalid");
    });
  }

  if (edit) {
    $("#edit_form input, #edit_form select").on("input change", function () {
      if ($(this).val()) $(this).removeClass("is-invalid");
    });
  }
});

