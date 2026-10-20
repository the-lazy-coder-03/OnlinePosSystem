document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("form[data-login-form]").forEach((form) => {
        const button = form.querySelector("button[type='submit']");
        const passwordInput = form.querySelector("[data-password-input]");
        const passwordToggle = form.querySelector("[data-password-toggle]");
        let submitting = false;

        if (passwordInput && passwordToggle) {
            passwordToggle.addEventListener("click", () => {
                const revealed = passwordInput.type === "password";
                passwordInput.type = revealed ? "text" : "password";
                passwordToggle.textContent = revealed ? "Hide" : "Show";
                passwordToggle.setAttribute("aria-label", revealed ? "Hide password" : "Show password");
                passwordToggle.setAttribute("aria-pressed", String(revealed));
                passwordInput.focus({preventScroll: true});
            });
        }

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
