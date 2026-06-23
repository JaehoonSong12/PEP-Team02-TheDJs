# Requirements Document

## Introduction

This feature implements the Dashboard view for the Todo Management Application's Angular frontend.
The dashboard is the primary authenticated view that users arrive at after a successful login. It
provides full CRUD (create, read, update, delete) capabilities for tasks and subtasks, communicating
with the Spring Boot backend via the REST API endpoints defined in US03 (Task Management) and
US04 (Subtask Organization).

The dashboard is connected to the login page: upon successful authentication, the user is redirected
here. If the user is not authenticated (no valid JWT in storage), they are redirected back to the
login page.

All HTTP requests from the dashboard attach the JWT via an `Authorization: Bearer <token>` header,
handled by an Angular HTTP interceptor.

---

## Glossary

- **DashboardComponent**: The Angular standalone component that renders the authenticated user's task list and provides CRUD interactions for tasks and subtasks.
- **TaskService**: The Angular service (`task.service.ts`) responsible for HTTP communication with the backend task endpoints (`/api/todos`).
- **SubtaskService**: The Angular service (`subtask.service.ts`) responsible for HTTP communication with the backend subtask endpoints (`/api/todos/{id}/subtasks`).
- **AuthInterceptor**: The Angular HTTP interceptor that attaches the `Authorization: Bearer <token>` header to outgoing requests targeting protected API routes.
- **AuthGuard**: A route guard that checks for a valid JWT in localStorage; redirects to the login page if absent.
- **Task**: A TypeScript interface representing a task object: `{ id: string, userId: string, title: string, completed: boolean }`.
- **Subtask**: A TypeScript interface representing a subtask object: `{ id: string, taskId: string, title: string, completed: boolean }`.
- **JWT**: The JSON Web Token received from `POST /api/auth/login`, stored in `localStorage` under the key `token`.
- **blank**: A string that is `null`, `undefined`, empty (`""`), or contains only whitespace characters.

---

## Requirements

### Requirement 1: Route Configuration and Navigation

**User Story:** As an authenticated user, I want to be redirected to the dashboard after login, so that I can immediately see my tasks.

#### Acceptance Criteria

1. THE application SHALL define a route `/dashboard` that renders the `DashboardComponent`.
2. THE `/dashboard` route SHALL be protected by an `AuthGuard` that checks for a non-null JWT in `localStorage` under the key `token`.
3. WHEN the `AuthGuard` determines no valid token is present, THE guard SHALL redirect the user to the `/login` route and return `false`.
4. WHEN login succeeds (HTTP 200 with `Authorization` header from `POST /api/auth/login`), THE login component SHALL store the JWT in `localStorage` under the key `token` and navigate to `/dashboard`.
5. THE `DashboardComponent` SHALL provide a logout action that removes the `token` from `localStorage` and navigates the user to `/login`.

---

### Requirement 2: Display Tasks

**User Story:** As an authenticated user, I want to see all my tasks on the dashboard, so that I can track my outstanding work.

#### Acceptance Criteria

1. WHEN the `DashboardComponent` initializes, THE component SHALL call `TaskService.getTasks()` which issues `GET /api/todos` and display the returned tasks.
2. EACH task SHALL be rendered showing its `title` and `completed` status.
3. WHEN the backend returns an empty array, THE dashboard SHALL display a message indicating no tasks exist (e.g., "No tasks yet").
4. WHEN the backend returns an error (4xx or 5xx), THE dashboard SHALL display a user-friendly error message.
5. IF the backend returns HTTP 401, THE dashboard SHALL remove the stored token and redirect the user to `/login`.

---

### Requirement 3: Create Task

**User Story:** As an authenticated user, I want to create a new task from the dashboard, so that I can add work items to my list.

#### Acceptance Criteria

1. THE dashboard SHALL provide an input field and a submit mechanism (button or form submission) for creating a new task.
2. WHEN the user submits a non-blank title, THE `DashboardComponent` SHALL call `TaskService.createTask(task)` which issues `POST /api/todos` with body `{ "title": "<value>", "completed": false }`.
3. WHEN the backend returns HTTP 200 with the persisted task, THE dashboard SHALL add the new task to the displayed list without a full page reload.
4. WHEN the user attempts to submit a blank title, THE dashboard SHALL display a validation message and SHALL NOT issue the HTTP request.
5. WHEN the backend returns HTTP 400 (blank title rejected server-side), THE dashboard SHALL display the error message from the response body.

---

### Requirement 4: Update Task

**User Story:** As an authenticated user, I want to edit a task's title and toggle its completed status, so that I can keep my task list accurate.

#### Acceptance Criteria

1. EACH task in the list SHALL provide a mechanism to edit its title (e.g., inline edit or edit mode).
2. EACH task in the list SHALL provide a checkbox or toggle to mark it as completed or not completed.
3. WHEN the user updates a task's title or completed status, THE `DashboardComponent` SHALL call `TaskService.updateTask(id, updates)` which issues `PUT /api/todos/{id}` with the updated fields.
4. WHEN the backend returns HTTP 200 with the updated task, THE dashboard SHALL reflect the changes in the displayed list.
5. WHEN the user attempts to save a blank title during editing, THE dashboard SHALL display a validation message and SHALL NOT issue the HTTP request.
6. WHEN the backend returns HTTP 404 (task not found), THE dashboard SHALL remove the task from the displayed list and show an informational message.
7. WHEN the backend returns HTTP 403 (ownership violation), THE dashboard SHALL display an error message indicating access is denied.

---

### Requirement 5: Delete Task

**User Story:** As an authenticated user, I want to delete a task, so that I can remove work I no longer need to track.

#### Acceptance Criteria

1. EACH task in the list SHALL provide a delete action (e.g., a delete button).
2. WHEN the user confirms deletion, THE `DashboardComponent` SHALL call `TaskService.deleteTask(id)` which issues `DELETE /api/todos/{id}`.
3. WHEN the backend returns HTTP 204, THE dashboard SHALL remove the task (and any displayed subtasks) from the list without a full page reload.
4. WHEN the backend returns HTTP 404 (task not found), THE dashboard SHALL remove the task from the displayed list.
5. WHEN the backend returns HTTP 403 (ownership violation), THE dashboard SHALL display an error message.

---

### Requirement 6: Display Subtasks

**User Story:** As an authenticated user, I want to see subtasks nested under their parent task, so that I can view the breakdown of my work.

#### Acceptance Criteria

1. EACH task in the list SHALL provide a mechanism to expand or reveal its subtasks.
2. WHEN the user expands a task, THE `DashboardComponent` SHALL call `SubtaskService.getSubtasks(taskId)` which issues `GET /api/todos/{id}/subtasks` and display the returned subtasks beneath the parent task.
3. EACH subtask SHALL be rendered showing its `title` and `completed` status.
4. WHEN the backend returns an empty array for subtasks, THE dashboard SHALL display a message (e.g., "No subtasks") or simply show no items beneath the parent.
5. WHEN the backend returns HTTP 404 (parent task not found), THE dashboard SHALL display an error and refresh the task list.

---

### Requirement 7: Create Subtask

**User Story:** As an authenticated user, I want to create subtasks under a task, so that I can break my work into smaller pieces.

#### Acceptance Criteria

1. WHEN a task is expanded, THE dashboard SHALL provide an input field and a submit mechanism for creating a new subtask under that task.
2. WHEN the user submits a non-blank title, THE `DashboardComponent` SHALL call `SubtaskService.createSubtask(taskId, subtask)` which issues `POST /api/todos/{id}/subtasks` with body `{ "title": "<value>", "completed": false }`.
3. WHEN the backend returns HTTP 200 with the persisted subtask, THE dashboard SHALL add the new subtask to the displayed subtask list without a full page reload.
4. WHEN the user attempts to submit a blank title, THE dashboard SHALL display a validation message and SHALL NOT issue the HTTP request.
5. WHEN the backend returns HTTP 400 (blank title), THE dashboard SHALL display the error message.

---

### Requirement 8: Update Subtask

**User Story:** As an authenticated user, I want to edit a subtask's title and toggle its completed status.

#### Acceptance Criteria

1. EACH subtask SHALL provide a mechanism to edit its title and toggle its completed status.
2. WHEN the user updates a subtask, THE `DashboardComponent` SHALL call `SubtaskService.updateSubtask(taskId, subtaskId, updates)` which issues `PUT /api/todos/{id}/subtasks/{subtaskId}`.
3. WHEN the backend returns HTTP 200, THE dashboard SHALL reflect the updated subtask in the displayed list.
4. WHEN the user attempts to save a blank title, THE dashboard SHALL display a validation message and SHALL NOT issue the HTTP request.
5. WHEN the backend returns HTTP 404, THE dashboard SHALL remove the subtask from the displayed list.

---

### Requirement 9: Delete Subtask

**User Story:** As an authenticated user, I want to delete a subtask, so that I can remove sub-items I no longer need.

#### Acceptance Criteria

1. EACH subtask SHALL provide a delete action.
2. WHEN the user confirms deletion, THE `DashboardComponent` SHALL call `SubtaskService.deleteSubtask(taskId, subtaskId)` which issues `DELETE /api/todos/{id}/subtasks/{subtaskId}`.
3. WHEN the backend returns HTTP 204, THE dashboard SHALL remove the subtask from the displayed list.
4. WHEN the backend returns HTTP 404, THE dashboard SHALL remove the subtask from the displayed list.

---

### Requirement 10: HTTP Interceptor

**User Story:** As a developer, I want all API requests to automatically include the JWT, so that the dashboard does not need to manually attach auth headers on every call.

#### Acceptance Criteria

1. THE `AuthInterceptor` SHALL be registered as an Angular HTTP interceptor via `provideHttpClient(withInterceptors([...]))` in the application config.
2. FOR every outgoing HTTP request to the backend API, THE `AuthInterceptor` SHALL read the JWT from `localStorage` (key `token`) and attach it as the header `Authorization: Bearer <token>`.
3. IF no token is present in `localStorage`, THE `AuthInterceptor` SHALL send the request without the `Authorization` header (allowing public endpoints like login/register to work).
4. WHEN any HTTP response returns status 401, THE `AuthInterceptor` SHALL remove the stored token and redirect the user to `/login`.

---

### Requirement 11: TypeScript Interfaces

**User Story:** As a developer, I want shared TypeScript interfaces for API data models, so that type safety is maintained across components and services.

#### Acceptance Criteria

1. THE application SHALL define a `Task` interface: `{ id: string; userId: string; title: string; completed: boolean; }`.
2. THE application SHALL define a `Subtask` interface: `{ id: string; taskId: string; title: string; completed: boolean; }`.
3. THE `TaskService` and `SubtaskService` SHALL use these interfaces as the generic type parameter in `HttpClient` calls for type-safe responses.
