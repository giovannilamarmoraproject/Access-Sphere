// loader.js
document.addEventListener("DOMContentLoaded", function () {
  const loader = document.getElementById("loader");
  if (loader) {
    //loader.innerHTML = `<div class="loader__inner">
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //        <div class="loader__column"></div>
    //      </div>
    //      <div class="loader--spinner">
    //        <img
    //          src="../../app/shared/loader/oval.svg"
    //          alt=""
    //        />
    //      </div>`;

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
        <div class="loader--spinner">
      </div>
      <div class="animation-loader">
      <div class="circle"></div>

      <div class="pc">
        <div class="pcscreen"></div>

        <div class="base-one"></div>
        <div class="base-two"></div>
      </div>

      <div class="inner-sq"></div>
      <div class="outer-sq"></div>

      <div class="ipad">
        <div class="line"></div>
      </div>

      <div class="phone"></div></div>`;
  }
});
document.addEventListener("DOMContentLoaded", function () {});

function disableLoader() {
  setTimeout(function () {
    const loader = document.querySelector(".loader");
    const spinner = document.querySelector(".loader--spinner");
    const animationLoader = document.querySelector(".animation-loader");

    if (loader) loader.classList.add("loader--finish");
    if (spinner) spinner.style.display = "none";
    if (animationLoader) animationLoader.style.display = "none";
    setTimeout(function () {
      loader.remove();
    }, 700);
  }, 500);
}
