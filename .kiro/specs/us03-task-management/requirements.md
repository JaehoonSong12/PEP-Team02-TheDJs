# Requirements Document

## Introduction

This feature implements the Task Management vertical slice for the Todo Management Application.
It covers the HTTP layer (`TodoController`), business logic (`TaskService`), exception classes,
and global exception handling — all within the existing Spring Boot 4.1.0 / Spring Data JPA stack.

The public endpoints delivered are:
- `POST /api/todos`        — create a new task
- `GET /api/todos`         — list all tasks for the authenticated user
- `GET /api/todos/{id}`    — retrieve a single task
- `PUT /api/todos/{id}`    — update a task's title and/or completed status
- `DELETE /api/todos/{id}` — delete a task (cascade-deletes its subtasks first)

---

## Glossary

- **TodoController**: The Spring `@RestController` that handles requests to `/api/todos`.
- **TaskService**: The existing Spring `@Service` class containing all business logic for task CRUD.
- **TaskRepository**: The Spring Data JPA interface for `Task` persistence operations.
- **SubtaskRepository**: The Spring Data JPA interface used by `TaskService` to cascade-delete subtasks.
- **Task**: The existing JPA entity (`entity/Task.java`) with fields `UUID id`, `UUID userId`, `String title`, `boolean completed`.
- **TaskNotFoundException**: Custom exception thrown when a task ID does not exist in the database.
- **TaskOwnershipException**: Custom exception thrown when the authenticated user does not own the requested task.
- **GlobalExceptionHandler**: A `@ControllerAdvice` class that maps exceptions to HTTP error responses.
- **userId**: The UUID of the authenticated user, extracted from the JWT and passed to the service layer.
- **blank**: A string that is `null`, empty (`""`), or contains only whitespace.
- **cascade delete**: When a task is deleted, all subtasks with a matching `taskId` are deleted first.

---

## Requirements

### Requirement 1: Create Task

**User Story:** As a user, I can create a new todo item so that I can start tracking a piece of work.

#### Acceptance Criteria

1. WHEN a `POST /api/todos` request is received with a valid JSON body containing a non-blank `title`, THE `TodoController` SHALL delegate to `TaskService.createTask(userId, task)` and return HTTP 200 with the persisted `Task` as the response body.
2. WHEN `TaskService.createTask` is called with a non-blank `title`, THE `TaskService` SHALL set `id` to `null`, set `userId` to the provided value, persist via `TaskRepository`, and return the saved `Task`.
3. WHEN `TaskService.createTask` is called with a blank `title`, THE `TaskService` SHALL throw an `IllegalArgumentException` before any database access occurs.
4. IF an `IllegalArgumentException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 400 with body `{"status": 400, "message": "..."}`.
5. WHEN the `completed` field is omitted, THE `TaskService` SHALL default it to `false`.

---

### Requirement 2: List Tasks

**User Story:** As a user, I can retrieve all of my todo items so that I can see my outstanding work.

#### Acceptance Criteria

1. WHEN a `GET /api/todos` request is received, THE `TodoController` SHALL delegate to `TaskService.getTasksForUser(userId)` and return HTTP 200 with a JSON array of the user's tasks.
2. WHEN `TaskService.getTasksForUser` is called, THE `TaskService` SHALL return only tasks whose `userId` matches the authenticated user's ID.
3. WHEN the user has no tasks, THE `TodoController` SHALL return HTTP 200 with an empty JSON array.

---

### Requirement 3: Retrieve Single Task

**User Story:** As a user, I can retrieve a single todo item by ID so that I can view its details.

#### Acceptance Criteria

1. WHEN a `GET /api/todos/{id}` request is received with a valid task ID, THE `TodoController` SHALL delegate to `TaskService.getTaskById(userId, taskId)` and return HTTP 200 with the matching `Task`.
2. WHEN `TaskService.getTaskById` is called with a non-existent ID, THE `TaskService` SHALL throw `TaskNotFoundException`.
3. WHEN `TaskService.getTaskById` is called with an ID belonging to a different user, THE `TaskService` SHALL throw `TaskOwnershipException`.
4. IF a `TaskNotFoundException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 404 with body `{"status": 404, "message": "..."}`.
5. IF a `TaskOwnershipException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 403 with body `{"status": 403, "message": "..."}`.

---

### Requirement 4: Update Task

**User Story:** As a user, I can edit a todo item so that I can correct its title or mark it as completed.

#### Acceptance Criteria

1. WHEN a `PUT /api/todos/{id}` request is received with a valid JSON body, THE `TodoController` SHALL delegate to `TaskService.updateTask(userId, taskId, updates)` and return HTTP 200 with the updated `Task`.
2. WHEN `TaskService.updateTask` is called with a non-null `title`, THE `TaskService` SHALL update the task's title to that value.
3. WHEN `TaskService.updateTask` is called with an explicit blank `title`, THE `TaskService` SHALL throw an `IllegalArgumentException` before persisting.
4. WHEN `TaskService.updateTask` is called, THE `TaskService` SHALL apply the `completed` value from the update object.
5. WHEN `TaskService.updateTask` is called with a non-existent ID, THE `TaskService` SHALL throw `TaskNotFoundException`.
6. WHEN `TaskService.updateTask` is called with an ID belonging to a different user, THE `TaskService` SHALL throw `TaskOwnershipException`.

---

### Requirement 5: Delete Task

**User Story:** As a user, I can delete a todo item so that I can remove work I no longer need to track.

#### Acceptance Criteria

1. WHEN a `DELETE /api/todos/{id}` request is received, THE `TodoController` SHALL delegate to `TaskService.deleteTask(userId, taskId)` and return HTTP 204 with no response body.
2. WHEN `TaskService.deleteTask` is called, THE `TaskService` SHALL first delete all subtasks whose `taskId` matches the given task ID, then delete the task itself.
3. WHEN `TaskService.deleteTask` is called with a non-existent ID, THE `TaskService` SHALL throw `TaskNotFoundException`.
4. WHEN `TaskService.deleteTask` is called with an ID belonging to a different user, THE `TaskService` SHALL throw `TaskOwnershipException`.

---

### Requirement 6: Ownership Enforcement

**User Story:** As a user, I can only access and modify my own todo items so that other users cannot see or change my data.

#### Acceptance Criteria

1. FOR every `GET`, `PUT`, and `DELETE` operation on `/api/todos/{id}`, THE `TaskService` SHALL verify that `task.getUserId()` equals the authenticated user's ID before returning or modifying data.
2. WHEN an ownership check fails, THE `TaskService` SHALL throw `TaskOwnershipException` regardless of the operation type.
3. THE `TodoController` SHALL extract the authenticated user's ID from the JWT on every request and pass it to the service.

---

### Requirement 7: Global Exception Handling

**User Story:** As a developer, I want task-related exceptions mapped to consistent HTTP responses so that API clients receive predictable error payloads.

#### Acceptance Criteria

1. WHEN a `TaskNotFoundException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 404 with a JSON body containing `"status": 404` and a non-empty `"message"` string.
2. WHEN a `TaskOwnershipException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 403 with a JSON body containing `"status": 403` and a non-empty `"message"` string.
3. WHEN an `IllegalArgumentException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 400 with a JSON body containing `"status": 400` and a non-empty `"message"` string.
4. THE `GlobalExceptionHandler` SHALL set `Content-Type: application/json` on all error responses.
