function refreshUsers() {
  console.log("Refreshing Users...");
  localStorage.removeItem(config.client_id + "_usersData");
  refreshAnimation("refresh-icon");
  getUsers();
  refreshAnimation("refresh-icon");
}

function getUsers() {
  const url = config.users_url;
  const token = getCookieOrStorage(config.access_token);

  GET(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      const error = getErrorCode(responseData.error);
      return sweetalert("error", error.title, error.message);
    } else {
      fetchHeader(data.headers);
      // ✅ Salva i dati in localStorage come stringa JSON
      localStorage.setItem(
        config.client_id + "_usersData",
        JSON.stringify(responseData.data)
      );
      displayUsersTable(responseData.data);
    }
  });
}

function displayUsersTable(users) {
  console.log("Displaying Users...");
  var datatable = $("#users-table").DataTable();

  // Distruggi la DataTable esistente
  datatable.destroy();

  const table = document.getElementById("users-data");
  table.innerHTML = ""; // Pulisce eventuali dati precedenti

  users.forEach((user) => {
    table.innerHTML += `<tr style="height: 50px; vertical-align: middle">
                            <td class="text-center">
                            <a href="/app/users/details/${user.identifier}">
                              <img
                                src="${getOrDefault(
                                  user.profilePhoto,
                                  "https://bootdey.com/img/Content/avatar/avatar7.png"
                                )}"
                                alt="Admin"
                                class="rounded-circle"
                                width="50"
                                style="width: 50px; height: 50px; object-fit: cover;"
                              /></a>
                            </td>
                            <td class="hidden-mobile"><a style="color: inherit;" href="/app/users/details/${
                              user.identifier
                            }">${user.name}</a></td>
                            <td class="hidden-mobile"><a style="color: inherit;" href="/app/users/details/${
                              user.identifier
                            }">${user.surname}</a></td>
                            <td><a style="color: inherit;" href="/app/users/details/${
                              user.identifier
                            }">${user.username}</a></td>
                            <td class="hidden-mobile"><a style="color: inherit;" href="/app/users/details/${
                              user.identifier
                            }">${user.email}</a></td>
                            <td>${
                              user.blocked
                                ? "<span class='badge text-bg-danger status_blocked'>BLOCKED</span>"
                                : "<span class='badge text-bg-success status_active'>ACTIVE</span>"
                            }</td>
                            <td class="text-center" style="min-width: 110px;">
                              <a hidden class="m-1" href="/app/users/details/${
                                user.identifier
                              }"><i class="fa-solid fa-eye"></i></a>
                              <a class="m-1" href="/app/users/edit/${
                                user.identifier
                              }"><i class="fa-solid fa-user-pen"></i></a>
                              <a class="m-1" href="/app/users/roles/${
                                user.identifier
                              }"><i class="fa-solid fa-shield-keyhole"></i></a>
                              <a class="m-1 clickable" onclick="deleteUser('${
                                user.identifier
                              }','${
      user.username
    }')"><i class="fa-solid fa-trash-xmark"></i></a>
                            </td>
                          </tr>`;
  });
  var datatable = $("#users-table").DataTable();

  // Distruggi la DataTable esistente
  datatable.destroy();
  // Inizializza la tabella DataTable dopo aver inserito il codice HTML
  $("#users-table").DataTable({
    pageLength: 15, // Numero di righe di default
    responsive: true,
    lengthMenu: [10, 15, 25, 50, 100], // Opzioni della select
    //dom: '<"top"lfB>rt<"bottom"ip>', // Separazione logica degli elementi
    //layout: {
    //  bottom: {
    //    buttons: ["csv", "excel", "pdf"],
    //    //buttons: ["csv", "excel", "pdf", "print"],
    //  },
    //},
    order: [], // Non specifica nessun ordinamento iniziale
    paging: true,
    searching: true,
    ordering: true,
    info: true,
  });
  setDatatablesStyle("users-table");
}

function setDatatablesStyle(tableId) {
  // Seleziona l'input di ricerca
  const searchInput = document.querySelector('.dt-search input[type="search"]');

  if (searchInput) {
    searchInput.style.borderRadius = "12px";
    searchInput.style.padding = "8px 12px";
    searchInput.style.marginLeft = "10px";
    searchInput.style.fontSize = "14px";
    searchInput.style.width = "300px";
  }

  // Seleziona la select per il numero di righe da visualizzare
  //const lengthSelect = document.querySelector(`.dt-input[id="dt-length-0"]`);
  // Seleziona l'elemento .dt-input che contiene "dt-length" nell'ID
  const lengthSelect = document.querySelector('.dt-input[id*="dt-length"]');

  if (lengthSelect) {
    lengthSelect.style.borderRadius = "12px";
    lengthSelect.style.padding = "3px 8px";
    lengthSelect.style.fontSize = "14px";
    lengthSelect.style.marginRight = "10px";
  }

  // Funzione per applicare lo stile alla numerazione delle pagine
  const applyPaginationStyles = () => {
    const paginationButtons = document.querySelectorAll(".dt-paging-button");

    if (paginationButtons)
      paginationButtons.forEach((button) => {
        button.style.borderRadius = "12px";
        button.style.padding = "8px 12px";
        button.style.fontSize = "14px";
      });
  };
  const applyExportButtonsStyles = () => {
    const exportButtons = document.querySelectorAll(".dt-buttons button");

    if (exportButtons)
      // Applica la classe Neverland a ciascun bottone
      exportButtons.forEach((button) => {
        button.style.borderRadius = "12px";
        button.style.width = "100px";
        button.style.marginBottom = "10px";
        button.classList.add("btn"); // Aggiunge la classe 'btn-primary btn-block btn-lg'
        button.classList.add("btn-primary"); // Aggiunge la classe 'btn-primary btn-block btn-lg'
        //button.classList.add('btn-block'); // Aggiunge la classe 'btn-primary btn-block btn-lg'
      });
  };

  // Applica gli stili alla paginazione subito dopo il caricamento della pagina
  applyExportButtonsStyles();
  applyPaginationStyles();

  const observePaginationChanges = () => {
    const paginationContainer = document.querySelector(".dt-paging");
    if (!paginationContainer) return;

    const observer = new MutationObserver(() => {
      applyPaginationStyles();
    });

    observer.observe(paginationContainer, { childList: true, subtree: true });
  };

  // Avvia il MutationObserver dopo l'inizializzazione della tabella
  observePaginationChanges();

  // Se usi DataTables, ascolta l'evento 'draw' per applicare gli stili quando la tabella viene ridisegnata (ad esempio, dopo un cambio pagina)
  if (typeof $ !== "undefined" && $.fn.dataTable) {
    $(`#${tableId}`).on("draw.dt", () => {
      applyExportButtonsStyles();
      applyPaginationStyles();
    });
  }
}

$(document).ready(function () {
  refreshAnimation("refresh-icon");
  const usersJSON = localStorage.getItem(config.client_id + "_usersData");
  if (usersJSON) {
    const users = JSON.parse(usersJSON);
    displayUsersTable(users);
  } else getUsers();
  refreshAnimation("refresh-icon");
});
