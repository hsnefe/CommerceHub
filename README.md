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

Inventory management service for CommerceHub. Tracks per-product available and reserved stock, low-stock thresholds, and admin increment/decrement endpoints. Order stock changes happen via RabbitMQ reserve/release (not sync REST).

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/v1/inventory` | Public | List inventory records (`?page=0&size=20`) |
| GET | `/api/v1/inventory/{productId}` | Public | Get stock for a product |
| POST | `/api/v1/inventory` | ADMIN | Create inventory record (validates product via product-service) |
| PATCH | `/api/v1/inventory/{productId}` | ADMIN | Update quantity and/or low-stock threshold |
| POST | `/internal/inventory/{productId}/decrement` | Internal | Manual decrement (admin/tools) |
| POST | `/internal/inventory/{productId}/increment` | Internal | Manual increment (admin/tools) |

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

Order orchestration service for CommerceHub. Creates orders, snapshots product prices, and publishes domain events. Stock reservation and notifications are asynchronous via RabbitMQ.

Statuses (State Pattern): `CREATED` → `STOCK_RESERVED` → `PAID` → `PREPARING` → `SHIPPED` → `DELIVERED`, plus `CANCELLED` from `CREATED`/`STOCK_RESERVED`. After stock reservation, payment-service drives `PAID` or compensation cancel (Saga).

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/api/v1/orders` | Authenticated | Create order (`CREATED`, publishes `OrderCreatedEvent`) |
| GET | `/api/v1/orders/{orderId}` | Owner or ADMIN | Get order detail |
| GET | `/api/v1/orders/user/{userId}` | Owner or ADMIN | List orders for a user |
| DELETE | `/api/v1/orders/{orderId}` | Owner or ADMIN | Cancel order (publishes `OrderCancelledEvent`) |
| PATCH | `/api/v1/orders/{orderId}/status` | ADMIN | Transition status (`{ "status": "PAID" }`, etc.) |

### Run with Docker Compose

```bash
docker compose up --build
```

- Auth Service: http://localhost:8081
- Product Service: http://localhost:8082
- Inventory Service: http://localhost:8083
- Order Service: http://localhost:8084
- Notification Service: http://localhost:8085
- Analytics Service: http://localhost:8086
- Payment Service: http://localhost:8087
- API Gateway: http://localhost:8080
- Eureka: http://localhost:8761
- Auth Swagger UI: http://localhost:8081/swagger-ui.html
- Product Swagger UI: http://localhost:8082/swagger-ui.html
- Inventory Swagger UI: http://localhost:8083/swagger-ui.html
- Order Swagger UI: http://localhost:8084/swagger-ui.html
- Notification Swagger UI: http://localhost:8085/swagger-ui.html
- Analytics Swagger UI: http://localhost:8086/swagger-ui.html
- Auth PostgreSQL: `localhost:5433` (user: `auth_user`, db: `auth_db`)
- Product PostgreSQL: `localhost:5434` (user: `product_user`, db: `product_db`)
- Inventory PostgreSQL: `localhost:5435` (user: `inventory_user`, db: `inventory_db`)
- Order PostgreSQL: `localhost:5436` (user: `order_user`, db: `order_db`)
- Analytics PostgreSQL: `localhost:5437` (user: `analytics_user`, db: `analytics_db`)

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
# or
mvn -pl analytics-service spring-boot:run
```

Environment variables:

| Variable | Auth | Product | Inventory | Order | Notification | Analytics |
|----------|------|---------|-----------|-------|--------------|-----------|
| `SPRING_DATASOURCE_URL` | `...5433/auth_db` | `...5434/product_db` | `...5435/inventory_db` | `...5436/order_db` | — | `...5437/analytics_db` |
| `SPRING_DATASOURCE_USERNAME` | `auth_user` | `product_user` | `inventory_user` | `order_user` | — | `analytics_user` |
| `SPRING_DATASOURCE_PASSWORD` | `auth_pass` | `product_pass` | `inventory_pass` | `order_pass` | — | `analytics_pass` |
| `JWT_SECRET` | (dev default) | Same as auth | Same as auth | Same as auth | Same as auth | Same as auth |
| `PRODUCT_SERVICE_BASE_URL` | — | — | `http://localhost:8082` | `http://localhost:8082` | — | — |
| `AUTH_SERVICE_BASE_URL` | — | — | — | `http://localhost:8081` | — | — |
| `SPRING_RABBITMQ_*` | — | — | yes | yes | yes | yes |

### Run tests

```bash
cd services
mvn -pl order-service test
```

## Notification Service

Lightweight notification service for CommerceHub. Simulates email delivery by logging the message (no database, no real SMTP). Consumes order and stock domain events from RabbitMQ; HTTP endpoint remains for manual testing.

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/api/v1/notifications` | Public | Send notification (email simulation) |

Request body: `{ "email", "subject", "message" }` → `{ "success": true }`.

### Run tests

```bash
cd services
mvn -pl notification-service test
```

## Analytics Service

Collects domain events from RabbitMQ for logging and simple business metrics. Own PostgreSQL database (`analytics_db`).

### Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/v1/analytics/events` | Authenticated | List recorded events (`?page=0&size=20`) |
| GET | `/api/v1/analytics/stats` | ADMIN | Counts: ordersCreated, ordersCancelled, stockReserved, stockReleased |

### Run tests

```bash
cd services
mvn -pl analytics-service test
```

## V2 Messaging (RabbitMQ + Domain Events)

Event-driven order/inventory/notification/analytics flow on top of the RabbitMQ broker.

### Flow

1. Order create → `OrderCreatedEvent` (includes user email)
2. Inventory reserves stock (`available` ↓, `reserved` ↑) → `StockReservedEvent`
3. Order status → `STOCK_RESERVED` (State Pattern)
4. ADMIN may advance `PAID` → `PREPARING` → `SHIPPED` → `DELIVERED`
5. Notification logs order/stock emails; Analytics persists all events
6. Order cancel (from `CREATED`/`STOCK_RESERVED`) → `OrderCancelledEvent` → inventory release → `StockReleasedEvent`

Product price snapshot remains a synchronous REST call from order-service.

### Broker

| Item | Value |
|------|--------|
| AMQP | `localhost:5672` |
| Management UI | http://localhost:15672 |
| User / password | `commercehub` / `commercehub` |

Start only the broker:

```bash
docker compose up rabbitmq -d
```

### Topology (`common-messaging`)

| Kind | Name |
|------|------|
| Exchange | `commercehub.events` (topic, durable) |
| Routing keys | `order.created`, `order.cancelled`, `stock.reserved`, `stock.released`, `payment.succeeded`, `payment.failed` |
| Queues | `inventory.order-created`, `inventory.order-cancelled`, `order.stock-reserved`, `payment.stock-reserved`, `order.payment-succeeded`, `order.payment-failed`, `notification.order-events` (`order.#`), `notification.stock-events` (`stock.#`), `notification.payment-events` (`payment.#`), `analytics.events` (`order.#` + `stock.#` + `payment.#`) |

Shared event records live in `services/common-messaging` (`OrderCreatedEvent`, `OrderCancelledEvent`, `StockReservedEvent`, `StockReleasedEvent`, `PaymentSucceededEvent`, `PaymentFailedEvent`).

### Environment (order / inventory / notification / analytics)

| Variable | Default (local) |
|----------|-----------------|
| `SPRING_RABBITMQ_HOST` | `localhost` |
| `SPRING_RABBITMQ_PORT` | `5672` |
| `SPRING_RABBITMQ_USERNAME` | `commercehub` |
| `SPRING_RABBITMQ_PASSWORD` | `commercehub` |

In Docker Compose these point at the `rabbitmq` service.

## V3 API Gateway + Eureka

Service discovery and a single entry point for clients. Route resilience is documented in the V3 Resilience section below.

### Architecture

- **Eureka** (`eureka-server`): registry on port **8761**
- **API Gateway** (`api-gateway`): Spring Cloud Gateway on port **8080**; resolves backends via Eureka (`lb://…`)
- All business services register with Eureka

Clients should call the gateway, not service ports directly.

| Path | Target |
|------|--------|
| `/api/v1/auth/**` | `auth-service` |
| `/api/v1/products/**` | `product-service` |
| `/api/v1/inventory/**` | `inventory-service` |
| `/api/v1/orders/**` | `order-service` |
| `/api/v1/notifications/**` | `notification-service` |
| `/api/v1/analytics/**` | `analytics-service` |

`/internal/**` is **not** exposed through the gateway (service-to-service only).

JWT is validated at the gateway (`Authorization: Bearer …`). `/api/v1/auth/**` and Swagger paths are public; other `/api/v1/**` routes require a token. Downstream services still enforce JWT as a second check.

### URLs

| Item | URL |
|------|-----|
| API Gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Example login | `POST http://localhost:8080/api/v1/auth/login` |
| Example products | `GET http://localhost:8080/api/v1/products` (Bearer token) |

### Run locally

```bash
# Eureka
cd services && mvn -pl eureka-server spring-boot:run

# Gateway (after Eureka and at least one backend are up)
mvn -pl api-gateway spring-boot:run
```

| Variable | Default |
|----------|---------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` |
| `JWT_SECRET` | Same as auth / other services |

## V3 Redis (tokens, cache, rate limiting)

Shared Redis for auth refresh tokens, product-by-id caching, and gateway rate limiting.

### Broker

| Item | Value |
|------|--------|
| Host / port | `localhost:6379` |
| Image | `redis:7-alpine` |

```bash
docker compose up redis -d
```

### Refresh tokens (auth-service)

- Active refresh tokens live in Redis only: key `refresh:{sha256}` → `userId`, TTL = `auth.refresh-token.expiration-days` (default 7 days)
- Raw tokens are never stored; SHA-256 hash is the key
- Rotate / logout → `DEL` the key
- Users remain in Postgres; Postgres `refresh_tokens` table is unused by the app (Flyway history kept)

### Product cache (product-service)

- Key `product:{id}` → JSON `ProductResponse`, TTL default **300** seconds (`product.cache.ttl-seconds`)
- `GET` by id and internal snapshot use the cache; list endpoints are not cached
- Update / soft-delete evicts `product:{id}`

### Gateway rate limiting (api-gateway)

- Redis token-bucket limiter per client IP (`X-Forwarded-For` first hop, else remote address)
- Applied **before** JWT validation (`RateLimitGlobalFilter` order `-200`)
- Default: **10 req/s**, burst **20** (`gateway.rate-limit.*`)
- Limit exceeded → **429** JSON `{ "error": "TOO_MANY_REQUESTS", ... }`
- Disable locally: `gateway.rate-limit.enabled=false`

### Environment

| Variable | Default (local) | Used by |
|----------|-----------------|---------|
| `SPRING_DATA_REDIS_HOST` | `localhost` | auth-service, product-service, api-gateway |
| `SPRING_DATA_REDIS_PORT` | `6379` | auth-service, product-service, api-gateway |
| `PRODUCT_CACHE_TTL_SECONDS` | `300` | product-service |
| `GATEWAY_RATE_LIMIT_ENABLED` | `true` | api-gateway (`gateway.rate-limit.enabled`) |
| `GATEWAY_RATE_LIMIT_REPLENISH_RATE` | `10` | api-gateway |
| `GATEWAY_RATE_LIMIT_BURST_CAPACITY` | `20` | api-gateway |
| `GATEWAY_RATE_LIMIT_REQUESTED_TOKENS` | `1` | api-gateway |

In Docker Compose, auth, product, and api-gateway set `SPRING_DATA_REDIS_HOST=redis` and depend on the healthy Redis container.

## V3 Saga (payment + compensation)

Choreography saga after stock reservation. No HTTP payment API — `payment-service` (port **8087**) only consumes/publishes RabbitMQ events.

### Flow

1. Order create → `order.created` → inventory reserve → `stock.reserved` → order `STOCK_RESERVED`
2. `payment-service` consumes `stock.reserved`
3. **Success** (`PAYMENT_SIMULATE_FAILURE=false`): `payment.succeeded` → order `PAID`
4. **Failure** (`PAYMENT_SIMULATE_FAILURE=true`): `payment.failed` → order `CANCELLED` + `order.cancelled` → inventory release → `stock.released`

Admin `PATCH /api/v1/orders/{id}/status` to `PAID` still works as a manual override.

### Environment

| Variable | Default | Used by |
|----------|---------|---------|
| `PAYMENT_SIMULATE_FAILURE` | `false` | payment-service |

```bash
# Demo compensation path
PAYMENT_SIMULATE_FAILURE=true docker compose up payment-service -d
```

## V3 Observability (Prometheus, Grafana, logging)

Metrics and centralized logs for all runnable services. Config under [`observability/`](observability/).

### Metrics

| Item | URL |
|------|-----|
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (`admin` / `admin`) |
| Per-service scrape | `http://<service>:<port>/actuator/prometheus` |

Grafana ships with a provisioned **CommerceHub Overview** dashboard (HTTP rate, latency, 5xx, JVM heap).

```bash
docker compose up prometheus grafana -d
```

### Correlation ID

- Header: `X-Correlation-Id` (generated at the gateway if missing)
- Propagated to downstream services; echoed on responses
- Included in console logs via MDC (`correlationId`, `serviceName`)

```bash
curl -i -H "X-Correlation-Id: demo-123" http://localhost:8080/api/v1/products
```

### Centralized logging (ELK-lite)

| Item | URL / note |
|------|------------|
| Elasticsearch | http://localhost:9200 |
| Kibana | http://localhost:5601 |
| Shipper | Filebeat (Docker container logs) — Logstash omitted to keep Compose lighter |

Index pattern: `commercehub-logs-*`.

```bash
docker compose up elasticsearch kibana filebeat -d
```

### Actuator

Exposed on every service: `health`, `info`, `prometheus` (permitAll / gateway public).

## V3 Resilience (Resilience4j)

Order and inventory protect their synchronous GET dependencies with explicit HTTP timeouts, limited retries, and independent circuit breakers:

- `orderProduct`: order-service → product-service
- `orderAuth`: order-service → auth-service
- `inventoryProduct`: inventory-service → product-service

The circuit state model is `CLOSED → OPEN → HALF_OPEN`. A 20-call count window opens at a 50% failure rate after at least 10 calls. It remains open for 10 seconds, then permits 3 half-open probe calls. Each dependency has its own state, so an auth failure does not open the product circuit.

### Retry and timeout policy

- Connect timeout: 500 ms; read timeout: 2 seconds.
- At most 3 total attempts with 200 ms exponential backoff.
- Retry only connection/refusal/timeout failures and HTTP 502, 503, or 504.
- Do not retry 4xx responses, response decoding/schema failures, validation failures, or mutating HTTP calls.
- Exhausted retries and open circuits produce controlled HTTP 503 responses. No fake product, price, or email fallback data is generated.

Order and inventory support `HTTP_CLIENT_CONNECT_TIMEOUT_MS`, `HTTP_CLIENT_READ_TIMEOUT_MS`, `RETRY_MAX_ATTEMPTS`, `RETRY_WAIT_DURATION`, `CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE`, `CIRCUIT_BREAKER_MINIMUM_CALLS`, `CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD`, `CIRCUIT_BREAKER_OPEN_WAIT`, and `CIRCUIT_BREAKER_HALF_OPEN_CALLS`.

### Gateway policy

Each of the six `lb://` routes has its own circuit breaker and forwards failures to `/fallback/{service}`, which returns a standard HTTP 503 JSON response. The gateway intentionally has no Retry filter because replaying POST, PATCH, or DELETE requests can duplicate mutations.

Gateway circuit settings are overridable with the corresponding `GATEWAY_CIRCUIT_BREAKER_*` environment variables.

### Metrics

Resilience4j metrics are exported through each service's `/actuator/prometheus` endpoint. Useful series include `resilience4j_circuitbreaker_state`, `resilience4j_circuitbreaker_calls_seconds`, and `resilience4j_retry_calls`.

The next V3 stage is CI/CD.

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
