// loader.js - Access Sphere High Performance Ultra-Fast Loader
function initLoader() {
  const loader = document.getElementById("loader");
  if (loader && !loader.innerHTML.trim()) {
    loader.innerHTML = `
      <div class="loader__inner">
        ${'<div class="loader__column"></div>'.repeat(20)}
      </div>
      <div class="loader--spinner">
        <img src="/app/shared/loader/oval.svg" alt="Caricamento..." />
      </div>`;
  }
  // Rimuovi immediatamente il loader senza ritardi artificiali (instant reveal)
  disableLoader();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initLoader);
} else {
  initLoader();
}

window.addEventListener("load", function () {
  disableLoader();
});

function enableLoader() {
  let loader = document.getElementById("loader");
  if (loader) {
    loader.style.display = "block";
    loader.classList.remove("loader--finish");
    const spinner = loader.querySelector(".loader--spinner");
    if (spinner) spinner.style.display = "block";
  } else {
    loader = document.createElement("div");
    loader.id = "loader";
    loader.className = "loader";
    loader.innerHTML = `
      <div class="loader__inner">
        ${'<div class="loader__column"></div>'.repeat(20)}
      </div>
      <div class="loader--spinner">
        <img src="/app/shared/loader/oval.svg" alt="Caricamento..." />
      </div>`;
    document.body.appendChild(loader);
  }
}

function disableLoader() {
  return new Promise((resolve) => {
    const loader = document.getElementById("loader");
    if (!loader) return resolve();

    const spinner = loader.querySelector(".loader--spinner");
    if (spinner) spinner.style.display = "none";
    loader.classList.add("loader--finish");

    setTimeout(() => {
      if (loader && loader.parentNode) {
        loader.style.display = "none";
      }
      resolve();
    }, 60);
  });
}
