$(document).ready(function () {
  getClients();
  getUser();

  // Disabilita il bottone all'inizio
  $("#add_role_btn").prop("disabled", true);

  // Event listener per il cambio di ruolo o client
  $(document).on(
    "change",
    "#role_select, #client_id_select",
    validateRoleSelection
  );

  // Gestione click sul bottone "Add Role"
  $("#add_role_btn").on("click", addRole);
});

function getClients() {
  const clientsJSON = localStorage.getItem(config.client_id + "_clients");
  if (clientsJSON) {
    displayClientData(JSON.parse(clientsJSON));
  } else {
    getClient();
  }
}

function getClient() {
  const url = config.client_id_url;
  const token = getCookieOrStorage(config.access_token);

  GET(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    }
    fetchHeader(data.headers);
    localStorage.setItem(
      config.client_id + "_clients",
      JSON.stringify(responseData.data)
    );
    displayClientData(responseData.data);
  });
}

function displayClientData(clients) {
  const clientSelect = $("#client_id_select")
    .empty()
    .append(
      '<option id="register_form_client_choose" selected disabled value="">' +
        currentTranslations.register_form_client_choose +
        "</option>"
    );

  clients.forEach((client) => {
    $("<option>")
      .val(client.clientId)
      .text(client.clientId)
      .appendTo(clientSelect);
  });

  $("#client_id_select").on("change", function () {
    const selectedClient = clients.find(
      (client) => client.clientId === $(this).val()
    );
    const roleSelect = $("#role_select")
      .empty()
      .append(
        '<option id="register_form_roles_choose" selected disabled value="">' +
          currentTranslations.register_form_roles_choose +
          "</option>"
      );

    if (selectedClient?.appRoles) {
      selectedClient.appRoles.forEach((role) => {
        if (role.role) {
          $("<option>").val(role.role).text(role.role).appendTo(roleSelect);
        }
      });
      roleSelect.prop("disabled", false);
    } else {
      roleSelect.prop("disabled", true);
    }
  });
}

function validateRoleSelection() {
  const selectedRole = $("#role_select").val();
  const alreadyAdded =
    $(".role-card span").filter((_, el) => $(el).text() === selectedRole)
      .length > 0;

  if (alreadyAdded) {
    $("#role_select").addClass("is-invalid");
    $("#validationRoles").text("Il ruolo selezionato esiste già.");
    $("#add_role_btn").prop("disabled", true);
  } else {
    $("#role_select").removeClass("is-invalid");
    $("#validationRoles").text("");
    $("#add_role_btn").prop("disabled", !selectedRole); // Abilita solo se un ruolo è selezionato
  }
}

function addRole() {
  const selectedRole = $("#role_select").val();
  if (!selectedRole) return;

  if (
    $(".role-card span").filter((_, el) => $(el).text() === selectedRole)
      .length > 0
  ) {
    $("#role_select").addClass("is-invalid");
    $("#validationRoles").text("Il ruolo selezionato esiste già.");
    $("#add_role_btn").prop("disabled", true);
    return;
  }

  const roleCard = $(`
    <div class="card m-1 col role-card" style="min-width:250px">
      <div class="card-body">
        <span>${selectedRole}</span>
        <i class="fa-solid fa-trash-xmark remove-role clickable float-end mt-1"></i>
      </div>
    </div>
  `);

  roleCard.find(".remove-role").on("click", function () {
    $(this).closest(".role-card").remove();
    updateStoredRoles();
    $("#role_select option[value='" + selectedRole + "']").prop(
      "disabled",
      false
    );
    $("#add_role_btn").prop("disabled", true);
  });

  $("#role_container").append(roleCard);
  updateStoredRoles();
  $("#role_select option[value='" + selectedRole + "']").prop("disabled", true);
  $("#add_role_btn").prop("disabled", true);
  $("#role_select").removeClass("is-invalid");
  $("#validationRoles").text("");
}

function updateStoredRoles() {
  const roles = $(".role-card span")
    .map((_, el) => $(el).text())
    .get();
  localStorage.setItem("selected_roles", JSON.stringify(roles));
}

function getUser() {
  const identifier = window.location.href.split("roles/")[1];
  const usersJSON = localStorage.getItem(config.client_id + "_usersData");

  if (usersJSON) {
    const user = JSON.parse(usersJSON).find((u) => u.identifier == identifier);
    if (user) displayUserData(user);
  } else {
    logout();
  }
}

function displayUserData(user) {
  console.log(user);
  const roleContainer = $("#role_container").empty();

  if (!user.roles || user.roles.length === 0) {
    $("#add_role_btn").prop("disabled", true); // Nessun ruolo presente → disabilita il bottone
  } else {
    user.roles.forEach((role) => {
      const roleCard = $(`
      <div class="card m-1 col role-card" style="min-width:250px">
        <div class="card-body">
          <span>${role}</span>
          <i class="fa-solid fa-trash-xmark remove-role clickable float-end mt-1"></i>
        </div>
      </div>
    `);

      roleContainer.append(roleCard);
    });
  }
  // Event listener per rimuovere i ruoli
  $(document).on("click", ".remove-role", function () {
    $(this).closest(".role-card").remove();
    updateStoredRoles();

    if ($(".role-card").length === 0) {
      $("#client_id_select").prop("disabled", false);
    }
    validateRoleSelection(); // Controlla se il bottone va disabilitato
  });
}

function changeRoles() {
  const identifier = window.location.href.split("roles/")[1];
  const rolesJSON = localStorage.getItem("selected_roles");
  if (!rolesJSON) return;
  const roles = JSON.parse(rolesJSON);
  const changeRolesUrl =
    window.location.origin + "/v1/users/" + identifier + "/roles";
  const token = getCookieOrStorage(config.access_token);
  const body = { roles: roles };
  PUT(changeRolesUrl, token, body).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      resetRolesSaved();
      return sweetalert(
        "success",
        currentTranslations.roles_response_title,
        currentTranslations.roles_response_text
      ).then((result) => {
        /* Read more about isConfirmed, isDenied below */
        if (result.isConfirmed) {
          const origin = window.location.origin;

          // Costruisci l'URL completo aggiungendo il path
          const fullUrl = `${origin}/app/users`;

          // Reindirizza l'utente al nuovo URL
          window.location.href = fullUrl;
        }
      });
    }
  });
}

function resetRolesSaved() {
  localStorage.removeItem("selected_roles");
}
