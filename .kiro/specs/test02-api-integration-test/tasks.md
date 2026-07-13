# Implementation Plan: REST Assured Integration Test Suite

## Overview

This plan implements a comprehensive REST Assured integration test suite for the Todo Management API. The suite contains 8 test classes covering registration, authentication, token validation, task CRUD, subtask CRUD, ownership enforcement, cascade delete, and business rules. All tests run against an embedded Spring Boot server with H2 in-memory database for complete isolation.

## Tasks

- [ ] 1. Set up test infrastructure and base configuration
  - [ ] 1.1 Create test properties file
    - Ensure `src/test/resources/test.properties` contains: H2 datasource URL (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`), H2 driver class, H2 dialect, `create-drop` DDL auto, `spring.docker.compose.enabled=false`, `jwt.secret=TestSecretKeyThatIsAtLeast32CharactersLong!!`, and `cors.allowed-origins=http://localhost:4200`
    - _Requirements: 1.1, 10.1_

  - [ ] 1.2 Add REST Assured dependency to build.gradle.kts
    - Ensure `testImplementation("io.rest-assured:rest-assured:6.0.0")` is in the dependencies block
    - Ensure `org.seleniumhq.selenium:selenium-java` and `io.cucumber:cucumber-spring` use `testImplementation` scope (not `implementation`)
    - _Requirements: 1.1_

  - [ ] 1.3 Implement BaseIntegrationTest abstract class
    - Create `src/test/java/com/revature/todomanagement/BaseIntegrationTest.java`
    - Annotate with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and `@TestPropertySource(locations = "classpath:test.properties")`
    - Inject random port with `@LocalServerPort`
    - Implement `@BeforeEach setupRestAssured()` that sets `RestAssured.baseURI`, `RestAssured.port`, content type JSON, and enables logging on validation failure
    - Implement `@AfterAll teardownRestAssured()` that resets `baseURI`, `port`, `basePath`, and request filters
    - Implement `getAuthToken(String username, String password)` that POSTs to `/api/auth/register` (asserts 201), POSTs to `/api/auth/login` (asserts 200), extracts `Authorization` header, strips `Bearer ` prefix, and returns the token string
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

- [ ] 2. Implement registration validation tests
  - [ ] 2.1 Create RegistrationIT test class
    - Create `src/test/java/com/revature/todomanagement/RegistrationIT.java` extending `BaseIntegrationTest`
    - Implement 7 test methods: `blankUsername_returns400`, `shortUsername_returns400`, `longUsername_returns400`, `blankPassword_returns400`, `noSpecialCharPassword_returns400`, `validRegistration_returns201`, `duplicateUsername_returns400`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)` for the duplicate username test that depends on prior registration
    - Each test uses REST Assured BDD-style `given()`/`when()`/`then()` syntax
    - Assert exact error messages per requirements (e.g., "Username must not be blank.", "Username must be between 5 and 18 characters.", "Password must not be blank.", "Password must contain at least one special character (!@#$%^&*).", "is already taken.")
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ]* 2.2 Write property test for password special character rejection
    - **Property 1: Password without special character is rejected**
    - Use jqwik `@Property(tries = 100)` to generate passwords without characters from `!@#$%^&*`
    - Assert HTTP 400 with message containing "Password must contain at least one special character"
    - **Validates: Requirements 2.5**

- [ ] 3. Implement authentication tests
  - [ ] 3.1 Create AuthenticationIT test class
    - Create `src/test/java/com/revature/todomanagement/AuthenticationIT.java` extending `BaseIntegrationTest`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)` to register a user in setup before login tests
    - Implement test methods: `registerSetupUser_returns201`, `validLogin_returns200WithToken`, `wrongPassword_returns401`, `unknownUsername_returns401`, `emptyCredentials_returns401`
    - Verify valid login returns 200 with `Authorization` header starting with "Bearer " followed by 20+ characters
    - Verify invalid credentials return 401 with "Invalid username or password"
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ]* 3.2 Write property test for valid login token format
    - **Property 2: Valid login produces a well-formed Bearer token**
    - Use jqwik `@Property(tries = 20)` to generate valid username/password pairs, register, login, and verify token format
    - **Validates: Requirements 3.2**

- [ ] 4. Implement auth interceptor tests
  - [ ] 4.1 Create AuthInterceptorIT test class
    - Create `src/test/java/com/revature/todomanagement/AuthInterceptorIT.java` extending `BaseIntegrationTest`
    - Implement `noAuthHeader_returns401` — request to `/api/todos` with no Authorization header, assert 401 with "Missing or malformed Authorization header"
    - Implement `invalidSignature_returns401` — craft a JWT signed with a wrong secret using `Jwts.builder()` from JJWT, assert 401 with "Invalid or expired token"
    - Implement `malformedHeader_returns401` — send `"Token xyz"` as Authorization value, assert 401 with "Missing or malformed Authorization header"
    - Implement `expiredToken_returns401` — craft a JWT with `exp` in the past using JJWT, assert 401 with "Invalid or expired token"
    - Implement `validToken_proceeds` — use `getAuthToken()` to get a valid token, send request to `/api/todos`, assert response is NOT 401
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 5. Checkpoint - Verify base infrastructure and auth tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement task CRUD tests
  - [ ] 6.1 Create TaskCrudIT test class
    - Create `src/test/java/com/revature/todomanagement/TaskCrudIT.java` extending `BaseIntegrationTest`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)` to share created task IDs across ordered tests
    - Acquire token in `@BeforeAll` using `getAuthToken()`
    - Implement 10 test methods: `createWithValidTitle_returns200`, `createWithBlankTitle_returns400`, `listTasks_returnsUserTasksOnly`, `getById_returns200`, `getByNonExistentId_returns404`, `updateTask_returns200`, `updateWithBlankTitle_returns400`, `deleteTask_returns204`, `deleteAlreadyDeleted_returns404`, `deleteThenGet_returns404`
    - Extract task ID from creation response and use in subsequent GET/PUT/DELETE tests
    - Verify JSON structure: non-null `id`, correct `userId`, submitted `title`, `completed` = false
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 10.3_

  - [ ]* 6.2 Write property test for task creation with valid title
    - **Property 3: Task creation with valid title produces correct structure**
    - Use jqwik `@Property(tries = 50)` to generate non-blank strings (1-255 chars), POST to `/api/todos`, verify response contains non-null id, correct userId, submitted title verbatim, and completed = false
    - **Validates: Requirements 5.1**

  - [ ]* 6.3 Write property test for blank task title rejection
    - **Property 4: Blank or whitespace-only task title is rejected**
    - Use jqwik `@Property(tries = 50)` to generate empty/whitespace-only strings, POST to `/api/todos`, verify HTTP 400 with "Task title must not be blank."
    - **Validates: Requirements 5.2**

  - [ ]* 6.4 Write property test for task listing user isolation
    - **Property 5: Task listing returns only the authenticated user's tasks**
    - Register two users, create tasks for each, verify GET `/api/todos` for each user returns only their own tasks (all `userId` fields match)
    - **Validates: Requirements 5.3, 7.6**

- [ ] 7. Implement subtask CRUD tests
  - [ ] 7.1 Create SubtaskCrudIT test class
    - Create `src/test/java/com/revature/todomanagement/SubtaskCrudIT.java` extending `BaseIntegrationTest`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)`
    - Acquire token and create a parent task in `@BeforeAll`
    - Implement 11 test methods: `createWithValidTitle_returns200`, `createWithBlankTitle_returns400`, `listSubtasks_returnsAll`, `listSubtasksEmpty_returnsEmptyArray`, `getById_returns200`, `getByNonExistentId_returns404`, `updateSubtask_returns200`, `updateWithBlankTitle_returns400`, `deleteSubtask_returns204`, `deleteAlreadyDeleted_returns404`, `listSubtasksNonExistentTask_returns404`
    - Verify subtask JSON: non-null UUID `id`, matching `taskId`, submitted `title`, `completed` = false
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10, 6.11, 10.3_

  - [ ]* 7.2 Write property test for subtask creation with valid title
    - **Property 6: Subtask creation with valid title produces correct structure**
    - Use jqwik `@Property(tries = 50)` to generate non-blank strings (1-255 chars), POST to `/api/todos/{taskId}/subtasks`, verify correct structure
    - **Validates: Requirements 6.1**

  - [ ]* 7.3 Write property test for blank subtask title rejection
    - **Property 7: Blank or whitespace-only subtask title is rejected**
    - Use jqwik `@Property(tries = 50)` to generate empty/whitespace-only strings, POST to subtask endpoint, verify HTTP 400 with "Subtask title must not be blank."
    - **Validates: Requirements 6.2**

- [ ] 8. Checkpoint - Verify CRUD tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement ownership enforcement tests
  - [ ] 9.1 Create OwnershipIT test class
    - Create `src/test/java/com/revature/todomanagement/OwnershipIT.java` extending `BaseIntegrationTest`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)`
    - In `@BeforeAll`, register User A and User B, acquire tokens for both, create a task as User A
    - Implement `crossUserGet_returns403` — User B GETs User A's task, assert 403 with "does not own task"
    - Implement `crossUserUpdate_returns403` — User B PUTs User A's task, assert 403 with "does not own task"
    - Implement `crossUserDelete_returns403AndPreservesTask` — User B DELETEs User A's task, assert 403, then verify User A can still GET the task with 200
    - Implement `crossUserListSubtasks_returns403` — User B GETs subtasks of User A's task, assert 403 with "does not own task"
    - Implement `userBListOwnTasks_returnsEmpty` — User B GETs `/api/todos`, assert 200 with empty JSON array
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [ ]* 9.2 Write property test for non-owner access denial
    - **Property 8: Non-owner access to any task operation is denied**
    - For a task owned by User A, verify all operations (GET, PUT, DELETE, list subtasks) by User B return 403 with "does not own task", and task remains accessible to User A
    - **Validates: Requirements 7.2, 7.3, 7.4, 7.5**

- [ ] 10. Implement cascade delete tests
  - [ ] 10.1 Create CascadeDeleteIT test class
    - Create `src/test/java/com/revature/todomanagement/CascadeDeleteIT.java` extending `BaseIntegrationTest`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)`
    - In setup, authenticate, create a task, and create at least one subtask
    - Implement `subtasksExistBeforeDelete` — verify GET subtasks returns array with at least 1 element
    - Implement `deleteTaskWithSubtasks_returns204` — DELETE the parent task, assert 204
    - Implement `getDeletedTask_returns404` — GET the deleted task, assert 404
    - Implement `listTasksExcludesDeleted` — GET `/api/todos`, assert deleted task ID not in array
    - Implement `getSubtasksOfDeletedTask_returns404` — GET subtasks of deleted task, assert 404
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ]* 10.2 Write property test for cascade delete
    - **Property 9: Cascade delete removes all subtasks of a task**
    - Create a task with subtasks, delete the task, verify subtask endpoint returns 404
    - **Validates: Requirements 8.2, 8.3, 8.5**

- [ ] 11. Implement business rule tests
  - [ ] 11.1 Create BusinessRuleIT test class
    - Create `src/test/java/com/revature/todomanagement/BusinessRuleIT.java` extending `BaseIntegrationTest`
    - Use `@TestMethodOrder(OrderAnnotation.class)` and `@TestInstance(Lifecycle.PER_CLASS)`
    - In setup, authenticate and create a task
    - Implement `markTaskCompleted_returns200` — PUT task with `completed: true`, assert 200 with `completed: true`
    - Implement `createSubtaskOnCompleted_returns400` — POST subtask on completed task, assert 400 with "Cannot add subtasks to a completed task."
    - Implement `createSubtaskOnIncomplete_returns200` — create a new task (not completed), POST subtask, assert 200
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ]* 11.2 Write property test for subtask on completed task rejection
    - **Property 10: Subtask creation on a completed task is rejected**
    - Use jqwik to generate valid subtask titles, attempt creation on a completed task, verify HTTP 400 with "Cannot add subtasks to a completed task."
    - **Validates: Requirements 9.2**

- [ ] 12. Final checkpoint - Ensure full test suite passes
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik 1.9.3
- Unit tests validate specific examples and edge cases
- All test classes extend `BaseIntegrationTest` for consistent configuration
- Build command: `gradlew.bat test` (Windows) — runs all tests including unit tests (test01) and E2E (test03)
- To run ONLY integration tests: `gradlew.bat test --tests "*IT"`
- Test execution is ordered within classes using `@TestMethodOrder(OrderAnnotation.class)`
- Dynamic resource IDs are extracted from responses — never hardcoded
- The `test.properties` file is shared with test03 (E2E Cucumber tests) — ensure no conflicting properties
- Dependency scope fix: `selenium-java` and `cucumber-spring` should be `testImplementation` (done in task 1.2)
- **Execution order**: test01 (unit, no Spring context) → test02 (integration, embedded server) → test03 (E2E, requires external frontend)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3"] },
    { "id": 2, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 3, "tasks": ["2.2", "3.2"] },
    { "id": 4, "tasks": ["6.1", "7.1"] },
    { "id": 5, "tasks": ["6.2", "6.3", "6.4", "7.2", "7.3"] },
    { "id": 6, "tasks": ["9.1", "10.1", "11.1"] },
    { "id": 7, "tasks": ["9.2", "10.2", "11.2"] }
  ]
}
```
