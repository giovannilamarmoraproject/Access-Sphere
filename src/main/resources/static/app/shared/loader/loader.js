// loader.js
document.addEventListener("DOMContentLoaded", function () {
  const loader = document.getElementById("loader");
  if (loader) {
    loader.innerHTML = `<div class="loader__inner">
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
            <div class="loader__column"></div>
          </div>
          <div class="loader--spinner">
            <img
              src="../../app/shared/loader/oval.svg"
              alt=""
            />
          </div>`;
  }
});
document.addEventListener("DOMContentLoaded", function () {});

function enableLoader() {
  const loader = document.getElementById("loader");

  if (loader && loader.classList.contains("loader--finish")) {
    loader.classList.remove("loader--finish");
    const spinner = loader.querySelector(".loader--spinner");
    if (spinner) spinner.style.display = "block";
  } else {
    // Se è stato rimosso dal DOM, lo ricreo
    const newLoader = document.createElement("div");
    newLoader.id = "loader";
    newLoader.className = "loader";
    newLoader.innerHTML = `
      <div class="loader__inner">
        ${'<div class="loader__column"></div>'.repeat(20)}
      </div>
      <div class="loader--spinner">
        <img src="../../app/shared/loader/oval.svg" alt="" />
      </div>
    `;
    document.body.appendChild(newLoader);
  }
}

function disableLoader() {
  return new Promise((resolve) => {
    const loader = document.getElementById("loader");
    const spinner = loader?.querySelector(".loader--spinner");

    // se non c'è nessun loader risolvo subito
    if (
      !loader ||
      (loader.classList.contains("loader--finish") &&
        spinner.style.display == "none")
    ) {
      if (spinner) spinner.style.display = "none";
      return resolve();
    }
    // avvia l'animazione di uscita
    if (!loader.classList.contains("loader--finish"))
      loader.classList.add("loader--finish");
    if (spinner) spinner.style.display = "none";

    /* 1 - risolvo appena finisce la transition */
    loader.addEventListener("transitionend", function handler() {
      loader.removeEventListener("transitionend", handler);
      loader.remove(); // (o nascondi se preferisci)
      resolve();
    });

    /* 2 - fallback di sicurezza: max 1 s */
    setTimeout(() => {
      if (document.body.contains(loader)) loader.remove();
      resolve();
    }, 10);
  });
}

function disableLoaderOld() {
  setTimeout(function () {
    const loader = document.querySelector(".loader");
    const spinner = document.querySelector(".loader--spinner");

    if (loader) loader.classList.add("loader--finish");
    if (spinner) spinner.style.display = "none";
    setTimeout(function () {
      loader.remove();
    }, 700);
  }, 500);
}
