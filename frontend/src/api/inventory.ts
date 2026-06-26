import { apiRequest } from './client';

export interface InventoryResponse {
  productId: string;
  availableQuantity: number;
  lowStockThreshold: number;
  lowStock: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryPageResponse {
  content: InventoryResponse[];
  page: number;
  size: number;
  totalElements: number;
}

export interface InventoryRequest {
  productId: string;
  availableQuantity: number;
  lowStockThreshold: number;
}

export interface InventoryUpdateRequest {
  availableQuantity?: number;
  lowStockThreshold?: number;
}

export async function listInventory(params: { page?: number; size?: number }) {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  const suffix = query.toString() ? `?${query}` : '';
  return apiRequest<InventoryPageResponse>('inventory', `/api/v1/inventory${suffix}`, {
    method: 'GET',
  });
}

export async function getInventory(productId: string) {
  return apiRequest<InventoryResponse>('inventory', `/api/v1/inventory/${productId}`, {
    method: 'GET',
  });
}

export async function createInventory(body: InventoryRequest) {
  return apiRequest<InventoryResponse>('inventory', '/api/v1/inventory', {
    method: 'POST',
    auth: true,
    body: JSON.stringify(body),
  });
}

export async function updateInventory(productId: string, body: InventoryUpdateRequest) {
  return apiRequest<InventoryResponse>('inventory', `/api/v1/inventory/${productId}`, {
    method: 'PATCH',
    auth: true,
    body: JSON.stringify(body),
  });
}

export async function decrementInventory(productId: string, amount: number) {
  return apiRequest<InventoryResponse>(
    'inventory',
    `/internal/inventory/${productId}/decrement`,
    {
      method: 'POST',
      body: JSON.stringify({ amount }),
    },
  );
}

export async function incrementInventory(productId: string, amount: number) {
  return apiRequest<InventoryResponse>(
    'inventory',
    `/internal/inventory/${productId}/increment`,
    {
      method: 'POST',
      body: JSON.stringify({ amount }),
    },
  );
}
