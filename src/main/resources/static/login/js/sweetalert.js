function sweetalert(icon, title, message) {
  const customClassSwal = Swal.mixin({
    customClass: {
      confirmButton: "rounded-pill buttonInput",
      denyButton: "rounded-pill buttonInput",
      popup: "border_round blur-effect",
    },
    buttonsStyling: true,
  });

  return customClassSwal.fire({
    icon: icon,
    title: title,
    text: message,
    color: "#FFFFFF",
    //background: "rgba(56, 62, 66, 0.8)",
    //backdrop: "rgba(0, 0, 0, 0.5)",
    showCancelButton: false,
  });
}
