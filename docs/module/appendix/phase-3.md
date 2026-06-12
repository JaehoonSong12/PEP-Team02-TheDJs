# Phase 3: Backend Development

## Objective
The goal of this phase is to build a robust, secure REST API. This phase focuses on the "engine" of the application: data persistence, business logic, and security. The backend must be fully functional and testable via tools like Postman or cURL before the frontend development begins.

## Checkpoints
- [ ] **Project Scaffolding:** Initialize Gradle, Spring Boot, and SQLite configuration.
- [ ] **Data Layer:** Implement Spring Data JPA repositories and entity mappings.
- [ ] **Security Implementation:** Build the authentication and authorization logic
- [ ] **Business Logic:** Develop required CRUD services for all entities.
- [ ] **Design Validation:** Confirm all implemented endpoints and data models match Phase 1/2 specifications.

## Technical Requirements

### 1. API Compliance
All developed endpoints **must** match the API Contract defined in Phase 1/2. Any deviation from the agreed-upon JSON structure or endpoint path must be approved by the team before implementation.

### 2. Security Standards
*   **Authentication:** Ensure all protected routes (Todo/Subtask management) require a valid user session or token.
*   **Authorization:** Ensure users can only access, edit, or delete data that belongs to their own account.

### 3. Error Handling
The API must return meaningful HTTP status codes:
*   `200 OK` / `201 Created`: Successful operations.
*   `400 Bad Request`: Validation errors (e.g., missing required fields).
*   `401 Unauthorized`: Missing or invalid authentication.
*   `403 Forbidden`: Authenticated user attempting to access unauthorized data.
*   `404 Not Found`: Resource does not exist.

## Deliverables for Phase 3
By the end of this phase, your team should have:
1.  **Functional REST API:** A running Spring Boot application.
2.  **Verified Endpoints:** A collection of successful tests (via Postman, cURL, or similar) demonstrating all CRUD operations.
3.  **Persisted Data:** Evidence that data persists in the SQLite database after application restarts.

## Moving to Phase 4
Once the backend is stable and the API endpoints are verified, proceed to **Phase 4: Frontend Development**.

---

[Return to README](../README.md)