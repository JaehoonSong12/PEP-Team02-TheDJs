# Implementation Plan: Docker Containerization

## Overview

Containerize the Todo Management Application using multi-stage Docker builds and Docker Compose. The implementation creates Dockerfiles for the Angular frontend (Nginx Alpine) and Spring Boot backend (JRE 21 Alpine), a custom Nginx config for API proxying, and a root-level docker-compose.yml that orchestrates the full stack with environment variable configuration and volume-based SQLite persistence.

## Tasks

- [x] 1. Create Backend Dockerfile with multi-stage build
  - [x] 1.1 Create `spring-todo-backend/Dockerfile` with Gradle build stage and JRE runtime stage
    - Use `gradle:8-jdk21` as the build stage base image
    - Copy Gradle wrapper files (`gradlew`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`) first for layer caching
    - Run `./gradlew dependencies` to download and cache dependencies
    - Copy full source code and run `./gradlew bootJar -x test`
    - Use `eclipse-temurin:21-jre-alpine` as the runtime stage base image
    - Create non-root user `appuser` (UID 1001)
    - Create `/app` and `/app/data` directories with proper ownership
    - Copy the built JAR from the build stage
    - Set ownership to `appuser`, switch to non-root user
    - Expose port 8080
    - Set entrypoint to run the JAR with datasource URL pointing to `/app/data/todo.db`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 8.2, 8.4_

- [x] 2. Create Frontend Dockerfile and Nginx configuration
  - [x] 2.1 Create `angular-todo-frontend/nginx.conf` with API reverse proxy and Angular routing support
    - Configure Nginx to serve static files from `/usr/share/nginx/html`
    - Add `location /api/` block that proxies to `http://backend:8080/api/`
    - Include `proxy_set_header` directives for Host, X-Real-IP, X-Forwarded-For, X-Forwarded-Proto
    - Add `try_files $uri $uri/ /index.html` fallback for Angular client-side routing
    - Enable gzip compression for text/css, application/javascript, application/json
    - Listen on port 80
    - _Requirements: 1.4, 5.2_

  - [x] 2.2 Create `angular-todo-frontend/Dockerfile` with Node build stage and Nginx runtime stage
    - Use `node:22-alpine` as the build stage base image
    - Copy `package.json` and `package-lock.json` first for layer caching
    - Run `npm ci` to install dependencies
    - Copy full source code and run `npm run build`
    - Use `nginx:alpine` as the runtime stage base image
    - Create non-root user `nginx-user` (UID 1001)
    - Remove default Nginx config and copy custom `nginx.conf`
    - Copy compiled assets from build stage (`dist/angular-todo-frontend/browser/`) to Nginx html directory
    - Set appropriate file ownership on Nginx directories (`/var/cache/nginx`, `/var/run`, `/usr/share/nginx/html`)
    - Switch to non-root user
    - Expose port 80
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 2.2, 8.1, 8.3_

- [x] 3. Create Docker Compose orchestration
  - [x] 3.1 Create root-level `docker-compose.yml` with frontend, backend services, network, and volume
    - Define `backend` service: build from `./spring-todo-backend`, expose `8080:8080`, attach to `app-network`
    - Set backend environment variables: `SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/todo.db`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS=http://localhost:4200`
    - Mount named volume `todo-data` to `/app/data` on the backend service
    - Define `frontend` service: build from `./angular-todo-frontend`, expose `4200:80`, attach to `app-network`
    - Set `depends_on: backend` on the frontend service
    - Define custom bridge network `app-network`
    - Define named volume `todo-data`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3_

- [x] 4. Create .dockerignore files for build optimization
  - [x] 4.1 Create `spring-todo-backend/.dockerignore` to exclude build artifacts and unnecessary files
    - Exclude `build/`, `.gradle/`, `todo.db`, `.idea/`, `*.iml`, `.jqwik-database`
    - _Requirements: 4.1, 4.2_

  - [x] 4.2 Create `angular-todo-frontend/.dockerignore` to exclude node_modules and build output
    - Exclude `node_modules/`, `dist/`, `.angular/`, `.vscode/`
    - _Requirements: 2.1, 2.2_

- [x] 5. Checkpoint - Verify Docker builds complete successfully
  - Ensure `docker compose build` completes without errors from the project root.
  - Ensure `docker compose up -d` starts both containers.
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Clean up and finalize configuration
  - [x] 6.1 Remove or replace the existing empty `spring-todo-backend/compose.yaml` to avoid conflicts
    - Delete or empty the existing `compose.yaml` that is no longer needed since orchestration moves to project root
    - _Requirements: 5.1_

- [x] 7. Final checkpoint - Verify full stack operation
  - Ensure `docker compose up --build` launches the full stack from the project root.
  - Verify frontend is accessible at `http://localhost:4200` and backend at `http://localhost:8080`.
  - Verify API proxying works (`http://localhost:4200/api/` routes to backend).
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- This feature has no property-based tests since it is Infrastructure as Code (declarative Docker configuration)
- Each task references specific requirements from the requirements document for traceability
- Checkpoints ensure incremental validation of the Docker setup
- The `.dockerignore` files are essential for build performance and keeping image sizes small
- The backend environment variable `SPRING_DATASOURCE_URL` overrides `spring.datasource.url` via Spring Boot's relaxed binding
- The existing empty `compose.yaml` in `spring-todo-backend/` should be removed to avoid confusion with the root-level `docker-compose.yml`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "4.1", "4.2"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2"] },
    { "id": 3, "tasks": ["3.1", "6.1"] }
  ]
}
```
