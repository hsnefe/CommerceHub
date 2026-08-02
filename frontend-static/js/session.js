const SESSION_KEY = "commercehub_session";

export function getSession() {
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setSession(session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

/**
 * @param {"user" | "admin"} [expectedRole]
 */
export function requireAuth(expectedRole) {
  const session = getSession();
  if (!session?.email || !session?.accessToken) {
    window.location.href = "login.html";
    return null;
  }
  if (expectedRole && session.role !== expectedRole) {
    window.location.href = session.role === "admin" ? "admin.html" : "user.html";
    return null;
  }
  return session;
}

export function logout() {
  clearSession();
  window.location.href = "login.html";
}

/**
 * @param {string[]} roles
 * @returns {"admin" | "user"}
 */
export function roleFromRoles(roles) {
  if (Array.isArray(roles) && roles.some((r) => String(r).toUpperCase() === "ADMIN")) {
    return "admin";
  }
  return "user";
}
