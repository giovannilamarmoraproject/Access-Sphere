// loader.js - Access Sphere High Performance Non-Blocking M3 Loader
let loaderRafId = null;

function initLoader() {
  if (loaderRafId) {
    cancelAnimationFrame(loaderRafId);
    loaderRafId = null;
  }
  const loader = document.getElementById("loader");
  if (loader) {
    loader.classList.remove("is-active");
    loader.style.display = "none";
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initLoader, { once: true });
} else {
  initLoader();
}

window.addEventListener("pageshow", initLoader);

function enableLoader() {
  let loader = document.getElementById("loader");
  if (!loader && document.body) {
    loader = document.createElement("div");
    loader.id = "loader";
    loader.className = "loader";
    document.body.appendChild(loader);
  }
  if (loader) {
    if (loaderRafId) {
      cancelAnimationFrame(loaderRafId);
      loaderRafId = null;
    }
    loader.style.display = "flex";
    loaderRafId = requestAnimationFrame(() => {
      loader.classList.add("is-active");
      loaderRafId = null;
    });
  }
}

function disableLoader() {
  return new Promise((resolve) => {
    if (loaderRafId) {
      cancelAnimationFrame(loaderRafId);
      loaderRafId = null;
    }
    const loader = document.getElementById("loader");
    if (!loader) {
      return resolve();
    }
    loader.classList.remove("is-active");
    loader.style.display = "none";
    setTimeout(resolve, 40);
  });
}

