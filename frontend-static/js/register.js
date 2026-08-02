import { register } from "./api.js";

const form = document.getElementById("register-form");
const errorEl = document.getElementById("register-error");

function showError(message) {
  if (!errorEl) return;
  errorEl.textContent = message;
  errorEl.hidden = !message;
}

form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  showError("");

  const email = /** @type {HTMLInputElement} */ (document.getElementById("email")).value.trim();
  const password = /** @type {HTMLInputElement} */ (document.getElementById("password")).value;
  const confirm = /** @type {HTMLInputElement} */ (document.getElementById("confirm-password"))
    .value;

  if (password !== confirm) {
    showError("Passwords do not match.");
    return;
  }

  const submitBtn = form.querySelector('button[type="submit"]');
  if (submitBtn) submitBtn.disabled = true;

  try {
    const result = await register(email, password);
    if (!result.ok) {
      const msg =
        typeof result.data === "object" && result.data?.message
          ? result.data.message
          : "Registration failed.";
      showError(msg);
      return;
    }
    window.location.href = "login.html";
  } catch {
    showError("Could not reach the auth service. Is it running on port 8081?");
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
});
