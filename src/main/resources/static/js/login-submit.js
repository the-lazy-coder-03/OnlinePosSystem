document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("form[data-login-form]").forEach((form) => {
        const button = form.querySelector("button[type='submit']");
        let submitting = false;

        form.addEventListener("submit", (event) => {
            if (submitting) {
                event.preventDefault();
                return;
            }

            submitting = true;
            if (button) {
                button.disabled = true;
                button.textContent = button.dataset.submittingText || "Signing in...";
            }
        });
    });
});
