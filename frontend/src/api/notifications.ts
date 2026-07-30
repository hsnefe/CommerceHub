import { apiRequest } from './client';

export interface SendNotificationRequest {
  email: string;
  subject: string;
  message: string;
}

export interface SendNotificationResponse {
  success: boolean;
}

export async function sendNotification(body: SendNotificationRequest) {
  return apiRequest<SendNotificationResponse>('notification', '/api/v1/notifications', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
