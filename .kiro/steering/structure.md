# Project Structure

## Repository Root
```
PEP-Team02-TheDJs/
├── spring-todo-backend/   # Java/Spring Boot backend
├── docs/                  # Project documentation (LaTeX + wireframes)
├── discussions/           # Team design decisions and API contracts
├── references/            # Reference READMEs and setup scripts
└── README.md              # Project overview and roadmap
```

## Backend — `spring-todo-backend/`
```
spring-todo-backend/
├── build.gradle.kts                  # Gradle build config (Kotlin DSL)
├── gradlew / gradlew.bat             # Gradle wrapper
├── compose.yaml                      # Docker Compose (currently empty)
└── src/
    ├── main/
    │   ├── java/com/revature/todomanagement/
    │   │   ├── TodomanagementApplication.java   # Spring Boot entry point
    │   │   ├── controller/                      # REST controllers (HTTP layer)
    │   │   ├── service/                         # Business logic
    │   │   ├── repository/                      # Spring Data JPA repositories
    │   │   ├── entity/                          # JPA entities (User, Task, Subtask)
    │   │   └── exception/                       # Custom exceptions / error handling
    │   └── resources/
    │       └── application.properties           # App config (DB, JPA, logging)
    └── test/
        └── java/com/revature/todomanagement/    # JUnit 5 tests (H2 in-memory DB)
```

## Backend Layer Conventions

| Layer | Package | Responsibility |
|---|---|---|
| Controller | `controller/` | Map HTTP requests/responses; no business logic |
| Service | `service/` | Business rules, ownership checks, validation |
| Repository | `repository/` | Spring Data JPA interfaces; extend `JpaRepository` |
| Entity | `entity/` | JPA-annotated domain models |
| Exception | `exception/` | Custom exception classes; use `@ControllerAdvice` for global handling |
| DTO | `dto/` *(to be added)* | Request/response payloads — keep separate from entities |
| Security | `security/` *(to be added)* | `JwtFilter`, `JwtUtil`, `SecurityConfig` |

## Planned Entities
- `User` — id, username, password (hashed)
- `Task` (Todo) — id, title, description, completed, owner (User FK)
- `Subtask` — id, title, description, completed, parent task (Task FK)

## API Structure
All endpoints are under `/api/`:
- `POST /api/auth/register` — user registration
- `POST /api/auth/login` — authentication, returns JWT
- `GET|POST /api/todos` — list / create tasks
- `GET|PUT|DELETE /api/todos/{id}` — read / update / delete a task
- `GET|POST /api/todos/{id}/{subtask}` — subtask operations
- `PUT|DELETE /api/todos/{id}/{subtask}` — update / delete a subtask

## Documentation — `docs/`
```
docs/
├── main.tex                # Root LaTeX document
├── module/                 # Individual LaTeX sections (overview, ERD, API, etc.)
│   └── appendix/           # Phase instruction markdown files (phase-1.md … phase-5.md)
└── Wireframes/             # UI mockups (Login, Register, Dashboard)
```

## Git Conventions
- Branch naming: `feature/<name>`, `bugfix/<name>`
- Commits should be atomic and descriptive
- All changes go through peer-reviewed Pull Requests
- Use GitHub Issues and GitHub Projects to track work
