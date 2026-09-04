import { clearSession, getSession, setSession } from "./session.js";

// Every service is reached through the API Gateway on the same origin as this
// page, so the browser never needs a cross-origin request. Serve these files
// behind a proxy that forwards /api/* to the gateway (port 8080).
export const API = {
  auth: "",
  product: "",
  inventory: "",
  order: "",
};

/**
 * @param {"auth" | "product" | "inventory" | "order"} service
 * @param {string} path
 * @param {{ method?: string, body?: unknown, auth?: boolean, retried?: boolean }} [options]
 */
export async function apiRequest(service, path, options = {}) {
  const headers = new Headers();
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (options.auth) {
    const session = getSession();
    if (session?.accessToken) {
      headers.set("Authorization", `Bearer ${session.accessToken}`);
    }
  }

  const response = await fetch(`${API[service]}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401 && options.auth && !options.retried) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      return apiRequest(service, path, { ...options, retried: true });
    }
  }

  const data = await parseBody(response);
  return {
    ok: response.ok,
    status: response.status,
    data,
  };
}

async function parseBody(response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function tryRefreshToken() {
  const session = getSession();
  if (!session?.refreshToken) return false;

  const response = await fetch(`${API.auth}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  });

  if (!response.ok) {
    clearSession();
    return false;
  }

  const data = await parseBody(response);
  if (!data?.accessToken || !data?.refreshToken) {
    clearSession();
    return false;
  }

  setSession({
    ...session,
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
  });
  return true;
}

export function login(email, password) {
  return apiRequest("auth", "/api/v1/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export function register(email, password) {
  return apiRequest("auth", "/api/v1/auth/register", {
    method: "POST",
    body: { email, password },
  });
}

export function me() {
  return apiRequest("auth", "/api/v1/auth/me", { auth: true });
}

export function listCategories() {
  return apiRequest("product", "/api/v1/products/categories");
}

export function createCategory(name) {
  return apiRequest("product", "/api/v1/products/categories", {
    method: "POST",
    auth: true,
    body: { name },
  });
}

export function updateCategory(id, name) {
  return apiRequest("product", `/api/v1/products/categories/${id}`, {
    method: "PUT",
    auth: true,
    body: { name },
  });
}

export function deleteCategory(id) {
  return apiRequest("product", `/api/v1/products/categories/${id}`, {
    method: "DELETE",
    auth: true,
  });
}

/**
 * @param {{ page?: number, size?: number, categoryId?: string, name?: string }} params
 */
export function listProducts(params = {}) {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.categoryId) query.set("categoryId", params.categoryId);
  if (params.name) query.set("name", params.name);
  const suffix = query.toString() ? `?${query}` : "";
  return apiRequest("product", `/api/v1/products${suffix}`);
}

export function getProduct(id) {
  return apiRequest("product", `/api/v1/products/${id}`);
}

/**
 * @param {{ name: string, description?: string, price: number, categoryId: string }} body
 */
export function createProduct(body) {
  return apiRequest("product", "/api/v1/products", {
    method: "POST",
    auth: true,
    body,
  });
}

/**
 * @param {string} id
 * @param {{ name: string, description?: string, price: number, categoryId: string }} body
 */
export function updateProduct(id, body) {
  return apiRequest("product", `/api/v1/products/${id}`, {
    method: "PUT",
    auth: true,
    body,
  });
}

export function deleteProduct(id) {
  return apiRequest("product", `/api/v1/products/${id}`, {
    method: "DELETE",
    auth: true,
  });
}

/**
 * @param {{ page?: number, size?: number }} params
 */
export function listInventory(params = {}) {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  const suffix = query.toString() ? `?${query}` : "";
  return apiRequest("inventory", `/api/v1/inventory${suffix}`, { auth: true });
}

export function getInventory(productId) {
  return apiRequest("inventory", `/api/v1/inventory/${productId}`, { auth: true });
}

/**
 * @param {{ productId: string, availableQuantity: number, lowStockThreshold: number }} body
 */
export function createInventory(body) {
  return apiRequest("inventory", "/api/v1/inventory", {
    method: "POST",
    auth: true,
    body,
  });
}

/**
 * @param {string} productId
 * @param {{ availableQuantity?: number, lowStockThreshold?: number }} body
 */
export function updateInventory(productId, body) {
  return apiRequest("inventory", `/api/v1/inventory/${productId}`, {
    method: "PATCH",
    auth: true,
    body,
  });
}

/**
 * @param {{ items: { productId: string, quantity: number }[] }} body
 */
export function createOrder(body) {
  return apiRequest("order", "/api/v1/orders", {
    method: "POST",
    auth: true,
    body,
  });
}

export function listOrdersByUser(userId) {
  return apiRequest("order", `/api/v1/orders/user/${userId}`, { auth: true });
}

export function getOrder(orderId) {
  return apiRequest("order", `/api/v1/orders/${orderId}`, { auth: true });
}

export function cancelOrder(orderId) {
  return apiRequest("order", `/api/v1/orders/${orderId}`, {
    method: "DELETE",
    auth: true,
  });
}
