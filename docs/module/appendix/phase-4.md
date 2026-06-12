# Phase 4: Frontend Development

## Objective
The goal of this phase is to build a responsive and interactive user interface. This phase focuses on the "interface" of the application: consuming the Spring Boot API, managing user authentication state via JWT, and creating a seamless user experience for task management.

## Checkpoints
- [ ] **Project Scaffolding:** Initialize Angular and configure environment settings.
- [ ] **Service Layer:** Build Angular services to consume the Spring Boot API.
- [ ] **Auth Integration:** Implement login/register forms and JWT-based state management.
- [ ] **Task Management UI:** Build the dashboard, task creation forms, and subtask nested lists.
- [ ] **Design Validation:** Confirm the UI components and user workflows match the Phase 2 wireframes.

## Technical Requirements

### 1. Authentication & JWT Handling
*   **Token Storage:** Securely store the JWT (e.g., in `localStorage` or a state management store) upon successful login.
*   **Interceptors:** Implement an Angular `HttpInterceptor` to automatically attach the `Authorization: Bearer <token>` header to all outgoing API requests.
*   **Route Guards:** Implement `CanActivate` guards to prevent unauthenticated users from accessing the Task Dashboard.
*   **Logout Logic:** Implement a clear logout flow that clears the token and redirects the user to the login page.

### 2. API Integration
*   **Asynchronous Operations:** Use RxJS Observables to handle all API calls to ensure the UI remains responsive during data fetching.
*   **Error Handling:** Implement user-friendly error messages (e.g., "Invalid credentials" or "Task not found") based on the HTTP status codes returned by the backend.

### 3. User Interface (UI)
*   **Responsiveness:** Ensure the layout is functional across different screen sizes.
*   **Component Hierarchy:** Follow the component structure defined during the Phase 2 design to maintain code modularity.

## Deliverables for Phase 4
By the end of this phase, your team should have:
1.  **Functional Angular Application:** A complete, running frontend integrated with the backend.
2.  **End-to-End Workflow:** A user can successfully register, log in, manage todos, and manage subtasks without errors.
3.  **Verified UI/UX:** A UI that matches the wireframes and provides a smooth, intuitive user experience.

## Moving to Phase 5
Once the frontend is fully integrated and all user stories are functional, proceed to **Phase 5: Presentation**.

---

[Return to README](../README.md)