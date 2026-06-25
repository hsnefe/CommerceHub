import * as productsApi from '../api/products';
import type { CategoryResponse } from '../api/products';
import { fillProductCategoryId } from './products-panel';
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

function renderCategoryList(categories: CategoryResponse[]): string {
  if (categories.length === 0) {
    return '<p class="hint">Henüz kategori yok.</p>';
  }
  return `
    <ul class="id-list">
      ${categories
        .map(
          (category) => `
        <li>
          <button type="button" class="link-btn" data-category-id="${category.id}">
            <strong>${category.name}</strong>
            <code>${category.id}</code>
          </button>
        </li>
      `,
        )
        .join('')}
    </ul>
    <p class="hint">Kategori satırına tıklayarak ID'yi ürün formlarına kopyalayın.</p>
  `;
}

export function mountCategoriesPanel(container: HTMLElement): void {
  container.innerHTML = `
    ${renderSessionBanner()}
    <div class="flow-banner flow-banner-info">
      Adım 2: Kategori oluştur, listele ve satıra tıklayarak ID'yi ürün formuna kopyala.
    </div>
    ${section(
      'Listele (Public)',
      `
      <button id="list-categories" type="button" class="btn-primary">Kategorileri getir</button>
      <div id="category-list" class="category-list"></div>
    `,
    )}
    ${section(
      'Oluştur (ADMIN)',
      `
      <form id="create-category-form" class="form-grid">
        ${field('İsim', 'create-category-name', 'text', 'Electronics')}
        <button type="submit" class="btn-primary">Oluştur</button>
      </form>
    `,
    )}
    ${section(
      'Güncelle (ADMIN)',
      `
      <form id="update-category-form" class="form-grid">
        ${field('Kategori ID', 'update-category-id')}
        ${field('Yeni isim', 'update-category-name', 'text', 'Gadgets')}
        <button type="submit" class="btn-primary">Güncelle</button>
      </form>
    `,
    )}
    ${section(
      'Sil (ADMIN)',
      `
      <form id="delete-category-form" class="form-grid">
        ${field('Kategori ID', 'delete-category-id')}
        <button type="submit" class="btn-danger">Sil</button>
      </form>
    `,
    )}
  `;

  const listContainer = container.querySelector('#category-list');

  const loadCategories = async () => {
    const result = await productsApi.listCategories();
    if (listContainer && result.ok && Array.isArray(result.data)) {
      listContainer.innerHTML = renderCategoryList(result.data as CategoryResponse[]);
      listContainer.querySelectorAll('[data-category-id]').forEach((button) => {
        button.addEventListener('click', () => {
          const id = button.getAttribute('data-category-id');
          if (id) {
            fillProductCategoryId(id);
            fillCategoryId(id);
          }
        });
      });
    }
  };

  container.querySelector('#list-categories')?.addEventListener('click', () => {
    void loadCategories();
  });

  container.querySelector('#create-category-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const name = (form.querySelector('#create-category-name') as HTMLInputElement).value.trim();
    const result = await productsApi.createCategory(name);
    if (result.ok) {
      await loadCategories();
    }
  });

  container.querySelector('#update-category-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const id = (form.querySelector('#update-category-id') as HTMLInputElement).value.trim();
    const name = (form.querySelector('#update-category-name') as HTMLInputElement).value.trim();
    const result = await productsApi.updateCategory(id, name);
    if (result.ok) {
      await loadCategories();
    }
  });

  container.querySelector('#delete-category-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const id = (form.querySelector('#delete-category-id') as HTMLInputElement).value.trim();
    const result = await productsApi.deleteCategory(id);
    if (result.ok) {
      await loadCategories();
    }
  });
}

function fillCategoryId(categoryId: string): void {
  for (const id of ['update-category-id', 'delete-category-id']) {
    const input = document.querySelector<HTMLInputElement>(`#${id}`);
    if (input) {
      input.value = categoryId;
    }
  }
}
