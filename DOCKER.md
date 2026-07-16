# Docker Deployment

The entire application can be launched with a single command using Docker Compose. No Java, Node.js, or Gradle installation required on the host — just Docker.

## Prerequisites

- **Docker** (v20+)
- **Docker Compose** (v2+)

## Quick Start

```bash
# Build images and start both containers
docker compose up --build

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

### Command Flags

| Command | Behavior |
|---------|----------|
| `docker compose up` | Start with existing images, logs in foreground |
| `docker compose up -d` | Start with existing images, runs in background |
| `docker compose up --build` | Rebuild images then start, logs in foreground |
| `docker compose up --build -d` | Rebuild images then start, runs in background |

- `--build` forces Docker to rebuild images from the Dockerfiles (picks up code changes)
- `-d` (detached) runs containers in the background so you get your terminal back

## Deployment Architecture

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

## How Nginx Works in This Setup

Nginx inside the frontend container serves three roles:

### 1. Static File Server

After the Angular app is compiled (`npm run build`), it produces plain HTML, JavaScript, and CSS files. Nginx serves these static assets directly to the browser — no Node.js runtime needed in production.

### 2. Reverse Proxy for API Calls

When the Angular app makes HTTP requests to `/api/*`, the browser sends them to `localhost:4200` (Nginx). Nginx intercepts these and forwards them internally to the backend container:

```
Browser → localhost:4200/api/todos → Nginx → http://backend:8080/api/todos → Spring Boot
```

This works because Docker Compose places both containers on the same network (`app-network`) and registers each service name as a DNS entry. Nginx resolves `backend` to the backend container's internal IP automatically.

The `backend` hostname comes from the service name defined in `docker-compose.yml`:

```yaml
services:
  backend:    # ← this becomes a DNS hostname on the Docker network
    build: ./spring-todo-backend
    ...
```

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

The `proxy_set_header` lines forward the original client information (IP address, protocol) so the backend knows who's actually making the request rather than thinking every request comes from Nginx.

### 3. SPA Routing Fallback

Angular uses client-side routing — URLs like `/login` or `/dashboard` don't correspond to actual files on disk. Without this rule, refreshing the browser on `/dashboard` would return a 404. Nginx catches any path that doesn't match a real file and serves `index.html`, letting Angular's router take over:

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

## Request Flow Examples

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

## Port Mapping

| Service | Container Port | Host Port | Why |
|---------|:--------------:|:---------:|-----|
| Frontend (Nginx) | 80 | 4200 | Matches Angular's default dev port for consistency |
| Backend (Tomcat) | 8080 | 8080 | Standard Spring Boot port |

The port mapping is defined in `docker-compose.yml` under the `ports` key:

```yaml
frontend:
  ports:
    - "4200:80"   # host:container — maps your machine's 4200 to Nginx's 80
```

Inside the container, Nginx always listens on port 80. Docker's port mapping is what makes it accessible from your host at `localhost:4200`.

## Data Persistence

The SQLite database is stored in a Docker named volume (`todo-data`), mapped to `/app/data/todo.db` inside the backend container. This means:
- Data survives `docker compose down` and `docker compose restart`
- Data is only deleted with `docker compose down -v` (removes volumes)

## Multi-Stage Builds

Both Dockerfiles use multi-stage builds to keep production images small:

| Image | Build Stage | Runtime Stage | Final Size |
|-------|-------------|---------------|:----------:|
| Frontend | `node:22-alpine` (compile Angular) | `nginx:alpine` (serve static files) | ~94 MB |
| Backend | `gradle:8-jdk21` (compile JAR) | `eclipse-temurin:21-jre-alpine` (run JAR) | ~593 MB |

Source code, compilers, and dev dependencies are discarded after the build stage — only the compiled output makes it into the final image.

### Why Multi-Stage?

A single-stage build would include the entire Gradle SDK, JDK, source code, and all build dependencies in the final image. Multi-stage lets you use a heavy image to compile, then copy only the artifact (JAR or static files) into a minimal runtime image.

## Security

Both containers run as non-root users:
- Backend: `appuser` (UID 1001)
- Frontend: `nginx-user` (UID 1001)

If a container is compromised, the attacker has limited privileges — they can't install packages, modify system files, or escalate to root.

## Docker Files Overview

| File | Location | Purpose |
|------|----------|---------|
| `Dockerfile` | `spring-todo-backend/` | Multi-stage build for the Spring Boot JAR |
| `Dockerfile` | `angular-todo-frontend/` | Multi-stage build for Angular + Nginx |
| `.dockerignore` | `spring-todo-backend/` | Excludes build/, .gradle/, todo.db, IDE files |
| `.dockerignore` | `angular-todo-frontend/` | Excludes node_modules/, dist/, .angular/ |
| `nginx.conf` | `angular-todo-frontend/` | Nginx config: static serving + API proxy + SPA routing |
| `docker-compose.yml` | Project root | Orchestrates both services with network and volume |
