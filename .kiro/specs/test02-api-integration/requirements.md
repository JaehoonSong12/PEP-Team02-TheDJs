# Requirements Document

## Introduction

This document specifies requirements for a comprehensive REST Assured integration test suite that exercises the full API surface of the Todo Management application. The test suite translates 45+ manually-verified curl test scenarios into automated, repeatable JUnit 5 tests using REST Assured's BDD-style DSL. The suite runs against an embedded Spring Boot server with an H2 in-memory database, ensuring complete isolation from production data.

## Glossary

- **Test_Suite**: The complete collection of REST Assured integration test classes that verify the Todo Management REST API
- **Base_Test**: An abstract class (`BaseIntegrationTest`) providing shared REST Assured configuration, port injection, and helper methods for all test classes
- **Auth_Helper**: A reusable method within Base_Test that registers a user and logs in, returning a valid JWT token for authenticated requests
- **JWT_Token**: A JSON Web Token returned in the `Authorization` response header after successful login, used as a Bearer token for protected endpoints
- **Test_User**: A dynamically-created user account used within a test class to authenticate and perform API operations
- **Task**: A top-level todo item owned by a specific user, containing an id, userId, title, and completed flag
- **Subtask**: A child item nested under a Task, containing an id, taskId, title, and completed flag
- **Ownership_Check**: Server-side enforcement that a user can only access Task and Subtask resources they own
- **Cascade_Delete**: Server behavior where deleting a Task also removes all Subtask records associated with it
- **Validation_Error**: A 400 response returned when request payload violates business rules (blank title, weak password, etc.)
- **REST_Assured**: A Java DSL library (version 6.0.0) for testing RESTful APIs using a given-when-then BDD syntax
- **H2_Database**: An in-memory relational database used during test execution to isolate tests from production SQLite storage
- **RequestSpecification**: A REST Assured object that bundles base URI, port, content type, and authorization header into a reusable request template

## Requirements

### Requirement 1: Test Infrastructure and Base Configuration

**User Story:** As a developer, I want an abstract base test class that configures REST Assured with the embedded server's random port and H2 database, so that all integration tests share consistent setup without duplication.

#### Acceptance Criteria

1. THE Base_Test SHALL be annotated with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and `@TestPropertySource(locations = "classpath:test.properties")`
2. WHEN a test class extends Base_Test, THE Base_Test SHALL inject the random server port using `@LocalServerPort` and configure REST Assured's `baseURI` to `http://localhost` and `port` to the injected value before each test method
3. THE Base_Test SHALL enable logging of request and response details only when assertion validation fails
4. WHEN all tests in a subclass complete, THE Base_Test SHALL reset REST Assured's `baseURI`, `port`, `basePath`, and request filters to their default values
5. THE Base_Test SHALL provide an Auth_Helper method that accepts a username and password, sends a POST request with a JSON body containing those credentials to `/api/auth/register`, then sends a POST request with the same JSON body to `/api/auth/login`, and returns the extracted JWT_Token as a plain token string
6. WHEN the Auth_Helper method is called, THE Base_Test SHALL extract the token from the `Authorization` response header by stripping the `Bearer ` prefix (first 7 characters)
7. IF the Auth_Helper registration request returns a non-201 status or the login request returns a non-200 status, THEN THE Base_Test SHALL throw an AssertionError indicating which step failed

### Requirement 2: User Registration Validation Tests

**User Story:** As a developer, I want automated tests that verify all registration validation rules, so that input boundary enforcement is continuously validated.

#### Acceptance Criteria

1. WHEN a registration request has a blank username, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Username must not be blank."
2. WHEN a registration request has a username shorter than 5 characters, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Username must be between 5 and 18 characters."
3. WHEN a registration request has a username longer than 18 characters, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Username must be between 5 and 18 characters."
4. WHEN a registration request has a blank password, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Password must not be blank."
5. WHEN a registration request has a password without a special character from the set !@#$%^&*, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Password must contain at least one special character (!@#$%^&*)."
6. WHEN a registration request has a username between 5 and 18 characters and a password containing at least one special character from !@#$%^&* and the username does not already exist, THE Test_Suite SHALL verify the API returns HTTP 201 with an empty response body.
7. WHEN a registration request uses a username that was successfully registered by a prior request in the same test, THE Test_Suite SHALL verify the API returns HTTP 400 with a message containing "is already taken."

### Requirement 3: Authentication Tests

**User Story:** As a developer, I want automated tests that verify login behavior for valid and invalid credentials, so that authentication logic is continuously validated.

#### Acceptance Criteria

1. WHEN the test suite starts the authentication test group, THE Test_Suite SHALL register a user via POST /api/auth/register to serve as the known-valid credential set for subsequent login tests
2. WHEN a login request is sent with the registered username and matching password, THE Test_Suite SHALL verify the API returns HTTP 200 with an empty response body and a non-empty `Authorization` header whose value starts with "Bearer " followed by at least 20 characters
3. WHEN a login request is sent with the registered username and a password that does not match, THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing the message "Invalid username or password"
4. WHEN a login request is sent with a username that was never registered, THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing the message "Invalid username or password"
5. WHEN a login request is sent with an empty string for both username and password, THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing the message "Invalid username or password"

### Requirement 4: Auth Interceptor and Token Validation Tests

**User Story:** As a developer, I want automated tests that verify token validation at the interceptor level, so that unauthorized access is continuously blocked.

#### Acceptance Criteria

1. WHEN a request to a protected endpoint has no Authorization header, THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing "Missing or malformed Authorization header"
2. WHEN a request to a protected endpoint has an Authorization header containing a JWT with an invalid signature, THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing "Invalid or expired token"
3. WHEN a request to a protected endpoint has a malformed Authorization header (not prefixed with "Bearer "), THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing "Missing or malformed Authorization header"
4. WHEN a request to a protected endpoint has an Authorization header containing an expired JWT, THE Test_Suite SHALL verify the API returns HTTP 401 with a response body containing "Invalid or expired token"
5. WHEN a request to a protected endpoint has an Authorization header containing a valid, non-expired JWT with correct signature, THE Test_Suite SHALL verify the API does not return HTTP 401 and the request proceeds to the controller

### Requirement 5: Task CRUD Tests

**User Story:** As a developer, I want automated tests that verify all task create, read, update, and delete operations, so that core task management functionality is continuously validated.

#### Acceptance Criteria

1. WHEN an authenticated user creates a task with a valid title (1 to 255 characters, not blank or whitespace-only), THE Test_Suite SHALL verify the API returns HTTP 200 with a JSON body containing a non-null id, the user's userId, the submitted title, and completed set to false
2. WHEN an authenticated user creates a task with a blank title (empty string or whitespace-only), THE Test_Suite SHALL verify the API returns HTTP 400 with message "Task title must not be blank."
3. WHEN an authenticated user requests the list of tasks, THE Test_Suite SHALL verify the API returns HTTP 200 with a JSON array containing all tasks owned by that user and no tasks owned by other users
4. WHEN an authenticated user requests a task by its valid ID, THE Test_Suite SHALL verify the API returns HTTP 200 with the correct task JSON object
5. WHEN an authenticated user requests a task by a non-existent ID, THE Test_Suite SHALL verify the API returns HTTP 404 with a message containing "Task not found"
6. WHEN an authenticated user updates a task with a valid title and completed flag, THE Test_Suite SHALL verify the API returns HTTP 200 with the updated title and updated completed value in the response body
7. WHEN an authenticated user updates a task with a blank title, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Task title must not be blank."
8. WHEN an authenticated user deletes a task by its valid ID, THE Test_Suite SHALL verify the API returns HTTP 204 with an empty body
9. WHEN an authenticated user deletes a task that has already been deleted, THE Test_Suite SHALL verify the API returns HTTP 404 with a message containing "Task not found"
10. WHEN an authenticated user deletes a task by its valid ID, THE Test_Suite SHALL verify that a subsequent GET request for the same task ID returns HTTP 404 with a message containing "Task not found"

### Requirement 6: Subtask CRUD Tests

**User Story:** As a developer, I want automated tests that verify all subtask create, read, update, and delete operations, so that nested subtask management is continuously validated.

#### Acceptance Criteria

1. WHEN an authenticated user creates a subtask under an existing task with a title between 1 and 255 characters, THE Test_Suite SHALL verify the API returns HTTP 200 with a JSON body containing a non-null UUID id, the parent taskId matching the path parameter, the submitted title, and completed set to false
2. WHEN an authenticated user creates a subtask with a blank title, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Subtask title must not be blank."
3. WHEN an authenticated user requests the list of subtasks for a task that has one or more subtasks, THE Test_Suite SHALL verify the API returns HTTP 200 with a JSON array containing all subtasks belonging to that task, each element matching the subtask JSON structure
4. WHEN an authenticated user requests the list of subtasks for a task that has zero subtasks, THE Test_Suite SHALL verify the API returns HTTP 200 with an empty JSON array
5. WHEN an authenticated user requests a subtask by its valid ID, THE Test_Suite SHALL verify the API returns HTTP 200 with the correct subtask JSON object including id, taskId, title, and completed fields
6. WHEN an authenticated user requests a subtask by a non-existent ID, THE Test_Suite SHALL verify the API returns HTTP 404 with a message containing "Subtask not found"
7. WHEN an authenticated user updates a subtask with a valid title and a completed flag, THE Test_Suite SHALL verify the API returns HTTP 200 with the updated title and completed values in the response body
8. WHEN an authenticated user updates a subtask with a blank title, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Subtask title must not be blank."
9. WHEN an authenticated user deletes a subtask by its valid ID, THE Test_Suite SHALL verify the API returns HTTP 204 with an empty body
10. WHEN an authenticated user deletes a subtask that has already been deleted, THE Test_Suite SHALL verify the API returns HTTP 404 with a message containing "Subtask not found"
11. WHEN an authenticated user requests subtasks for a non-existent task ID, THE Test_Suite SHALL verify the API returns HTTP 404 with a message containing "Task not found"

### Requirement 7: Ownership Enforcement Tests

**User Story:** As a developer, I want automated tests that verify cross-user access is denied, so that data isolation between users is continuously validated.

#### Acceptance Criteria

1. WHEN the test suite initializes the ownership enforcement scenario, THE Test_Suite SHALL register and authenticate two separate users (User A and User B), then create at least one task as User A, confirming the task creation returns HTTP 200 with a valid task ID before proceeding with cross-access attempts
2. WHEN User B attempts to GET a task owned by User A, THE Test_Suite SHALL verify the API returns HTTP 403 with a JSON body containing a "message" field that includes the text "does not own task"
3. WHEN User B attempts to UPDATE a task owned by User A, THE Test_Suite SHALL verify the API returns HTTP 403 with a JSON body containing a "message" field that includes the text "does not own task"
4. WHEN User B attempts to DELETE a task owned by User A, THE Test_Suite SHALL verify the API returns HTTP 403 with a JSON body containing a "message" field that includes the text "does not own task", and THE Test_Suite SHALL subsequently verify that User A can still retrieve the task with HTTP 200 confirming the task was not deleted
5. WHEN User B attempts to list subtasks of a task owned by User A, THE Test_Suite SHALL verify the API returns HTTP 403 with a JSON body containing a "message" field that includes the text "does not own task"
6. WHEN User B lists their own tasks, THE Test_Suite SHALL verify the API returns HTTP 200 with an empty JSON array, confirming no data leakage from User A's tasks into User B's task list

### Requirement 8: Cascade Delete Tests

**User Story:** As a developer, I want automated tests that verify deleting a task also removes its subtasks, so that referential integrity is continuously validated.

#### Acceptance Criteria

1. WHEN an authenticated user has a task with at least one subtask, THE Test_Suite SHALL verify that GET /api/todos/{id}/subtasks returns HTTP 200 with an array containing at least 1 element before deletion
2. WHEN an authenticated user sends DELETE /api/todos/{id} for a task that has subtasks, THE Test_Suite SHALL verify the API returns HTTP 204 with an empty response body
3. WHEN the parent task has been deleted via cascade, THE Test_Suite SHALL verify that GET /api/todos/{id} for the deleted task returns HTTP 404
4. WHEN the parent task has been deleted via cascade, THE Test_Suite SHALL verify that GET /api/todos no longer contains the deleted task ID in the response array
5. WHEN the parent task has been deleted via cascade, THE Test_Suite SHALL verify that GET /api/todos/{id}/subtasks for the deleted task returns HTTP 404, confirming all subtasks were removed

### Requirement 9: Business Rule — No Subtasks on Completed Task

**User Story:** As a developer, I want an automated test that verifies the business rule preventing subtask creation on completed tasks, so that this invariant is continuously validated.

#### Acceptance Criteria

1. WHEN an authenticated user sends a PUT request to /api/todos/{id} with completed set to true for a task that exists with completed set to false, THE Test_Suite SHALL verify the API returns HTTP 200 with completed set to true in the response body
2. WHEN an authenticated user sends a POST request to /api/todos/{id}/subtasks with a valid non-empty title for a task that has completed set to true, THE Test_Suite SHALL verify the API returns HTTP 400 with message "Cannot add subtasks to a completed task."
3. WHEN an authenticated user sends a POST request to /api/todos/{id}/subtasks with a valid non-empty title for a task that has completed set to false, THE Test_Suite SHALL verify the API returns HTTP 200 confirming that subtask creation succeeds on non-completed tasks

### Requirement 10: Test Isolation and Data Independence

**User Story:** As a developer, I want each test class to operate with independent data, so that tests can run in any order without side effects.

#### Acceptance Criteria

1. THE Test_Suite SHALL use H2_Database configured with `create-drop` schema generation and `DB_CLOSE_DELAY=-1` so that each test class application context starts with an empty schema containing no rows
2. THE Test_Suite SHALL create at least one dedicated Test_User account (with a unique username and valid password) within each test class setup, rather than depending on pre-existing data or accounts created by other test classes
3. THE Test_Suite SHALL extract dynamic resource IDs (task ID, subtask ID) from POST creation response bodies and use those extracted IDs in subsequent GET, PUT, and DELETE requests within the same test flow, never using hardcoded UUID values
4. IF a test depends on a prior state (such as a task existing before deletion), THEN THE Test_Suite SHALL establish that state within the same test method or within an ordered test sequence using JUnit 5 `@TestMethodOrder(OrderAnnotation.class)` with `@TestInstance(Lifecycle.PER_CLASS)`
5. IF a preceding step in an ordered test sequence fails, THEN THE Test_Suite SHALL skip dependent subsequent steps in that sequence rather than executing them against an invalid state

### Requirement 11: Test Organization and Structure

**User Story:** As a developer, I want the test suite organized into logically-separated test classes, so that test failures can be quickly traced to the relevant API domain.

#### Acceptance Criteria

1. THE Test_Suite SHALL organize tests into exactly one class per API domain, using the following class names: `RegistrationIT` (registration), `AuthenticationIT` (authentication), `AuthInterceptorIT` (auth interceptor), `TaskCrudIT` (task CRUD), `SubtaskCrudIT` (subtask CRUD), `OwnershipIT` (ownership), `CascadeDeleteIT` (cascade delete), and `BusinessRuleIT` (business rules)
2. THE Test_Suite SHALL place all test classes under the package `com.revature.todomanagement` in the test source tree
3. THE Test_Suite SHALL name each test method using camelCase that combines the action or scenario with the expected outcome, separated by an underscore (e.g., `createTaskWithBlankTitle_returns400`)
4. THE Test_Suite SHALL use REST Assured's BDD-style given-when-then syntax (calling `given()`, `when()`, and `then()` in sequence) for all request-response assertions
5. THE Test_Suite SHALL require every test class to extend `BaseIntegrationTest` to inherit shared REST Assured configuration and Auth_Helper methods
