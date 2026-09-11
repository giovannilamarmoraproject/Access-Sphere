let cachedClientsData = [];

function refreshClients() {
  console.log("Refreshing Clients...");
  localStorage.removeItem(config.client_id + "_clients");
  refreshAnimation("refresh-icon");
  getClient();
  refreshAnimation("refresh-icon");
}

$(document).ready(function () {
  const textarea = $("#attributes");

  textarea.on("input", function () {
    try {
      const parsedJson = JSON.parse(this.value);
      this.value = JSON.stringify(parsedJson, null, 2);
      textarea.removeClass("is-invalid");
      $("#validationAttributes").text("");
    } catch (e) {
      $("#validationAttributes").text(
        currentTranslations.edit_attributes_valid
      );
      textarea.addClass("is-invalid");
      $("#register_form_submit").prop("disabled", true);
    }

    this.style.height = "auto";
    this.style.height = this.scrollHeight + "px";
  });

  if (textarea.val() && textarea.val().trim() !== "") {
    try {
      const parsedJson = JSON.parse(textarea.val());
      textarea.val(JSON.stringify(parsedJson, null, 2));
    } catch (e) {}
  }
});

$(document).ready(function () {
  $("#add_role_btn").prop("disabled", true);

  $("#role_select").on("change", function () {
    const selectedRole = $(this).val();
    const alreadyAdded =
      $(".role-card span").filter(function () {
        return $(this).text() === selectedRole;
      }).length > 0;

    $("#add_role_btn").prop("disabled", !selectedRole || alreadyAdded);
  });

  refreshAnimation("refresh-icon");
  const clientsJSON = localStorage.getItem(config.client_id + "_clients");
  if (clientsJSON) {
    try {
      cachedClientsData = JSON.parse(clientsJSON);
      displayClientData(cachedClientsData);
    } catch(e) {
      getClient();
    }
  } else {
    getClient();
  }
  refreshAnimation("refresh-icon");

  let profileImage;

  const fileInput = document.getElementById("file");
  if (fileInput) {
    fileInput.addEventListener("change", function (event) {
      const file = event.target.files[0];

      if (file) {
        const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
        if (!allowedTypes.includes(file.type)) {
          sweetalert(
            "error",
            currentTranslations.register_form_profile_invalid_title,
            currentTranslations.register_form_profile_invalid_text
          );
          event.target.value = "";
          return;
        }

        const reader = new FileReader();
        reader.onload = function () {
          const base64String = reader.result;
          const profileImg = document.getElementById("profile");
          if (profileImg) {
            profileImg.src = base64String;
            profileImg.style.display = "block";
          }
          profileImage = base64String;
        };
        reader.readAsDataURL(file);
      }
    });
  }

  $(".form").on("submit", function (event) {
    event.preventDefault();
    const roles = JSON.parse(localStorage.getItem("selected_roles") || "[]");
    const profilePhoto =
      profileImage || "https://bootdey.com/img/Content/avatar/avatar7.png";
    const attributes = $("#attributes").val() ? $("#attributes").val().trim() : null;
    const prefix = $("#phone_prefix").val();
    const phone = $("#phone").val() ? $("#phone").val().trim() : "";
    let phoneNumber;
    if (phone && prefix)
      phoneNumber = prefix + " " + phone;

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
      clientId: $("#client_id_select").val(),
      roles: roles,
      profile: profilePhoto,
    };
    registerUser(userData, cachedClientsData);
  });
});

function registerUser(userForm, clients) {
  let userClient = (clients || []).find(
    (client) => client.clientId == userForm.clientId
  );

  if (!userClient) {
    const stored = localStorage.getItem(config.client_id + "_clients");
    if (stored) {
      const parsed = JSON.parse(stored);
      userClient = parsed.find(c => c.clientId == userForm.clientId);
    }
  }

  if (!userClient) {
    return sweetalert("error", "Errore Client", "Seleziona un Client ID valido per proseguire.");
  }

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
      sweetalert(
        "success",
        currentTranslations.register_form_confirm,
        responseData.message
      );
      setTimeout(() => {
        window.location.href = "/app/users";
      }, 1500);
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
      cachedClientsData = responseData.data || [];
      localStorage.setItem(
        config.client_id + "_clients",
        JSON.stringify(cachedClientsData)
      );
      displayClientData(cachedClientsData);
    }
  });
}

function displayClientData(clients) {
  const clientSelect = $("#client_id_select");
  clientSelect.empty();
  clientSelect.append(
    '<option id="register_form_client_choose" selected disabled value="">' +
      (currentTranslations.register_form_client_choose || "Scegli un Client...") +
      "</option>"
  );

  (clients || []).forEach((client) => {
    const option = $("<option>").val(client.clientId).text(client.clientId);
    clientSelect.append(option);
  });

  function populateRoles(clientId) {
    const selectedClient = (clients || []).find(
      (client) => client.clientId === clientId
    );

    if (selectedClient && selectedClient.appRoles) {
      const roleSelect = $("#role_select");
      roleSelect.empty();
      roleSelect.append(
        '<option id="register_form_roles_choose" selected disabled value="">' +
          (currentTranslations.register_form_roles_choose || "Scegli un Ruolo...") +
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

  $("#client_id_select").off("change").on("change", function () {
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
  const selectedRole = $("#role_select").val();
  if (!selectedRole) return;

  const roles = JSON.parse(localStorage.getItem("selected_roles") || "[]");
  if (!roles.includes(selectedRole)) {
    roles.push(selectedRole);
    localStorage.setItem("selected_roles", JSON.stringify(roles));
    renderRolesList(roles);
  }
  $("#add_role_btn").prop("disabled", true);
  $("#role_select").val("");
}

function removeRole(roleToRemove) {
  let roles = JSON.parse(localStorage.getItem("selected_roles") || "[]");
  roles = roles.filter(r => r !== roleToRemove);
  localStorage.setItem("selected_roles", JSON.stringify(roles));
  renderRolesList(roles);
}

function renderRolesList(roles) {
  const container = $("#roles_list_container");
  if (!container.length) return;
  container.empty();
  roles.forEach(role => {
    container.append(`
      <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-mono bg-purple-500/20 text-purple-300 border border-purple-500/30">
        ${role}
        <button type="button" onclick="removeRole('${role}')" class="text-gray-400 hover:text-red-400 ml-1 cursor-pointer">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </span>
    `);
  });
}
