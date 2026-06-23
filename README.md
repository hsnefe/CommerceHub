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

### Run with Docker Compose

```bash
docker compose up --build
```

- Auth Service: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- PostgreSQL: `localhost:5433` (user: `auth_user`, db: `auth_db`)

### Run locally (requires Java 21+ and Maven)

Start PostgreSQL first (or use `docker compose up auth-db`), then:

```bash
cd services/auth-service
mvn spring-boot:run
```

Environment variables:

| Variable | Default |
|----------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/auth_db` |
| `SPRING_DATASOURCE_USERNAME` | `auth_user` |
| `SPRING_DATASOURCE_PASSWORD` | `auth_pass` |
| `JWT_SECRET` | (dev default in application.yml) |
| `AUTH_EMAIL_VERIFICATION_REQUIRED` | `false` |

### Run tests

```bash
cd services/auth-service
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
