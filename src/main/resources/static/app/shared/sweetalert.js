function safeDisableLoader() {
  if (typeof disableLoader === "function") {
    try {
      const res = disableLoader();
      if (res && typeof res.then === "function") {
        return res;
      }
    } catch (e) {
      console.warn("Loader disable error:", e);
    }
  }
  return Promise.resolve();
}

/**
 * Access Sphere M3 SweetAlert Mixin
 */
function getM3SwalMixin() {
  return Swal.mixin({
    customClass: {
      popup: "m3-swal-popup",
      title: "m3-swal-title",
      htmlContainer: "m3-swal-html",
      confirmButton: "m3-swal-confirm-btn",
      cancelButton: "m3-swal-cancel-btn",
      denyButton: "m3-swal-deny-btn",
      input: "m3-swal-input",
    },
    buttonsStyling: false,
  });
}

function sweetalert(icon, title, message, html = false) {
  const customClassSwal = getM3SwalMixin();

  const options = {
    icon: icon,
    title: title,
    color: "#FFFFFF",
    showCancelButton: false,
    confirmButtonText: "OK",
  };

  if (html) {
    options.html = message;
  } else {
    options.text = message;
  }

  return safeDisableLoader().then(() => {
    return customClassSwal.fire(options);
  });
}

function sweetalertConfirm(icon, title, message, btnConfirm, btnDeny) {
  const customClassSwal = getM3SwalMixin();

  return safeDisableLoader().then(() => {
    return customClassSwal.fire({
      icon: icon,
      title: title,
      text: message,
      color: "#FFFFFF",
      showDenyButton: true,
      showCancelButton: false,
      confirmButtonText: btnConfirm,
      denyButtonText: btnDeny,
    });
  });
}

function inputSweetAlert(title, confirm) {
  const customClassSwal = getM3SwalMixin();

  return safeDisableLoader().then(() => {
    return customClassSwal.fire({
      title: title,
      input: "text",
      color: "#FFFFFF",
      inputAttributes: {
        autocapitalize: "off",
      },
      showCancelButton: true,
      confirmButtonText: confirm,
      cancelButtonText: "Annulla",
      preConfirm: async (text) => {
        return text;
      },
    });
  });
}

