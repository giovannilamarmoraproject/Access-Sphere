function refreshClients() {
  console.log("Refreshing Clients...");
  localStorage.removeItem(config.client_id + "_clients");
  getClient();
}

$(document).ready(function () {
  const clientsJSON = localStorage.getItem(config.client_id + "_clients");
  if (clientsJSON) {
    const clients = JSON.parse(clientsJSON);
    displayClientData(clients);
  } else getClient();

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
    let userData = {
      firstName: $("#name").val().trim(),
      lastName: $("#surname").val().trim(),
      birthdate: $("#birthdate").val(),
      gender: $("#gender").val(),
      nationality: $("#nationality").val().trim(),
      ssn: $("#ssn").val().trim(),
      email: $("#email").val().trim(),
      phone: $("#phone_prefix").val() + $("#phone").val().trim(),
      username: $("#validationDefaultUsername").val().trim(),
      password: $("#password").val(),
      confirmPassword: $("#confirm_password").val(),
      occupation: $("#occupation").val().trim(),
      education: $("#education").val().trim(),
      attributes: $("#attributes").val().trim(),
      clientId: $("#client_id_select").val(), // ID Cliente
      roles: [$("#role_select").val()], // Ruoli
      profile: profileImage,
    };
    console.log(userData);
  });
});

function registerUser(userForm, clients) {
  const userClient = clients.filter(
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

  registerUserData(registrationUrl, token, user).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      return sweetalert("success", "Saved", responseData.message);
    }
  });
}

function getClient() {
  const url = config.client_id_url;
  const token = getCookieOrStorage(config.access_token);

  clientID(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      // ✅ Salva i dati in localStorage come stringa JSON
      localStorage.setItem(
        config.client_id + "_clients",
        JSON.stringify(responseData.data)
      );
      console.log(responseData.data);
      displayClientData(responseData.data);
    }
  });
}

function displayClientData(clients) {
  // Popola il select con i client
  const clientSelect = $("#client_id_select");
  clientSelect.empty(); // Rimuove le opzioni esistenti (per evitare duplicati)
  clientSelect.append('<option selected disabled value="">Choose...</option>'); // Prima opzione disabilitata

  clients.forEach((client) => {
    const option = $("<option>").val(client.clientId).text(client.clientId);
    clientSelect.append(option);
  });

  // Funzione per popolare i ruoli quando viene selezionato un client
  function populateRoles(clientId) {
    // Trova il client selezionato
    const selectedClient = clients.find(
      (client) => client.clientId === clientId
    );

    if (selectedClient && selectedClient.appRoles) {
      // Popola il select con i ruoli
      const roleSelect = $("#role_select");
      roleSelect.empty(); // Rimuove tutte le opzioni esistenti
      roleSelect.append(
        '<option selected disabled value="">Choose...</option>'
      ); // Prima opzione disabilitata

      // Estrae e popola i ruoli da 'appRoles'
      selectedClient.appRoles.forEach((role) => {
        if (role.role) {
          // Assicurati che il campo `role` esista
          const option = $("<option>").val(role.role).text(role.role);
          roleSelect.append(option);
        }
      });

      // Abilita il campo ruolo
      roleSelect.prop("disabled", false);
    }
  }

  // Gestisce la selezione del client
  $("#client_id_select").on("change", function () {
    const selectedClientId = $(this).val();
    if (selectedClientId) {
      // Popola i ruoli per il client selezionato
      populateRoles(selectedClientId);
    } else {
      // Se nessun client è selezionato, disabilita il selettore dei ruoli
      $("#role_select")
        .prop("disabled", true)
        .empty()
        .append('<option selected disabled value="">Choose...</option>');
    }
  });
}

const clientID = async (url, bearer) => {
  try {
    const response = await fetch(url, {
      method: "GET", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "same-origin", // include, *same-origin, omit
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + bearer,
        ...getSavedHeaders(),
        // 'Content-Type': 'application/x-www-form-urlencoded',
      },
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      //body: JSON.stringify(data), // body data type must match "Content-Type" header
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const registerUserData = async (url, bearer, data) => {
  try {
    const response = await fetch(url, {
      method: "POST", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "same-origin", // include, *same-origin, omit
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + bearer,
        ...getSavedHeaders(),
        // 'Content-Type': 'application/x-www-form-urlencoded',
      },
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: JSON.stringify(data), // body data type must match "Content-Type" header
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};
