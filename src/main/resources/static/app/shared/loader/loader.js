// loader.js - Access Sphere High Performance Loader
document.addEventListener("DOMContentLoaded", function () {
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
  // Fallback di sicurezza: rimuovi automaticamente il loader dopo 800ms se non già disabilitato
  setTimeout(() => {
    disableLoader();
  }, 800);
});

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
    }, 150);
  });
}
