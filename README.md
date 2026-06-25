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

## Front-End Dev Console

Manual API testing UI for auth-service and product-service. Runs on `http://localhost:5173` (already allowed in backend CORS).

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

The dev server proxies `/api/v1/auth` → auth-service (`8081`) and `/api/v1/products` → product-service (`8082`), so login tokens from auth are sent automatically to product endpoints on the same origin.

### Environment

| Variable | Default |
|----------|---------|
| `VITE_AUTH_API_URL` | empty (Vite proxy) |
| `VITE_PRODUCT_API_URL` | empty (Vite proxy) |

### Manual test flow

1. **Step 1 — Auth:** Click **Kayıt ol ve giriş yap** (register + auto-login) or **Sadece giriş yap**.
2. You are redirected to **Categories** automatically after login.
3. **ADMIN role (for create/update/delete):** Run the SQL above in `auth-db`, then login again.
4. **Step 2 — Categories:** Create a category (ADMIN), click a row to copy its ID.
5. **Step 3 — Products:** Create a product with that category ID, list and fetch by ID.
6. **Auth:** Logout to confirm protected endpoints return 401.
