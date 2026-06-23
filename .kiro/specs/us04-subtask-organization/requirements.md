# Requirements Document

## Introduction

This feature implements the Subtask Organization vertical slice for the Todo Management Application.
It covers the HTTP layer (`SubtaskController`), business logic (`SubtaskService`), exception classes,
and global exception handling — all within the existing Spring Boot 4.1.0 / Spring Data JPA stack.

The public endpoints delivered are:
- `GET /api/todos/{id}/subtasks`               — list all subtasks for a task
- `POST /api/todos/{id}/subtasks`              — create a new subtask under a task
- `GET /api/todos/{id}/subtasks/{subtaskId}`   — retrieve a single subtask
- `PUT /api/todos/{id}/subtasks/{subtaskId}`   — update a subtask's title and/or completed status
- `DELETE /api/todos/{id}/subtasks/{subtaskId}` — delete a subtask

All subtask operations first verify that the parent task exists and belongs to the authenticated user.

---

## Glossary

- **SubtaskController**: The Spring `@RestController` that handles requests to `/api/todos/{id}/subtasks`.
- **SubtaskService**: The existing Spring `@Service` class containing all business logic for subtask CRUD.
- **SubtaskRepository**: The Spring Data JPA interface for `Subtask` persistence operations.
- **TaskRepository**: Used by `SubtaskService` to verify parent task existence and ownership.
- **Subtask**: The existing JPA entity (`entity/Subtask.java`) with fields `UUID id`, `UUID taskId`, `String title`, `boolean completed`.
- **SubtaskNotFoundException**: Custom exception thrown when a subtask ID does not exist or does not belong to the given parent task.
- **TaskNotFoundException**: Custom exception thrown when the parent task ID does not exist.
- **TaskOwnershipException**: Custom exception thrown when the authenticated user does not own the parent task.
- **GlobalExceptionHandler**: A `@ControllerAdvice` class that maps exceptions to HTTP error responses.
- **userId**: The UUID of the authenticated user, extracted from the JWT and passed to the service layer.
- **taskId**: The UUID of the parent task, extracted from the URL path variable `{id}`.
- **blank**: A string that is `null`, empty (`""`), or contains only whitespace.

---

## Requirements

### Requirement 1: List Subtasks

**User Story:** As a user, I can retrieve all subtasks for a todo item so that I can see how it is broken down.

#### Acceptance Criteria

1. WHEN a `GET /api/todos/{id}/subtasks` request is received, THE `SubtaskController` SHALL delegate to `SubtaskService.getSubtasksForTask(userId, taskId)` and return HTTP 200 with a JSON array of subtasks.
2. WHEN `SubtaskService.getSubtasksForTask` is called, THE `SubtaskService` SHALL verify the parent task exists and belongs to the authenticated user before querying subtasks.
3. WHEN the parent task has no subtasks, THE `SubtaskController` SHALL return HTTP 200 with an empty JSON array.
4. WHEN the parent task does not exist, THE `SubtaskService` SHALL throw `TaskNotFoundException`.
5. WHEN the parent task belongs to a different user, THE `SubtaskService` SHALL throw `TaskOwnershipException`.

---

### Requirement 2: Create Subtask

**User Story:** As a user, I can create a subtask under a todo item so that I can break it into smaller pieces of work.

#### Acceptance Criteria

1. WHEN a `POST /api/todos/{id}/subtasks` request is received with a valid JSON body containing a non-blank `title`, THE `SubtaskController` SHALL delegate to `SubtaskService.createSubtask(userId, taskId, subtask)` and return HTTP 200 with the persisted `Subtask` as the response body.
2. WHEN `SubtaskService.createSubtask` is called with a non-blank `title`, THE `SubtaskService` SHALL set `id` to `null`, set `taskId` to the provided value, persist via `SubtaskRepository`, and return the saved `Subtask`.
3. WHEN `SubtaskService.createSubtask` is called with a blank `title`, THE `SubtaskService` SHALL throw an `IllegalArgumentException` before any database access occurs.
4. IF an `IllegalArgumentException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 400 with body `{"status": 400, "message": "..."}`.
5. WHEN the parent task does not exist, THE `SubtaskService` SHALL throw `TaskNotFoundException` before attempting to persist.
6. WHEN the parent task belongs to a different user, THE `SubtaskService` SHALL throw `TaskOwnershipException` before attempting to persist.
7. WHEN the `completed` field is omitted, THE `SubtaskService` SHALL default it to `false`.

---

### Requirement 3: Retrieve Single Subtask

**User Story:** As a user, I can retrieve a single subtask by ID so that I can view its details.

#### Acceptance Criteria

1. WHEN a `GET /api/todos/{id}/subtasks/{subtaskId}` request is received, THE `SubtaskController` SHALL delegate to `SubtaskService.getSubtaskById(userId, taskId, subtaskId)` and return HTTP 200 with the matching `Subtask`.
2. WHEN `SubtaskService.getSubtaskById` is called with a non-existent subtask ID, THE `SubtaskService` SHALL throw `SubtaskNotFoundException`.
3. WHEN `SubtaskService.getSubtaskById` is called with a subtask ID that belongs to a different parent task, THE `SubtaskService` SHALL throw `SubtaskNotFoundException`.
4. IF a `SubtaskNotFoundException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 404 with body `{"status": 404, "message": "..."}`.
5. WHEN the parent task does not exist, THE `SubtaskService` SHALL throw `TaskNotFoundException`.
6. WHEN the parent task belongs to a different user, THE `SubtaskService` SHALL throw `TaskOwnershipException`.

---

### Requirement 4: Update Subtask

**User Story:** As a user, I can edit a subtask so that I can correct its title or mark it as completed.

#### Acceptance Criteria

1. WHEN a `PUT /api/todos/{id}/subtasks/{subtaskId}` request is received with a valid JSON body, THE `SubtaskController` SHALL delegate to `SubtaskService.updateSubtask(userId, taskId, subtaskId, updates)` and return HTTP 200 with the updated `Subtask`.
2. WHEN `SubtaskService.updateSubtask` is called with a non-null `title`, THE `SubtaskService` SHALL update the subtask's title to that value.
3. WHEN `SubtaskService.updateSubtask` is called with an explicit blank `title`, THE `SubtaskService` SHALL throw an `IllegalArgumentException` before persisting.
4. WHEN `SubtaskService.updateSubtask` is called, THE `SubtaskService` SHALL apply the `completed` value from the update object.
5. WHEN `SubtaskService.updateSubtask` is called with a non-existent subtask ID, THE `SubtaskService` SHALL throw `SubtaskNotFoundException`.
6. WHEN the parent task does not exist, THE `SubtaskService` SHALL throw `TaskNotFoundException`.
7. WHEN the parent task belongs to a different user, THE `SubtaskService` SHALL throw `TaskOwnershipException`.

---

### Requirement 5: Delete Subtask

**User Story:** As a user, I can delete a subtask so that I can remove work I no longer need.

#### Acceptance Criteria

1. WHEN a `DELETE /api/todos/{id}/subtasks/{subtaskId}` request is received, THE `SubtaskController` SHALL delegate to `SubtaskService.deleteSubtask(userId, taskId, subtaskId)` and return HTTP 204 with no response body.
2. WHEN `SubtaskService.deleteSubtask` is called with a valid owned task and existing subtask, THE `SubtaskService` SHALL delete the subtask and return without error.
3. WHEN `SubtaskService.deleteSubtask` is called with a non-existent subtask ID, THE `SubtaskService` SHALL throw `SubtaskNotFoundException`.
4. WHEN the parent task does not exist, THE `SubtaskService` SHALL throw `TaskNotFoundException`.
5. WHEN the parent task belongs to a different user, THE `SubtaskService` SHALL throw `TaskOwnershipException`.

---

### Requirement 6: Ownership Enforcement

**User Story:** As a user, I can only access and modify subtasks that belong to my own todo items.

#### Acceptance Criteria

1. FOR every subtask operation, THE `SubtaskService` SHALL verify the parent task's `userId` matches the authenticated user's ID before performing any subtask read or write.
2. WHEN an ownership check fails, THE `SubtaskService` SHALL throw `TaskOwnershipException` regardless of the operation type.
3. THE `SubtaskController` SHALL extract the authenticated user's ID from the JWT on every request and pass it to the service layer.

---

### Requirement 7: Global Exception Handling

**User Story:** As a developer, I want subtask-related exceptions mapped to consistent HTTP responses.

#### Acceptance Criteria

1. WHEN a `TaskNotFoundException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 404 with a JSON body containing `"status": 404` and a non-empty `"message"` string.
2. WHEN a `TaskOwnershipException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 403 with a JSON body containing `"status": 403` and a non-empty `"message"` string.
3. WHEN a `SubtaskNotFoundException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 404 with a JSON body containing `"status": 404` and a non-empty `"message"` string.
4. WHEN an `IllegalArgumentException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 400 with a JSON body containing `"status": 400` and a non-empty `"message"` string.
5. THE `GlobalExceptionHandler` SHALL set `Content-Type: application/json` on all error responses.
