# Implementation Plan: subtask-organization

## Overview

This plan delivers the Subtask Organization vertical slice: HTTP layer (`SubtaskController`)
with controller-local `@ExceptionHandler` methods (consistent with `TodoController` and
`LoginController`), and the full test suite (service and controller layers). The service layer
(`SubtaskService`) and repositories are already implemented. Tasks are ordered so each step
builds on the previous ones.

---

## Tasks

- [x] 1. Create `SubtaskController` in the `controller/` package
  - Create `controller/SubtaskController.java`
  - Annotate with `@RestController`, `@RequestMapping("/todos/{id}/subtasks")`, `@RequiredArgsConstructor`
  - Inject `SubtaskService` via a final field
  - [x] 1.1 Implement `GET /todos/{id}/subtasks`
    - Accept `@RequestAttribute UUID userId` and `@PathVariable UUID id`
    - Delegate to `subtaskService.getSubtasksForTask(userId, id)`
    - Return `ResponseEntity.ok(subtasks)`
    - _Requirements: 1.1_
  - [x] 1.2 Implement `POST /todos/{id}/subtasks`
    - Accept `@RequestAttribute UUID userId`, `@PathVariable UUID id`, `@RequestBody Subtask subtask`
    - Delegate to `subtaskService.createSubtask(userId, id, subtask)`
    - Return `ResponseEntity.ok(savedSubtask)`
    - _Requirements: 2.1_
  - [x] 1.3 Implement `GET /todos/{id}/subtasks/{subtaskId}`
    - Accept `@RequestAttribute UUID userId`, `@PathVariable UUID id`, `@PathVariable UUID subtaskId`
    - Delegate to `subtaskService.getSubtaskById(userId, id, subtaskId)`
    - Return `ResponseEntity.ok(subtask)`
    - _Requirements: 3.1_
  - [x] 1.4 Implement `PUT /todos/{id}/subtasks/{subtaskId}`
    - Accept `@RequestAttribute UUID userId`, `@PathVariable UUID id`, `@PathVariable UUID subtaskId`, `@RequestBody Subtask updates`
    - Delegate to `subtaskService.updateSubtask(userId, id, subtaskId, updates)`
    - Return `ResponseEntity.ok(updatedSubtask)`
    - _Requirements: 4.1_
  - [x] 1.5 Implement `DELETE /todos/{id}/subtasks/{subtaskId}`
    - Accept `@RequestAttribute UUID userId`, `@PathVariable UUID id`, `@PathVariable UUID subtaskId`
    - Delegate to `subtaskService.deleteSubtask(userId, id, subtaskId)`
    - Return `ResponseEntity.noContent().build()`
    - _Requirements: 5.1_
  - [x] 1.6 Add controller-local `@ExceptionHandler` methods
    - Add `@ExceptionHandler(SubtaskNotFoundException.class)` → HTTP 404, body `{"status": 404, "message": "..."}`
    - Add `@ExceptionHandler(TaskNotFoundException.class)` → HTTP 404, body `{"status": 404, "message": "..."}`
    - Add `@ExceptionHandler(TaskOwnershipException.class)` → HTTP 403, body `{"status": 403, "message": "..."}`
    - Add `@ExceptionHandler(IllegalArgumentException.class)` → HTTP 400, body `{"status": 400, "message": "..."}`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 2. Checkpoint — compile check
  - Run `.\gradlew build -x test` and confirm the project compiles without errors
  - Ensure all imports in `SubtaskController` resolve

- [x] 3. Write `SubtaskServiceTest`
  - Create `test/.../SubtaskServiceTest.java`
  - Annotate with `@ExtendWith(MockitoExtension.class)`
  - Mock `SubtaskRepository` and `TaskRepository`
  - [x] 3.1 Create subtask — valid title
    - Stub `taskRepository.findById` to return an owned task
    - Stub `subtaskRepository.save` to return the input subtask with a generated ID
    - Assert `save` called once and returned subtask has the correct `taskId`
    - _Requirements: 2.2_
  - [x] 3.2 Create subtask — blank title
    - Stub `taskRepository.findById` to return an owned task
    - Assert `createSubtask` throws `IllegalArgumentException`
    - Assert `subtaskRepository.save` is never called
    - _Requirements: 2.3_
  - [x] 3.3 Create subtask — unknown parent task
    - Stub `taskRepository.findById` to return `Optional.empty()`
    - Assert `createSubtask` throws `TaskNotFoundException`
    - _Requirements: 2.5_
  - [x] 3.4 Create subtask — wrong owner
    - Stub `taskRepository.findById` to return a task with a different `userId`
    - Assert `createSubtask` throws `TaskOwnershipException`
    - _Requirements: 2.6_
  - [x] 3.5 List subtasks
    - Stub `taskRepository.findById` to return an owned task
    - Stub `subtaskRepository.findAllByTaskId` to return a list
    - Assert the returned list matches
    - _Requirements: 1.2_
  - [x] 3.6 Get subtask by ID — not found
    - Stub `taskRepository.findById` to return an owned task
    - Stub `subtaskRepository.findById` to return `Optional.empty()`
    - Assert `getSubtaskById` throws `SubtaskNotFoundException`
    - _Requirements: 3.2_
  - [x] 3.7 Get subtask by ID — wrong parent task
    - Stub `subtaskRepository.findById` to return a subtask with a different `taskId`
    - Assert `getSubtaskById` throws `SubtaskNotFoundException`
    - _Requirements: 3.3_
  - [x] 3.8 Update subtask — valid fields
    - Stub repositories to return a valid owned task and existing subtask
    - Assert `save` called once with updated title and completed values
    - _Requirements: 4.2, 4.4_
  - [x] 3.9 Update subtask — blank title
    - Assert `updateSubtask` throws `IllegalArgumentException`
    - Assert `subtaskRepository.save` is never called
    - _Requirements: 4.3_
  - [x] 3.10 Delete subtask — success
    - Stub repositories to return a valid owned task and existing subtask
    - Assert `subtaskRepository.delete` called once with the correct subtask
    - _Requirements: 5.2_
  - [x] 3.11 Delete subtask — unknown parent task
    - Stub `taskRepository.findById` to return `Optional.empty()`
    - Assert `deleteSubtask` throws `TaskNotFoundException`
    - _Requirements: 5.4_
  - [x] 3.12 Delete subtask — wrong owner
    - Stub `taskRepository.findById` to return a task with a different `userId`
    - Assert `deleteSubtask` throws `TaskOwnershipException`
    - _Requirements: 5.5_

- [ ] 4. Write `SubtaskControllerTest`
  - Create `test/.../SubtaskControllerTest.java`
  - Annotate with `@WebMvcTest(SubtaskController.class)`
  - Declare `@MockBean SubtaskService subtaskService`
  - Inject `MockMvc` via `@Autowired`
  - [ ] 4.1 GET /todos/{id}/subtasks → HTTP 200, JSON array
    - Mock `subtaskService.getSubtasksForTask` to return a list
    - Assert status 200 and response is a JSON array
    - _Requirements: 1.1_
  - [ ] 4.2 POST /todos/{id}/subtasks — valid body → HTTP 200
    - Mock `subtaskService.createSubtask` to return a subtask
    - Assert status 200 and response body contains `id`, `taskId`, `title`, `completed`
    - _Requirements: 2.1_
  - [ ] 4.3 POST /todos/{id}/subtasks — blank title → HTTP 400
    - Mock `subtaskService.createSubtask` to throw `IllegalArgumentException`
    - Assert status 400 and response body contains `"status": 400`
    - _Requirements: 2.3, 2.4_
  - [ ] 4.4 GET /todos/{id}/subtasks/{subtaskId} — not found → HTTP 404
    - Mock `subtaskService.getSubtaskById` to throw `SubtaskNotFoundException`
    - Assert status 404 and response body contains `"status": 404`
    - _Requirements: 3.2, 3.4_
  - [ ] 4.5 GET /todos/{id}/subtasks — unknown parent task → HTTP 404
    - Mock `subtaskService.getSubtasksForTask` to throw `TaskNotFoundException`
    - Assert status 404
    - _Requirements: 1.4_
  - [ ] 4.6 GET /todos/{id}/subtasks — wrong owner → HTTP 403
    - Mock `subtaskService.getSubtasksForTask` to throw `TaskOwnershipException`
    - Assert status 403 and response body contains `"status": 403`
    - _Requirements: 1.5_
  - [ ] 4.7 PUT /todos/{id}/subtasks/{subtaskId} — valid body → HTTP 200
    - Mock `subtaskService.updateSubtask` to return the updated subtask
    - Assert status 200
    - _Requirements: 4.1_
  - [ ] 4.8 DELETE /todos/{id}/subtasks/{subtaskId} → HTTP 204
    - Mock `subtaskService.deleteSubtask` to do nothing
    - Assert status 204 and no response body
    - _Requirements: 5.1_

- [ ] 5. Final checkpoint — full test suite
  - Run `gradlew test` and confirm all tests pass
  - Fix any compilation or assertion errors before marking complete

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4", "1.5", "1.6"] },
    { "id": 1, "tasks": ["2"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8", "3.9", "3.10", "3.11", "3.12"] },
    { "id": 3, "tasks": ["4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8"] },
    { "id": 4, "tasks": ["5"] }
  ]
}
```
