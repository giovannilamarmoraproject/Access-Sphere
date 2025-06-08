function sweetalert(icon, title, message, html = false) {
  const customClassSwal = Swal.mixin({
    customClass: {
      confirmButton: "rounded-pill buttonInput width-100 bg-blue-500",
      denyButton: "rounded-pill buttonInput width-100",
      popup: "border_round blur-effect",
    },
    buttonsStyling: true,
  });

  if (html)
    return customClassSwal.fire({
      icon: icon,
      title: title,
      html: message,
      color: "#FFFFFF",
      //background: "rgba(56, 62, 66, 0.8)",
      //backdrop: "rgba(0, 0, 0, 0.5)",
      showCancelButton: false,
    });

  return disableLoader().then(() => {
    return customClassSwal.fire({
      icon: icon,
      title: title,
      text: message,
      color: "#FFFFFF",
      //background: "rgba(56, 62, 66, 0.8)",
      //backdrop: "rgba(0, 0, 0, 0.5)",
      showCancelButton: false,
    });
  });
}

function sweetalertConfirm(icon, title, message, btnConfirm, btnDeny) {
  const customClassSwal = Swal.mixin({
    customClass: {
      confirmButton: "rounded-pill buttonInput bg-blue-500",
      denyButton: "rounded-pill buttonInput width-100",
      popup: "border_round blur-effect",
    },
    buttonsStyling: true,
  });

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
}

function inputSweetAlert(title, confirm) {
  const customClassSwal = Swal.mixin({
    customClass: {
      confirmButton: "rounded-pill buttonInput width-100 bg-blue-500",
      cancelButton: "rounded-pill buttonInput width-100",
      denyButton: "rounded-pill buttonInput width-100",
      popup: "border_round blur-effect",
    },
    buttonsStyling: true,
  });

  return customClassSwal.fire({
    title: title,
    input: "text",
    color: "#FFFFFF",
    inputAttributes: {
      autocapitalize: "off",
    },
    //background: "rgba(56, 62, 66, 0.8)",
    //backdrop: "rgba(0, 0, 0, 0.5)",
    showCancelButton: true,
    confirmButtonText: confirm,
    preConfirm: async (text) => {
      return text;
    },
  });
}
