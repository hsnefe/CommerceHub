import { apiRequest } from './client';

export interface OrderItemRequest {
  productId: string;
  quantity: number;
}

export interface CreateOrderRequest {
  items: OrderItemRequest[];
}

export interface CreateOrderResponse {
  orderId: string;
  status: string;
  totalPrice: number;
  createdAt: string;
}

export interface OrderSummaryResponse {
  orderId: string;
  status: string;
  totalPrice: number;
}

export interface OrderDetailResponse {
  orderId: string;
  userId: string;
  status: string;
  totalPrice: number;
  items: {
    productId: string;
    productName: string;
    unitPrice: number;
    quantity: number;
  }[];
}

export interface CancelOrderResponse {
  orderId: string;
  status: string;
}

export async function createOrder(body: CreateOrderRequest) {
  return apiRequest<CreateOrderResponse>('order', '/api/v1/orders', {
    method: 'POST',
    auth: true,
    body: JSON.stringify(body),
  });
}

export async function getOrder(orderId: string) {
  return apiRequest<OrderDetailResponse>('order', `/api/v1/orders/${orderId}`, {
    method: 'GET',
    auth: true,
  });
}

export async function listOrdersByUser(userId: string) {
  return apiRequest<OrderSummaryResponse[]>('order', `/api/v1/orders/user/${userId}`, {
    method: 'GET',
    auth: true,
  });
}

export async function cancelOrder(orderId: string) {
  return apiRequest<CancelOrderResponse>('order', `/api/v1/orders/${orderId}`, {
    method: 'DELETE',
    auth: true,
  });
}
