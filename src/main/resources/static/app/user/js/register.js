function refreshClients() {
  console.log("Refreshing Clients...");
  localStorage.removeItem(config.client_id + "_clients");
  refreshAnimation("refresh-icon");
  getClient();
  refreshAnimation("refresh-icon");
}

$(document).ready(function () {
  const textarea = $("#attributes");

  // Auto-espansione e auto-formattazione mentre scrivi
  textarea.on("input", function () {
    try {
      const parsedJson = JSON.parse(this.value); // Prova a parsare il JSON
      this.value = JSON.stringify(parsedJson, null, 2); // Riformatta con indentazione
      textarea.removeClass("is-invalid");
      $("#validationAttributes").text("");
    } catch (e) {
      // Ignora l'errore finché il JSON non è valido
      $("#validationAttributes").text(
        currentTranslations.edit_attributes_valid
      );
      textarea.addClass("is-invalid");
      $("#register_form_submit").prop("disabled", true);
    }

    // Auto-espansione del campo
    this.style.height = "auto";
    this.style.height = this.scrollHeight + "px";
  });

  // Formatta inizialmente il JSON già presente
  if (textarea.val().trim() !== "") {
    try {
      const parsedJson = JSON.parse(textarea.val());
      textarea.val(JSON.stringify(parsedJson, null, 2));
    } catch (e) {
      // Il JSON iniziale potrebbe essere non valido, quindi lo lasciamo così com'è
    }
  }
});

$(document).ready(function () {
  // Disabilita il bottone all'inizio
  $("#add_role_btn").prop("disabled", true);

  // Ascolta il cambiamento del select dei ruoli
  $("#role_select").on("change", function () {
    const selectedRole = $(this).val();
    const alreadyAdded =
      $(".role-card span").filter(function () {
        return $(this).text() === selectedRole;
      }).length > 0;

    // Abilita il bottone solo se il ruolo è stato selezionato e non è già aggiunto
    $("#add_role_btn").prop("disabled", !selectedRole || alreadyAdded);
  });

  refreshAnimation("refresh-icon");
  const clientsJSON = localStorage.getItem(config.client_id + "_clients");
  let clients;
  if (clientsJSON) {
    clients = JSON.parse(clientsJSON);
    displayClientData(clients);
  } else getClient();
  refreshAnimation("refresh-icon");

  let profileImage;

  document.getElementById("file").addEventListener("change", function (event) {
    const file = event.target.files[0]; // Ottieni il file selezionato

    if (file) {
      // Verifica il tipo di file
      const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
      if (!allowedTypes.includes(file.type)) {
        //sweetalert(
        //  "error",
        //  "Invalid Image",
        //  "Invalid file type. Please upload a JPEG, PNG, or WEBP image."
        //);
        sweetalert(
          "error",
          currentTranslations.register_form_profile_invalid_title,
          currentTranslations.register_form_profile_invalid_text
        );
        event.target.value = ""; // Resetta l'input file
        return;
      }

      // Converte il file in Base64
      const reader = new FileReader();
      reader.onload = function () {
        const base64String = reader.result; // Il risultato include già il prefisso corretto

        // Mostra l'anteprima dell'immagine
        const profileImg = document.getElementById("profile");
        profileImg.src = base64String;
        profileImg.style.display = "block"; // Rendi visibile l'immagine
        profileImage = base64String;
      };
      reader.readAsDataURL(file);
    }
  });

  $(".form").on("submit", function (event) {
    event.preventDefault(); // Evita il ricaricamento della pagina
    // Recupero i valori dai campi del modulo
    const roles = JSON.parse(localStorage.getItem("selected_roles") || []);
    const profilePhoto =
      profileImage || "https://bootdey.com/img/Content/avatar/avatar7.png";
    const attributes = $("#attributes").val().trim() || null;
    const prefix = $("#phone_prefix").val();
    const phone = $("#phone").val().trim();
    let phoneNumber;
    if (phone && prefix)
      phoneNumber = $("#phone_prefix").val() + " " + $("#phone").val().trim();

    let userData = {
      firstName: $("#name").val().trim(),
      lastName: $("#surname").val().trim(),
      birthdate: $("#birthdate").val(),
      gender: $("#gender").val(),
      nationality: $("#nationality").val().trim(),
      ssn: $("#ssn").val().trim(),
      email: $("#email").val().trim(),
      phone: phoneNumber,
      username: $("#validationDefaultUsername").val().trim(),
      password: $("#password").val(),
      confirmPassword: $("#confirm_password").val(),
      occupation: $("#occupation").val().trim(),
      education: $("#education").val().trim(),
      attributes: attributes ? JSON.parse(attributes) : null,
      clientId: $("#client_id_select").val(), // ID Cliente
      //roles: [$("#role_select").val()], // Ruoli
      roles: roles, // Ruoli
      profile: profilePhoto,
    };
    registerUser(userData, clients);
  });
});

function registerUser(userForm, clients) {
  const userClient = clients.find(
    (client) => client.clientId == userForm.clientId
  );
  const registrationUrl =
    config.register_user_url +
    "?client_id=" +
    userClient.clientId +
    "&registration_token=" +
    userClient.registrationToken;
  const token = getCookieOrStorage(config.access_token);

  const user = {
    name: userForm.firstName,
    surname: userForm.lastName,
    email: userForm.email,
    username: userForm.username,
    password: userForm.password,
    roles: userForm.roles,
    profilePhoto: userForm.profile,
    phoneNumber: userForm.phone,
    birthDate: userForm.birthdate,
    gender: userForm.gender,
    ssn: userForm.ssn,
    education: userForm.education,
    occupation: userForm.occupation,
    nationality: userForm.nationality,
    attributes: userForm.attributes,
  };

  POST(registrationUrl, token, user).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      localStorage.removeItem("selected_roles");
      return sweetalert(
        "success",
        currentTranslations.register_form_confirm,
        responseData.message
      );
    }
  });
}

function getClient() {
  const url = config.client_id_url;
  const token = getCookieOrStorage(config.access_token);

  GET(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.setItem(
        config.client_id + "_clients",
        JSON.stringify(responseData.data)
      );
      displayClientData(responseData.data);
    }
  });
}

function displayClientData(clients) {
  const clientSelect = $("#client_id_select");
  clientSelect.empty();
  clientSelect.append(
    '<option id="register_form_client_choose" selected disabled value="">' +
      currentTranslations.register_form_client_choose +
      "</option>"
  );

  clients.forEach((client) => {
    const option = $("<option>").val(client.clientId).text(client.clientId);
    clientSelect.append(option);
  });

  function populateRoles(clientId) {
    const selectedClient = clients.find(
      (client) => client.clientId === clientId
    );

    if (selectedClient && selectedClient.appRoles) {
      const roleSelect = $("#role_select");
      roleSelect.empty();
      roleSelect.append(
        '<option id="register_form_roles_choose" selected disabled value="">' +
          currentTranslations.register_form_roles_choose +
          "</option>"
      );

      selectedClient.appRoles.forEach((role) => {
        if (role.role) {
          const option = $("<option>").val(role.role).text(role.role);
          roleSelect.append(option);
        }
      });
      roleSelect.prop("disabled", false);
    }
  }

  $("#client_id_select").on("change", function () {
    const selectedClientId = $(this).val();
    if (selectedClientId) {
      populateRoles(selectedClientId);
    } else {
      $("#role_select")
        .prop("disabled", true)
        .empty()
        .append('<option selected disabled value="">Choose...</option>');
    }
  });
}

function addRole() {
  $("#role_select").on("change", function () {
    const selectedRole = $(this).val();
    const alreadyAdded =
      $(".role-card span").filter(function () {
        return $(this).text() === selectedRole;
      }).length > 0;

    $("#add_role_btn").prop("disabled", !selectedRole || alreadyAdded);
  });

  const selectedRole = $("#role_select").val();
  if (!selectedRole) return;

  const alreadyAdded =
    $(".role-card span").filter(function () {
      return $(this).text() === selectedRole;
    }).length > 0;

  if (alreadyAdded) {
    $("#add_role_btn").prop("disabled", true);
    return;
  }

  const roleContainer = $("#role_container");
  const roleCard = $(`
      <div class="card m-1 col role-card">
        <div class="card-body">
          <span><code style="color: inherit">${selectedRole}</code></span>
          <i class="fa-duotone fa-solid fa-trash-xmark remove-role clickable float-end mt-1"></i>
        </div>
      </div>
    `);

  roleCard.find(".remove-role").on("click", function () {
    roleCard.remove();
    updateStoredRoles();
    if ($(".role-card").length === 0) {
      $("#client_id_select").prop("disabled", false);
    }
    $("#role_select option[value='" + selectedRole + "']").prop(
      "disabled",
      false
    );
    $("#add_role_btn").prop("disabled", false);
  });

  roleContainer.append(roleCard);
  updateStoredRoles();
  $("#client_id_select").prop("disabled", true);
  $("#role_select option[value='" + selectedRole + "']").prop("disabled", true);
  $("#add_role_btn").prop("disabled", true);
}

function updateStoredRoles() {
  const roles = $(".role-card span")
    .map(function () {
      return $(this).text();
    })
    .get();
  localStorage.setItem("selected_roles", JSON.stringify(roles));
}

function addImageUrl() {
  return inputSweetAlert(
    currentTranslations.register_form_profile_image_upload,
    currentTranslations.inputGroupFileAddon04
  ).then((result) => {
    if (result.isConfirmed) {
      profileImage = result.value;
      const profileImg = document.getElementById("profile");
      profileImg.src = profileImage;
      profileImg.style.display = "block"; // Rendi visibile l'immagine
    }
  });
}
