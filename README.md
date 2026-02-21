# BeautyFinder B2B Backend

Backend API for BeautyFinder B2B salon management platform. Built with Kotlin, Spring Boot 3, and PostgreSQL.

## Prerequisites

- JDK 21
- Docker & Docker Compose
- (Optional) Gradle 8.12+ (wrapper included)

## Quick Start

```bash
# Start all services (PostgreSQL + app)
docker compose up -d

# Or start only the database and run the app locally
docker compose up -d postgres
./gradlew bootRun
```

The application will be available at `http://localhost:8080`.

## API Docs

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Running Tests

```bash
# All tests
./gradlew test

# Unit tests only (exclude integration tests requiring Testcontainers)
./gradlew test --tests "*.config.*" --tests "*.api.*"
```

> Integration tests use Testcontainers and require Docker to be running.

## Environment Variables

| Variable            | Description                    | Default                                         |
|---------------------|--------------------------------|-------------------------------------------------|
| `DATABASE_URL`      | PostgreSQL JDBC URL            | `jdbc:postgresql://localhost:5432/beautyfinder`  |
| `DATABASE_USERNAME` | Database username              | `beautyfinder`                                   |
| `DATABASE_PASSWORD` | Database password              | `beautyfinder`                                   |
| `JWT_SECRET`        | Secret key for JWT signing     | dev fallback (change in production!)             |
| `SERVER_PORT`       | Application port               | `8080`                                           |

## Project Structure

```
src/main/kotlin/com/beautyfinder/b2b/
├── domain/           # JPA entities, enums, value objects
├── application/      # Application services / use cases
├── infrastructure/   # JPA repositories
├── api/              # REST controllers, DTOs, exception handler
└── config/           # Security, JWT, OpenAPI, tenant context
```

## Tools (optional)

```bash
# Start pgAdmin alongside the stack
docker compose --profile tools up -d
```

pgAdmin: [http://localhost:5050](http://localhost:5050) (admin@beautyfinder.com / admin)
