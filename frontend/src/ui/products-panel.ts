import * as productsApi from '../api/products';
import { renderSessionBanner } from './session-banner';
import { fillInventoryProductId } from './inventory-panel';
import { fillOrderProductId } from './orders-panel';

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

export function mountProductsPanel(container: HTMLElement): void {
  container.innerHTML = `
    ${renderSessionBanner()}
    <div class="flow-banner flow-banner-info">
      Adım 3: Kategori ID'si ile ürün oluştur, listele ve detay getir. Token auth-service'ten gelir, product-service doğrular.
    </div>
    ${section(
      'Listele (Public)',
      `
      <form id="list-products-form" class="form-grid">
        ${field('Sayfa', 'list-page', 'number', '0')}
        ${field('Boyut', 'list-size', 'number', '20')}
        ${field('Kategori ID', 'list-category-id')}
        ${field('İsim filtresi', 'list-name')}
        <button type="submit" class="btn-primary">Listele</button>
      </form>
    `,
    )}
    ${section(
      'Detay (Public)',
      `
      <form id="get-product-form" class="form-grid">
        ${field('Ürün ID', 'get-product-id')}
        <button type="submit" class="btn-primary">Getir</button>
      </form>
    `,
    )}
    ${section(
      'Oluştur (ADMIN)',
      `
      <form id="create-product-form" class="form-grid">
        ${field('İsim', 'create-name', 'text', 'Widget')}
        ${field('Açıklama', 'create-description', 'text', 'A widget')}
        ${field('Fiyat', 'create-price', 'number', '9.99')}
        ${field('Kategori ID', 'create-category-id')}
        <button type="submit" class="btn-primary">Oluştur</button>
      </form>
    `,
    )}
    ${section(
      'Güncelle (ADMIN)',
      `
      <form id="update-product-form" class="form-grid">
        ${field('Ürün ID', 'update-id')}
        ${field('İsim', 'update-name', 'text', 'Widget')}
        ${field('Açıklama', 'update-description', 'text', 'Updated widget')}
        ${field('Fiyat', 'update-price', 'number', '12.99')}
        ${field('Kategori ID', 'update-category-id')}
        <button type="submit" class="btn-primary">Güncelle</button>
      </form>
    `,
    )}
    ${section(
      'Sil (ADMIN)',
      `
      <form id="delete-product-form" class="form-grid">
        ${field('Ürün ID', 'delete-id')}
        <button type="submit" class="btn-danger">Sil</button>
      </form>
    `,
    )}
  `;

  container.querySelector('#list-products-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const page = Number((form.querySelector('#list-page') as HTMLInputElement).value);
    const size = Number((form.querySelector('#list-size') as HTMLInputElement).value);
    const categoryId = (form.querySelector('#list-category-id') as HTMLInputElement).value.trim();
    const name = (form.querySelector('#list-name') as HTMLInputElement).value.trim();
    await productsApi.listProducts({
      page: Number.isFinite(page) ? page : 0,
      size: Number.isFinite(size) ? size : 20,
      categoryId: categoryId || undefined,
      name: name || undefined,
    });
  });

  container.querySelector('#get-product-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const id = (form.querySelector('#get-product-id') as HTMLInputElement).value.trim();
    await productsApi.getProduct(id);
  });

  container.querySelector('#create-product-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    await productsApi.createProduct({
      name: (form.querySelector('#create-name') as HTMLInputElement).value.trim(),
      description: (form.querySelector('#create-description') as HTMLInputElement).value.trim(),
      price: Number((form.querySelector('#create-price') as HTMLInputElement).value),
      categoryId: (form.querySelector('#create-category-id') as HTMLInputElement).value.trim(),
    });
  });

  container.querySelector('#update-product-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const id = (form.querySelector('#update-id') as HTMLInputElement).value.trim();
    await productsApi.updateProduct(id, {
      name: (form.querySelector('#update-name') as HTMLInputElement).value.trim(),
      description: (form.querySelector('#update-description') as HTMLInputElement).value.trim(),
      price: Number((form.querySelector('#update-price') as HTMLInputElement).value),
      categoryId: (form.querySelector('#update-category-id') as HTMLInputElement).value.trim(),
    });
  });

  container.querySelector('#delete-product-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const id = (form.querySelector('#delete-id') as HTMLInputElement).value.trim();
    await productsApi.deleteProduct(id);
  });
}

export function fillProductCategoryId(categoryId: string): void {
  for (const id of ['create-category-id', 'update-category-id', 'list-category-id']) {
    const input = document.querySelector<HTMLInputElement>(`#${id}`);
    if (input) {
      input.value = categoryId;
    }
  }
}

export function fillProductId(productId: string): void {
  for (const id of ['get-product-id', 'update-id', 'delete-id']) {
    const input = document.querySelector<HTMLInputElement>(`#${id}`);
    if (input) {
      input.value = productId;
    }
  }
  fillInventoryProductId(productId);
  fillOrderProductId(productId);
}
