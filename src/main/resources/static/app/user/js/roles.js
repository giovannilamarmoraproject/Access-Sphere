$(document).ready(function () {
  getUser();

  const clientsJSON = localStorage.getItem(config.client_id + "_clients");
  if (clientsJSON) {
    try {
      displayClientData(JSON.parse(clientsJSON));
    } catch(e) {
      getClient();
    }
  } else {
    getClient();
  }

  $("#role_select").on("change", function () {
    validateRoleSelection();
  });
});

function getClient() {
  const url = config.client_id_url;
  const token = getCookieOrStorage(config.access_token);

  GET(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      console.warn("getClient error in roles:", responseData.error);
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
  const clientSelect = $("#client_id_select").empty().append(
    '<option selected disabled value="">Scegli un Client...</option>'
  );

  (clients || []).forEach((client) => {
    $("<option>").val(client.clientId).text(client.clientId).appendTo(clientSelect);
  });

  $("#client_id_select").off("change").on("change", function () {
    const selectedClient = (clients || []).find(
      (client) => client.clientId === $(this).val()
    );
    const roleSelect = $("#role_select").empty().append(
      '<option selected disabled value="">Scegli un Ruolo...</option>'
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
    $("#add_role_btn").prop("disabled", true);
  });
}

function validateRoleSelection() {
  const selectedRole = $("#role_select").val();
  const alreadyAdded =
    $(".role-chip span[data-role]").filter((_, el) => $(el).attr("data-role") === selectedRole)
      .length > 0;

  if (alreadyAdded) {
    $("#role_select").addClass("is-invalid");
    $("#validationRoles").text("Questo ruolo è già stato assegnato all'utente.");
    $("#add_role_btn").prop("disabled", true);
  } else {
    $("#role_select").removeClass("is-invalid");
    $("#validationRoles").text("");
    $("#add_role_btn").prop("disabled", !selectedRole);
  }
}

function addRole() {
  const selectedRole = $("#role_select").val();
  if (!selectedRole) return;

  const roleCard = $(`
    <div class="role-chip inline-flex items-center gap-2.5 px-4 py-2.5 rounded-2xl border border-purple-500/35 bg-purple-500/20 text-purple-100 shadow-md transition-all hover:border-purple-400 hover:scale-105">
      <div class="w-6 h-6 rounded-full bg-purple-500/30 flex items-center justify-center text-purple-300">
        <i class="fa-solid fa-shield-halved text-xs"></i>
      </div>
      <span data-role="${selectedRole}" class="font-mono text-xs font-bold">${selectedRole}</span>
      <button type="button" class="remove-role ml-1.5 w-5 h-5 rounded-full bg-red-500/15 hover:bg-red-500 hover:text-white text-red-300 transition-all cursor-pointer border-0 flex items-center justify-center" title="Revoca Ruolo">
        <i class="fa-solid fa-xmark text-[11px]"></i>
      </button>
    </div>
  `);

  roleCard.find(".remove-role").on("click", function () {
    $(this).closest(".role-chip").remove();
    updateStoredRoles();
    validateRoleSelection();
  });

  // Remove empty state message if present
  if ($("#role_container .role-chip").length === 0) {
    $("#role_container").empty();
  }

  $("#role_container").append(roleCard);
  updateStoredRoles();
  $("#role_select").val("");
  $("#add_role_btn").prop("disabled", true);
}

function updateStoredRoles() {
  const roles = $(".role-chip span[data-role]")
    .map((_, el) => $(el).attr("data-role"))
    .get();
  localStorage.setItem("selected_roles", JSON.stringify(roles));
  $("#user-roles-count-badge").text(`${roles.length} Ruoli Assegnati`);
  $("#save_role_btn").prop("disabled", false);

  if (roles.length === 0) {
    $("#role_container").html('<span class="text-xs text-purple-300 py-2">Nessun ruolo assegnato. Seleziona un ruolo nel riquadro sottostante per assegnarlo.</span>');
  }
}

function getUser() {
  const urlParams = window.location.href;
  let identifier = null;
  if (urlParams.includes("roles/")) {
    identifier = urlParams.split("roles/")[1].split("/")[0].split("?")[0].split("#")[0];
  } else {
    identifier = new URLSearchParams(window.location.search).get("identifier");
  }

  if (!identifier) return;

  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    try {
      const user = JSON.parse(usersJSON).find((u) => u.identifier == identifier || u.username == identifier);
      if (user) {
        displayUserData(user);
        return;
      }
    } catch(e) {}
  }

  const token = getCookieOrStorage(config.access_token);
  GET(config.users_url, token).then(async (res) => {
    try {
      const data = await res.json();
      if (data && data.data) {
        localStorage.setItem(config.client_id + "_usersData", JSON.stringify(data.data));
        const user = data.data.find((u) => u.identifier == identifier || u.username == identifier);
        if (user) displayUserData(user);
      }
    } catch(e) {}
  });
}

function displayUserData(user) {
  // Update User Profile Header Banner
  if (user) {
    $("#user-full-name").text((user.name || "") + " " + (user.surname || "") || "Utente Access Sphere");
    $("#user-username-badge").text("@" + (user.username || ""));
    $("#user-email-text").text(user.email || "");
    if (user.profilePhoto) {
      $("#user-avatar-img").attr("src", user.profilePhoto);
    }
  }

  const roleContainer = $("#role_container").empty();

  if (!user.roles || user.roles.length === 0) {
    roleContainer.html('<span class="text-xs text-purple-300 py-2">Nessun ruolo assegnato. Seleziona un ruolo nel riquadro sottostante per assegnarlo.</span>');
    $("#user-roles-count-badge").text("0 Ruoli Assegnati");
  } else {
    $("#user-roles-count-badge").text(`${user.roles.length} Ruoli Assegnati`);
    user.roles.forEach((role) => {
      const roleCard = $(`
        <div class="role-chip inline-flex items-center gap-2.5 px-4 py-2.5 rounded-2xl border border-purple-500/35 bg-purple-500/20 text-purple-100 shadow-md transition-all hover:border-purple-400 hover:scale-105">
          <div class="w-6 h-6 rounded-full bg-purple-500/30 flex items-center justify-center text-purple-300">
            <i class="fa-solid fa-shield-halved text-xs"></i>
          </div>
          <span data-role="${role}" class="font-mono text-xs font-bold">${role}</span>
          <button type="button" class="remove-role ml-1.5 w-5 h-5 rounded-full bg-red-500/15 hover:bg-red-500 hover:text-white text-red-300 transition-all cursor-pointer border-0 flex items-center justify-center" title="Revoca Ruolo">
            <i class="fa-solid fa-xmark text-[11px]"></i>
          </button>
        </div>
      `);

      roleCard.find(".remove-role").on("click", function () {
        $(this).closest(".role-chip").remove();
        updateStoredRoles();
      });

      roleContainer.append(roleCard);
    });
  }
}

function changeRoles() {
  const urlParams = window.location.href;
  let identifier = urlParams.includes("roles/")
    ? urlParams.split("roles/")[1].split("/")[0].split("?")[0].split("#")[0]
    : new URLSearchParams(window.location.search).get("identifier");

  const roles = $(".role-chip span[data-role]")
    .map((_, el) => $(el).attr("data-role"))
    .get();

  const url = config.users_url + "/" + identifier + "/roles";
  const token = getCookieOrStorage(config.access_token);

  fetch(url, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + token
    },
    body: JSON.stringify({ roles: roles })
  }).then(async (res) => {
    if (res.ok) {
      localStorage.removeItem(config.client_id + "_usersData");
      sweetalert("success", "Ruoli Aggiornati", "I ruoli dell'utente sono stati aggiornati con successo.");
      setTimeout(() => {
        window.location.href = "/app/users/details/" + identifier;
      }, 1500);
    } else {
      const err = await res.json().catch(() => ({}));
      sweetalert("error", "Errore", err.message || "Impossibile salvare i ruoli.");
    }
  }).catch((err) => {
    console.error(err);
    sweetalert("error", "Errore di Connessione", "Impossibile contattare il server.");
  });
}
