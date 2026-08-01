import * as inventoryApi from '../api/inventory';
import type { InventoryResponse } from '../api/inventory';
import { renderSessionBanner } from './session-banner';

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function isUuid(value: string): boolean {
  return UUID_RE.test(value);
}

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

function renderInventoryList(items: InventoryResponse[]): string {
  if (items.length === 0) {
    return '<p class="hint">Kayıt yok.</p>';
  }
  return `
    <ul class="id-list">
      ${items
        .map(
          (item) => `
        <li>
          <button type="button" class="link-btn" data-product-id="${item.productId}">
            <strong>Stok: ${item.availableQuantity}</strong>
            <span>${item.lowStock ? 'Düşük stok' : 'Normal'} · eşik ${item.lowStockThreshold}</span>
            <code>${item.productId}</code>
          </button>
        </li>
      `,
        )
        .join('')}
    </ul>
    <p class="hint">Satıra tıklayarak ürün ID'sini formlara kopyala.</p>
  `;
}

export function fillInventoryProductId(productId: string): void {
  for (const id of [
    'get-inventory-product-id',
    'create-inventory-product-id',
    'update-inventory-product-id',
    'decrement-product-id',
    'increment-product-id',
  ]) {
    const input = document.querySelector<HTMLInputElement>(`#${id}`);
    if (input) {
      input.value = productId;
    }
  }
}

export function mountInventoryPanel(container: HTMLElement): void {
  container.innerHTML = `
    ${renderSessionBanner()}
    <div class="flow-banner flow-banner-info">
      Adım 4: Ürün ID'si ile stok kaydı oluştur, güncelle veya internal increment/decrement dene.
    </div>
    ${section(
      'Listele (Public)',
      `
      <form id="list-inventory-form" class="form-grid">
        ${field('Sayfa', 'inventory-list-page', 'number', '0')}
        ${field('Boyut', 'inventory-list-size', 'number', '20')}
        <button type="submit" class="btn-primary">Listele</button>
      </form>
      <div id="inventory-list" class="category-list"></div>
    `,
    )}
    ${section(
      'Detay (Public)',
      `
      <form id="get-inventory-form" class="form-grid">
        ${field('Ürün ID', 'get-inventory-product-id')}
        <button type="submit" class="btn-primary">Getir</button>
      </form>
    `,
    )}
    ${section(
      'Oluştur (ADMIN)',
      `
      <form id="create-inventory-form" class="form-grid">
        ${field('Ürün ID', 'create-inventory-product-id')}
        ${field('Mevcut miktar', 'create-available-quantity', 'number', '100')}
        ${field('Düşük stok eşiği', 'create-low-stock-threshold', 'number', '10')}
        <button type="submit" class="btn-primary">Oluştur</button>
      </form>
    `,
    )}
    ${section(
      'Güncelle (ADMIN)',
      `
      <form id="update-inventory-form" class="form-grid">
        ${field('Ürün ID', 'update-inventory-product-id')}
        ${field('Mevcut miktar', 'update-available-quantity', 'number', '50')}
        ${field('Düşük stok eşiği', 'update-low-stock-threshold', 'number', '5')}
        <button type="submit" class="btn-primary">Güncelle</button>
      </form>
    `,
    )}
    ${section(
      'Internal (Public)',
      `
      <form id="decrement-inventory-form" class="form-grid">
        ${field('Ürün ID', 'decrement-product-id')}
        ${field('Miktar', 'decrement-amount', 'number', '1')}
        <button type="submit" class="btn-secondary">Decrement</button>
      </form>
      <form id="increment-inventory-form" class="form-grid">
        ${field('Ürün ID', 'increment-product-id')}
        ${field('Miktar', 'increment-amount', 'number', '1')}
        <button type="submit" class="btn-secondary">Increment</button>
      </form>
      <p class="hint">Order-service simülasyonu — JWT gerekmez.</p>
    `,
    )}
  `;

  const listContainer = container.querySelector('#inventory-list');

  const showList = (items: InventoryResponse[]) => {
    if (!listContainer) return;
    listContainer.innerHTML = renderInventoryList(items);
    listContainer.querySelectorAll('[data-product-id]').forEach((button) => {
      button.addEventListener('click', () => {
        const id = button.getAttribute('data-product-id');
        if (id) {
          fillInventoryProductId(id);
        }
      });
    });
  };

  container.querySelector('#list-inventory-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const page = Number((form.querySelector('#inventory-list-page') as HTMLInputElement).value);
    const size = Number((form.querySelector('#inventory-list-size') as HTMLInputElement).value);
    const result = await inventoryApi.listInventory({
      page: Number.isFinite(page) ? page : 0,
      size: Number.isFinite(size) ? size : 20,
    });
    if (result.ok && result.data && 'content' in (result.data as object)) {
      showList((result.data as { content: InventoryResponse[] }).content);
    }
  });

  container.querySelector('#get-inventory-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const productId = (form.querySelector('#get-inventory-product-id') as HTMLInputElement).value.trim();
    await inventoryApi.getInventory(productId);
  });

  container.querySelector('#create-inventory-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const productId = (form.querySelector('#create-inventory-product-id') as HTMLInputElement).value.trim();
    if (!isUuid(productId)) {
      window.alert('Geçerli bir Ürün ID girin. Önce Products sekmesinden ürün oluşturup ID’yi buraya yapıştırın.');
      return;
    }
    await inventoryApi.createInventory({
      productId,
      availableQuantity: Number(
        (form.querySelector('#create-available-quantity') as HTMLInputElement).value,
      ),
      lowStockThreshold: Number(
        (form.querySelector('#create-low-stock-threshold') as HTMLInputElement).value,
      ),
    });
  });

  container.querySelector('#update-inventory-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const productId = (form.querySelector('#update-inventory-product-id') as HTMLInputElement).value.trim();
    await inventoryApi.updateInventory(productId, {
      availableQuantity: Number(
        (form.querySelector('#update-available-quantity') as HTMLInputElement).value,
      ),
      lowStockThreshold: Number(
        (form.querySelector('#update-low-stock-threshold') as HTMLInputElement).value,
      ),
    });
  });

  container.querySelector('#decrement-inventory-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const productId = (form.querySelector('#decrement-product-id') as HTMLInputElement).value.trim();
    const amount = Number((form.querySelector('#decrement-amount') as HTMLInputElement).value);
    await inventoryApi.decrementInventory(productId, amount);
  });

  container.querySelector('#increment-inventory-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const productId = (form.querySelector('#increment-product-id') as HTMLInputElement).value.trim();
    const amount = Number((form.querySelector('#increment-amount') as HTMLInputElement).value);
    await inventoryApi.incrementInventory(productId, amount);
  });
}
