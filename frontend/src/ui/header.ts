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
      <p>Auth → Product akışını tek oturumda test et</p>
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
      <p class="hint">Proxy modunda istekler <code>/api/v1/auth</code> → 8081, <code>/api/v1/products</code> → 8082</p>
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
  const sessionInfo = header.querySelector('#session-info');
  const connectionInfo = header.querySelector('#connection-info');
  const clearBtn = header.querySelector('#clear-tokens');

  if (authInput) authInput.value = config.authBaseUrl;
  if (productInput) productInput.value = config.productBaseUrl;

  const applyConfig = () => {
    setApiConfig({
      authBaseUrl: authInput?.value.trim() ?? '',
      productBaseUrl: productInput?.value.trim() ?? '',
    });
    void checkConnections();
  };

  authInput?.addEventListener('change', applyConfig);
  productInput?.addEventListener('change', applyConfig);

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
        ? 'Ürün/kategori yazma işlemleri açık'
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
    const authOk = await fetch(authUrl, { method: 'GET' }).then((r) => r.status === 401 || r.ok);

    if (authOk) {
      connectionInfo.textContent = 'Auth-service (8081) erişilebilir';
      connectionInfo.className = 'connection-info connection-ok';
      return;
    }
    connectionInfo.textContent = 'Auth-service kapalı — docker compose up --build çalıştır';
    connectionInfo.className = 'connection-info connection-error';
  }

  void refreshSession();
  void checkConnections();
}

export function setSessionUser(user: UserResponse | null): void {
  currentUser = user;
  notifySession();
}
