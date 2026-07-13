# Requirements Document

## Introduction

This specification defines a comprehensive JUnit 5 unit test suite for the Todo Management Spring Boot backend. The test suite validates business logic, security components, and controller endpoint behavior in complete isolation using Mockito mocks and Spring WebMvc test slices. The suite complements the existing SubtaskServiceTest (12 tests) by covering the remaining service, security, and controller layers.

## Glossary

- **Test_Suite**: The complete collection of JUnit 5 test classes produced by this feature
- **Service_Test**: A unit test class that validates business logic using @ExtendWith(MockitoExtension.class) with mocked repository dependencies
- **Security_Test**: A unit test class that validates security components (PasswordValidator, JwtUtil, AuthInterceptor) in isolation
- **Controller_Test**: A unit test class that validates REST controller behavior using @WebMvcTest slice with mocked service dependencies
- **MockMvc**: Spring test utility that simulates HTTP requests against a controller without starting a full server
- **PasswordValidator**: Component that checks password strength against seven independent rules and returns a list of violation messages
- **JwtUtil**: Component that generates, parses, and validates JSON Web Tokens using HMAC-SHA256 signing
- **AuthInterceptor**: Spring HandlerInterceptor that enforces Bearer token authentication on protected endpoints
- **RegistrationService**: Service that validates username/password fields in strict order, checks for duplicates, and persists new users
- **UserService**: Service that authenticates users by verifying username and password against stored credentials
- **TaskService**: Service that performs CRUD operations on tasks with ownership enforcement and cascade deletion of subtasks
- **Property_Test**: A test using the jqwik library (@Property annotation) that verifies invariants hold across randomly generated inputs
- **STRICT_STUBS**: Default Mockito behavior under MockitoExtension that fails tests containing unused stubbing

## Requirements

### Requirement 1: RegistrationService Unit Tests

**User Story:** As a developer, I want isolated unit tests for RegistrationService, so that I can verify the registration validation order, username rules, password delegation, duplicate checking, and persistence logic independently.

#### Acceptance Criteria

1. WHEN a User with a null or blank username is passed to registerUser, THE Service_Test SHALL verify that RegistrationService throws RegistrationFailure with a message containing "blank", and that neither PasswordValidator.getViolations nor any UserRepository method is invoked
2. WHEN a User with a username shorter than 5 characters is passed to registerUser, THE Service_Test SHALL verify that RegistrationService throws RegistrationFailure with a message containing "between 5 and 18", and that neither PasswordValidator.getViolations nor any UserRepository method is invoked
3. WHEN a User with a username longer than 18 characters is passed to registerUser, THE Service_Test SHALL verify that RegistrationService throws RegistrationFailure with a message containing "between 5 and 18", and that neither PasswordValidator.getViolations nor any UserRepository method is invoked
4. WHEN a User with a valid-length username (5 to 18 characters) but null or blank password is passed to registerUser, THE Service_Test SHALL verify that RegistrationService throws RegistrationFailure with a message containing "blank", and that no UserRepository method is invoked
5. WHEN a User with a valid username and a non-blank password is passed to registerUser and the PasswordValidator mock's getViolations method is stubbed to return a non-empty list of violation strings, THE Service_Test SHALL verify that RegistrationService throws RegistrationFailure whose message contains each violation string returned by the mock, and that no UserRepository method is invoked
6. WHEN a User with valid credentials is passed to registerUser and the PasswordValidator mock's getViolations is stubbed to return an empty list and the UserRepository mock's existsByUsername is stubbed to return true, THE Service_Test SHALL verify that RegistrationService throws RegistrationFailure with a message containing "already taken", and that UserRepository.save is never invoked
7. WHEN a User with valid credentials is passed to registerUser and the PasswordValidator mock's getViolations is stubbed to return an empty list and the UserRepository mock's existsByUsername is stubbed to return false, THE Service_Test SHALL verify that UserRepository.save is invoked exactly once with that User argument
8. THE Service_Test SHALL verify that no UserRepository method is called when field validation (steps 1-4) fails, confirming validation order precedes database access

### Requirement 2: UserService Unit Tests

**User Story:** As a developer, I want isolated unit tests for UserService, so that I can verify login authentication logic handles all credential scenarios correctly.

#### Acceptance Criteria

1. WHEN a User with a username that exists in the mocked UserRepository and a password that equals the stored User's password is passed to login, THE Service_Test SHALL verify that UserService returns the same User entity retrieved from the repository (not the credentials object)
2. WHEN a User with a null or blank username is passed to login, THE Service_Test SHALL verify that UserService throws InvalidCredentialsException without invoking UserRepository.findByUsername
3. WHEN a User with a null or blank password is passed to login, THE Service_Test SHALL verify that UserService throws InvalidCredentialsException without invoking UserRepository.findByUsername
4. WHEN a User with a username that causes UserRepository.findByUsername to return an empty Optional is passed to login, THE Service_Test SHALL verify that UserService throws InvalidCredentialsException
5. WHEN a User with a valid username but a password that does not equal the stored User's password is passed to login, THE Service_Test SHALL verify that UserService throws InvalidCredentialsException
6. WHEN login is called with valid credentials, THE Service_Test SHALL verify that UserRepository.findByUsername is invoked exactly once with the provided username

### Requirement 3: TaskService Unit Tests

**User Story:** As a developer, I want isolated unit tests for TaskService, so that I can verify CRUD operations, ownership enforcement, cascade delete behavior, and input validation.

#### Acceptance Criteria

1. WHEN a Task with a non-null, non-blank title is passed to createTask, THE Service_Test SHALL verify that TaskService sets the userId on the Task and invokes TaskRepository.save exactly once
2. WHEN a Task with a null or blank title is passed to createTask, THE Service_Test SHALL verify that TaskService throws IllegalArgumentException and does not invoke TaskRepository.save
3. WHEN getTasksForUser is called, THE Service_Test SHALL verify that TaskService delegates to TaskRepository.findAllByUserId and returns the result
4. WHEN getTaskById is called with a taskId that exists and belongs to the requesting userId, THE Service_Test SHALL verify that TaskService returns the Task
5. WHEN getTaskById is called with a taskId that does not exist, THE Service_Test SHALL verify that TaskService throws TaskNotFoundException
6. WHEN getTaskById is called with a taskId that belongs to a different user, THE Service_Test SHALL verify that TaskService throws TaskOwnershipException
7. WHEN updateTask is called with a non-null, non-blank title, THE Service_Test SHALL verify that TaskService updates the title on the existing Task and invokes TaskRepository.save exactly once
8. WHEN updateTask is called with an explicit blank title (empty string or whitespace-only), THE Service_Test SHALL verify that TaskService throws IllegalArgumentException and does not invoke TaskRepository.save
9. WHEN updateTask is called with a null title, THE Service_Test SHALL verify that TaskService does not modify the existing title and invokes TaskRepository.save with the title unchanged
10. WHEN deleteTask is called for an owned task, THE Service_Test SHALL verify that TaskService invokes SubtaskRepository to delete all subtasks for the given taskId, then invokes TaskRepository to delete the Task, in that order
11. WHEN deleteTask is called for a task belonging to a different user, THE Service_Test SHALL verify that TaskService throws TaskOwnershipException without invoking any delete operation on SubtaskRepository or TaskRepository
12. WHEN updateTask is called with a taskId belonging to a different user, THE Service_Test SHALL verify that TaskService throws TaskOwnershipException without invoking TaskRepository.save

### Requirement 4: PasswordValidator Unit Tests

**User Story:** As a developer, I want isolated unit tests for PasswordValidator, so that I can verify each of the seven password strength rules independently, boundary conditions, and multiple-violation aggregation.

#### Acceptance Criteria

1. WHEN a password shorter than 8 characters (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the minimum-length violation message "Password must be at least 8 characters long."
2. WHEN a password longer than 72 characters (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the maximum-length violation message "Password must be no more than 72 characters long."
3. WHEN a password containing no uppercase letter (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the uppercase violation message "Password must contain at least one uppercase letter."
4. WHEN a password containing no lowercase letter (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the lowercase violation message "Password must contain at least one lowercase letter."
5. WHEN a password containing no digit (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the digit violation message "Password must contain at least one digit."
6. WHEN a password containing no special character from the set !@#$%^&* (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the special-character violation message "Password must contain at least one special character (!@#$%^&*)."
7. WHEN a password containing whitespace (that satisfies all other rules) is passed to getViolations, THE Security_Test SHALL verify that the returned list contains the whitespace violation message "Password must not contain whitespace."
8. WHEN a password satisfying all seven rules is passed to getViolations, THE Security_Test SHALL verify that the returned list is empty
9. WHEN a password violating multiple rules simultaneously is passed to getViolations, THE Security_Test SHALL verify that the returned list size equals the number of violated rules and contains one message per violated rule
10. THE Security_Test SHALL use @ParameterizedTest with @MethodSource to verify boundary-length passwords (exactly 8 characters and exactly 72 characters) produce an empty violations list when all other rules are satisfied
11. FOR ALL passwords matching PASSWORD_REGEX, THE Property_Test SHALL verify that getViolations returns an empty list (round-trip invariant between regex and rule engine)

### Requirement 5: JwtUtil Unit Tests

**User Story:** As a developer, I want isolated unit tests for JwtUtil, so that I can verify token generation, claim extraction, expiration handling, and tamper detection.

#### Acceptance Criteria

1. WHEN generateToken is called with a User entity (non-null username and UUID id), THE Security_Test SHALL verify that the returned token is non-null, has a length of at least 1 character, and contains exactly two period characters (three Base64URL-encoded segments)
2. WHEN extractUsername is called with a valid token, THE Security_Test SHALL verify that the returned username matches the User's username used during generation
3. WHEN extractUserId is called with a valid token, THE Security_Test SHALL verify that the returned userId matches the User's id formatted via UUID.toString() used during generation
4. WHEN extractUsername is called with a token signed by a different valid 32-byte HS256 secret, THE Security_Test SHALL verify that null is returned
5. WHEN extractUsername is called with a token generated by a JwtUtil instance configured with an expiry of 0 milliseconds and at least 1 millisecond has elapsed since generation, THE Security_Test SHALL verify that null is returned
6. WHEN isTokenValid is called with a valid token and the correct username, THE Security_Test SHALL verify that true is returned
7. WHEN isTokenValid is called with a valid token but a username that differs from the token's subject, THE Security_Test SHALL verify that false is returned
8. WHEN isTokenValid is called with a valid token and a null username argument, THE Security_Test SHALL verify that false is returned
9. FOR ALL User entities where username is a non-blank String of 1 to 255 characters and id is a non-null UUID (supplied via a jqwik Arbitrary provider), THE Property_Test SHALL verify that extractUsername(generateToken(user)) equals user.getUsername()
10. WHEN extractUserId is called with a token signed by a different valid 32-byte HS256 secret, THE Security_Test SHALL verify that null is returned

### Requirement 6: AuthInterceptor Unit Tests

**User Story:** As a developer, I want isolated unit tests for AuthInterceptor, so that I can verify the CORS bypass, header validation, token validation, and request attribute injection logic.

#### Acceptance Criteria

1. WHEN an HTTP OPTIONS request is received, THE Security_Test SHALL verify that AuthInterceptor returns true without inspecting the Authorization header
2. WHEN a request has no Authorization header, THE Security_Test SHALL verify that AuthInterceptor sets HTTP 401 status on the MockHttpServletResponse and returns false
3. WHEN a request has an Authorization header that does not start with "Bearer ", THE Security_Test SHALL verify that AuthInterceptor sets HTTP 401 status on the MockHttpServletResponse and returns false
4. WHEN a request has a Bearer token and the JwtUtil mock's extractUsername is stubbed to return null, THE Security_Test SHALL verify that AuthInterceptor sets HTTP 401 status and returns false
5. WHEN a request has a Bearer token and extractUsername returns a non-null username but the JwtUtil mock's isTokenValid(token, username) is stubbed to return false, THE Security_Test SHALL verify that AuthInterceptor sets HTTP 401 status and returns false
6. WHEN a request has a valid Bearer token (extractUsername returns non-null username and isTokenValid returns true), THE Security_Test SHALL verify that AuthInterceptor sets the "userId" request attribute to the value returned by JwtUtil.extractUserId
7. WHEN a request has a valid Bearer token (extractUsername returns non-null username and isTokenValid returns true), THE Security_Test SHALL verify that AuthInterceptor sets the "username" request attribute to the value returned by JwtUtil.extractUsername
8. WHEN a request has a valid Bearer token, THE Security_Test SHALL verify that AuthInterceptor returns true
9. WHEN AuthInterceptor rejects a request (criteria 2-5), THE Security_Test SHALL verify that no request attributes are set on the MockHttpServletRequest
10. THE Security_Test SHALL verify that AuthInterceptor passes the extracted username to isTokenValid as the second argument (correct wiring verification via verify(jwtUtil).isTokenValid(token, extractedUsername))

### Requirement 7: LoginController Unit Tests

**User Story:** As a developer, I want @WebMvcTest slice tests for LoginController, so that I can verify HTTP response codes and the Authorization header on login success and failure.

#### Acceptance Criteria

1. WHEN a POST to /api/auth/login with a JSON body containing username and password succeeds, THE Controller_Test SHALL verify that the response status is 200, the response body is empty, and the Authorization header starts with "Bearer "
2. WHEN a POST to /api/auth/login triggers InvalidCredentialsException, THE Controller_Test SHALL verify that the response status is 401 and the response body contains the exception message as plain text
3. THE Controller_Test SHALL mock UserService and JwtUtil as @MockBean dependencies and exclude AuthInterceptor and WebConfig from the slice context

### Requirement 8: RegistrationController Unit Tests

**User Story:** As a developer, I want @WebMvcTest slice tests for RegistrationController, so that I can verify HTTP response codes for registration success, validation failure, and data conflicts.

#### Acceptance Criteria

1. WHEN a POST to /api/auth/register with a JSON body containing username and password succeeds, THE Controller_Test SHALL verify that the response status is 201 and the response body is empty
2. WHEN a POST to /api/auth/register triggers RegistrationFailure, THE Controller_Test SHALL verify that the response status is 400 and the response body contains the exception message as plain text
3. WHEN a POST to /api/auth/register triggers DataIntegrityViolationException, THE Controller_Test SHALL verify that the response status is 409 and the response body contains a conflict indication as plain text
4. THE Controller_Test SHALL mock RegistrationService as a @MockBean dependency and exclude AuthInterceptor and WebConfig from the slice context

### Requirement 9: TodoController Unit Tests

**User Story:** As a developer, I want @WebMvcTest slice tests for TodoController, so that I can verify CRUD status codes and exception-to-HTTP-status mapping.

#### Acceptance Criteria

1. WHEN a POST to /api/todos with a JSON Task body and a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains the created Task as JSON
2. WHEN a GET to /api/todos with a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains a JSON array of Tasks
3. WHEN a GET to /api/todos/{id} with a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains the Task as JSON
4. WHEN a PUT to /api/todos/{id} with a JSON Task body and a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains the updated Task as JSON
5. WHEN a DELETE to /api/todos/{id} with a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 204 and the response body is empty
6. WHEN TaskService throws TaskNotFoundException, THE Controller_Test SHALL verify that the response status is 404 and the body contains a JSON object with "status" and "message" fields
7. WHEN TaskService throws TaskOwnershipException, THE Controller_Test SHALL verify that the response status is 403 and the body contains a JSON object with "status" and "message" fields
8. WHEN TaskService throws IllegalArgumentException, THE Controller_Test SHALL verify that the response status is 400 and the body contains a JSON object with "status" and "message" fields
9. THE Controller_Test SHALL mock TaskService as a @MockBean dependency and exclude AuthInterceptor and WebConfig from the slice context

### Requirement 10: SubtaskController Unit Tests

**User Story:** As a developer, I want @WebMvcTest slice tests for SubtaskController, so that I can verify CRUD status codes and exception-to-HTTP-status mapping for subtask operations.

#### Acceptance Criteria

1. WHEN a GET to /api/todos/{id}/subtasks with a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains a JSON array of Subtasks
2. WHEN a POST to /api/todos/{id}/subtasks with a JSON Subtask body and a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains the created Subtask as JSON
3. WHEN a GET to /api/todos/{id}/subtasks/{subtaskId} with a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains the Subtask as JSON
4. WHEN a PUT to /api/todos/{id}/subtasks/{subtaskId} with a JSON Subtask body and a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 200 and the body contains the updated Subtask as JSON
5. WHEN a DELETE to /api/todos/{id}/subtasks/{subtaskId} with a requestAttr "userId" set to a UUID succeeds, THE Controller_Test SHALL verify that the response status is 204 and the response body is empty
6. WHEN SubtaskService throws TaskNotFoundException, THE Controller_Test SHALL verify that the response status is 404 and the body contains a JSON object with "status" and "message" fields
7. WHEN SubtaskService throws SubtaskNotFoundException, THE Controller_Test SHALL verify that the response status is 404 and the body contains a JSON object with "status" and "message" fields
8. WHEN SubtaskService throws TaskOwnershipException, THE Controller_Test SHALL verify that the response status is 403 and the body contains a JSON object with "status" and "message" fields
9. WHEN SubtaskService throws IllegalArgumentException, THE Controller_Test SHALL verify that the response status is 400 and the body contains a JSON object with "status" and "message" fields
10. THE Controller_Test SHALL mock SubtaskService as a @MockBean dependency and exclude AuthInterceptor and WebConfig from the slice context

### Requirement 11: TaskService Ownership Property Test

**User Story:** As a developer, I want a property-based test for TaskService ownership enforcement, so that I can verify the invariant holds across arbitrary user and task ID combinations.

#### Acceptance Criteria

1. FOR ALL arbitrary pairs of requestingUserId and taskOwnerId where requestingUserId does not equal taskOwnerId, WHEN TaskRepository is stubbed to return a Task whose userId field equals taskOwnerId and TaskService.getTaskById is invoked with (requestingUserId, taskId), THE Property_Test SHALL verify that TaskOwnershipException is thrown
2. THE Property_Test SHALL execute a minimum of 100 jqwik tries (the jqwik default) per @Property method
3. THE Property_Test SHALL use @ExtendWith(MockitoExtension.class) with @Mock TaskRepository and @InjectMocks TaskService, consistent with the project unit-test conventions

### Requirement 12: Test Infrastructure Constraints

**User Story:** As a developer, I want all unit tests to adhere to project testing conventions, so that the test suite is fast, deterministic, and maintainable.

#### Acceptance Criteria

1. THE Test_Suite SHALL use @ExtendWith(MockitoExtension.class) for all service and security test classes (no Spring context boot)
2. THE Test_Suite SHALL use @WebMvcTest for all controller test classes with service dependencies declared as @MockBean
3. THE Test_Suite SHALL place test classes in packages mirroring source packages: com.revature.todomanagement.service, com.revature.todomanagement.security, and com.revature.todomanagement.controller
4. THE Test_Suite SHALL name test methods using the pattern methodUnderTest_stateOrInput_expectedBehavior
5. THE Test_Suite SHALL use @Nested classes to group related test contexts within each test class
6. THE Test_Suite SHALL use @DisplayName annotations to provide human-readable descriptions for test classes and nested groups
7. THE Test_Suite SHALL use STRICT_STUBS mode (the MockitoExtension default) with no lenient stubs unless a shared @BeforeEach setup configures stubs consumed by only a subset of tests in the class
8. THE Test_Suite SHALL use @ParameterizedTest with @MethodSource for data-driven validation scenarios in PasswordValidator tests
