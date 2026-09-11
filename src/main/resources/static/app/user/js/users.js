function refreshUsers() {
  console.log("Refreshing Users...");
  localStorage.removeItem(config.client_id + "_usersData");
  const icon = document.getElementById("refresh-icon");
  if (icon) icon.classList.add("fa-spin");
  getUsers().finally(() => {
    if (icon) setTimeout(() => icon.classList.remove("fa-spin"), 500);
  });
}

function getUsers() {
  const url = config.users_url;
  const token = getCookieOrStorage(config.access_token);

  if (!token) {
    console.warn("getUsers: Nessun token disponibile.");
    if (typeof disableLoader === "function") disableLoader();
    return Promise.resolve();
  }

  return GET(url, token).then(async (data) => {
    const responseData = await data.json();
    if (responseData.error != null) {
      console.warn("getUsers error response:", responseData.error);
      // Se abbiamo già dati in tabella/cache, non mostrare alert bloccante
      const cached = localStorage.getItem(config.client_id + "_usersData");
      if (!cached) {
        const error = getErrorCode(responseData.error);
        sweetalert("error", error.title, error.message);
      }
    } else {
      fetchHeader(data.headers);
      localStorage.setItem(
        config.client_id + "_usersData",
        JSON.stringify(responseData.data)
      );
      displayUsersTable(responseData.data);
      updateUserKpis(responseData.data);
    }
  }).catch((err) => {
    console.error("Fetch users network error:", err);
  }).finally(() => {
    if (typeof disableLoader === "function") disableLoader();
  });
}

function updateUserKpis(users) {
  if (!Array.isArray(users)) return;
  const total = users.length;
  const active = users.filter(u => !u.blocked).length;
  const blocked = users.filter(u => !!u.blocked).length;

  const totalEl = document.getElementById("stat-total-users");
  const activeEl = document.getElementById("stat-active-users");
  const blockedEl = document.getElementById("stat-blocked-users");

  if (totalEl) totalEl.innerText = total;
  if (activeEl) activeEl.innerText = active;
  if (blockedEl) blockedEl.innerText = blocked;
}

function displayUsersTable(users) {
  if (!Array.isArray(users)) return;
  console.log("Displaying Users table with " + users.length + " entries");

  if ($.fn.DataTable.isDataTable('#users-table')) {
    $('#users-table').DataTable().destroy();
  }

  const table = document.getElementById("users-data");
  if (!table) return;
  table.innerHTML = "";

  users.forEach((user) => {
    const photo = getOrDefault(user.profilePhoto, "https://bootdey.com/img/Content/avatar/avatar7.png");
    const statusBadge = user.blocked
      ? "<span class='px-3 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-300 border border-red-500/30'>BLOCKED</span>"
      : "<span class='px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'>ACTIVE</span>";

    const tr = document.createElement("tr");
    tr.style.height = "56px";
    tr.style.verticalAlign = "middle";
    tr.className = "cursor-pointer hover:bg-purple-500/10 transition-colors";
    tr.onclick = (e) => {
      if (e.target.closest("button") || e.target.closest("a")) return;
      window.location.href = `/app/users/details/${encodeURIComponent(user.identifier)}`;
    };

    tr.innerHTML = `
      <td class="text-center">
        <a href="/app/users/details/${encodeURIComponent(user.identifier)}">
          <img src="${photo}" alt="${user.username}" class="rounded-full mx-auto object-cover border border-purple-500/30" style="width: 42px; height: 42px;" />
        </a>
      </td>
      <td class="hidden-mobile"><a class="text-white font-medium hover:underline" href="/app/users/details/${encodeURIComponent(user.identifier)}">${user.name || ""}</a></td>
      <td class="hidden-mobile"><a class="text-white font-medium hover:underline" href="/app/users/details/${encodeURIComponent(user.identifier)}">${user.surname || ""}</a></td>
      <td><a class="text-purple-300 font-mono font-medium hover:underline" href="/app/users/details/${encodeURIComponent(user.identifier)}">@${user.username || ""}</a></td>
      <td class="hidden-mobile text-gray-300">${user.email || ""}</td>
      <td>${statusBadge}</td>
      <td class="text-center" style="min-width: 130px;">
        <a class="m3-action-btn m3-action-edit" title="Modifica Utente" href="/app/users/edit/${encodeURIComponent(user.identifier)}">
          <i class="fa-solid fa-user-pen text-xs"></i>
        </a>
        <a class="m3-action-btn m3-action-role" title="Gestisci Ruoli" href="/app/users/roles/${encodeURIComponent(user.identifier)}">
          <i class="fa-solid fa-shield-halved text-xs"></i>
        </a>
        <button class="m3-action-btn m3-action-delete" title="Elimina Utente" onclick="event.stopPropagation(); deleteUser('${user.identifier}','${user.username}')">
          <i class="fa-solid fa-trash text-xs"></i>
        </button>
      </td>
    `;
    table.appendChild(tr);
  });

  const userDt = $('#users-table').DataTable({
    pageLength: 10,
    responsive: true,
    language: {
      search: "Cerca utente:",
      lengthMenu: "Mostra _MENU_ utenti",
      info: "Visualizzati _START_ a _END_ di _TOTAL_ utenti",
      paginate: {
        first: "Primo",
        last: "Ultimo",
        next: "Succ.",
        previous: "Prec."
      }
    }
  });

  function stylePaginationButtons() {
    $('.dt-paging-button').each(function() {
      this.style.setProperty('color', '#FFFFFF', 'important');
      this.style.setProperty('-webkit-text-fill-color', '#FFFFFF', 'important');
      this.style.setProperty('opacity', '1', 'important');
    });
  }

  userDt.on('draw', stylePaginationButtons);
  stylePaginationButtons();

  updateUserKpis(users);
}
