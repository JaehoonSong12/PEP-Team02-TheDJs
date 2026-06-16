# Implementation Plan: Todo Management Backend

## Overview

The implementation closes four gaps in the existing Spring Boot 4.1.0 REST API: replacing
mocked authentication with real JWT-based Spring Security; replacing plaintext password
handling with BCrypt; adding Jakarta validation annotations to all DTOs; and adding the missing
`GET /api/todos/{id}/subtask` endpoint. All changes target the existing three-layer convention
under `spring-todo-backend/src/main/java/com/revature/todomanagement/`.

## Tasks

- [ ] 1. Add required dependencies and update build configuration
  - [ ] 1.1 Add Spring Security and test dependencies to `build.gradle.kts`
    - Add `implementation("org.springframework.boot:spring-boot-starter-security")` to the
      `dependencies {}` block
    - Add `testImplementation("org.springframework.security:spring-security-test")` to the
      `dependencies {}` block
    - Add `testImplementation("net.jqwik:jqwik:1.9.1")` to the `dependencies {}` block
    - _Requirements: 3.1, 3.2_

  - [ ] 1.2 Update `application.properties` with JWT secret, DDL mode, and SQLite pragma
    - Change `spring.jpa.hibernate.ddl-auto` from `create-drop` to `update`
    - Add `spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON`
    - Add `jwt.secret=REPLACE_WITH_BASE64URL_ENCODED_SECRET_OF_AT_LEAST_32_BYTES` placeholder
    - Add `spring.security.user.password=unused` to suppress the default login banner
    - _Requirements: 7.4, 7.6_

- [ ] 2. Create the `security/` package and JWT infrastructure
  - [ ] 2.1 Implement `security/JwtUtil.java`
    - Create the class under `com.revature.todomanagement.security`
    - Inject the Base64url secret from `${jwt.secret}` via `@Value` and build the
      `SecretKey` once in the constructor using `Keys.hmacShaKeyFor()`
    - Implement `generateToken(Long userId)` -- sets `sub` to `String.valueOf(userId)`,
      `iat` to now, `exp` to now + 3600 seconds, signs with HMAC-SHA256
    - Implement `extractSubject(String token)` -- parses and returns the `sub` claim
    - Implement `isTokenValid(String token)` -- returns `true` only when parsing succeeds
      without a `JwtException` or `IllegalArgumentException`
    - _Requirements: 2.4, 2.5, 3.4, 3.5_

  - [ ]* 2.2 Write property test for JWT authenticity (Property 1)
    - **Property 1: JWT Authenticity**
    - **Validates: Requirements 2.4, 2.5**
    - Create `PbtJwtTests` (or include in a shared `PbtTests` class) annotated with
      `@ExtendWith(JqwikExtension.class)`
    - Use `Arbitraries.longs().filter(id -> id > 0)` to generate user IDs
    - Assert `isTokenValid(generateToken(u)) == true` and subject equals `String.valueOf(u)`
    - Assert `isTokenValid` returns `false` for any token with its signature segment altered
    - Include the comment
      `// Feature: todo-management-backend, Property 1: JWT Authenticity`

  - [ ] 2.3 Implement `exception/UnauthorizedAccessException.java`
    - Create the class under `com.revature.todomanagement.exception`
    - Annotate with `@ResponseStatus(HttpStatus.FORBIDDEN)`
    - Extend `RuntimeException` with a single `String message` constructor
    - _Requirements: 4.8_

  - [ ] 2.4 Implement `exception/GlobalExceptionHandler.java`
    - Create `@RestControllerAdvice` class under `com.revature.todomanagement.exception`
    - Add handler for `MethodArgumentNotValidException` returning `HTTP 400` with body
      `{"status": 400}`
    - Add handler for `ResourceNotFoundException` returning `HTTP 404` with body
      `{"status": 404}`
    - Add handler for `ResourceConflictException` returning `HTTP 409` with body
      `{"status": 409}`
    - Add handler for `UnauthorizedAccessException` returning `HTTP 403` with body
      `{"status": 403}`
    - Add catch-all handler for `Exception` returning `HTTP 500` with body `{"status": 500}`
    - All response bodies use `Map.of("status", N)` as the return type
    - _Requirements: 4.5, 4.6, 4.7, 4.8, 4.9_

  - [ ] 2.5 Implement `security/SecurityConfig.java`
    - Create `@Configuration @EnableWebSecurity` class under
      `com.revature.todomanagement.security`
    - Inject `JwtFilter` via constructor (`@RequiredArgsConstructor`)
    - Declare `@Bean SecurityFilterChain filterChain(HttpSecurity)` -- disable CSRF, set
      session to `STATELESS`, permit `/api/auth/**`, require authentication for all other
      requests, register `JwtFilter` before `UsernamePasswordAuthenticationFilter`
    - Declare `@Bean BCryptPasswordEncoder passwordEncoder()` with strength 10
    - _Requirements: 1.5, 3.1, 3.2_

  - [ ] 2.6 Implement `security/JwtFilter.java`
    - Create class extending `OncePerRequestFilter` under
      `com.revature.todomanagement.security`
    - Inject `JwtUtil` and `UserService` via `@RequiredArgsConstructor`
    - In `doFilterInternal`: extract the raw token from the `Authorization: Bearer ...`
      header; if absent or invalid, skip population of the `SecurityContext` and continue
      the filter chain (Spring Security will enforce authentication for protected paths)
    - If token is valid: parse the subject as `Long`, load the `User` via
      `userService.findById()`, wrap in `UsernamePasswordAuthenticationToken` with an empty
      authorities list, and store in `SecurityContextHolder`
    - If any exception is thrown during user lookup, write `{"status":401}` with HTTP 401
      directly to the response and return without continuing the chain
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7_

- [ ] 3. Checkpoint -- Ensure security infrastructure compiles and all existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Apply BCrypt password handling in `UserService`
  - [ ] 4.1 Modify `UserService.java` to inject `BCryptPasswordEncoder` and hash passwords
    - Add `BCryptPasswordEncoder passwordEncoder` to the constructor (constructor injection)
    - In `register()`: call `passwordEncoder.encode(user.getPassword())` and assign the
      result back to `user` before `userRepository.save(user)`; the plaintext value is
      never persisted
    - In `login()`: replace the direct equality comparison with
      `passwordEncoder.matches(rawPassword, storedHash)`; throw `ResourceNotFoundException`
      (or a dedicated `InvalidCredentialsException` mapping to HTTP 401 if added) when the
      check fails
    - _Requirements: 1.5, 2.7_

  - [ ]* 4.2 Write property test for BCrypt hash verifiability (Property 5)
    - **Property 5: BCrypt Hash Verifiability**
    - **Validates: Requirements 1.5, 2.7**
    - Use `Arbitraries.strings().withCharRange(' ', '~').ofMinLength(8).ofMaxLength(128)` to
      generate passwords
    - Assert `encoder.matches(p, encoder.encode(p)) == true`
    - Assert `!encoder.encode(p).equals(p)` (hash never equals plaintext)
    - Assert `encoder.matches(p1, encoder.encode(p2)) == false` for any two distinct passwords
    - No Spring context required; instantiate `BCryptPasswordEncoder` directly
    - Include the comment
      `// Feature: todo-management-backend, Property 5: BCrypt Hash Verifiability`

  - [ ]* 4.3 Write unit tests for `UserService` password logic
    - Create `UserServiceTest` in `src/test/java/.../service/`
    - `register_hashesPassword`: verify stored hash differs from plaintext and `matches()` is
      true
    - `register_throwsConflict_onDuplicateUsername`: stub `existsByUsername` to return true;
      assert `ResourceConflictException`
    - `login_throwsNotFound_onUnknownUsername`: stub `findByUsername` to return empty; assert
      exception
    - `login_throwsOnWrongPassword`: stub user with a known hash; supply wrong raw password;
      assert exception
    - _Requirements: 1.2, 1.5, 2.3, 2.7_

- [ ] 5. Add Jakarta validation annotations to all request DTOs
  - [ ] 5.1 Annotate `dto/UserRegistrationRequest.java` with validation constraints
    - Add `@NotBlank @Size(min=1, max=50)` to `username`
    - Add `@NotBlank @Email @Size(max=254)` to `email`
    - Add `@NotBlank @Size(min=8, max=128)` to `password`
    - _Requirements: 1.4, 4.1_

  - [ ] 5.2 Annotate `dto/LoginRequest.java` with validation constraints
    - Add `@NotBlank` to `username`
    - Add `@NotBlank` to `password`
    - _Requirements: 2.6, 4.2_

  - [ ] 5.3 Annotate `dto/TodoRequest.java` with validation constraints
    - Add `@NotBlank @Size(min=1, max=255)` to `title`
    - Add `@Size(max=1000)` to `description`
    - _Requirements: 5.2, 4.3_

  - [ ] 5.4 Annotate `dto/SubtaskRequest.java` with validation constraints
    - Add `@NotBlank @Size(min=1, max=255)` to `title`
    - _Requirements: 6.5 (duplicate numbering in spec), 4.4_

  - [ ] 5.5 Add `accountId` field to `dto/SubtaskResponse.java`
    - Add `private Long accountId` to the `@Builder`-annotated `SubtaskResponse` class
    - _Requirements: 6.14_

- [ ] 6. Update `AuthController` to use `@Valid` and emit the JWT header
  - [ ] 6.1 Modify `AuthController.java` to add `@Valid` and wire `JwtUtil`
    - Inject `JwtUtil` via constructor or `@Autowired`
    - Add `@Valid` to the `@RequestBody` parameter of `register()`
    - Add `@Valid` to the `@RequestBody` parameter of `login()`
    - In `login()`: call `jwtUtil.generateToken(user.getId())` and include the result in the
      `Authorization` response header as `"Bearer " + token`
    - _Requirements: 2.1, 2.4, 4.1, 4.2_

  - [ ]* 6.2 Write integration tests for `AuthController`
    - Create `AuthControllerTest` with `@WebMvcTest(AuthController.class)`
    - `register_returns200_onValidRequest`: POST with a valid body; assert 200 and
      `AuthResponse` fields
    - `register_returns400_onBlankUsername`: assert 400 and `{"status":400}`
    - `register_returns409_onDuplicateUsername`: service stub throws
      `ResourceConflictException`; assert 409
    - `login_returns200_andBearerHeader`: assert 200 and `Authorization` header starts with
      `"Bearer "`
    - `login_returns400_onBlankPassword`: assert 400
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.6_

  - [ ]* 6.3 Write property test for registration idempotency (Property 4)
    - **Property 4: Registration Idempotency**
    - **Validates: Requirements 1.2, 1.7**
    - Use `@SpringBootTest` with H2 override in `test/resources/application.properties`
    - Use `Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)` for usernames
    - First registration for any username returns HTTP 200; every subsequent registration
      with the same username returns HTTP 409 regardless of email or password supplied
    - Test via `MockMvc`; stub or use real `UserService` with in-memory H2
    - Include the comment
      `// Feature: todo-management-backend, Property 4: Registration Idempotency`

- [ ] 7. Update `TodoController` to resolve the authenticated user from `SecurityContext`
  - [ ] 7.1 Replace the hardcoded user lookup in `TodoController.java`
    - Remove (or replace) the `getAuthenticatedUser()` helper that calls
      `userService.findById(1L)`
    - Implement a new `getAuthenticatedUser()` private method that casts the principal from
      `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` to `User`
    - Verify that the hardcoded call `userService.findById(1L)` no longer appears anywhere
      in the codebase
    - _Requirements: 3.8_

  - [ ] 7.2 Add `@Valid` to all `@RequestBody` parameters in `TodoController.java`
    - Add `@Valid` to the `@RequestBody` on `createTodo()`
    - Add `@Valid` to the `@RequestBody` on `updateTodo()`
    - Add `@Valid` to the `@RequestBody` on `createSubtask()`
    - Add `@Valid` to the `@RequestBody` on `updateSubtask()`
    - _Requirements: 4.3, 4.4_

  - [ ] 7.3 Update `mapToSubtaskResponse()` helper in `TodoController.java` to populate `accountId`
    - Add `.accountId(subtask.getTask().getUser().getId())` to the builder call
    - _Requirements: 6.14_

- [ ] 8. Replace `RuntimeException` ownership errors and add missing subtask endpoint
  - [ ] 8.1 Modify `TaskService.java` to throw `UnauthorizedAccessException`
    - Replace `throw new RuntimeException("Unauthorized access to task")` in `getTaskById()`
      with `throw new UnauthorizedAccessException("Access denied to task " + id)`
    - _Requirements: 5.6, 5.9, 4.8_

  - [ ] 8.2 Modify `SubtaskService.java` to throw `UnauthorizedAccessException` and add
    `getSubtasksByTask()`
    - Replace all `throw new RuntimeException(...)` ownership-violation throws in
      `updateSubtask()` and `deleteSubtask()` with `throw new UnauthorizedAccessException(...)`
    - Add `getSubtasksByTask(Long taskId, User user)` -- delegates ownership check to
      `taskService.getTaskById(taskId, user)`, then calls
      `subtaskRepository.findByTask(task)` and returns the result; annotate the method with
      `@Transactional(readOnly = true)`
    - _Requirements: 6.1, 6.3, 6.9, 6.12, 4.8_

  - [ ]* 8.3 Write unit tests for ownership enforcement in `TaskService` and `SubtaskService`
    - Create `TaskServiceTest` in `src/test/java/.../service/`
    - `getTaskById_throwsUnauthorized_whenOwnerMismatch`: two User stubs with distinct IDs;
      assert `UnauthorizedAccessException`
    - `deleteTask_delegatesToRepository`: verify `taskRepository.delete()` is called once
    - Create `SubtaskServiceTest` in `src/test/java/.../service/`
    - `getSubtasksByTask_returnsListForOwner`: stub `findByTask`; assert returned list matches
    - `updateSubtask_throwsUnauthorized_whenOwnerMismatch`: verify `UnauthorizedAccessException`
    - `deleteSubtask_throwsUnauthorized_whenOwnerMismatch`: verify `UnauthorizedAccessException`
    - _Requirements: 5.6, 5.9, 5.13, 6.3, 6.9, 6.12_

  - [ ]* 8.4 Write property test for ownership isolation (Property 2)
    - **Property 2: Ownership Isolation**
    - **Validates: Requirements 5.6, 5.9, 5.13**
    - Generate two `User` instances in-memory with distinct IDs; one `Task` entity with
      `user` set to user A
    - Assert `assertThatThrownBy(() -> taskService.getTaskById(task.getId(), userB))`
      is of type `UnauthorizedAccessException`
    - No Spring context required; mock repositories with Mockito
    - Include the comment
      `// Feature: todo-management-backend, Property 2: Ownership Isolation`

  - [ ] 8.5 Add `GET /api/todos/{id}/subtask` endpoint to `TodoController.java`
    - Add `@GetMapping("/{id}/subtask") ResponseEntity<List<SubtaskResponse>> getSubtasksByTodo(@PathVariable Long id)`
    - Call `getAuthenticatedUser()` to resolve the current principal
    - Call `subtaskService.getSubtasksByTask(id, user)`, stream the result through
      `mapToSubtaskResponse()`, collect to list, and return `ResponseEntity.ok(list)`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 9. Checkpoint -- Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Write cascade integrity property test and integration tests for `TodoController`
  - [ ]* 10.1 Write property test for cascade integrity (Property 3)
    - **Property 3: Cascade Integrity**
    - **Validates: Requirements 5.12**
    - Use `@SpringBootTest` with H2 in-memory override
    - Use `@ForAll @IntRange(min=0, max=20)` for the subtask count n
    - Persist a task with n subtasks; call `taskService.deleteTask()`; assert
      `subtaskRepository.findByTask(task)` returns an empty list and
      `taskRepository.findById(id)` returns `Optional.empty()`
    - Include the comment
      `// Feature: todo-management-backend, Property 3: Cascade Integrity`

  - [ ]* 10.2 Write integration tests for `TodoController`
    - Create `TodoControllerTest` with `@WebMvcTest(TodoController.class)` and security
      autoconfiguration active
    - `getAllTodos_returns401_withoutToken`: no `Authorization` header; assert 401
    - `getAllTodos_returns200_withValidToken`: inject principal via
      `SecurityMockMvcRequestPostProcessors.user()`; assert 200
    - `createTodo_returns400_onBlankTitle`: valid token, blank title; assert 400
    - `getTodoById_returns403_onOwnerMismatch`: service stub throws
      `UnauthorizedAccessException`; assert 403 and `{"status":403}`
    - `getTodoById_returns404_onMissingTask`: service stub throws
      `ResourceNotFoundException`; assert 404
    - `getSubtasksByTodo_returns200_withList`: stub returns two subtasks; assert 200 and
      response array length 2
    - _Requirements: 3.3, 5.2, 5.3, 5.5, 5.6, 6.1_

- [ ] 11. Final checkpoint -- Ensure all tests pass and the application starts cleanly
  - Ensure all tests pass and `./gradlew bootRun` starts without errors. Ask the user if
    questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP delivery.
- Each task references specific requirements for traceability.
- All five correctness properties from the design document are covered by property-based
  tests using `jqwik 1.9.1`.
- The H2 in-memory database (already on the classpath) is used for all slice and
  `@SpringBootTest` tests; a `test/resources/application.properties` override is required for
  Properties 3 and 4 and for the `@SpringBootTest` integration tests.
- The `spring.security.user.password=unused` property suppresses the auto-generated password
  banner that Spring Security emits when no `UserDetailsService` bean is detected.
- `subtask.getTask().getUser()` traverses a lazy association; all service methods that back
  the subtask-response mapping must be `@Transactional` (or `@Transactional(readOnly=true)`)
  to keep the Hibernate session open during controller-layer mapping.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.3", "2.4"] },
    { "id": 2, "tasks": ["2.2", "2.5", "2.6"] },
    { "id": 3, "tasks": ["4.1", "5.1", "5.2", "5.3", "5.4", "5.5", "7.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "6.1", "7.2", "8.1", "8.2"] },
    { "id": 5, "tasks": ["6.2", "6.3", "7.3", "8.3", "8.4", "8.5"] },
    { "id": 6, "tasks": ["10.1", "10.2"] }
  ]
}
```
