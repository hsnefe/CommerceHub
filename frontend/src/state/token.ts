const ACCESS_TOKEN_KEY = 'commercehub.accessToken';
const REFRESH_TOKEN_KEY = 'commercehub.refreshToken';
const EXPIRES_AT_KEY = 'commercehub.expiresAt';

export interface TokenState {
  accessToken: string | null;
  refreshToken: string | null;
  expiresAt: number | null;
}

export function getTokens(): TokenState {
  return {
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
    expiresAt: readExpiresAt(),
  };
}

export function saveTokens(accessToken: string, refreshToken: string, expiresIn: number): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  localStorage.setItem(EXPIRES_AT_KEY, String(Date.now() + expiresIn * 1000));
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(EXPIRES_AT_KEY);
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getExpiresAt(): number | null {
  return readExpiresAt();
}

function readExpiresAt(): number | null {
  const raw = localStorage.getItem(EXPIRES_AT_KEY);
  if (!raw) {
    return null;
  }
  const value = Number(raw);
  return Number.isFinite(value) ? value : null;
}
