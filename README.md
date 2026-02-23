# BeautyFinder B2B Backend

Backend API for BeautyFinder B2B salon management platform. Built with Kotlin, Spring Boot 3, and PostgreSQL.

## Prerequisites

- JDK 21 (only for local development outside Docker)
- Docker & Docker Compose
- (Optional) Gradle 8.12+ (wrapper included)

## Quick Start

```bash
# Start all services (PostgreSQL + app) — no local JDK needed
docker compose up -d

# Or start only the database and run the app locally
docker compose up -d postgres
./gradlew bootRun
```

The application will be available at `http://localhost:8080`.

> On first start, Flyway runs all migrations automatically (V1–V9) and loads Polish mock data (salons, users, employees, services) via the seed migration V6.

## Docker Images

| Image | Stage | Purpose |
|---|---|---|
| `eclipse-temurin:21-jdk-alpine` | Build (multi-stage) | Compiles the app and produces the fat JAR via `./gradlew bootJar` |
| `eclipse-temurin:21-jre-alpine` | Runtime | Runs the compiled `app.jar` — minimal JRE-only image |
| `postgres:16-alpine` | Infrastructure | Primary database |
| `dpage/pgadmin4` | Tools (optional) | Web-based DB admin UI |

The Dockerfile uses a **multi-stage build**: the JDK builder stage is discarded after compilation, and only the JAR is copied into the lean JRE runtime image.

## API Docs

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Test Credentials (seed data)

All seed users share the same password: **`password123`**

| Email | Role | Salon |
|---|---|---|
| `anna.kowalska@glamourstudio.pl` | OWNER | Glamour Studio |
| `ewa.zielinska@glamourstudio.pl` | MANAGER | Glamour Studio |
| `monika.dabrowska@beautylab.pl` | OWNER | Beauty Lab Kraków |
| `beata.wojcik@salonperla.pl` | OWNER | Salon Perła |

## Running Tests

```bash
# All tests
./gradlew test

# Unit tests only (exclude integration tests requiring Testcontainers)
./gradlew test --tests "*.config.*" --tests "*.api.*"
```

> Integration tests use Testcontainers and require Docker to be running. Testcontainers pulls `postgres:16-alpine` automatically.

## Environment Variables

| Variable            | Description                    | Default                                          |
|---------------------|--------------------------------|--------------------------------------------------|
| `DATABASE_URL`      | PostgreSQL JDBC URL            | `jdbc:postgresql://localhost:5432/beautyfinder`  |
| `DATABASE_USERNAME` | Database username              | `beautyfinder`                                   |
| `DATABASE_PASSWORD` | Database password              | `beautyfinder`                                   |
| `JWT_SECRET`        | Secret key for JWT signing     | dev fallback — **change in production!**         |
| `STORAGE_PATH`      | Directory for uploaded files   | `./uploads`                                      |
| `SERVER_PORT`       | Application port               | `8080`                                           |

## Project Structure

```
src/main/kotlin/com/beautyfinder/b2b/
├── domain/           # JPA entities, enums, value objects
├── application/      # Application services / use cases
├── infrastructure/   # JPA repositories
├── api/              # REST controllers, DTOs, exception handler
└── config/           # Security, JWT, OpenAPI, tenant context

src/main/resources/
├── application.yml
└── db/migration/     # Flyway migrations (V1–V9), applied automatically on startup
```

## Tools (optional)

```bash
# Start pgAdmin alongside the stack
docker compose --profile tools up -d
```

pgAdmin: [http://localhost:5050](http://localhost:5050) (admin@beautyfinder.com / admin)
