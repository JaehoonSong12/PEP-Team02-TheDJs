# Requirements Document

## Introduction

This document specifies the requirements for containerizing the Todo Management Application using Docker. The goal is to package the Angular frontend, Spring Boot backend, and SQLite database into Docker containers orchestrated by Docker Compose, enabling the entire application stack to be launched with a single command. The containers must follow industry best practices for image size, security, and layer optimization.

## Glossary

- **Frontend_Container**: The Docker container running the Angular application served via Nginx
- **Backend_Container**: The Docker container running the Spring Boot application with an embedded SQLite database
- **Compose_Stack**: The Docker Compose configuration that defines and orchestrates all application containers as a unified stack
- **Multi_Stage_Build**: A Docker build technique that uses separate build and runtime stages to minimize final image size
- **Non_Root_User**: A user account inside a container with restricted privileges, used instead of the default root user for security
- **Docker_Network**: The internal network created by Docker Compose that enables inter-container communication via service names
- **Volume_Mount**: A Docker volume that persists container data (such as the SQLite database file) across container restarts

## Requirements

### Requirement 1: Frontend Multi-Stage Dockerfile

**User Story:** As a developer, I want the Angular frontend containerized using a multi-stage Dockerfile, so that the production image is small and contains only compiled static assets served by Nginx.

#### Acceptance Criteria

1. WHEN the Frontend_Container image is built, THE Multi_Stage_Build SHALL use a Node.js base image in the build stage to install dependencies and compile the Angular application
2. WHEN the build stage completes, THE Multi_Stage_Build SHALL copy only the compiled output to an Nginx Alpine base image for the runtime stage
3. THE Frontend_Container SHALL serve the Angular application on port 80 using Nginx
4. THE Frontend_Container SHALL include an Nginx configuration that proxies API requests (paths starting with /api) to the Backend_Container
5. THE Frontend_Container SHALL run the Nginx process as a Non_Root_User

### Requirement 2: Frontend Layer Optimization

**User Story:** As a developer, I want the frontend Dockerfile to leverage Docker layer caching, so that rebuilds are faster when only source code changes.

#### Acceptance Criteria

1. THE Frontend_Container Dockerfile SHALL copy package.json and package-lock.json before copying the full source code
2. WHEN only application source files change, THE Frontend_Container SHALL reuse the cached dependency installation layer

### Requirement 3: Backend Multi-Stage Dockerfile

**User Story:** As a developer, I want the Spring Boot backend containerized using a multi-stage Dockerfile, so that the production image contains only the application JAR and a minimal JRE.

#### Acceptance Criteria

1. WHEN the Backend_Container image is built, THE Multi_Stage_Build SHALL use a Gradle base image with JDK 21 in the build stage to compile and package the Spring Boot application
2. WHEN the build stage completes, THE Multi_Stage_Build SHALL copy only the built JAR file to an Eclipse Temurin JRE 21 Alpine base image for the runtime stage
3. THE Backend_Container SHALL expose port 8080 for the Spring Boot application
4. THE Backend_Container SHALL run the Java process as a Non_Root_User

### Requirement 4: Backend Layer Optimization

**User Story:** As a developer, I want the backend Dockerfile to leverage Docker layer caching, so that rebuilds are faster when only source code changes.

#### Acceptance Criteria

1. THE Backend_Container Dockerfile SHALL copy the Gradle wrapper and build configuration files before copying the full source code
2. WHEN only application source files change, THE Backend_Container SHALL reuse the cached dependency download layer

### Requirement 5: Docker Compose Orchestration

**User Story:** As a developer, I want a single docker-compose.yml file that launches the frontend, backend, and database as a cohesive unit, so that the entire application can be started with one command.

#### Acceptance Criteria

1. THE Compose_Stack SHALL define a service for the Frontend_Container and a service for the Backend_Container
2. THE Compose_Stack SHALL create a Docker_Network that enables the Frontend_Container to communicate with the Backend_Container using service names
3. THE Compose_Stack SHALL configure the Frontend_Container to depend on the Backend_Container so that the backend starts before the frontend
4. THE Compose_Stack SHALL expose port 4200 on the host mapped to port 80 on the Frontend_Container
5. THE Compose_Stack SHALL expose port 8080 on the host mapped to port 8080 on the Backend_Container

### Requirement 6: Environment Variable Configuration

**User Story:** As a developer, I want configuration values passed into containers via environment variables, so that no credentials or endpoints are hardcoded in images.

#### Acceptance Criteria

1. THE Compose_Stack SHALL pass the database file path to the Backend_Container via an environment variable
2. THE Compose_Stack SHALL pass the JWT secret to the Backend_Container via an environment variable
3. THE Compose_Stack SHALL pass the CORS allowed origins to the Backend_Container via an environment variable
4. THE Backend_Container SHALL use Spring Boot environment variable binding to override application.properties values at runtime

### Requirement 7: SQLite Data Persistence

**User Story:** As a developer, I want the SQLite database file persisted via a Docker volume, so that data survives container restarts and recreation.

#### Acceptance Criteria

1. THE Compose_Stack SHALL define a named volume for the SQLite database file
2. THE Compose_Stack SHALL mount the named volume to the directory containing the todo.db file inside the Backend_Container
3. WHEN the Backend_Container is stopped and restarted, THE Volume_Mount SHALL retain all previously stored data

### Requirement 8: Container Security

**User Story:** As a developer, I want all containers to run as non-root users, so that the attack surface is minimized if a container is compromised.

#### Acceptance Criteria

1. THE Frontend_Container SHALL create and use a Non_Root_User to run the Nginx process
2. THE Backend_Container SHALL create and use a Non_Root_User to run the Java process
3. THE Frontend_Container SHALL set appropriate file ownership for the Non_Root_User on Nginx directories
4. THE Backend_Container SHALL set appropriate file ownership for the Non_Root_User on the application directory and data volume
