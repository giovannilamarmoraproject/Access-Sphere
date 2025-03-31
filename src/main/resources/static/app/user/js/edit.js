$(document).ready(function () {
  getUser();

  let profileImage;

  // Selezione immagine profilo
  document.getElementById("file").addEventListener("change", function (event) {
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

      // Converti immagine in Base64
      const reader = new FileReader();
      reader.onload = function () {
        const base64String = reader.result;
        document.getElementById("profile").src = base64String;
        document.getElementById("profile").style.display = "block";
        profileImage = base64String;
      };
      reader.readAsDataURL(file);
    }
  });

  // Form submit per la modifica utente
  $(".form").on("submit", function (event) {
    event.preventDefault();

    const currentProfilePhoto = $("#profile").attr("src"); // Immagine attuale
    const profilePhoto = profileImage || currentProfilePhoto;
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
      occupation: $("#occupation").val().trim(),
      education: $("#education").val().trim(),
      attributes: attributes ? JSON.parse(attributes) : null,
      profile: profilePhoto,
    };

    editUser(userData);
  });
});

function getUser() {
  const user = getUserData();

  if (user) populateUserData(user);
  else {
    logout();
  }
}

function getUserData() {
  const identifier = window.location.href.split("edit/")[1];
  const usersJSON = localStorage.getItem(config.client_id + "_usersData");

  if (usersJSON) {
    const user = JSON.parse(usersJSON).find((u) => u.identifier == identifier);
    return user;
  }
  return null;
}

// Funzione per recuperare i dati dell'utente e popolare il form
function populateUserData(user) {
  // Popoliamo i campi del form
  $("#name").val(user.name);
  $("#surname").val(user.surname);
  $("#birthdate").val(user.birthDate);
  $("#gender").val(user.gender);
  $("#nationality").val(user.nationality);
  $("#ssn").val(user.ssn);
  $("#email").val(user.email);
  if (user.phoneNumber)
    $("#phone_prefix").val(user.phoneNumber.match(/^\+\d+/)[0]);
  if (user.phoneNumber)
    $("#phone").val(user.phoneNumber.replace(/^\+\d+\s*/, ""));
  $("#validationDefaultUsername").val(user.username);
  $("#occupation").val(user.occupation);
  $("#education").val(user.education);
  const attributes = user.attributes
    ? JSON.stringify(user.attributes, null, 2)
    : "";
  $("#attributes").val(attributes);

  // Adatta automaticamente l'altezza del textarea
  const textarea = document.getElementById("attributes");
  textarea.style.height = "auto"; // Resetta l'altezza
  textarea.style.height = textarea.scrollHeight + "px"; // Imposta l'altezza in base al contenuto

  // Popola l'immagine del profilo
  const profilePhoto =
    user.profilePhoto || "https://bootdey.com/img/Content/avatar/avatar7.png";
  $("#profile").attr("src", profilePhoto).show();
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
      checkRequiredFields();
    } catch (e) {
      // Ignora l'errore finché il JSON non è valido
      $("#validationAttributes").text(
        currentTranslations.edit_attributes_valid
      );
      textarea.addClass("is-invalid");
      $("#edit_user_button").prop("disabled", true);
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

// Funzione per modificare utente
function editUser(userForm) {
  const token = getCookieOrStorage(config.access_token);
  const userUpdateUrl = config.edit_user_url; // URL per la modifica

  const user = getUserData();
  const userToEdit = applyUserEdit(user, userForm);

  PUT(userUpdateUrl, token, userToEdit).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      return sweetalert(
        "success",
        currentTranslations.edit_user_response_title,
        currentTranslations.edit_user_response_text.replace(
          "#USER#",
          userToEdit.username
        )
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

function applyUserEdit(userToEdit, userForm) {
  userToEdit.name = userForm.firstName;
  userToEdit.surname = userForm.lastName;
  userToEdit.email = userForm.email;
  userToEdit.username = userForm.username;
  userToEdit.profilePhoto = userForm.profile;
  userToEdit.phoneNumber = userForm.phone;
  userToEdit.birthDate = userForm.birthdate;
  userToEdit.gender = userForm.gender;
  userToEdit.ssn = userForm.ssn;
  userToEdit.education = userForm.education;
  userToEdit.occupation = userForm.occupation;
  userToEdit.nationality = userForm.nationality;
  userToEdit.attributes = userForm.attributes;
  return userToEdit;
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
