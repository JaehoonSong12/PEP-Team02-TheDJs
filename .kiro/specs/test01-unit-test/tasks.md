# Implementation Plan: Todo Management Unit Test Suite

## Overview

This plan implements a JUnit 5 unit test suite for the Todo Management Spring Boot backend. Tests are organized into service tests (Mockito-only), security tests (Mockito-only), property-based tests (jqwik 1.9.3), and one controller exception-handler edge case. Each task creates one or more test classes under `src/test/java/com/revature/todomanagement/` in the appropriate sub-package. HTTP endpoint behavior (status codes, CRUD operations, error responses) is covered by the separate API integration test suite (test02-api-integration-test).

## Tasks

- [ ] 1. Set up test package structure and verify jqwik dependency
  - [ ] 1.1 Create test sub-package directories and verify build configuration
    - Create directories: `src/test/java/com/revature/todomanagement/service/`, `src/test/java/com/revature/todomanagement/security/`
    - Verify `build.gradle.kts` includes jqwik 1.9.3 dependency and JUnit Platform configuration
    - Add jqwik dependency if not already present: `testImplementation("net.jqwik:jqwik:1.9.3")`
    - _Requirements: 8.2_

- [ ] 2. Implement RegistrationService unit tests
  - [ ] 2.1 Create RegistrationServiceTest with username validation and password validation nested groups
    - Create `src/test/java/com/revature/todomanagement/service/RegistrationServiceTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` with `@Mock UserRepository`, `@Mock PasswordValidator`, `@InjectMocks RegistrationService`
    - Implement `@Nested` class `UsernameValidation` with tests: null username, blank username, short username (<5), long username (>18)
    - Implement `@Nested` class `PasswordValidation` with tests: null password, blank password, password violations list
    - Verify validation order: no repository calls when field validation fails
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.8_

  - [ ] 2.2 Add duplicate checking and persistence nested groups to RegistrationServiceTest
    - Implement `@Nested` class `DuplicateChecking` with test: existsByUsername returns true throws RegistrationFailure "already taken"
    - Implement `@Nested` class `Persistence` with test: valid credentials saves user exactly once
    - Verify UserRepository.save never called when duplicate detected
    - _Requirements: 1.6, 1.7_

- [ ] 3. Implement UserService unit tests
  - [ ] 3.1 Create UserServiceTest with login nested group
    - Create `src/test/java/com/revature/todomanagement/service/UserServiceTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` with `@Mock UserRepository`, `@InjectMocks UserService`
    - Implement `@Nested` class `Login` with tests: valid credentials returns User, null username throws, blank username throws, null password throws, blank password throws, username not found throws, password mismatch throws
    - Verify findByUsername called exactly once on valid credentials
    - Verify findByUsername never called when username is null or blank
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 4. Implement TaskService unit tests
  - [ ] 4.1 Create TaskServiceTest with createTask and getTasksForUser nested groups
    - Create `src/test/java/com/revature/todomanagement/service/TaskServiceTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` with `@Mock TaskRepository`, `@Mock SubtaskRepository`, `@InjectMocks TaskService`
    - Implement `@Nested` class `CreateTask` with tests: valid title sets userId and saves, null title throws IllegalArg, blank title throws IllegalArg
    - Implement `@Nested` class `GetTasksForUser` with test: delegates to repository
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 4.2 Add getTaskById, updateTask, and deleteTask nested groups to TaskServiceTest
    - Implement `@Nested` class `GetTaskById` with tests: owned task returns task, non-existent throws TaskNotFoundException, wrong owner throws TaskOwnershipException
    - Implement `@Nested` class `UpdateTask` with tests: valid title updates and saves, blank title throws IllegalArg, null title preserves original, wrong owner throws TaskOwnershipException
    - Implement `@Nested` class `DeleteTask` with tests: owned task deletes subtasks then task (in order), wrong owner throws and no deletes
    - _Requirements: 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 3.12_

  - [ ]* 4.3 Write property test for TaskService ownership invariant
    - **Property 1: TaskService ownership invariant**
    - Create `src/test/java/com/revature/todomanagement/service/TaskServiceOwnershipPropertyTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` with `@Mock TaskRepository`, `@Mock SubtaskRepository`, `@InjectMocks TaskService`
    - Implement `@Property` method: for all UUID pairs where requestingUserId ≠ taskOwnerId, getTaskById always throws TaskOwnershipException
    - Filter: `!requestingUserId.equals(taskOwnerId)`
    - Minimum 100 tries (jqwik default)
    - **Validates: Requirements 3.6, 11.1, 11.2, 11.3**

- [ ] 5. Checkpoint - Verify service tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement PasswordValidator unit tests
  - [ ] 6.1 Create PasswordValidatorTest with individual rules and boundary tests
    - Create `src/test/java/com/revature/todomanagement/security/PasswordValidatorTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` (for convention) with direct instantiation: `PasswordValidator validator = new PasswordValidator()`
    - Implement `@Nested` class `IndividualRules` with 7 tests: too short, too long, no uppercase, no lowercase, no digit, no special char, whitespace
    - Implement `@Nested` class `AllRulesPass` with test: valid password returns empty list
    - Implement `@Nested` class `MultipleViolations` with test: multiple rules violated returns correct count
    - Implement `@Nested` class `BoundaryLengths` with `@ParameterizedTest` + `@MethodSource`: exactly 8 chars and exactly 72 chars pass
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10_

  - [ ]* 6.2 Write property test for PasswordValidator regex/rule round-trip
    - **Property 2: PasswordValidator regex/rule-engine round-trip**
    - Create `src/test/java/com/revature/todomanagement/security/PasswordValidatorPropertyTest.java`
    - Implement `@Property` method: for all passwords matching PASSWORD_REGEX, getViolations returns empty list
    - Implement `@Provide` method `passwordsMatchingRegex()` that builds strings: length 8-72, at least one uppercase, one lowercase, one digit, one special char from `!@#$%^&*`, no whitespace
    - **Validates: Requirements 4.11**

- [ ] 7. Implement JwtUtil unit tests
  - [ ] 7.1 Create JwtUtilTest with generateToken, extractUsername, extractUserId, and isTokenValid nested groups
    - Create `src/test/java/com/revature/todomanagement/security/JwtUtilTest.java`
    - Construct JwtUtil directly with a known 32-byte secret: `new JwtUtil("my-test-secret-key-32-bytes-lon!")`
    - Implement `@Nested` class `GenerateToken` with test: returns non-null token with 3 segments
    - Implement `@Nested` class `ExtractUsername` with tests: valid token returns username, different secret returns null, expired token returns null
    - Implement `@Nested` class `ExtractUserId` with tests: valid token returns userId, different secret returns null
    - Implement `@Nested` class `IsTokenValid` with tests: valid token + correct username returns true, wrong username returns false, null username returns false
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.10_

  - [ ]* 7.2 Write property test for JwtUtil claim extraction round-trip
    - **Property 3: JwtUtil claim extraction round-trip**
    - Create `src/test/java/com/revature/todomanagement/security/JwtUtilPropertyTest.java`
    - Implement `@Property` method: for all User entities with non-blank username (1-255 chars) and non-null UUID id, extractUsername(generateToken(user)) equals username AND extractUserId(generateToken(user)) equals id.toString()
    - Implement `@Provide` method `validUsers()` using `Arbitraries.strings().ofMinLength(1).ofMaxLength(255).alpha().numeric()` for usernames and `Arbitraries.create(UUID::randomUUID)` for ids
    - **Validates: Requirements 5.2, 5.3, 5.9**

- [ ] 8. Implement AuthInterceptor unit tests
  - [ ] 8.1 Create AuthInterceptorTest with CORS, header validation, token validation, and success nested groups
    - Create `src/test/java/com/revature/todomanagement/security/AuthInterceptorTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` with `@Mock JwtUtil`, `@InjectMocks AuthInterceptor`
    - Set up `MockHttpServletRequest`, `MockHttpServletResponse` in `@BeforeEach`
    - Implement `@Nested` class `CorsPreflight` with test: OPTIONS returns true without header inspection
    - Implement `@Nested` class `HeaderValidation` with tests: no auth header returns 401, non-Bearer header returns 401
    - Implement `@Nested` class `TokenValidation` with tests: extractUsername returns null gives 401, isTokenValid returns false gives 401, verify correct wiring of extractUsername to isTokenValid
    - Implement `@Nested` class `SuccessfulAuth` with tests: sets userId attribute, sets username attribute, returns true
    - Implement `@Nested` class `RejectionSideEffects` with test: no attributes set on rejection
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10_

- [ ] 9. Checkpoint - Verify security tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
  - Run `gradlew.bat test` from `spring-todo-backend/` and verify green build
  - Confirm test count is approximately 50+ example-based tests plus 3 property tests

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation after each test layer
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples, edge cases, and wiring
- All test classes use `@DisplayName` and `@Nested` per requirement 8.4 and 8.5
- Test method naming follows `methodUnderTest_stateOrInput_expectedBehavior` per requirement 8.3
- The existing `SubtaskServiceTest.java` in the root test package is not modified
- HTTP endpoint behavior (status codes, CRUD, error responses) is tested in test02-api-integration-test — not here

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "4.2", "6.1", "7.1", "8.1"] },
    { "id": 3, "tasks": ["4.3", "6.2", "7.2"] }
  ]
}
```
