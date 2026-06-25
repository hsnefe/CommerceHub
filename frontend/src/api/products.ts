import { apiRequest } from './client';

export interface CategoryResponse {
  id: string;
  name: string;
}

export interface ProductSummaryResponse {
  id: string;
  name: string;
  price: number;
  currency: string;
  categoryId: string;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  categoryId: string;
}

export interface ProductPageResponse {
  content: ProductSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
}

export interface ProductRequest {
  name: string;
  description?: string;
  price: number;
  categoryId: string;
}

export async function listProducts(params: {
  page?: number;
  size?: number;
  categoryId?: string;
  name?: string;
}) {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.categoryId) query.set('categoryId', params.categoryId);
  if (params.name) query.set('name', params.name);
  const suffix = query.toString() ? `?${query}` : '';
  return apiRequest<ProductPageResponse>('product', `/api/v1/products${suffix}`, {
    method: 'GET',
  });
}

export async function getProduct(id: string) {
  return apiRequest<ProductResponse>('product', `/api/v1/products/${id}`, {
    method: 'GET',
  });
}

export async function createProduct(body: ProductRequest) {
  return apiRequest('product', '/api/v1/products', {
    method: 'POST',
    auth: true,
    body: JSON.stringify(body),
  });
}

export async function updateProduct(id: string, body: ProductRequest) {
  return apiRequest('product', `/api/v1/products/${id}`, {
    method: 'PUT',
    auth: true,
    body: JSON.stringify(body),
  });
}

export async function deleteProduct(id: string) {
  return apiRequest('product', `/api/v1/products/${id}`, {
    method: 'DELETE',
    auth: true,
  });
}

export async function listCategories() {
  return apiRequest<CategoryResponse[]>('product', '/api/v1/products/categories', {
    method: 'GET',
  });
}

export async function createCategory(name: string) {
  return apiRequest('product', '/api/v1/products/categories', {
    method: 'POST',
    auth: true,
    body: JSON.stringify({ name }),
  });
}

export async function updateCategory(id: string, name: string) {
  return apiRequest('product', `/api/v1/products/categories/${id}`, {
    method: 'PUT',
    auth: true,
    body: JSON.stringify({ name }),
  });
}

export async function deleteCategory(id: string) {
  return apiRequest('product', `/api/v1/products/categories/${id}`, {
    method: 'DELETE',
    auth: true,
  });
}
