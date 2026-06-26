import { getAccessToken, getRefreshToken, saveTokens } from '../state/token';

export interface ApiResult<T = unknown> {
  ok: boolean;
  status: number;
  durationMs: number;
  data: T;
}

export interface ApiConfig {
  authBaseUrl: string;
  productBaseUrl: string;
  inventoryBaseUrl: string;
}

interface RequestOptions extends RequestInit {
  auth?: boolean;
  retried?: boolean;
}

let config: ApiConfig = {
  authBaseUrl: import.meta.env.VITE_AUTH_API_URL ?? '',
  productBaseUrl: import.meta.env.VITE_PRODUCT_API_URL ?? '',
  inventoryBaseUrl: import.meta.env.VITE_INVENTORY_API_URL ?? '',
};

type ResponseListener = (result: ApiResult) => void;

const listeners = new Set<ResponseListener>();

export function getApiConfig(): ApiConfig {
  return { ...config };
}

export function setApiConfig(next: ApiConfig): void {
  config = { ...next };
}

export function onApiResponse(listener: ResponseListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function notify(result: ApiResult): void {
  for (const listener of listeners) {
    listener(result);
  }
}

function resolveUrl(baseUrl: 'auth' | 'product' | 'inventory', path: string): string {
  const root =
    baseUrl === 'auth'
      ? config.authBaseUrl
      : baseUrl === 'product'
        ? config.productBaseUrl
        : config.inventoryBaseUrl;
  return `${root}${path}`;
}

async function parseBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function tryRefreshToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return false;
  }

  const response = await fetch(resolveUrl('auth', '/api/v1/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    return false;
  }

  const data = (await parseBody(response)) as {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
  };
  saveTokens(data.accessToken, data.refreshToken, data.expiresIn);
  return true;
}

export async function apiRequest<T = unknown>(
  baseUrl: 'auth' | 'product' | 'inventory',
  path: string,
  options: RequestOptions = {},
): Promise<ApiResult<T>> {
  const started = performance.now();
  const headers = new Headers(options.headers ?? {});
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (options.auth) {
    const token = getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  const response = await fetch(resolveUrl(baseUrl, path), {
    ...options,
    headers,
  });

  if (response.status === 401 && options.auth && !options.retried) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      return apiRequest<T>(baseUrl, path, { ...options, retried: true });
    }
  }

  const data = (await parseBody(response)) as T;
  const result: ApiResult<T> = {
    ok: response.ok,
    status: response.status,
    durationMs: Math.round(performance.now() - started),
    data,
  };
  notify(result);
  return result;
}
