import { getApiConfig, setApiConfig } from '../api/client';
import { me } from '../api/auth';
import { clearTokens, getExpiresAt, getTokens } from '../state/token';
import { emitWorkflowEvent } from '../state/workflow';
import type { UserResponse } from '../api/auth';

export type SessionChangeListener = (user: UserResponse | null) => void;

const listeners = new Set<SessionChangeListener>();
let currentUser: UserResponse | null = null;

export function onSessionChange(listener: SessionChangeListener): () => void {
  listeners.add(listener);
  listener(currentUser);
  return () => listeners.delete(listener);
}

export function getCurrentUser(): UserResponse | null {
  return currentUser;
}

export function isAuthenticated(): boolean {
  return Boolean(getTokens().accessToken);
}

export function isAdmin(): boolean {
  return currentUser?.roles.includes('ADMIN') ?? false;
}

function notifySession(): void {
  for (const listener of listeners) {
    listener(currentUser);
  }
}

export async function refreshSession(): Promise<void> {
  const tokens = getTokens();
  if (!tokens.accessToken) {
    currentUser = null;
    notifySession();
    return;
  }
  const result = await me();
  currentUser = result.ok ? (result.data as UserResponse) : null;
  notifySession();
}

export function mountHeader(root: HTMLElement): void {
  const header = document.createElement('header');
  header.className = 'app-header';
  header.innerHTML = `
    <div class="header-brand">
      <h1>CommerceHub Dev Console</h1>
      <p>Auth → Product → Inventory → Order → Notification akışını tek oturumda test et</p>
    </div>
    <div class="header-config">
      <label>
        Auth API
        <input id="auth-base-url" type="text" placeholder="Boş = Vite proxy (önerilen)" />
      </label>
      <label>
        Product API
        <input id="product-base-url" type="text" placeholder="Boş = Vite proxy (önerilen)" />
      </label>
      <label>
        Inventory API
        <input id="inventory-base-url" type="text" placeholder="Boş = Vite proxy (önerilen)" />
      </label>
      <label>
        Order API
        <input id="order-base-url" type="text" placeholder="Boş = Vite proxy (önerilen)" />
      </label>
      <label>
        Notification API
        <input id="notification-base-url" type="text" placeholder="Boş = Vite proxy (önerilen)" />
      </label>
      <p class="hint">Proxy: auth→8081, products→8082, inventory→8083, orders→8084, notifications→8085</p>
    </div>
    <div class="header-session">
      <div id="session-info" class="session-info">Oturum yok</div>
      <div id="connection-info" class="connection-info">Bağlantı kontrol ediliyor...</div>
      <button id="clear-tokens" type="button" class="btn-secondary">Token temizle</button>
    </div>
  `;
  root.appendChild(header);

  const config = getApiConfig();
  const authInput = header.querySelector<HTMLInputElement>('#auth-base-url');
  const productInput = header.querySelector<HTMLInputElement>('#product-base-url');
  const inventoryInput = header.querySelector<HTMLInputElement>('#inventory-base-url');
  const orderInput = header.querySelector<HTMLInputElement>('#order-base-url');
  const notificationInput = header.querySelector<HTMLInputElement>('#notification-base-url');
  const sessionInfo = header.querySelector('#session-info');
  const connectionInfo = header.querySelector('#connection-info');
  const clearBtn = header.querySelector('#clear-tokens');

  if (authInput) authInput.value = config.authBaseUrl;
  if (productInput) productInput.value = config.productBaseUrl;
  if (inventoryInput) inventoryInput.value = config.inventoryBaseUrl;
  if (orderInput) orderInput.value = config.orderBaseUrl;
  if (notificationInput) notificationInput.value = config.notificationBaseUrl;

  const applyConfig = () => {
    setApiConfig({
      authBaseUrl: authInput?.value.trim() ?? '',
      productBaseUrl: productInput?.value.trim() ?? '',
      inventoryBaseUrl: inventoryInput?.value.trim() ?? '',
      orderBaseUrl: orderInput?.value.trim() ?? '',
      notificationBaseUrl: notificationInput?.value.trim() ?? '',
    });
    void checkConnections();
  };

  authInput?.addEventListener('change', applyConfig);
  productInput?.addEventListener('change', applyConfig);
  inventoryInput?.addEventListener('change', applyConfig);
  orderInput?.addEventListener('change', applyConfig);
  notificationInput?.addEventListener('change', applyConfig);

  const renderSession = (user: UserResponse | null) => {
    if (!sessionInfo) return;
    const tokens = getTokens();
    const expiresAt = getExpiresAt();
    if (!tokens.accessToken) {
      sessionInfo.textContent = 'Oturum yok — önce Auth sekmesinden giriş yap';
      return;
    }
    const expiry = expiresAt
      ? new Date(expiresAt).toLocaleTimeString('tr-TR')
      : 'bilinmiyor';
    if (user) {
      const adminNote = user.roles.includes('ADMIN')
        ? 'Ürün/kategori/stok yazma işlemleri açık'
        : 'Sadece okuma — ADMIN için README SQL';
      sessionInfo.innerHTML = `
        <strong>${user.email}</strong>
        <span class="roles">${user.roles.join(', ')}</span>
        <span class="expiry">Token bitiş: ${expiry}</span>
        <span class="hint">${adminNote}</span>
      `;
    } else {
      sessionInfo.textContent = `Token var, profil alınamadı (bitiş: ${expiry})`;
    }
  };

  onSessionChange(renderSession);

  clearBtn?.addEventListener('click', () => {
    clearTokens();
    currentUser = null;
    notifySession();
    emitWorkflowEvent('logout');
  });

  async function checkConnections(): Promise<void> {
    if (!connectionInfo) return;
    const cfg = getApiConfig();
    const authUrl = `${cfg.authBaseUrl}/api/v1/auth/me`;
    const productUrl = `${cfg.productBaseUrl}/api/v1/products/categories`;
    const inventoryUrl = `${cfg.inventoryBaseUrl}/api/v1/inventory?page=0&size=1`;
    const orderUrl = `${cfg.orderBaseUrl}/v3/api-docs`;
    const notificationUrl = `${cfg.notificationBaseUrl}/v3/api-docs`;

    const [authOk, productOk, inventoryOk, orderOk, notificationOk] = await Promise.all([
      fetch(authUrl, { method: 'GET' }).then((r) => r.status === 401 || r.ok),
      fetch(productUrl, { method: 'GET' }).then((r) => r.ok),
      fetch(inventoryUrl, { method: 'GET' }).then((r) => r.ok),
      fetch(orderUrl, { method: 'GET' }).then((r) => r.ok),
      fetch(notificationUrl, { method: 'GET' }).then((r) => r.ok),
    ]);

    if (authOk && productOk && inventoryOk && orderOk && notificationOk) {
      connectionInfo.textContent =
        'Auth (8081), Product (8082), Inventory (8083), Order (8084), Notification (8085) erişilebilir';
      connectionInfo.className = 'connection-info connection-ok';
      return;
    }
    connectionInfo.textContent = 'Servisler kapalı olabilir — docker compose up --build çalıştır';
    connectionInfo.className = 'connection-info connection-error';
  }

  void refreshSession();
  void checkConnections();
}

export function setSessionUser(user: UserResponse | null): void {
  currentUser = user;
  notifySession();
}
