import * as notificationsApi from '../api/notifications';
import { getCurrentUser } from './header';
import { renderSessionBanner } from './session-banner';

function field(label: string, id: string, type = 'text', value = ''): string {
  return `
    <label for="${id}">
      ${label}
      <input id="${id}" name="${id}" type="${type}" value="${value}" />
    </label>
  `;
}

function section(title: string, body: string): string {
  return `
    <section class="panel-section">
      <h3>${title}</h3>
      ${body}
    </section>
  `;
}

export function mountNotificationsPanel(container: HTMLElement): void {
  const user = getCurrentUser();
  const defaultEmail = user?.email ?? '';

  container.innerHTML = `
    ${renderSessionBanner()}
    <div class="flow-banner flow-banner-info">
      Adım 6: Email simülasyonu — gerçek SMTP yok; notification-service log'una yazar.
      Sipariş oluşturunca da otomatik tetiklenir.
    </div>
    ${section(
      'Bildirim gönder (Public)',
      `
      <form id="send-notification-form" class="form-grid">
        ${field('Email', 'notification-email', 'email', defaultEmail)}
        ${field('Konu', 'notification-subject', 'text', 'Order Created')}
        <label for="notification-message">
          Mesaj
          <textarea id="notification-message" name="notification-message" rows="3">Your order has been created.</textarea>
        </label>
        <button type="submit" class="btn-primary">Gönder</button>
      </form>
      <p class="hint">JWT gerekmez. docker compose log'larında "Email sent to …" satırını kontrol et.</p>
    `,
    )}
  `;

  container.querySelector('#send-notification-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    await notificationsApi.sendNotification({
      email: (form.querySelector('#notification-email') as HTMLInputElement).value.trim(),
      subject: (form.querySelector('#notification-subject') as HTMLInputElement).value.trim(),
      message: (form.querySelector('#notification-message') as HTMLTextAreaElement).value.trim(),
    });
  });
}
