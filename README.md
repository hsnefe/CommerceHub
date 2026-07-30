# CommerceHub

Distributed e-commerce backend platform built with Java microservices.

## Auth Service

Authentication and authorization service for CommerceHub. Handles user registration, login, JWT access tokens, refresh token rotation, and logout.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login and receive tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Revoke refresh token (requires JWT) |
| GET | `/api/v1/auth/me` | Get current user (requires JWT) |

## Product Service

Product catalog service for CommerceHub. Manages products, categories, pagination, filtering, and internal product snapshots for order processing.

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/v1/products` | Public | List products (`?page=0&size=20&categoryId=&name=`) |
| GET | `/api/v1/products/{id}` | Public | Get product details |
| POST | `/api/v1/products` | ADMIN | Create product |
| PUT | `/api/v1/products/{id}` | ADMIN | Update product |
| DELETE | `/api/v1/products/{id}` | ADMIN | Soft-delete product |
| GET | `/api/v1/products/categories` | Public | List categories |
| POST | `/api/v1/products/categories` | ADMIN | Create category |
| PUT | `/api/v1/products/categories/{id}` | ADMIN | Update category |
| DELETE | `/api/v1/products/categories/{id}` | ADMIN | Delete category |
| GET | `/internal/products/{id}` | Internal (V1 open) | Product snapshot for Order Service |

### Run with Docker Compose

```bash
docker compose up --build
```

- Auth Service: http://localhost:8081
- Product Service: http://localhost:8082
- Auth Swagger UI: http://localhost:8081/swagger-ui.html
- Product Swagger UI: http://localhost:8082/swagger-ui.html
- Auth PostgreSQL: `localhost:5433` (user: `auth_user`, db: `auth_db`)
- Product PostgreSQL: `localhost:5434` (user: `product_user`, db: `product_db`)

### Run locally (requires Java 21+ and Maven)

Start PostgreSQL first (or use `docker compose up auth-db product-db`), then:

```bash
cd services
mvn -pl auth-service spring-boot:run
# or
mvn -pl product-service spring-boot:run
```

Environment variables:

| Variable | Auth default | Product default |
|----------|--------------|-----------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/auth_db` | `jdbc:postgresql://localhost:5434/product_db` |
| `SPRING_DATASOURCE_USERNAME` | `auth_user` | `product_user` |
| `SPRING_DATASOURCE_PASSWORD` | `auth_pass` | `product_pass` |
| `JWT_SECRET` | (dev default in application.yml) | Same secret as auth-service |
| `AUTH_EMAIL_VERIFICATION_REQUIRED` | `false` | — |

### Run tests

```bash
cd services
mvn test
```

Integration tests use Testcontainers with PostgreSQL.

### Assign ADMIN role manually

```sql
-- After registering a user, assign ADMIN role via psql:
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@example.com' AND r.name = 'ADMIN';
```

Use the JWT from auth-service login to call ADMIN-only product endpoints.

## Inventory Service

Inventory management service for CommerceHub. Tracks per-product stock levels, low-stock thresholds, and exposes internal increment/decrement endpoints for order processing.

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/v1/inventory` | Public | List inventory records (`?page=0&size=20`) |
| GET | `/api/v1/inventory/{productId}` | Public | Get stock for a product |
| POST | `/api/v1/inventory` | ADMIN | Create inventory record (validates product via product-service) |
| PATCH | `/api/v1/inventory/{productId}` | ADMIN | Update quantity and/or low-stock threshold |
| POST | `/internal/inventory/{productId}/decrement` | Internal (V1 open) | Decrement stock for orders |
| POST | `/internal/inventory/{productId}/increment` | Internal (V1 open) | Increment stock for cancellations/returns |

### Seed data

Flyway seed migration includes sample product IDs (for local testing without product-service records):

| Product ID | Quantity | Low-stock threshold |
|------------|----------|---------------------|
| `a1000000-0000-4000-8000-000000000001` | 100 | 10 |
| `a1000000-0000-4000-8000-000000000002` | 3 | 5 |
| `a1000000-0000-4000-8000-000000000003` | 0 | 5 |

### Run with Docker Compose

```bash
docker compose up --build
```

- Auth Service: http://localhost:8081
- Product Service: http://localhost:8082
- Inventory Service: http://localhost:8083
- Auth Swagger UI: http://localhost:8081/swagger-ui.html
- Product Swagger UI: http://localhost:8082/swagger-ui.html
- Inventory Swagger UI: http://localhost:8083/swagger-ui.html
- Auth PostgreSQL: `localhost:5433` (user: `auth_user`, db: `auth_db`)
- Product PostgreSQL: `localhost:5434` (user: `product_user`, db: `product_db`)
- Inventory PostgreSQL: `localhost:5435` (user: `inventory_user`, db: `inventory_db`)

### Run locally (requires Java 21+ and Maven)

Start PostgreSQL first (or use `docker compose up auth-db product-db inventory-db`), then:

```bash
cd services
mvn -pl auth-service spring-boot:run
# or
mvn -pl product-service spring-boot:run
# or
mvn -pl inventory-service spring-boot:run
```

Environment variables:

| Variable | Auth default | Product default | Inventory default |
|----------|--------------|-----------------|-------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/auth_db` | `jdbc:postgresql://localhost:5434/product_db` | `jdbc:postgresql://localhost:5435/inventory_db` |
| `SPRING_DATASOURCE_USERNAME` | `auth_user` | `product_user` | `inventory_user` |
| `SPRING_DATASOURCE_PASSWORD` | `auth_pass` | `product_pass` | `inventory_pass` |
| `JWT_SECRET` | (dev default in application.yml) | Same secret as auth-service | Same secret as auth-service |
| `AUTH_EMAIL_VERIFICATION_REQUIRED` | `false` | — | — |
| `PRODUCT_SERVICE_BASE_URL` | — | — | `http://localhost:8082` |

### Run tests

```bash
cd services
mvn -pl inventory-service test
```

## Order Service

Order orchestration service for CommerceHub. Creates orders, snapshots product prices, decrements inventory, and best-effort notifies the user via notification-service. V1 statuses: `CREATED`, `CANCELLED`.

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/api/v1/orders` | Authenticated | Create order (decrements stock, notifies user) |
| GET | `/api/v1/orders/{orderId}` | Owner or ADMIN | Get order detail |
| GET | `/api/v1/orders/user/{userId}` | Owner or ADMIN | List orders for a user |
| DELETE | `/api/v1/orders/{orderId}` | Owner or ADMIN | Cancel order (increments stock back) |

### Run with Docker Compose

```bash
docker compose up --build
```

- Auth Service: http://localhost:8081
- Product Service: http://localhost:8082
- Inventory Service: http://localhost:8083
- Order Service: http://localhost:8084
- Notification Service: http://localhost:8085
- Auth Swagger UI: http://localhost:8081/swagger-ui.html
- Product Swagger UI: http://localhost:8082/swagger-ui.html
- Inventory Swagger UI: http://localhost:8083/swagger-ui.html
- Order Swagger UI: http://localhost:8084/swagger-ui.html
- Notification Swagger UI: http://localhost:8085/swagger-ui.html
- Auth PostgreSQL: `localhost:5433` (user: `auth_user`, db: `auth_db`)
- Product PostgreSQL: `localhost:5434` (user: `product_user`, db: `product_db`)
- Inventory PostgreSQL: `localhost:5435` (user: `inventory_user`, db: `inventory_db`)
- Order PostgreSQL: `localhost:5436` (user: `order_user`, db: `order_db`)

### Run locally (requires Java 21+ and Maven)

Start PostgreSQL first (or use `docker compose up auth-db product-db inventory-db order-db`), then:

```bash
cd services
mvn -pl auth-service spring-boot:run
# or
mvn -pl product-service spring-boot:run
# or
mvn -pl inventory-service spring-boot:run
# or
mvn -pl order-service spring-boot:run
# or
mvn -pl notification-service spring-boot:run
```

Environment variables:

| Variable | Auth | Product | Inventory | Order | Notification |
|----------|------|---------|-----------|-------|--------------|
| `SPRING_DATASOURCE_URL` | `...5433/auth_db` | `...5434/product_db` | `...5435/inventory_db` | `...5436/order_db` | — (no DB) |
| `SPRING_DATASOURCE_USERNAME` | `auth_user` | `product_user` | `inventory_user` | `order_user` | — |
| `SPRING_DATASOURCE_PASSWORD` | `auth_pass` | `product_pass` | `inventory_pass` | `order_pass` | — |
| `JWT_SECRET` | (dev default) | Same as auth | Same as auth | Same as auth | Same as auth |
| `PRODUCT_SERVICE_BASE_URL` | — | — | `http://localhost:8082` | `http://localhost:8082` | — |
| `INVENTORY_SERVICE_BASE_URL` | — | — | — | `http://localhost:8083` | — |
| `AUTH_SERVICE_BASE_URL` | — | — | — | `http://localhost:8081` | — |
| `NOTIFICATION_SERVICE_BASE_URL` | — | — | — | `http://localhost:8085` | — |

### Run tests

```bash
cd services
mvn -pl order-service test
```

## Notification Service

Lightweight notification service for CommerceHub. V1 simulates email delivery by logging the message (no database, no real SMTP).

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/api/v1/notifications` | Public (V1) | Send notification (email simulation) |

Request body: `{ "email", "subject", "message" }` → `{ "success": true }`.

Order-service calls this after creating an order (best-effort; order still succeeds if notification fails).

### Run tests

```bash
cd services
mvn -pl notification-service test
```

## Front-End Dev Console

Manual API testing UI for all V1 services. Runs on `http://localhost:5173` (already allowed in backend CORS).

### Setup

```bash
# Terminal 1 — backend
docker compose up --build

# Terminal 2 — dev console
cd frontend
cp .env.example .env   # optional; empty URLs use Vite proxy
npm install
npm run dev
```

Open http://localhost:5173

The dev server proxies `/api/v1/auth` → auth-service (`8081`), `/api/v1/products` → product-service (`8082`), `/api/v1/inventory` + `/internal/inventory` → inventory-service (`8083`), `/api/v1/orders` → order-service (`8084`), and `/api/v1/notifications` → notification-service (`8085`), so login tokens from auth are sent automatically to protected endpoints on the same origin.

### Environment

| Variable | Default |
|----------|---------|
| `VITE_AUTH_API_URL` | empty (Vite proxy) |
| `VITE_PRODUCT_API_URL` | empty (Vite proxy) |
| `VITE_INVENTORY_API_URL` | empty (Vite proxy) |
| `VITE_ORDER_API_URL` | empty (Vite proxy) |
| `VITE_NOTIFICATION_API_URL` | empty (Vite proxy) |

### Manual test flow

1. **Step 1 — Auth:** Click **Kayıt ol ve giriş yap** (register + auto-login) or **Sadece giriş yap**.
2. You are redirected to **Categories** automatically after login.
3. **ADMIN role (for create/update/delete):** Run the SQL above in `auth-db`, then login again.
4. **Step 2 — Categories:** Create a category (ADMIN), click a row to copy its ID.
5. **Step 3 — Products:** Create a product with that category ID, list and fetch by ID.
6. **Step 4 — Inventory:** Create stock for the product ID (ADMIN), list, patch, or try internal increment/decrement.
7. **Step 5 — Orders:** Create an order with that product ID, list by user ID, fetch detail, or cancel (stock restored).
8. **Step 6 — Notifications:** Send a manual email simulation, or confirm order-create triggers a log line in notification-service.
9. **Auth:** Logout to confirm protected endpoints return 401.
