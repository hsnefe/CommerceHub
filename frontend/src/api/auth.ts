import { apiRequest } from './client';
import { clearTokens, getRefreshToken, saveTokens } from '../state/token';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserResponse {
  id: string;
  email: string;
  roles: string[];
}

export async function register(email: string, password: string) {
  return apiRequest<UserResponse>('auth', '/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export async function login(email: string, password: string) {
  const result = await apiRequest<TokenResponse>('auth', '/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  if (result.ok && result.data) {
    saveTokens(result.data.accessToken, result.data.refreshToken, result.data.expiresIn);
  }
  return result;
}

export async function registerAndLogin(email: string, password: string) {
  const registerResult = await register(email, password);
  if (!registerResult.ok) {
    return { registerResult, loginResult: null };
  }
  const loginResult = await login(email, password);
  return { registerResult, loginResult };
}

export async function refresh() {
  const refreshToken = getRefreshToken();
  const result = await apiRequest<TokenResponse>('auth', '/api/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
  if (result.ok && result.data) {
    saveTokens(result.data.accessToken, result.data.refreshToken, result.data.expiresIn);
  }
  return result;
}

export async function logout() {
  const refreshToken = getRefreshToken();
  const result = await apiRequest('auth', '/api/v1/auth/logout', {
    method: 'POST',
    auth: true,
    body: JSON.stringify({ refreshToken }),
  });
  if (result.ok) {
    clearTokens();
  }
  return result;
}

export async function me() {
  return apiRequest<UserResponse>('auth', '/api/v1/auth/me', {
    method: 'GET',
    auth: true,
  });
}
