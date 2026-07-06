# Phase 2: Containerization

## Objective
The goal of this phase is to package your application into portable, isolated environments using Docker. Containerization ensures that your application runs identically in your local environment, on the Jenkins server, and in the AWS cloud, eliminating the "it works on my machine" problem.

## Checkpoints
- [ ] **Frontend Containerization:** Create a Dockerfile to containerize the Angular application (typically using an Nginx base image).
- [ ] **Backend Containerization:** Create a Dockerfile to containerize the Spring Boot application.
- [ ] **Multi-Container Orchestration:** Develop a `docker-compose.yml` file to run the frontend, backend, and database as a single cohesive unit.
- [ ] **Image Optimization:** Ensure Docker images are efficient, secure, and follow industry best practices.

## Technical Requirements

### 1. Dockerfile Standards
*   **Multi-Stage Builds:** For both Angular and Spring Boot, use multi-stage builds to keep final production images small. (e.g., Build the app in one stage, then copy only the artifacts/dist folder to a lightweight production stage like `nginx:alpine` or `openjdk:alpine`).
*   **Layer Optimization:** Order your commands to leverage Docker's layer caching (e.g., copy dependency files like `package.json` or `build.gradle` before copying the full source code).
*   **Non-Root Users:** For enhanced security, configure your containers to run as a non-privileged user rather than the default root user.

### 2. Docker Compose Orchestration
Your `docker-compose.yml` must facilitate a complete local environment:
*   **Service Linking:** Ensure the frontend container can communicate with the backend container via the Docker network.
*   **Environment Variables:** Use environment variables to pass configuration (like database URLs or API endpoints) into the containers rather than hardcoding them.
*   **Volume Mapping:** Use volumes to ensure data persistence for your SQLite database during container restarts.

## Deliverables for Phase 2
By the end of this phase, your team should have:
1.  **Dockerfiles:** Valid, optimized Dockerfiles for the frontend and backend components.
2.  **Docker Compose Configuration:** A single `docker-compose.yml` file that launches the entire application stack with one command.
3.  **Containerized Verification:** Confirmation that the full application stack runs successfully within Docker containers.

## Moving to Phase 3
Once your application is successfully running in a containerized environment, proceed to **Phase 3: Cloud Infrastructure**.

---

[Return to README.md](../README.md)