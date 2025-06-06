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

  /*
  * Vecchia Configurazione img
  min-width: ${
      isBelow(1200)
        ? isBelow(1000)
          ? "100px"
          : "150px"
        : "240px"
    };
    min-height: ${
      isBelow(1200)
        ? isBelow(1000)
          ? "100px"
          : "150px"
        : "240px"
    };

    Edit Button
    class="btn btn-outline-danger edit-btn ${
      isBetween(768, 991) ? "ps-3 pe-3" : "ps-4 pe-4"
    } ${isMobile() ? "mt-2 col-12" : ""} float-end clickable"

    Delete Button
    class="btn btn-outline-secondary delete-btn ${
      isBetween(768, 991) ? "ps-3 pe-3" : "ps-4 pe-4"
    } ${isMobile() ? "mt-2 col-12" : "me-1"} float-end"
   */

  container.innerHTML += `<div class="row">
            <div class="col-md-3">
              <div data-inviewport="fade-in" class="card mb-4 fade-in">
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
                        aspect-ratio: 1 / 1;
                      "
                      alt="Admin"
                      class="rounded-circle zoom_small"
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
              <div data-inviewport="fade-in" class="card fade-in">
                <div class="card-body m-2">
                  <h3 class="row">
                    <div class="col-md-5">
                    <h3 class="d-inline" id="user_details_title">User Details</h3>
                    </div>
                    <div class="col-md-7">
                    <a
                      id="delete_user_text"
                      class="btn btn-outline-danger edit-btn float-end clickable"
                      style="padding: 5px 20px; border-radius: 10px"
                      onclick="deleteUser('${user.identifier}','${
    user.username
  }')"
                      >Delete</a
                    >
                    <a
                      id="edit_user_title_btn"
                      class="btn btn-outline-secondary delete-btn float-end"
                      style="padding: 5px 20px; border-radius: 10px"
                      href="/app/users/edit/${user.identifier}"
                      >Edit</a
                    >
                    </div>
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
                <div data-inviewport="fade-in" class="card mt-4 fade-in">
                  <div class="card-body m-2">
                  <h3 class="row">
                  <div class="col-md-5">
                    <h3 id="user_details_user_roles">User Roles</h3>
                    </div>
                    <div class="col-md-7">
                    <a
                      id="roles_page_title"
                      class="btn btn-outline-primary edit-btn float-end clickable"
                      style="padding: 5px 20px; border-radius: 10px"
                      href="/app/users/roles/${user.identifier}"
                      >Edit Roles</a
                    >
                    </h3>
                    <div class="row mt-4">
                      <div class="col-sm-3">
                        <h6 class="mb-0" id="user_details_roles">App Roles</h6>
                      </div>
                      <div class="col-sm-9 text-secondary">
                        <!--<ul>-->
                        <ul style="margin-left: -32px">
                          ${user.roles
                            .map(
                              (role) =>
                                `<li><code style="color: inherit">${role}</code></li>`
                            )
                            .join("")}
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
                `
                  : `<div data-inviewport="fade-in" class="d-flex justify-content-center fade-in ${
                      isMobile() ? "mt-3" : "mt-4"
                    }"><a id='roles_page_title' href="/app/users/roles/${
                      user.identifier
                    }" class='btn btn-outline-primary delete-btn float-end clickable' style='padding: 5px 20px; border-radius: 10px'><i class='fa-solid fa-shield-plus me-1'></i> Edit Roles</a></div>`
              }
              ${
                user.mfaSettings
                  ? generateMFAHTML(user.mfaSettings)
                  : `<div data-inviewport="fade-in" class="d-flex justify-content-center fade-in ${
                      isMobile() ? "mt-3" : "mt-4"
                    }"><a id='user_details_mfa_new' href="/app/mfa/${
                      user.identifier
                    }" class='btn btn-outline-primary delete-btn float-end clickable' style='padding: 5px 20px; border-radius: 10px'><i class='fa-solid fa-shield-plus me-1'></i> Confiure New MFA</a></div>`
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
            class="btn btn-outline-success float-end"
          >Unlock</button>`
        : `<span class='badge text-bg-success status_active'>ACTIVE</span>
        <button
            onclick="lockUser(true)"
            id="lock_user"
            style="padding: 5px 20px; border-radius: 10px"
            class="btn btn-outline-danger float-end"
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

function generateMFAHTML(mfa_settings) {
  const urlParams = window.location.href;
  const identifier = urlParams.split("details/")[1];
  if (!mfa_settings || Object.keys(mfa_settings).length === 0) {
    return ""; // Se non ci sono attributi, non generare nulla
  }

  const entries = Object.entries(mfa_settings);
  return `
  <div data-inviewport="fade-in" class="card mt-4 fade-in">
    <div class="card-body m-2">
      <h3 class="row">
        <div class="col-md-5">
          <h3 id="user_details_mfa"><i class="fa-duotone fa-solid fa-shield-quartered me-1"></i> MFA Methods</h3>
        </div>
        <div class="col-md-7">
          ${
            mfa_settings.enabled
              ? `<a id='user_details_mfa_disable' onclick="manageMFA('DISABLE')" class='btn btn-outline-warning edit-btn float-end clickable' style='padding: 5px 20px; border-radius: 10px'><i class='fa-solid fa-shield-plus me-1'></i> Disable</a>`
              : `<a id='user_details_mfa_enable' onclick="manageMFA('ENABLE')" class='btn btn-outline-success edit-btn float-end clickable' style='padding: 5px 20px; border-radius: 10px'></a>`
          }
          <a id='user_details_mfa_new' href="/app/mfa/${identifier}" class='btn btn-outline-primary delete-btn float-end clickable' style='padding: 5px 20px; border-radius: 10px'><i class='fa-solid fa-shield-plus me-1'></i> Confiure New MFA</a>
        </div>
      </h3>
      ${entries
        .map(([key, value], index) => {
          let content = "";

          if (Array.isArray(value)) {
            // Se il valore è un array, iteriamo ogni elemento
            //content += `<h5>${key}</h5>`;
            value.forEach((item, i) => {
              content += `<div class="border rounded p-3 mb-3" style="border-color: #2d323e !important;">
                <h6 class="row">
                <div class="col-md-5 col-6">
                <h6 class="mfa_methods" style="padding-top: 8px">MFA Method ${
                  i + 1
                }</h6>
                </div>
                <div class="col-md-7 col-6">
                <button
            onclick="deleteMFA('${item.label}')"
            style="padding: 5px 20px; border-radius: 10px"
            class="btn btn-outline-danger float-end user_details_mfa_delete"
          >Delete</button>
                </div>
                </h6>
                <hr />`;
              Object.entries(item).forEach(([itemKey, itemValue]) => {
                let translation_key = "user_details_mfa_" + itemKey;
                content += `
                  <div class="row mt-2">
                    <div class="col-sm-3" style="align-content: center;"><strong><h6 class="mb-0 ${translation_key}">${itemKey}</h6></strong></div>
                    <div class="col-sm-9 text-secondary">${checkValue(
                      itemKey,
                      itemValue
                    )}</div>
                  </div>`;
              });
              content += `</div>`;
            });
          } else if (typeof value === "object" && value !== null) {
            // Se il valore è un oggetto (ma non un array)
            const subEntries = Object.entries(value);
            content += `<h5>${key}</h5>`;
            content += subEntries
              .map(
                ([subKey, subValue]) => `
                <div class="row mt-4">
                  <div class="col-sm-3"><h6 class="mb-0">${subKey}</h6></div>
                  <div class="col-sm-9 text-secondary"><code>${subValue}</code></div>
                </div>
              `
              )
              .join("");
          } else {
            // Se il valore è una stringa, numero o booleano
            let translation_key = "user_details_mfa_" + key;
            content += `
              <div class="row mt-4 mb-3">
                <div class="col-sm-3 col-6" style='align-content: center;'><h6 class="mb-0 ${translation_key}">${key}</h6></div>
                ${
                  value
                    ? `<div class='col-sm-9 col-6 ${
                        isMobile() ? "text-end" : ""
                      } text-secondary'><span class='badge text-bg-success status_active' style='vertical-align: 2px;'>ACTIVE</span></div>`
                    : `<div class='col-sm-9 col-6 ${
                        isMobile() ? "text-end" : ""
                      } text-secondary'><span class='badge text-bg-danger status_not_active' style='vertical-align: 2px;'>NOT ACTIVE</span></div>`
                }
                
              </div>
            `;
          }

          //if (index < entries.length - 1) {
          //  content += `<hr />`;
          //}

          return content;
        })
        .join("")}
    </div>
  </div>`;
}

function checkValue(key, value) {
  let translation_key = "user_details_mfa_" + key + "_" + value;
  // 1. Booleano
  if (typeof value === "boolean") {
    return value
      ? `<div class='col-sm-9 text-secondary'><span class='badge text-bg-success status_active ${translation_key}' style='vertical-align: 2px;'>ACTIVE</span></div>`
      : `<div class='col-sm-9 text-secondary'><span class='badge text-bg-danger status_not_active ${translation_key}' style='vertical-align: 2px;'>NOT ACTIVE</span></div>"`;
  }

  // 2. Stringa che sembra una data ISO (es. 2025-04-17T19:07:59.3415269)
  if (typeof value === "string" && !isNaN(Date.parse(value))) {
    return "<code style='color: inherit'>" + formatDateIntl(value) + "</code>"; // formatDateIntl si aspetta dd/mm/yyyy
  }

  // 2. Stringa che è un label
  if (typeof value === "string") {
    const matchedLabel = Object.values(totpLabel).find(
      (label) => label === value
    );

    if (matchedLabel) {
      // Capitalizza il nome visualizzato (opzionale)
      const formattedLabel = matchedLabel
        .replace("-", " ")
        .replace(/\b\w/g, (char) => char.toUpperCase());

      return `<code class="text-primary"><strong>${formattedLabel}</strong></code>`;
    }
  }

  // 3. Altro tipo (numeri, stringhe, ecc.)
  return "<code style='color: inherit'>" + value + "</code>";
}

function generateAttributesHTML(attributes) {
  if (!attributes || Object.keys(attributes).length === 0) {
    return ""; // Se non ci sono attributi, non generare nulla
  }

  const entries = Object.entries(attributes);
  return `
  <div data-inviewport="fade-in" class="card mt-4 fade-in">
    <div class="card-body m-2">
      <h3 id="user_details_attributes">Attributes</h3>
      ${entries
        .map(([key, value], index) => {
          let translation_key = "user_details_attributes_" + key;
          let content = "";
          if (typeof value === "object" && value !== null) {
            // Se il valore è un oggetto, generiamo una sezione con titolo e iteriamo i suoi valori
            const subEntries = Object.entries(value);
            content += `
              <h5 class="${translation_key}">${key}</h5>
              ${subEntries
                .map(([subKey, subValue]) => {
                  let translation_subkey =
                    "user_details_attributes_" + key + "_" + subKey;
                  return `
                  <div class="row mt-4">
                    <div class="col-sm-3"><h6 class="mb-0 ${translation_subkey}">${subKey}</h6></div>
                    <div class="col-sm-9 text-secondary"><code>${subValue}</code></div>
                  </div>
                `;
                })
                .join("")}
            `;
          } else {
            let translation_key = "user_details_attributes_" + key;
            // Se il valore è una stringa o un numero, lo stampiamo direttamente
            content += `
              <div class="row mt-4">
                <div class="col-sm-3"><h6 class="mb-0 ${translation_key}">${key}</h6></div>
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
  PATCH(unlockUrl, token).then(async (data) => {
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
        status
          ? currentTranslations.locked_user_text.replace(
              "#NAME#",
              responseData.data.username
            )
          : currentTranslations.unlocked_user_text.replace(
              "#NAME#",
              responseData.data.username
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

/**
 * ----------------------------------------------
 * @MFA_Methods
 * ----------------------------------------------
 */
function manageMFA(status) {
  if (status != "ENABLE")
    sweetalertConfirm(
      "question",
      currentTranslations.user_details_disable_question_title,
      currentTranslations.user_details_disable_question_text,
      currentTranslations.user_details_disable_question_deny_btn,
      currentTranslations.delete_user_btn_deny
    ).then((result) => {
      /* Read more about isConfirmed, isDenied below */
      if (result.isConfirmed) {
        return manageMFAStatus(status);
      } else if (result.isDenied) {
        return sweetalert(
          "info",
          currentTranslations.user_details_disable_question_deny_title,
          currentTranslations.user_details_disable_question_deny_text
        );
      }
    });
  else return manageMFAStatus(status);
}

function manageMFAStatus(status) {
  const urlParams = window.location.href;
  const identifier = urlParams.split("details/")[1];
  const manageUrl = window.location.origin + "/v1/mfa/manage";
  const token = getCookieOrStorage(config.access_token);
  const body = {
    identifier: identifier,
    action: status,
  };
  POST(manageUrl, token, body).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      localStorage.removeItem(config.client_id + "_usersData");
      return sweetalert(
        "success",
        status == "ENABLE"
          ? currentTranslations.user_details_enable_title
          : currentTranslations.user_details_disable_title,
        status == "ENABLE"
          ? currentTranslations.user_details_enable_text
          : currentTranslations.user_details_disable_text
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

function deleteMFA(label) {
  sweetalertConfirm(
    "question",
    currentTranslations.user_details_disable_question_delete_title,
    currentTranslations.user_details_disable_question_delete_text,
    currentTranslations.delete_user_btn_confirm,
    currentTranslations.delete_user_btn_deny
  ).then((result) => {
    /* Read more about isConfirmed, isDenied below */
    if (result.isConfirmed) {
      const urlParams = window.location.href;
      const identifier = urlParams.split("details/")[1];
      const manageUrl = window.location.origin + "/v1/mfa/manage";
      const token = getCookieOrStorage(config.access_token);
      const body = {
        identifier: identifier,
        action: "DELETE",
        label: label,
      };
      POST(manageUrl, token, body).then(async (data) => {
        const responseData = await data.json();
        if (responseData.error != null) {
          const error = getErrorCode(responseData.error);
          return sweetalert("error", error.title, error.message);
        } else {
          fetchHeader(data.headers);
          localStorage.removeItem(config.client_id + "_usersData");
          return sweetalert(
            "success",
            currentTranslations.user_details_delete_title,
            currentTranslations.user_details_delete_text
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
    } else if (result.isDenied) {
      return sweetalert(
        "info",
        currentTranslations.user_details_disable_deny_delete_title,
        currentTranslations.user_details_disable_deny_delete_text
      );
    }
  });
}
