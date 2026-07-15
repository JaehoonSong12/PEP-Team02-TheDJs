# Todo Management Application

A full-stack web application that lets users register, authenticate via JWT, and manage hierarchical todo tasks with nested subtasks. Built with Spring Boot 4.1 (Java 21) and Angular.

## Prerequisites

- **Java 21** (OpenJDK or Oracle JDK)
- **Gradle 8.x** (or use the included `gradlew` wrapper)
- **Node.js 22.x** and npm (for the Angular frontend)
- **Firefox** and **GeckoDriver** (for E2E browser tests only)

## Getting Started

### Backend

```bash
cd spring-todo-backend
./gradlew bootRun
```

Serves the REST API at `http://localhost:8080`.

### Frontend

```bash
cd angular-todo-frontend
npm install
npm start
```

Serves the Angular app at `http://localhost:4200`.

## Testing

All commands below run from `spring-todo-backend/`.

### Unit Tests

Validates service and security logic in isolation using Mockito. No Spring context is loaded.

```bash
./gradlew test --tests "com.revature.todomanagement.service.*" \
               --tests "com.revature.todomanagement.security.*"
```

### Integration Tests

Exercises the full REST API via REST Assured against an embedded server with H2 in-memory database.

```bash
./gradlew test --tests "com.revature.todomanagement.rest.*"
```

### End-to-End Tests

Drives the Angular frontend through Firefox using Selenium and Cucumber BDD scenarios. The backend is auto-started by the test runner on port 8080. Only the frontend needs to be running separately.

```bash
# Start frontend first (in a separate terminal):
#   cd angular-todo-frontend && npm start

./gradlew test --tests "com.revature.todomanagement.cucumber.*"
```

### All Tests

```bash
./gradlew test
```

> **Note:** E2E tests require the backend on `:8080`, the frontend on `:4200`, and Firefox/GeckoDriver available on PATH.

## Architecture

```
spring-todo-backend/src/
├── main/java/com/revature/todomanagement/
│   ├── controller/        # REST endpoints
│   ├── service/           # Business logic
│   ├── repository/        # Spring Data JPA interfaces
│   ├── entity/            # JPA domain models
│   ├── security/          # JWT, interceptor, password validation
│   └── exception/         # Custom exceptions and error handling
└── test/java/com/revature/todomanagement/
    ├── service/           # Unit tests (Mockito)
    ├── security/          # Unit tests (Mockito)
    ├── rest/              # API integration tests (REST Assured)
    └── cucumber/          # E2E browser tests (Selenium + Cucumber)
```

### Test Tiers

| Tier | Package | Tools | Boots Spring? | Purpose |
|------|---------|-------|:-------------:|---------|
| Unit | `.service`, `.security` | JUnit 5, Mockito, jqwik | No | Verify business logic and security rules in isolation |
| Integration | `.rest` | JUnit 5, REST Assured, H2 | Yes | Verify HTTP contracts, status codes, and error handling |
| E2E | `.cucumber` | Cucumber, Selenium, Firefox | Yes | Verify user journeys through the browser |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4.1, Spring Web MVC, Spring Data JPA |
| Auth | JJWT 0.13 (HS256) |
| Database | SQLite (production), H2 (tests) |
| Frontend | Angular, HttpClient with JWT interceptor |
| Build | Gradle (Kotlin DSL) |

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|:----:|-------------|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Authenticate and receive JWT |
| GET | `/api/todos` | Yes | List tasks for authenticated user |
| POST | `/api/todos` | Yes | Create a task |
| GET | `/api/todos/{id}` | Yes | Get a task by ID |
| PUT | `/api/todos/{id}` | Yes | Update a task |
| DELETE | `/api/todos/{id}` | Yes | Delete a task (cascades to subtasks) |
| GET | `/api/todos/{id}/subtasks` | Yes | List subtasks |
| POST | `/api/todos/{id}/subtasks` | Yes | Create a subtask |
| PUT | `/api/todos/{id}/subtasks/{sid}` | Yes | Update a subtask |
| DELETE | `/api/todos/{id}/subtasks/{sid}` | Yes | Delete a subtask |

## License

This project is part of a Revature training program (Team02 - TheDJs).
