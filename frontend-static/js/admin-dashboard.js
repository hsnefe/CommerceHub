import {
  createCategory,
  createInventory,
  createProduct,
  deleteCategory,
  deleteProduct,
  getProduct,
  listCategories,
  listInventory,
  listProducts,
  updateCategory,
  updateInventory,
  updateProduct,
} from "./api.js";
import { logout, requireAuth } from "./session.js";

const PAGE_SIZE = 20;

const session = requireAuth("admin");
if (session) {
  const emailEl = document.getElementById("session-email");
  if (emailEl) emailEl.textContent = session.email;
}

document.getElementById("logout-btn")?.addEventListener("click", () => logout());

if (session) {
  mountAdminDashboard();
}

function mountAdminDashboard() {
  /** @type {{ id: string, name: string }[]} */
  let categoriesCache = [];
  let productsPage = 0;
  let productsTotal = 0;
  let inventoryPage = 0;
  let inventoryTotal = 0;

  const panels = {
    categories: document.getElementById("panel-categories"),
    products: document.getElementById("panel-products"),
    inventory: document.getElementById("panel-inventory"),
  };

  document.querySelectorAll("[data-panel]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const panel = btn.getAttribute("data-panel");
      document.querySelectorAll("[data-panel]").forEach((el) => el.classList.remove("active"));
      btn.classList.add("active");
      Object.entries(panels).forEach(([key, el]) => {
        if (el) el.hidden = key !== panel;
      });
      if (panel === "categories") void loadCategories();
      if (panel === "products") {
        void refreshCategorySelect();
        void loadProducts();
      }
      if (panel === "inventory") void loadInventory();
    });
  });

  function apiError(result, fallback) {
    if (typeof result?.data === "object" && result.data?.message) {
      return result.data.message;
    }
    return fallback;
  }

  function setStatus(id, message, isError = false) {
    const el = document.getElementById(id);
    if (!el) return;
    if (!message) {
      el.hidden = true;
      el.textContent = "";
      return;
    }
    el.hidden = false;
    el.textContent = message;
    el.classList.toggle("status-error", isError);
  }

  // —— Categories ——
  const categoryForm = document.getElementById("category-form");
  const categoryIdInput = /** @type {HTMLInputElement} */ (document.getElementById("category-id"));
  const categoryNameInput = /** @type {HTMLInputElement} */ (document.getElementById("category-name"));
  const categorySubmit = document.getElementById("category-submit");
  const categoryReset = document.getElementById("category-reset");

  function resetCategoryForm() {
    categoryIdInput.value = "";
    categoryNameInput.value = "";
    if (categorySubmit) categorySubmit.textContent = "Create category";
    if (categoryReset) categoryReset.hidden = true;
  }

  categoryReset?.addEventListener("click", () => resetCategoryForm());

  categoryForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = categoryNameInput.value.trim();
    const id = categoryIdInput.value.trim();
    setStatus("categories-status", id ? "Updating…" : "Creating…");
    const result = id ? await updateCategory(id, name) : await createCategory(name);
    if (!result.ok) {
      setStatus("categories-status", apiError(result, "Category save failed."), true);
      return;
    }
    setStatus("categories-status", id ? "Category updated." : "Category created.");
    resetCategoryForm();
    await loadCategories();
    await refreshCategorySelect();
  });

  async function loadCategories() {
    const wrap = document.getElementById("categories-table");
    if (!wrap) return;
    wrap.innerHTML = `<p class="hint">Loading…</p>`;
    const result = await listCategories();
    if (!result.ok || !Array.isArray(result.data)) {
      wrap.innerHTML = `<p class="hint status-error">Could not load categories.</p>`;
      return;
    }
    categoriesCache = result.data;
    if (categoriesCache.length === 0) {
      wrap.innerHTML = `<p class="hint empty-state">No categories yet.</p>`;
      return;
    }
    wrap.innerHTML = `
      <table class="admin-table">
        <thead>
          <tr><th>Name</th><th>ID</th><th></th></tr>
        </thead>
        <tbody>
          ${categoriesCache
            .map(
              (c) => `
            <tr>
              <td>${escapeHtml(c.name)}</td>
              <td><code>${escapeHtml(c.id)}</code></td>
              <td class="admin-row-actions">
                <button type="button" class="btn btn-ghost btn-sm" data-edit-category="${c.id}">Edit</button>
                <button type="button" class="btn btn-ghost btn-sm" data-delete-category="${c.id}">Delete</button>
              </td>
            </tr>
          `,
            )
            .join("")}
        </tbody>
      </table>
    `;

    wrap.querySelectorAll("[data-edit-category]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-edit-category");
        const cat = categoriesCache.find((c) => c.id === id);
        if (!cat) return;
        categoryIdInput.value = cat.id;
        categoryNameInput.value = cat.name;
        if (categorySubmit) categorySubmit.textContent = "Update category";
        if (categoryReset) categoryReset.hidden = false;
      });
    });

    wrap.querySelectorAll("[data-delete-category]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-delete-category");
        if (!id || !window.confirm("Delete this category?")) return;
        setStatus("categories-status", "Deleting…");
        const result = await deleteCategory(id);
        if (!result.ok) {
          setStatus("categories-status", apiError(result, "Delete failed."), true);
          return;
        }
        setStatus("categories-status", "Category deleted.");
        resetCategoryForm();
        await loadCategories();
        await refreshCategorySelect();
      });
    });
  }

  async function refreshCategorySelect() {
    const select = /** @type {HTMLSelectElement} */ (document.getElementById("product-category-id"));
    if (!select) return;
    const current = select.value;
    const result = await listCategories();
    if (!result.ok || !Array.isArray(result.data)) return;
    categoriesCache = result.data;
    select.innerHTML = `<option value="">Select category</option>${categoriesCache
      .map((c) => `<option value="${escapeAttr(c.id)}">${escapeHtml(c.name)}</option>`)
      .join("")}`;
    if (current && categoriesCache.some((c) => c.id === current)) {
      select.value = current;
    }
  }

  // —— Products ——
  const productForm = document.getElementById("product-form");
  const productIdInput = /** @type {HTMLInputElement} */ (document.getElementById("product-id"));
  const productNameInput = /** @type {HTMLInputElement} */ (document.getElementById("product-name"));
  const productPriceInput = /** @type {HTMLInputElement} */ (document.getElementById("product-price"));
  const productDescInput = /** @type {HTMLInputElement} */ (document.getElementById("product-description"));
  const productCategorySelect = /** @type {HTMLSelectElement} */ (
    document.getElementById("product-category-id")
  );
  const productSubmit = document.getElementById("product-submit");
  const productReset = document.getElementById("product-reset");

  function resetProductForm() {
    productIdInput.value = "";
    productNameInput.value = "";
    productPriceInput.value = "";
    productDescInput.value = "";
    productCategorySelect.value = "";
    if (productSubmit) productSubmit.textContent = "Create product";
    if (productReset) productReset.hidden = true;
  }

  productReset?.addEventListener("click", () => resetProductForm());

  productForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const body = {
      name: productNameInput.value.trim(),
      description: productDescInput.value.trim() || undefined,
      price: Number(productPriceInput.value),
      categoryId: productCategorySelect.value,
    };
    const id = productIdInput.value.trim();
    setStatus("products-status", id ? "Updating…" : "Creating…");
    const result = id ? await updateProduct(id, body) : await createProduct(body);
    if (!result.ok) {
      setStatus("products-status", apiError(result, "Product save failed."), true);
      return;
    }
    const createdId = result.data?.id;
    setStatus(
      "products-status",
      id ? "Product updated." : `Product created${createdId ? `: ${createdId}` : "."}`,
    );
    resetProductForm();
    await loadProducts();
  });

  async function loadProducts() {
    const wrap = document.getElementById("products-table");
    if (!wrap) return;
    wrap.innerHTML = `<p class="hint">Loading…</p>`;
    const result = await listProducts({ page: productsPage, size: PAGE_SIZE });
    if (!result.ok || !result.data?.content) {
      wrap.innerHTML = `<p class="hint status-error">Could not load products.</p>`;
      updateProductsPagination(0);
      return;
    }
    const { content, totalElements } = result.data;
    productsTotal = Number(totalElements) || 0;
    updateProductsPagination(productsTotal);

    if (content.length === 0) {
      wrap.innerHTML = `<p class="hint empty-state">No products yet.</p>`;
      return;
    }

    wrap.innerHTML = `
      <table class="admin-table">
        <thead>
          <tr><th>Name</th><th>Price</th><th>ID</th><th></th></tr>
        </thead>
        <tbody>
          ${content
            .map(
              (p) => `
            <tr>
              <td>${escapeHtml(p.name)}</td>
              <td>${formatPrice(p.price, p.currency)}</td>
              <td><code>${escapeHtml(p.id)}</code></td>
              <td class="admin-row-actions">
                <button type="button" class="btn btn-ghost btn-sm" data-edit-product="${p.id}">Edit</button>
                <button type="button" class="btn btn-ghost btn-sm" data-stock-product="${p.id}">Stock</button>
                <button type="button" class="btn btn-ghost btn-sm" data-delete-product="${p.id}">Delete</button>
              </td>
            </tr>
          `,
            )
            .join("")}
        </tbody>
      </table>
    `;

    wrap.querySelectorAll("[data-edit-product]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-edit-product");
        if (!id) return;
        setStatus("products-status", "Loading product…");
        const result = await getProduct(id);
        if (!result.ok || !result.data) {
          setStatus("products-status", apiError(result, "Could not load product."), true);
          return;
        }
        const p = result.data;
        productIdInput.value = p.id;
        productNameInput.value = p.name ?? "";
        productPriceInput.value = String(p.price ?? "");
        productDescInput.value = p.description ?? "";
        productCategorySelect.value = p.categoryId ?? "";
        if (productSubmit) productSubmit.textContent = "Update product";
        if (productReset) productReset.hidden = false;
        setStatus("products-status", "");
      });
    });

    wrap.querySelectorAll("[data-stock-product]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-stock-product");
        if (!id) return;
        document.querySelector('[data-panel="inventory"]')?.click();
        const invProduct = /** @type {HTMLInputElement} */ (
          document.getElementById("inventory-product-id")
        );
        if (invProduct) invProduct.value = id;
        resetInventoryFormKeepProduct();
      });
    });

    wrap.querySelectorAll("[data-delete-product]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-delete-product");
        if (!id || !window.confirm("Soft-delete this product?")) return;
        setStatus("products-status", "Deleting…");
        const result = await deleteProduct(id);
        if (!result.ok) {
          setStatus("products-status", apiError(result, "Delete failed."), true);
          return;
        }
        setStatus("products-status", "Product deleted.");
        resetProductForm();
        await loadProducts();
      });
    });
  }

  function updateProductsPagination(total) {
    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    const label = document.getElementById("products-page-label");
    const prev = /** @type {HTMLButtonElement} */ (document.getElementById("products-prev"));
    const next = /** @type {HTMLButtonElement} */ (document.getElementById("products-next"));
    if (label) label.textContent = `Page ${productsPage + 1} of ${totalPages}`;
    if (prev) prev.disabled = productsPage <= 0;
    if (next) next.disabled = productsPage + 1 >= totalPages || total === 0;
  }

  document.getElementById("products-prev")?.addEventListener("click", () => {
    if (productsPage <= 0) return;
    productsPage -= 1;
    void loadProducts();
  });
  document.getElementById("products-next")?.addEventListener("click", () => {
    if (productsPage + 1 >= Math.ceil(productsTotal / PAGE_SIZE)) return;
    productsPage += 1;
    void loadProducts();
  });

  // —— Inventory ——
  const inventoryForm = document.getElementById("inventory-form");
  const inventoryEditMode = /** @type {HTMLInputElement} */ (
    document.getElementById("inventory-edit-mode")
  );
  const inventoryProductId = /** @type {HTMLInputElement} */ (
    document.getElementById("inventory-product-id")
  );
  const inventoryQty = /** @type {HTMLInputElement} */ (document.getElementById("inventory-qty"));
  const inventoryThreshold = /** @type {HTMLInputElement} */ (
    document.getElementById("inventory-threshold")
  );
  const inventorySubmit = document.getElementById("inventory-submit");
  const inventoryReset = document.getElementById("inventory-reset");

  function resetInventoryForm() {
    inventoryEditMode.value = "";
    inventoryProductId.value = "";
    inventoryProductId.readOnly = false;
    inventoryQty.value = "100";
    inventoryThreshold.value = "10";
    if (inventorySubmit) inventorySubmit.textContent = "Create stock";
    if (inventoryReset) inventoryReset.hidden = true;
  }

  function resetInventoryFormKeepProduct() {
    inventoryEditMode.value = "";
    inventoryProductId.readOnly = false;
    inventoryQty.value = "100";
    inventoryThreshold.value = "10";
    if (inventorySubmit) inventorySubmit.textContent = "Create stock";
    if (inventoryReset) inventoryReset.hidden = true;
  }

  inventoryReset?.addEventListener("click", () => resetInventoryForm());

  inventoryForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const productId = inventoryProductId.value.trim();
    const availableQuantity = Number(inventoryQty.value);
    const lowStockThreshold = Number(inventoryThreshold.value);
    const editing = inventoryEditMode.value === "1";

    setStatus("inventory-status", editing ? "Updating…" : "Creating…");
    const result = editing
      ? await updateInventory(productId, { availableQuantity, lowStockThreshold })
      : await createInventory({ productId, availableQuantity, lowStockThreshold });

    if (!result.ok) {
      setStatus("inventory-status", apiError(result, "Inventory save failed."), true);
      return;
    }
    setStatus("inventory-status", editing ? "Stock updated." : "Stock created.");
    resetInventoryForm();
    await loadInventory();
  });

  async function loadInventory() {
    const wrap = document.getElementById("inventory-table");
    if (!wrap) return;
    wrap.innerHTML = `<p class="hint">Loading…</p>`;
    const result = await listInventory({ page: inventoryPage, size: PAGE_SIZE });
    if (!result.ok || !result.data?.content) {
      wrap.innerHTML = `<p class="hint status-error">Could not load inventory.</p>`;
      updateInventoryPagination(0);
      return;
    }
    const { content, totalElements } = result.data;
    inventoryTotal = Number(totalElements) || 0;
    updateInventoryPagination(inventoryTotal);

    if (content.length === 0) {
      wrap.innerHTML = `<p class="hint empty-state">No inventory records yet.</p>`;
      return;
    }

    wrap.innerHTML = `
      <table class="admin-table">
        <thead>
          <tr><th>Product ID</th><th>Qty</th><th>Threshold</th><th>Low?</th><th></th></tr>
        </thead>
        <tbody>
          ${content
            .map(
              (row) => `
            <tr>
              <td><code>${escapeHtml(row.productId)}</code></td>
              <td>${row.availableQuantity}</td>
              <td>${row.lowStockThreshold}</td>
              <td>${row.lowStock ? "Yes" : "No"}</td>
              <td class="admin-row-actions">
                <button type="button" class="btn btn-ghost btn-sm"
                  data-edit-inventory="${escapeAttr(row.productId)}"
                  data-qty="${row.availableQuantity}"
                  data-threshold="${row.lowStockThreshold}">Edit</button>
              </td>
            </tr>
          `,
            )
            .join("")}
        </tbody>
      </table>
    `;

    wrap.querySelectorAll("[data-edit-inventory]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-edit-inventory");
        if (!id) return;
        inventoryEditMode.value = "1";
        inventoryProductId.value = id;
        inventoryProductId.readOnly = true;
        inventoryQty.value = btn.getAttribute("data-qty") || "0";
        inventoryThreshold.value = btn.getAttribute("data-threshold") || "0";
        if (inventorySubmit) inventorySubmit.textContent = "Update stock";
        if (inventoryReset) inventoryReset.hidden = false;
      });
    });
  }

  function updateInventoryPagination(total) {
    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    const label = document.getElementById("inventory-page-label");
    const prev = /** @type {HTMLButtonElement} */ (document.getElementById("inventory-prev"));
    const next = /** @type {HTMLButtonElement} */ (document.getElementById("inventory-next"));
    if (label) label.textContent = `Page ${inventoryPage + 1} of ${totalPages}`;
    if (prev) prev.disabled = inventoryPage <= 0;
    if (next) next.disabled = inventoryPage + 1 >= totalPages || total === 0;
  }

  document.getElementById("inventory-prev")?.addEventListener("click", () => {
    if (inventoryPage <= 0) return;
    inventoryPage -= 1;
    void loadInventory();
  });
  document.getElementById("inventory-next")?.addEventListener("click", () => {
    if (inventoryPage + 1 >= Math.ceil(inventoryTotal / PAGE_SIZE)) return;
    inventoryPage += 1;
    void loadInventory();
  });

  void loadCategories();
}

function formatPrice(price, currency) {
  const value = Number(price);
  const formatted = Number.isFinite(value) ? value.toFixed(2) : String(price ?? "");
  return currency ? `${formatted} ${currency}` : formatted;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function escapeAttr(value) {
  return escapeHtml(value).replaceAll("'", "&#39;");
}
