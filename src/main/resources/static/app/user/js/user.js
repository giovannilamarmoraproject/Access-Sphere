function getUser() {
  const urlParams = window.location.href;
  const identifier = urlParams.split("details/")[1];
  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    const users = JSON.parse(usersJSON);
    const user = users.filter((u) => u.identifier == identifier)[0];
    displayUserData(user);
  } else logout();
}

function displayUserData(user) {
  const container = document.getElementById("user-data");
  container.innerHTML = ""; // Pulisce eventuali dati precedenti

  container.innerHTML += `<div class="row">
            <div class="col-md-3">
              <div class="card mb-4 fade-in">
                <div class="card-body m-3">
                  <div
                    class="d-flex flex-column align-items-center text-center"
                  >
                    <img
                      src="${getOrDefault(
                        user.profilePhoto,
                        "https://bootdey.com/img/Content/avatar/avatar7.png"
                      )}"
                      style="
                        object-fit: cover;
                        min-width: 240px;
                        min-height: 240px;
                      "
                      alt="Admin"
                      class="rounded-circle"
                      width="150"
                    />
                    <div class="mt-3">
                      <h4>${user.name} ${getOrDefault(user.surname, "")}</h4>
                      <p class="text-secondary mb-1">${user.username}</p>
                      <p class="text-muted font-size-sm">
                      ${user.identifier}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-md-9">
              <div class="card fade-in">
                <div class="card-body m-2">
                  <h3>
                    <h3 class="d-inline" id="user_details_title">User Details</h3>
                    <a hidden
                      class="btn btn-primary ps-4 pe-4 float-end"
                      target="__blank"
                      href="https://www.bootdey.com/snippets/view/profile-edit-data-and-skills"
                      >Edit</a
                    >
                  </h3>
                  <div class="row mt-4">
                    <div class="col-sm-3">
                      <h6 class="mb-0" id="user_details_name">Full Name</h6>
                    </div>
                    <div class="col-sm-9 text-secondary">
                      ${user.name} ${getOrDefault(user.surname, "")}
                    </div>
                  </div>
                  <hr />
                  ${generateUserInfoHTML(user)}
                </div>
              </div>
              ${
                user.roles && user.roles.length > 0
                  ? `
                <div class="card mt-4 fade-in">
                  <div class="card-body m-2">
                    <h3 id="user_details_user_roles">User Roles</h3>
                    <div class="row mt-4">
                      <div class="col-sm-3">
                        <h6 class="mb-0" id="user_details_roles">App Roles</h6>
                      </div>
                      <div class="col-sm-9 text-secondary">
                        <ul>
                          ${user.roles
                            .map((role) => `<li>${role}</li>`)
                            .join("")}
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
                `
                  : ""
              }
              ${
                user.attributes ? generateAttributesHTML(user.attributes) : ""
              }`;
}

function generateUserInfoHTML(user) {
  if (!user) return "";

  const fields = [
    { label: "Birthdate", value: user.birthDate, id: "user_details_birth" },
    { label: "Gender", value: user.gender, id: "user_details_gender" },
    {
      label: "Nationality",
      value: user.nationality,
      id: "user_details_nationality",
    },
    { label: "Fiscal Code / SSN", value: user.ssn, id: "user_details_ssn" },
    { label: "Email", value: user.email, id: "user_details_email" },
    {
      label: "Phone Number",
      value: user.phoneNumber,
      id: "user_details_phoneNumber",
    },
    {
      label: "Occupation",
      value: user.occupation,
      id: "user_details_occupation",
    },
    { label: "Education", value: user.education, id: "user_details_education" },
    {
      label: "Status",
      value: user.blocked
        ? `<span class='badge text-bg-danger status_blocked'>BLOCKED</span>
          <button
            onclick="lockUser(false)"
            id="unlock_user"
            style="padding: 5px 20px; border-radius: 10px"
            class="btn btn-danger float-end"
          >Unlock</button>`
        : `<span class='badge text-bg-success status_active'>ACTIVE</span>
        <button
            onclick="lockUser(true)"
            id="lock_user"
            style="padding: 5px 20px; border-radius: 10px"
            class="btn btn-danger float-end"
          >Lock</button>`,
      id: "user_details_status",
    },
  ];

  // Filtra solo i campi presenti
  const validFields = fields.filter((field) => field.value);

  if (validFields.length === 0) return ""; // Se non ci sono dati, non generiamo nulla

  return validFields
    .map(
      (field, index) => `
      <div class="row">
        <div class="col-sm-3"><h6 class="mb-0" id="${field.id}">${
        field.label
      }</h6></div>
        <div class="col-sm-9 text-secondary">${field.value}</div>
      </div>
      ${index < validFields.length - 1 ? "<hr />" : ""}
    `
    )
    .join("");
}

function generateAttributesHTML(attributes) {
  if (!attributes || Object.keys(attributes).length === 0) {
    return ""; // Se non ci sono attributi, non generare nulla
  }

  const entries = Object.entries(attributes);
  return `
  <div class="card mt-4 fade-in">
    <div class="card-body m-2">
      <h3 id="user_details_attributes">Attributes</h3>
      ${entries
        .map(([key, value], index) => {
          let content = "";
          if (typeof value === "object" && value !== null) {
            // Se il valore è un oggetto, generiamo una sezione con titolo e iteriamo i suoi valori
            const subEntries = Object.entries(value);
            content += `
              <h5>${key}</h5>
              ${subEntries
                .map(
                  ([subKey, subValue]) => `
                  <div class="row mt-4">
                    <div class="col-sm-3"><h6 class="mb-0">${subKey}</h6></div>
                    <div class="col-sm-9 text-secondary"><code>${subValue}</code></div>
                  </div>
                `
                )
                .join("")}
            `;
          } else {
            // Se il valore è una stringa o un numero, lo stampiamo direttamente
            content += `
              <div class="row mt-4">
                <div class="col-sm-3"><h6 class="mb-0">${key}</h6></div>
                <div class="col-sm-9 text-secondary"><code>${value}</code></div>
              </div>
            `;
          }
          // Se non è l'ultimo elemento, aggiungiamo <hr />
          if (index < entries.length - 1) {
            content += `<hr />`;
          }
          return content;
        })
        .join("")}
    </div>
  </div>`;
}

$(document).ready(function () {
  getUser();
});

function lockUser(status) {
  const urlParams = window.location.href;
  const identifier = urlParams.split("details/")[1];
  const unlockUrl =
    window.location.origin + "/v1/users/" + identifier + "?block=" + status;
  const token = getCookieOrStorage(config.access_token);
  lock_unlock_user(unlockUrl, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      return sweetalert(
        "success",
        status
          ? currentTranslations.locked_user
          : currentTranslations.unlocked_user,
        responseData.message
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

const lock_unlock_user = async (url, bearer) => {
  try {
    const response = await fetch(url, {
      method: "PATCH", // *GET, POST, PUT, DELETE, etc.
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
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};
