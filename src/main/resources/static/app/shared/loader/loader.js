// loader.js - Access Sphere High Performance Non-Blocking M3 Loader
function initLoader() {
  const loader = document.getElementById("loader");
  if (loader) {
    loader.classList.remove("is-active");
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initLoader, { once: true });
} else {
  initLoader();
}

function enableLoader() {
  let loader = document.getElementById("loader");
  if (!loader && document.body) {
    loader = document.createElement("div");
    loader.id = "loader";
    loader.className = "loader";
    document.body.appendChild(loader);
  }
  if (loader) {
    loader.style.display = "flex";
    requestAnimationFrame(() => {
      loader.classList.add("is-active");
    });
  }
}

function disableLoader() {
  return new Promise((resolve) => {
    const loader = document.getElementById("loader");
    if (!loader || !loader.classList.contains("is-active")) {
      if (loader) loader.classList.remove("is-active");
      return resolve();
    }
    loader.classList.remove("is-active");
    setTimeout(resolve, 40);
  });
}
