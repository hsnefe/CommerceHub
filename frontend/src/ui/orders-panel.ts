import * as ordersApi from '../api/orders';
import type { OrderSummaryResponse } from '../api/orders';
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

function renderOrderList(orders: OrderSummaryResponse[]): string {
  if (orders.length === 0) {
    return '<p class="hint">Sipariş yok.</p>';
  }
  return `
    <ul class="id-list">
      ${orders
        .map(
          (order) => `
        <li>
          <button type="button" class="link-btn" data-order-id="${order.orderId}">
            <strong>${order.status}</strong>
            <span>${order.totalPrice}</span>
            <code>${order.orderId}</code>
          </button>
        </li>
      `,
        )
        .join('')}
    </ul>
    <p class="hint">Satıra tıklayarak sipariş ID'sini formlara kopyala.</p>
  `;
}

export function fillOrderProductId(productId: string): void {
  const input = document.querySelector<HTMLInputElement>('#create-order-product-id');
  if (input) {
    input.value = productId;
  }
}

export function fillOrderId(orderId: string): void {
  for (const id of ['get-order-id', 'cancel-order-id']) {
    const input = document.querySelector<HTMLInputElement>(`#${id}`);
    if (input) {
      input.value = orderId;
    }
  }
}

export function mountOrdersPanel(container: HTMLElement): void {
  const user = getCurrentUser();
  const defaultUserId = user?.id ?? '';

  container.innerHTML = `
    ${renderSessionBanner()}
    <div class="flow-banner flow-banner-info">
      Adım 5: Ürün ID + miktar ile sipariş oluştur; stok düşer ve notification-service log'una mail yazılır.
    </div>
    ${section(
      'Oluştur (JWT)',
      `
      <form id="create-order-form" class="form-grid">
        ${field('Ürün ID', 'create-order-product-id')}
        ${field('Miktar', 'create-order-quantity', 'number', '1')}
        <button type="submit" class="btn-primary">Sipariş oluştur</button>
      </form>
    `,
    )}
    ${section(
      'Kullanıcı siparişleri (JWT)',
      `
      <form id="list-orders-form" class="form-grid">
        ${field('Kullanıcı ID', 'list-orders-user-id', 'text', defaultUserId)}
        <button type="submit" class="btn-primary">Listele</button>
      </form>
      <div id="orders-list" class="category-list"></div>
    `,
    )}
    ${section(
      'Detay (JWT)',
      `
      <form id="get-order-form" class="form-grid">
        ${field('Sipariş ID', 'get-order-id')}
        <button type="submit" class="btn-primary">Getir</button>
      </form>
    `,
    )}
    ${section(
      'İptal (JWT)',
      `
      <form id="cancel-order-form" class="form-grid">
        ${field('Sipariş ID', 'cancel-order-id')}
        <button type="submit" class="btn-secondary">İptal et</button>
      </form>
      <p class="hint">CREATED siparişleri iptal edilir; stok geri yazılır.</p>
    `,
    )}
  `;

  const listContainer = container.querySelector('#orders-list');

  const showList = (orders: OrderSummaryResponse[]) => {
    if (!listContainer) return;
    listContainer.innerHTML = renderOrderList(orders);
    listContainer.querySelectorAll('[data-order-id]').forEach((button) => {
      button.addEventListener('click', () => {
        const id = button.getAttribute('data-order-id');
        if (id) {
          fillOrderId(id);
        }
      });
    });
  };

  container.querySelector('#create-order-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const productId = (form.querySelector('#create-order-product-id') as HTMLInputElement).value.trim();
    const quantity = Number((form.querySelector('#create-order-quantity') as HTMLInputElement).value);
    const result = await ordersApi.createOrder({
      items: [{ productId, quantity: Number.isFinite(quantity) ? quantity : 1 }],
    });
    if (result.ok && result.data && 'orderId' in (result.data as object)) {
      fillOrderId((result.data as { orderId: string }).orderId);
    }
  });

  container.querySelector('#list-orders-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const userId = (form.querySelector('#list-orders-user-id') as HTMLInputElement).value.trim();
    const result = await ordersApi.listOrdersByUser(userId);
    if (result.ok && Array.isArray(result.data)) {
      showList(result.data);
    }
  });

  container.querySelector('#get-order-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const orderId = (form.querySelector('#get-order-id') as HTMLInputElement).value.trim();
    await ordersApi.getOrder(orderId);
  });

  container.querySelector('#cancel-order-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const orderId = (form.querySelector('#cancel-order-id') as HTMLInputElement).value.trim();
    await ordersApi.cancelOrder(orderId);
  });
}
