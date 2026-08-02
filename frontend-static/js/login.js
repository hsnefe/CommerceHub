import { login, me } from "./api.js";
import { getSession, roleFromRoles, setSession } from "./session.js";

const form = document.getElementById("login-form");
const errorEl = document.getElementById("login-error");

const existing = getSession();
if (existing?.accessToken && existing?.role) {
  window.location.href = existing.role === "admin" ? "admin.html" : "user.html";
}

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

  const submitBtn = form.querySelector('button[type="submit"]');
  if (submitBtn) submitBtn.disabled = true;

  try {
    const tokenResult = await login(email, password);
    if (!tokenResult.ok || !tokenResult.data?.accessToken) {
      const msg =
        typeof tokenResult.data === "object" && tokenResult.data?.message
          ? tokenResult.data.message
          : "Login failed. Check your email and password.";
      showError(msg);
      return;
    }

    setSession({
      email,
      role: "user",
      userId: null,
      accessToken: tokenResult.data.accessToken,
      refreshToken: tokenResult.data.refreshToken,
    });

    const meResult = await me();
    if (!meResult.ok || !meResult.data) {
      showError("Logged in, but could not load your profile.");
      return;
    }

    const role = roleFromRoles(meResult.data.roles);
    setSession({
      email: meResult.data.email,
      role,
      userId: meResult.data.id,
      accessToken: tokenResult.data.accessToken,
      refreshToken: tokenResult.data.refreshToken,
    });

    window.location.href = role === "admin" ? "admin.html" : "user.html";
  } catch {
    showError("Could not reach the auth service. Is it running on port 8081?");
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
});
