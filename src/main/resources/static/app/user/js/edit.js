$(document).ready(function () {
  getUser();

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

  $(".form").on("submit", function (event) {
    event.preventDefault();
    if (typeof validateUserEditForm === "function") {
      if (!validateUserEditForm()) {
        return;
      }
    }

    const currentProfilePhoto = $("#profile").attr("src");
    const profilePhoto = profileImage || currentProfilePhoto;
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
      occupation: $("#occupation").val().trim(),
      education: $("#education").val().trim(),
      attributes: attributes ? JSON.parse(attributes) : null,
      profile: profilePhoto,
    };

    editUser(userData);
  });
});

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
    console.error("Identificativo mancante");
    return sweetalert("error", "Errore", "Identificativo utente mancante.");
  }

  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    try {
      const user = JSON.parse(usersJSON).find((u) => u.identifier == identifier || u.username == identifier);
      if (user) {
        populateUserData(user);
        return;
      }
    } catch(e) {}
  }

  // Fallback via API invece del logout immediato!
  console.log("Recupero dati utente da API...");
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
          sweetalert("error", "Utente non trovato", `Nessun utente con ID: ${identifier}`);
        }
      }
    } catch(err) {
      console.error(err);
    }
  }).catch(err => {
    console.error(err);
  });
}

function populateUserData(user) {
  $("#name").val(user.name);
  $("#surname").val(user.surname);
  $("#birthdate").val(user.birthDate);
  $("#gender").val(user.gender);
  $("#nationality").val(user.nationality);
  $("#ssn").val(user.ssn);
  $("#email").val(user.email);
  if (user.phoneNumber) {
    const match = user.phoneNumber.match(/^\+\d+/);
    if (match) $("#phone_prefix").val(match[0]);
    $("#phone").val(user.phoneNumber.replace(/^\+\d+\s*/, ""));
  }
  $("#validationDefaultUsername").val(user.username);
  $("#occupation").val(user.occupation);
  $("#education").val(user.education);
  if (user.attributes) {
    $("#attributes").val(JSON.stringify(user.attributes, null, 2));
    if (typeof loadAttributesIntoRepeater === "function") {
      loadAttributesIntoRepeater(user.attributes);
    }
  } else if (typeof loadAttributesIntoRepeater === "function") {
    loadAttributesIntoRepeater({});
  }
  if (user.profilePhoto) {
    $("#profile").attr("src", user.profilePhoto).show();
  }
}

function editUser(userData) {
  const urlParams = window.location.href;
  let identifier = urlParams.includes("edit/") 
    ? urlParams.split("edit/")[1].split("/")[0].split("?")[0].split("#")[0]
    : new URLSearchParams(window.location.search).get("identifier");

  const url = config.users_url + "/" + identifier;
  const token = getCookieOrStorage(config.access_token);

  fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + token
    },
    body: JSON.stringify(userData)
  }).then(async (res) => {
    if (res.ok) {
      localStorage.removeItem(config.client_id + "_usersData");
      sweetalert("success", "Aggiornato!", "Dati utente aggiornati con successo.");
      setTimeout(() => {
        window.location.href = "/app/users";
      }, 1500);
    } else {
      const err = await res.json().catch(() => ({}));
      sweetalert("error", "Errore", err.message || "Impossibile aggiornare i dati.");
    }
  }).catch(err => {
    console.error(err);
    sweetalert("error", "Errore di Rete", "Connessione fallita.");
  });
}
