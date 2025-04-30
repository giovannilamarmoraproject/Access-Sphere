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

function disableLoader() {
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
