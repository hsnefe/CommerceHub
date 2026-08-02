import {
  cancelOrder,
  createOrder,
  getOrder,
  listCategories,
  listOrdersByUser,
  listProducts,
} from "./api.js";
import { logout, requireAuth } from "./session.js";

const PAGE_SIZE = 20;

const role = document.body.dataset.role;
const session = requireAuth(role);

if (session) {
  const emailEl = document.getElementById("session-email");
  if (emailEl) {
    emailEl.textContent = session.email;
  }
}

document.getElementById("logout-btn")?.addEventListener("click", () => {
  logout();
});

if (role !== "user" || !session) {
  // Admin uses admin-dashboard.js
} else {
  mountUserDashboard(session);
}

/**
 * @param {{ email: string, userId: string, accessToken: string }} session
 */
function mountUserDashboard(session) {
  /** @type {string | null} */
  let selectedCategoryId = null;
  let page = 0;
  let totalElements = 0;

  /** @type {{ id: string, name: string, price: number, currency: string } | null} */
  let modalProduct = null;

  const viewCatalog = document.getElementById("view-catalog");
  const viewOrders = document.getElementById("view-orders");
  const categoryList = document.getElementById("category-list");
  const productGrid = document.getElementById("product-grid");
  const productStatus = document.getElementById("product-status");
  const pagePrev = document.getElementById("page-prev");
  const pageNext = document.getElementById("page-next");
  const pageLabel = document.getElementById("page-label");
  const ordersList = document.getElementById("orders-list");
  const orderDetail = document.getElementById("order-detail");
  const orderModal = document.getElementById("order-modal");
  const orderQty = /** @type {HTMLInputElement} */ (document.getElementById("order-qty"));
  const orderModalError = document.getElementById("order-modal-error");

  function showCatalog() {
    viewCatalog.hidden = false;
    viewOrders.hidden = true;
  }

  function showOrders() {
    viewCatalog.hidden = true;
    viewOrders.hidden = false;
    void loadOrders();
  }

  document.getElementById("orders-btn")?.addEventListener("click", () => {
    showOrders();
  });

  document.getElementById("back-to-shop")?.addEventListener("click", () => {
    showCatalog();
  });

  function setProductStatus(message, isError = false) {
    if (!productStatus) return;
    if (!message) {
      productStatus.hidden = true;
      productStatus.textContent = "";
      return;
    }
    productStatus.hidden = false;
    productStatus.textContent = message;
    productStatus.classList.toggle("status-error", isError);
  }

  async function loadCategories() {
    if (!categoryList) return;
    const result = await listCategories();
    if (!result.ok || !Array.isArray(result.data)) {
      categoryList.innerHTML = `<p class="hint status-error">Could not load categories.</p>`;
      return;
    }

    const categories = result.data;
    categoryList.innerHTML = `
      <button type="button" class="category-item active" data-category-id="">All</button>
      ${categories
        .map(
          (c) => `
        <button type="button" class="category-item" data-category-id="${c.id}">
          ${escapeHtml(c.name)}
        </button>
      `,
        )
        .join("")}
    `;

    categoryList.querySelectorAll(".category-item").forEach((btn) => {
      btn.addEventListener("click", () => {
        categoryList.querySelectorAll(".category-item").forEach((el) => {
          el.classList.remove("active");
        });
        btn.classList.add("active");
        const id = btn.getAttribute("data-category-id") || "";
        selectedCategoryId = id || null;
        page = 0;
        void loadProducts();
      });
    });
  }

  async function loadProducts() {
    if (!productGrid) return;
    setProductStatus("Loading products…");
    productGrid.innerHTML = "";

    const result = await listProducts({
      page,
      size: PAGE_SIZE,
      categoryId: selectedCategoryId ?? undefined,
    });

    if (!result.ok || !result.data?.content) {
      setProductStatus("Could not load products.", true);
      updatePagination(0);
      return;
    }

    const { content, totalElements: total } = result.data;
    totalElements = Number(total) || 0;
    setProductStatus("");

    if (content.length === 0) {
      productGrid.innerHTML = `<p class="hint empty-state">No products in this category.</p>`;
      updatePagination(totalElements);
      return;
    }

    productGrid.innerHTML = content
      .map(
        (p) => `
      <button type="button" class="product-card" data-product-id="${p.id}"
        data-name="${escapeAttr(p.name)}"
        data-price="${p.price}"
        data-currency="${escapeAttr(p.currency ?? "")}">
        <span class="product-card-name">${escapeHtml(p.name)}</span>
        <span class="product-card-price">${formatPrice(p.price, p.currency)}</span>
      </button>
    `,
      )
      .join("");

    productGrid.querySelectorAll(".product-card").forEach((card) => {
      card.addEventListener("click", () => {
        openOrderModal({
          id: card.getAttribute("data-product-id") || "",
          name: card.getAttribute("data-name") || "",
          price: Number(card.getAttribute("data-price")),
          currency: card.getAttribute("data-currency") || "",
        });
      });
    });

    updatePagination(totalElements);
  }

  function updatePagination(total) {
    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    if (pageLabel) pageLabel.textContent = `Page ${page + 1} of ${totalPages}`;
    if (pagePrev) pagePrev.disabled = page <= 0;
    if (pageNext) pageNext.disabled = page + 1 >= totalPages || total === 0;
  }

  pagePrev?.addEventListener("click", () => {
    if (page <= 0) return;
    page -= 1;
    void loadProducts();
  });

  pageNext?.addEventListener("click", () => {
    const totalPages = Math.ceil(totalElements / PAGE_SIZE);
    if (page + 1 >= totalPages) return;
    page += 1;
    void loadProducts();
  });

  function openOrderModal(product) {
    modalProduct = product;
    const nameEl = document.getElementById("order-modal-product");
    const priceEl = document.getElementById("order-modal-price");
    if (nameEl) nameEl.textContent = product.name;
    if (priceEl) priceEl.textContent = formatPrice(product.price, product.currency);
    if (orderQty) orderQty.value = "1";
    if (orderModalError) {
      orderModalError.hidden = true;
      orderModalError.textContent = "";
    }
    if (orderModal) orderModal.hidden = false;
  }

  function closeOrderModal() {
    modalProduct = null;
    if (orderModal) orderModal.hidden = true;
  }

  orderModal?.querySelectorAll("[data-close-modal]").forEach((el) => {
    el.addEventListener("click", () => closeOrderModal());
  });

  document.getElementById("qty-minus")?.addEventListener("click", () => {
    if (!orderQty) return;
    const next = Math.max(1, Number(orderQty.value) - 1);
    orderQty.value = String(next);
  });

  document.getElementById("qty-plus")?.addEventListener("click", () => {
    if (!orderQty) return;
    const next = Math.min(99, Number(orderQty.value) + 1);
    orderQty.value = String(next);
  });

  document.getElementById("order-confirm")?.addEventListener("click", async () => {
    if (!modalProduct) return;
    const quantity = Math.min(99, Math.max(1, Number(orderQty?.value) || 1));
    const confirmBtn = document.getElementById("order-confirm");
    if (confirmBtn) confirmBtn.disabled = true;
    if (orderModalError) {
      orderModalError.hidden = true;
      orderModalError.textContent = "";
    }

    try {
      const result = await createOrder({
        items: [{ productId: modalProduct.id, quantity }],
      });
      if (!result.ok) {
        const msg =
          typeof result.data === "object" && result.data?.message
            ? result.data.message
            : "Could not create order.";
        if (orderModalError) {
          orderModalError.textContent = msg;
          orderModalError.hidden = false;
        }
        return;
      }
      closeOrderModal();
      showOrders();
    } catch {
      if (orderModalError) {
        orderModalError.textContent = "Could not reach the order service.";
        orderModalError.hidden = false;
      }
    } finally {
      if (confirmBtn) confirmBtn.disabled = false;
    }
  });

  async function loadOrders() {
    if (!ordersList || !orderDetail) return;
    ordersList.innerHTML = `<p class="hint">Loading orders…</p>`;
    orderDetail.hidden = true;
    orderDetail.innerHTML = "";

    if (!session.userId) {
      ordersList.innerHTML = `<p class="hint status-error">Missing user id. Please log in again.</p>`;
      return;
    }

    const result = await listOrdersByUser(session.userId);
    if (!result.ok || !Array.isArray(result.data)) {
      ordersList.innerHTML = `<p class="hint status-error">Could not load orders.</p>`;
      return;
    }

    const orders = result.data;
    if (orders.length === 0) {
      ordersList.innerHTML = `<p class="hint empty-state">No orders yet. Place one from the catalog.</p>`;
      return;
    }

    ordersList.innerHTML = orders
      .map(
        (o) => `
      <button type="button" class="order-row" data-order-id="${o.orderId}">
        <span class="order-status">${escapeHtml(o.status)}</span>
        <span class="order-total">${formatPrice(o.totalPrice, "TRY")}</span>
        <code class="order-id">${escapeHtml(o.orderId)}</code>
      </button>
    `,
      )
      .join("");

    ordersList.querySelectorAll(".order-row").forEach((row) => {
      row.addEventListener("click", () => {
        ordersList.querySelectorAll(".order-row").forEach((el) => el.classList.remove("active"));
        row.classList.add("active");
        const id = row.getAttribute("data-order-id");
        if (id) void loadOrderDetail(id);
      });
    });
  }

  async function loadOrderDetail(orderId) {
    if (!orderDetail) return;
    orderDetail.hidden = false;
    orderDetail.innerHTML = `<p class="hint">Loading detail…</p>`;

    const result = await getOrder(orderId);
    if (!result.ok || !result.data) {
      orderDetail.innerHTML = `<p class="hint status-error">Could not load order detail.</p>`;
      return;
    }

    const order = result.data;
    const items = Array.isArray(order.items) ? order.items : [];
    const canCancel = String(order.status).toUpperCase() === "CREATED";

    orderDetail.innerHTML = `
      <div class="order-detail-header">
        <div>
          <p class="order-detail-status">${escapeHtml(order.status)}</p>
          <p class="order-detail-total">${formatPrice(order.totalPrice, "TRY")}</p>
          <code class="order-id">${escapeHtml(order.orderId)}</code>
        </div>
        ${
          canCancel
            ? `<button id="cancel-order-btn" type="button" class="btn btn-ghost">Cancel order</button>`
            : ""
        }
      </div>
      <ul class="order-items">
        ${items
          .map(
            (item) => `
          <li>
            <span>${escapeHtml(item.productName)}</span>
            <span>× ${item.quantity}</span>
            <span>${formatPrice(item.unitPrice, "TRY")}</span>
          </li>
        `,
          )
          .join("")}
      </ul>
      <p id="cancel-order-error" class="form-error" hidden></p>
    `;

    document.getElementById("cancel-order-btn")?.addEventListener("click", async () => {
      const errEl = document.getElementById("cancel-order-error");
      const btn = document.getElementById("cancel-order-btn");
      if (btn) btn.disabled = true;
      const cancelResult = await cancelOrder(orderId);
      if (!cancelResult.ok) {
        const msg =
          typeof cancelResult.data === "object" && cancelResult.data?.message
            ? cancelResult.data.message
            : "Could not cancel order.";
        if (errEl) {
          errEl.textContent = msg;
          errEl.hidden = false;
        }
        if (btn) btn.disabled = false;
        return;
      }
      await loadOrders();
      await loadOrderDetail(orderId);
    });
  }

  void loadCategories().then(() => loadProducts());
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
