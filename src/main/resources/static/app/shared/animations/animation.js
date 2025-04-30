document.addEventListener("DOMContentLoaded", () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const el = entry.target;
        const anim = el.getAttribute("data-inviewport");
        el.classList.add(anim);
      }
    });
  });

  const elements = document.querySelectorAll("[data-inviewport]");

  if (elements.length > 0) {
    elements.forEach((el) => {
      observer.observe(el);
    });
  }
});

function animations() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const el = entry.target;
        const anim = el.getAttribute("data-inviewport");
        el.classList.add(anim);
      }
    });
  });

  const elements = document.querySelectorAll("[data-inviewport]");

  if (elements.length > 0) {
    elements.forEach((el) => {
      observer.observe(el);
    });
  }
}
