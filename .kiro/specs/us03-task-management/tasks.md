# Implementation Plan: task-management

## Overview

This plan delivers the Task Management vertical slice: HTTP layer (`TodoController`), global
exception handling (`GlobalExceptionHandler`), and the full test suite (service and controller
layers). The service layer (`TaskService`) and repositories are already implemented.
Tasks are ordered so each step builds on the previous ones with no orphaned code.

---

## Tasks

- [ ] 1. Create `GlobalExceptionHandler` in the `exception/` package
  - Create `exception/GlobalExceptionHandler.java`
  - Annotate with `@RestControllerAdvice`
  - Handler for `TaskNotFoundException` → HTTP 404, body `{"status": 404, "message": "..."}`
  - Handler for `TaskOwnershipException` → HTTP 403, body `{"status": 403, "message": "..."}`
  - Handler for `IllegalArgumentException` → HTTP 400, body `{"status": 400, "message": "..."}`
  - Handler for `Exception` (catch-all) → HTTP 500, body `{"status": 500}`
  - Use `ResponseEntity<Map<String, Object>>` as the return type
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 2. Implement `TodoController` in the `controller/` package
  - Fill in the existing `controller/TodoController.java` shell
  - Annotate with `@RestController`, `@RequestMapping("/api/todos")`, `@RequiredArgsConstructor`
  - Inject `TaskService` via a final field
  - [ ] 2.1 Implement `POST /api/todos`
    - Accept `@RequestAttribute UUID userId` and `@RequestBody Task task`
    - Delegate to `taskService.createTask(userId, task)`
    - Return `ResponseEntity.ok(savedTask)`
    - _Requirements: 1.1_
  - [ ] 2.2 Implement `GET /api/todos`
    - Accept `@RequestAttribute UUID userId`
    - Delegate to `taskService.getTasksForUser(userId)`
    - Return `ResponseEntity.ok(tasks)`
    - _Requirements: 2.1, 2.3_
  - [ ] 2.3 Implement `GET /api/todos/{id}`
    - Accept `@RequestAttribute UUID userId` and `@PathVariable UUID id`
    - Delegate to `taskService.getTaskById(userId, id)`
    - Return `ResponseEntity.ok(task)`
    - _Requirements: 3.1_
  - [ ] 2.4 Implement `PUT /api/todos/{id}`
    - Accept `@RequestAttribute UUID userId`, `@PathVariable UUID id`, `@RequestBody Task updates`
    - Delegate to `taskService.updateTask(userId, id, updates)`
    - Return `ResponseEntity.ok(updatedTask)`
    - _Requirements: 4.1_
  - [ ] 2.5 Implement `DELETE /api/todos/{id}`
    - Accept `@RequestAttribute UUID userId` and `@PathVariable UUID id`
    - Delegate to `taskService.deleteTask(userId, id)`
    - Return `ResponseEntity.noContent().build()`
    - _Requirements: 5.1_

- [ ] 3. Checkpoint — compile check
  - Run `gradlew build -x test` and confirm the project compiles without errors
  - Ensure all imports in `TodoController` and `GlobalExceptionHandler` resolve

- [ ] 4. Write `TaskServiceTest`
  - Create `test/.../TaskServiceTest.java`
  - Annotate with `@ExtendWith(MockitoExtension.class)`
  - Mock `TaskRepository` and `SubtaskRepository`
  - [ ] 4.1 Create task — valid title
    - Stub `taskRepository.save` to return the input task with a generated ID
    - Assert `save` called once and returned task has the correct `userId`
    - _Requirements: 1.2_
  - [ ] 4.2 Create task — blank title
    - Assert `createTask` throws `IllegalArgumentException`
    - Assert `taskRepository.save` is never called
    - _Requirements: 1.3_
  - [ ] 4.3 List tasks
    - Stub `findAllByUserId` to return a list of tasks
    - Assert the returned list matches
    - _Requirements: 2.2_
  - [ ] 4.4 Get task by ID — not found
    - Stub `findById` to return `Optional.empty()`
    - Assert `getTaskById` throws `TaskNotFoundException`
    - _Requirements: 3.2_
  - [ ] 4.5 Get task by ID — wrong owner
    - Stub `findById` to return a task with a different `userId`
    - Assert `getTaskById` throws `TaskOwnershipException`
    - _Requirements: 3.3, 6.1, 6.2_
  - [ ] 4.6 Update task — valid fields
    - Stub `findById` to return an owned task, stub `save` to return updated task
    - Assert `save` called once with the updated title and completed values
    - _Requirements: 4.2, 4.4_
  - [ ] 4.7 Update task — blank title
    - Assert `updateTask` throws `IllegalArgumentException`
    - Assert `taskRepository.save` is never called
    - _Requirements: 4.3_
  - [ ] 4.8 Delete task — cascade
    - Stub `findById` to return an owned task
    - Stub `subtaskRepository.findAllByTaskId` to return a list of subtasks
    - Assert `subtaskRepository.deleteAll` is called before `taskRepository.delete`
    - _Requirements: 5.2_
  - [ ] 4.9 Delete task — not found
    - Stub `findById` to return `Optional.empty()`
    - Assert `deleteTask` throws `TaskNotFoundException`
    - _Requirements: 5.3_
  - [ ] 4.10 Delete task — wrong owner
    - Stub `findById` to return a task with a different `userId`
    - Assert `deleteTask` throws `TaskOwnershipException`
    - _Requirements: 5.4, 6.2_

- [ ] 5. Write `TodoControllerTest`
  - Create `test/.../TodoControllerTest.java`
  - Annotate with `@WebMvcTest(TodoController.class)`
  - Import `GlobalExceptionHandler` with `@Import(GlobalExceptionHandler.class)`
  - Declare `@MockBean TaskService taskService`
  - Inject `MockMvc` via `@Autowired`
  - [ ] 5.1 POST /api/todos — valid body → HTTP 200
    - Mock `taskService.createTask` to return a task
    - Assert status 200 and response body contains `id`, `title`, `completed`
    - _Requirements: 1.1_
  - [ ] 5.2 POST /api/todos — blank title → HTTP 400
    - Mock `taskService.createTask` to throw `IllegalArgumentException`
    - Assert status 400 and response body contains `"status": 400`
    - _Requirements: 1.3, 1.4_
  - [ ] 5.3 GET /api/todos → HTTP 200, JSON array
    - Mock `taskService.getTasksForUser` to return a list
    - Assert status 200 and response is a JSON array
    - _Requirements: 2.1_
  - [ ] 5.4 GET /api/todos/{id} — not found → HTTP 404
    - Mock `taskService.getTaskById` to throw `TaskNotFoundException`
    - Assert status 404 and response body contains `"status": 404`
    - _Requirements: 3.2, 3.4_
  - [ ] 5.5 GET /api/todos/{id} — wrong owner → HTTP 403
    - Mock `taskService.getTaskById` to throw `TaskOwnershipException`
    - Assert status 403 and response body contains `"status": 403`
    - _Requirements: 3.3, 3.5_
  - [ ] 5.6 PUT /api/todos/{id} — valid body → HTTP 200
    - Mock `taskService.updateTask` to return the updated task
    - Assert status 200
    - _Requirements: 4.1_
  - [ ] 5.7 DELETE /api/todos/{id} → HTTP 204
    - Mock `taskService.deleteTask` to do nothing
    - Assert status 204 and no response body
    - _Requirements: 5.1_

- [ ] 6. Final checkpoint — full test suite
  - Run `gradlew test` and confirm all tests pass
  - Fix any compilation or assertion errors before marking complete

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["3"] },
    { "id": 3, "tasks": ["4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9", "4.10"] },
    { "id": 4, "tasks": ["5.1", "5.2", "5.3", "5.4", "5.5", "5.6", "5.7"] },
    { "id": 5, "tasks": ["6"] }
  ]
}
```
