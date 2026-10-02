(() => {
    "use strict";

    document.querySelectorAll("[data-confirmation-map]").forEach(image => {
        image.addEventListener("error", () => {
            image.hidden = true;
            const fallback = image.closest(".map-frame")?.querySelector("[data-map-fallback]");
            if (fallback) fallback.hidden = false;
        }, {once: true});
    });
})();
