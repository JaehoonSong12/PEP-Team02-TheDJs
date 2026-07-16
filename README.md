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

Drives the Angular frontend through Firefox using Selenium and Cucumber BDD scenarios. Requires both backend and frontend to be running.

```bash
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

## Docker Deployment

The entire application can be launched with a single command using Docker Compose. No Java, Node.js, or Gradle installation required on the host — just Docker.

### Prerequisites

- **Docker** (v20+)
- **Docker Compose** (v2+)

### Quick Start

```bash
# Build images and start both containers
docker compose up --build -d

# View logs
docker compose logs -f

# Stop everything
docker compose down

# Stop and remove persisted data
docker compose down -v
```

Once running:
- **Frontend:** http://localhost:4200
- **Backend API:** http://localhost:8080

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Docker Compose                                    │
│                                                                          │
│  ┌─────────────────────────────────┐    ┌─────────────────────────────┐ │
│  │   frontend container             │    │   backend container          │ │
│  │   (nginx:alpine)                 │    │   (eclipse-temurin:21-jre)   │ │
│  │                                  │    │                              │ │
│  │  ┌───────────────────────────┐   │    │  ┌────────────────────────┐ │ │
│  │  │        Nginx              │   │    │  │    Spring Boot         │ │ │
│  │  │                           │   │    │  │    (Tomcat :8080)      │ │ │
│  │  │  • Serves Angular static  │   │    │  │                        │ │ │
│  │  │    files (HTML/JS/CSS)    │   │    │  │  • REST API            │ │ │
│  │  │                           │   │    │  │  • JWT Authentication  │ │ │
│  │  │  • Proxies /api/* ────────│───│────│──│→ /api/*                │ │ │
│  │  │                           │   │    │  │  • Spring Data JPA     │ │ │
│  │  │  • SPA routing fallback   │   │    │  │                        │ │ │
│  │  │    (try_files → index.html│)  │    │  └──────────┬─────────────┘ │ │
│  │  │                           │   │    │             │               │ │
│  │  │  • Gzip compression       │   │    │             ▼               │ │
│  │  └───────────────────────────┘   │    │  ┌────────────────────────┐ │ │
│  │                                  │    │  │   SQLite (todo.db)     │ │ │
│  │  Listens on :80                  │    │  │   /app/data/todo.db    │ │ │
│  └──────────┬───────────────────────┘    │  └──────────┬─────────────┘ │ │
│             │                            │             │               │ │
│             │                            └─────────────│───────────────┘ │
│             │                                          │                 │
│  ───────────│──── app-network (bridge) ────────────────│──────────────── │
│             │                                          │                 │
│             ▼                                          ▼                 │
│     Host port 4200                             Named volume: todo-data   │
└─────────────────────────────────────────────────────────────────────────┘

         ▲
         │
    Browser requests
    http://localhost:4200
```

### How Nginx Works in This Setup

Nginx inside the frontend container serves three roles:

**1. Static File Server**

After the Angular app is compiled (`npm run build`), it produces plain HTML, JavaScript, and CSS files. Nginx serves these static assets directly to the browser — no Node.js runtime needed in production.

**2. Reverse Proxy for API Calls**

When the Angular app makes HTTP requests to `/api/*`, the browser sends them to `localhost:4200` (Nginx). Nginx intercepts these and forwards them internally to the backend container:

```
Browser → localhost:4200/api/todos → Nginx → http://backend:8080/api/todos → Spring Boot
```

This works because Docker Compose places both containers on the same network (`app-network`) and registers each service name as a DNS entry. Nginx resolves `backend` to the backend container's internal IP.

The relevant `nginx.conf` block:

```nginx
location /api/ {
    proxy_pass http://backend:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

**3. SPA Routing Fallback**

Angular uses client-side routing — URLs like `/login` or `/dashboard` don't correspond to actual files on disk. Without this rule, refreshing the browser on `/dashboard` would return a 404. Nginx catches any path that doesn't match a real file and serves `index.html`, letting Angular's router take over:

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### Request Flow Examples

**Loading the app:**
```
Browser → GET localhost:4200/          → Nginx serves index.html
Browser → GET localhost:4200/main.js   → Nginx serves Angular bundle
Browser → GET localhost:4200/styles.css → Nginx serves stylesheet
```

**Navigating in the SPA:**
```
Browser → GET localhost:4200/dashboard → Nginx: no file "dashboard" found
                                       → Serves index.html instead
                                       → Angular Router handles /dashboard
```

**Making an API call:**
```
Browser → POST localhost:4200/api/auth/login → Nginx proxies to backend:8080
                                             → Spring Boot authenticates
                                             → Returns JWT token
```

### Port Mapping

| Service | Container Port | Host Port | Why |
|---------|:--------------:|:---------:|-----|
| Frontend (Nginx) | 80 | 4200 | Matches Angular's default dev port for consistency |
| Backend (Tomcat) | 8080 | 8080 | Standard Spring Boot port |

### Data Persistence

The SQLite database is stored in a Docker named volume (`todo-data`), mapped to `/app/data/todo.db` inside the backend container. This means:
- Data survives `docker compose down` and `docker compose restart`
- Data is only deleted with `docker compose down -v` (removes volumes)

### Multi-Stage Builds

Both Dockerfiles use multi-stage builds to keep production images small:

| Image | Build Stage | Runtime Stage | Final Size |
|-------|-------------|---------------|:----------:|
| Frontend | `node:22-alpine` (compile Angular) | `nginx:alpine` (serve static files) | ~94 MB |
| Backend | `gradle:8-jdk21` (compile JAR) | `eclipse-temurin:21-jre-alpine` (run JAR) | ~593 MB |

Source code, compilers, and dev dependencies are discarded after the build stage — only the compiled output makes it into the final image.

### Security

Both containers run as non-root users:
- Backend: `appuser` (UID 1001)
- Frontend: `nginx-user` (UID 1001)

## License

This project is part of a Revature training program (Team02 - TheDJs).
