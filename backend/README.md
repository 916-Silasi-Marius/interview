# Interview App — Task Management API

A Spring Boot REST API for managing tasks, employees, and tags with JWT authentication, role-based access control, and a full observability stack.

## Tech Stack

- **Java 21**, **Spring Boot 3.5**
- **H2** in-memory database with **Flyway** migrations
- **Spring Security** + JWT (OAuth2 Resource Server)
- **Micrometer** + **OpenTelemetry** for metrics and tracing
- **Grafana LGTM** stack (Loki, Grafana, Tempo, Prometheus, Pyroscope)
- **springdoc-openapi** for Swagger UI

---

## Prerequisites

- Java 21+
- Maven 3+
- Docker (for the observability stack)

---

## Quick Start

### 1. Start the observability stack

```bash
docker compose up -d
```

This starts Grafana, Prometheus, Loki, Tempo, and Pyroscope in a single container.

### 2. Build and run the app

```bash
mvn package -DskipTests && java -jar target/interview-1.0-SNAPSHOT.jar
```

### 3. Verify it's running

```bash
curl http://localhost:8080/actuator/health
```

---

## API Documentation

Once the app is running, open:

- **Swagger UI** → [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON** → [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **OpenAPI YAML** → [http://localhost:8080/v3/api-docs.yaml](http://localhost:8080/v3/api-docs.yaml)

---

## Authentication

The API uses JWT Bearer tokens. All endpoints except login require a valid token in the `Authorization` header.

### Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "jdoe", "password": "password"}' | jq .
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Use the token

Add the token to subsequent requests:

```bash
export TOKEN="<paste token here>"

curl -s http://localhost:8080/api/v1/task \
  -H "Authorization: Bearer $TOKEN" | jq .
```

> **Tip:** In Swagger UI, click the 🔓 **Authorize** button and paste the token (without "Bearer " prefix).

### Seed Users

The app ships with 4 pre-configured users (all passwords are `password`):

| Username  | Role              | Access Level                              |
|-----------|-------------------|-------------------------------------------|
| `jdoe`    | ADMIN             | Full access to all endpoints              |
| `asmith`  | PROJECT_MANAGER   | CRUD on tasks/tags, read employees        |
| `bwilson` | DEVELOPER         | Read tasks/tags, self-assign, update own task status |
| `cjones`  | DEVELOPER         | Same as above                             |

---

## API Endpoints

### Auth

| Method | Endpoint                | Auth     | Description          |
|--------|-------------------------|----------|----------------------|
| POST   | `/api/v1/auth/login`    | Public   | Login, returns JWT   |

### Tasks

| Method | Endpoint                          | Auth                      | Description                |
|--------|-----------------------------------|---------------------------|----------------------------|
| GET    | `/api/v1/task`                    | Any authenticated         | List all tasks (paginated) |
| GET    | `/api/v1/task/{id}`               | Any authenticated         | Get task by ID             |
| GET    | `/api/v1/task/search?query=...`   | Any authenticated         | Search tasks by keyword    |
| POST   | `/api/v1/task`                    | ADMIN, PROJECT_MANAGER    | Create a task              |
| PUT    | `/api/v1/task/{id}`               | ADMIN, PROJECT_MANAGER    | Full update                |
| PATCH  | `/api/v1/task/{id}`               | ADMIN, PROJECT_MANAGER    | Partial update             |
| PATCH  | `/api/v1/task/{id}/status`        | Assigned employee only    | Update own task status     |
| PATCH  | `/api/v1/task/{id}/self-assign`   | Any authenticated         | Self-assign a task         |
| DELETE | `/api/v1/task/{id}`               | ADMIN, PROJECT_MANAGER    | Delete a task              |

### Employees

| Method | Endpoint                  | Auth  | Description                    |
|--------|---------------------------|-------|--------------------------------|
| GET    | `/api/v1/employee`        | ADMIN | List all employees (paginated) |
| GET    | `/api/v1/employee/{id}`   | ADMIN | Get employee by ID             |
| POST   | `/api/v1/employee`        | ADMIN | Create an employee             |
| PUT    | `/api/v1/employee/{id}`   | ADMIN | Full update                    |
| PATCH  | `/api/v1/employee/{id}`   | ADMIN | Partial update                 |
| DELETE | `/api/v1/employee/{id}`   | ADMIN | Delete an employee             |

### Tags

| Method | Endpoint              | Auth                   | Description                |
|--------|-----------------------|------------------------|----------------------------|
| GET    | `/api/v1/tag`         | Any authenticated      | List all tags (paginated)  |
| GET    | `/api/v1/tag/{id}`    | Any authenticated      | Get tag by ID              |
| POST   | `/api/v1/tag`         | ADMIN, PROJECT_MANAGER | Create a tag               |
| PUT    | `/api/v1/tag/{id}`    | ADMIN, PROJECT_MANAGER | Full update                |
| PATCH  | `/api/v1/tag/{id}`    | ADMIN, PROJECT_MANAGER | Partial update             |
| DELETE | `/api/v1/tag/{id}`    | ADMIN, PROJECT_MANAGER | Delete a tag               |

---

## Example Requests

### Create a task

```bash
curl -s -X POST http://localhost:8080/api/v1/task \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskKey": "PROJ-6",
    "title": "Implement caching layer",
    "description": "Add Redis caching for frequently accessed endpoints",
    "priority": "HIGH",
    "storyPoints": 5,
    "assigneeId": 3,
    "tagIds": [2, 4]
  }' | jq .
```

### Search tasks

```bash
curl -s "http://localhost:8080/api/v1/task/search?query=login" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Self-assign a task

```bash
curl -s -X PATCH http://localhost:8080/api/v1/task/5/self-assign \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Update own task status

```bash
curl -s -X PATCH http://localhost:8080/api/v1/task/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}' | jq .
```

### Create an employee (admin only)

```bash
curl -s -X POST http://localhost:8080/api/v1/employee \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "new@example.com",
    "password": "securepass123",
    "fullName": "New User",
    "role": "DEVELOPER"
  }' | jq .
```

---

## Pagination

All list endpoints support pagination:

```
GET /api/v1/task?page=0&size=10
```

---

## Enums

**TaskStatus:** `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`

**TaskPriority:** `LOW`, `MEDIUM`, `HIGH`, `URGENT`

**EmployeeRole:** `DEVELOPER`, `PROJECT_MANAGER`, `QA`, `ADMIN`

---

## Running Tests

```bash
mvn test
```

Tests include:
- **Unit tests** — controllers, services, mappers, security components
- **Integration tests** — full HTTP flow with embedded database

---

## Observability

After starting the Docker stack (`docker compose up -d`):

| Tool        | URL                                      | Purpose                  |
|-------------|------------------------------------------|--------------------------|
| Grafana     | [http://localhost:3000](http://localhost:3000) | Dashboards, logs, traces |
| Prometheus  | Internal (port 9090)                     | Metrics storage          |
| Loki        | Internal (port 3100)                     | Log aggregation          |
| Tempo       | Internal (port 3200)                     | Distributed tracing      |
| Pyroscope   | Internal (port 4040)                     | Continuous profiling     |

A pre-built dashboard can be imported into Grafana:

```bash
curl -X POST http://localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @observability/dashboard.json
```

The dashboard includes: HTTP metrics, error rates, service method durations, JVM stats, database connection pools, and login counters.

---

## H2 Database Console

Available at [http://localhost:8080/h2-console](http://localhost:8080/h2-console) during runtime.

| Field     | Value                  |
|-----------|------------------------|
| JDBC URL  | `jdbc:h2:mem:testdb`   |
| Username  | `sa`                   |
| Password  | `password`             |

---

## Project Structure

```
src/main/java/com/interview/
├── config/             # Security, OpenAPI, Pyroscope, Observability configs
├── controller/         # REST controllers (Auth, Task, Tag, Employee)
├── exception/          # Custom exceptions + GlobalExceptionHandler
├── filter/             # RequestIdFilter (request correlation)
├── model/
│   ├── dto/            # Request/Response records
│   ├── entities/       # JPA entities (Task, Employee, Tag)
│   ├── enums/          # TaskStatus, TaskPriority, EmployeeRole
│   └── mapper/         # Entity ↔ DTO mappers
├── repository/         # Spring Data JPA repositories
│   └── specification/  # JPA Specifications for dynamic queries
├── security/           # UserDetailsService, TokenService, entry points
└── service/            # Business logic layer
```
