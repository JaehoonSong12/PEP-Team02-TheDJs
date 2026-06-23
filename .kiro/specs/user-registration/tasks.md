# Implementation Plan: User Registration

## Overview

Implement the `POST /api/auth/register` endpoint in the Spring Boot backend. Tasks proceed
bottom-up: password validation → custom exception → service → controller → tests.
Each step compiles and integrates with the previous one.

There are NO DTOs and NO `GlobalExceptionHandler`. The controller accepts `@RequestBody User`
directly and has local `@ExceptionHandler` methods for error mapping. The service returns void
on success and throws `RegistrationFailure` for all validation failures.

All code lives under `spring-todo-backend/src/` in the base package
`com.revature.todomanagement`.

## Tasks

- [ ] 1. Add jqwik dependency
  - Add `testImplementation("net.jqwik:jqwik:1.9.3")` to `build.gradle.kts` dependencies block
  - No entity changes needed — `User` entity already exists and is not modified
  - _Requirements: 3.2, 3.3 (property test infrastructure)_

- [ ] 2. Implement PasswordValidator
  - [ ] 2.1 Create `security/PasswordValidator.java`
    - Annotate `@Component`
    - Implement `public List<String> getViolations(String password)` that checks each of the seven rules independently and collects violation messages into a `List<String>`
    - Rules and messages match Requirements 3.2 exactly
    - Method never throws; returns empty list when all rules pass
    - Add the regex constant `PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])\\S{8,72}$"` as a `public static final String` for documentation purposes
    - _Requirements: 3.2, 3.3_

  - [ ]* 2.2 Write property test for PasswordValidator — Property 5: PasswordValidator returns a violation for each broken rule
    - **Property 5: PasswordValidator returns a violation for each broken rule**
    - **Validates: Requirements 3.2, 3.3**
    - Use jqwik `@Property` methods in `PasswordValidatorTest`
    - For each rule, use `@ForAll @StringLength` / `@CharsList` / `Arbitraries` providers to generate passwords violating only that rule; assert the specific message is present
    - For the valid-password case, generate strings matching all rules; assert violations list is empty
    - Tag: `// Feature: user-registration, Property 5: PasswordValidator rule coverage`

- [ ] 3. Create custom exception
  - [ ] 3.1 Create `exception/RegistrationFailure.java`
    - Extends `RuntimeException`
    - Constructor: `public RegistrationFailure(String message)` that calls `super(message)`
    - This is the single custom exception for ALL registration validation failures (blank username, bad length, bad password, duplicate username)
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.4, 5.2_

- [ ] 4. Implement RegistrationService
  - [ ] 4.1 Create `service/RegistrationService.java`
    - Annotate `@Service @RequiredArgsConstructor`
    - Inject: `UserRepository userRepository`, `PasswordValidator passwordValidator`
    - Implement `public void registerUser(User user)` following the seven-step validation and persistence order from Requirements 6.1 exactly:
      - Step 1: Validate username not blank → throw `RegistrationFailure("Username must not be blank.")`
      - Step 2: Validate username length 5–18 → throw `RegistrationFailure("Username must be between 5 and 18 characters.")`
      - Step 3: Validate password not blank → throw `RegistrationFailure("Password must not be blank.")`
      - Step 4: Validate password satisfies all strength rules → throw `RegistrationFailure` with joined violations
      - Step 5: Check duplicate username → throw `RegistrationFailure("Username '<username>' is already taken.")`
      - Step 6: Persist → `UserRepository.save(user)`
      - Step 7: Return (void)
    - Steps 1–4 (field validation) must complete before any `UserRepository` call
    - Returns void on success — no return value
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.4, 4.1, 4.2, 4.3, 6.1, 6.2_

  - [ ]* 4.2 Write unit tests for RegistrationService
    - In `RegistrationServiceTest`, mock `UserRepository` and `PasswordValidator` with Mockito
    - Test: valid input → `save` called once with `User` whose `username` and `password` fields equal the submitted values, method completes without exception (Req 7.1)
    - Test: `existsByUsername` returns `true` → `RegistrationFailure` thrown, `save` never called (Req 7.2)
    - Test: blank username → `RegistrationFailure("Username must not be blank.")`, no repo calls (Req 7.3)
    - Test: username length < 5 → `RegistrationFailure("Username must be between 5 and 18 characters.")`, no repo calls (Req 7.4)
    - Test: username length > 18 → same `RegistrationFailure`, no repo calls (Req 7.4)
    - Test: blank password → `RegistrationFailure("Password must not be blank.")`, `UserRepository` never called
    - Test: `passwordValidator.getViolations` returns non-empty list → `RegistrationFailure` whose message contains all returned violations, no repo calls
    - Test: `save` throws `DataAccessException` → exception propagates (Req 4.3)
    - Use `Mockito.verifyNoInteractions(userRepository)` where repo must not be touched
    - _Requirements: 6.1, 6.2, 7.1–7.4_

- [ ] 5. Checkpoint — compile and run unit tests
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement RegistrationController
  - [ ] 6.1 Create `controller/RegistrationController.java`
    - Annotate `@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor @Slf4j`
    - Inject `RegistrationService registrationService`
    - Method: `@PostMapping("/register") public ResponseEntity<Void> register(@RequestBody User user)`
    - Call `registrationService.registerUser(user)`
    - Return `ResponseEntity.status(HttpStatus.CREATED).build()` (empty body, HTTP 201)
    - No try/catch in the main method — exception handling is via local `@ExceptionHandler` methods
    - Add local `@ExceptionHandler` methods inside this controller class:
      - `@ExceptionHandler(RegistrationFailure.class)` → `ResponseEntity.status(400).body(ex.getMessage())` (returns `ResponseEntity<String>`)
      - `@ExceptionHandler(DataIntegrityViolationException.class)` → `ResponseEntity.status(409).body("Could not complete registration: data conflict")`, log at warn level
      - `@ExceptionHandler(DataAccessResourceFailureException.class)` → `ResponseEntity.status(503).body("Service temporarily unavailable, please try again later")`, log at error level
      - `@ExceptionHandler(QueryTimeoutException.class)` → `ResponseEntity.status(503).body("Request timed out, please try again later")`, log at warn level
      - `@ExceptionHandler(DataAccessException.class)` → `ResponseEntity.status(500).body("An unexpected error occurred during registration")`, log at error level
    - All error handler methods return `ResponseEntity<String>` with plain text bodies
    - _Requirements: 1.1, 1.2, 1.3, 5.1–5.7_

  - [ ]* 6.2 Write @WebMvcTest tests for RegistrationController
    - In `RegistrationControllerTest`, annotate `@WebMvcTest(RegistrationController.class)`, mock `RegistrationService`
    - Test: valid request → HTTP 201, empty response body (Req 7.9, 1.1, 1.2)
    - Test: service throws `RegistrationFailure` → HTTP 400, response body is the plain text exception message (Req 7.8, 5.2)
    - Test: service throws `DataIntegrityViolationException` → HTTP 409, response body is `"Could not complete registration: data conflict"` (Req 7.10, 5.3)
    - Test: service throws `DataAccessResourceFailureException` → HTTP 503, plain text body (Req 5.4)
    - Test: service throws `QueryTimeoutException` → HTTP 503, plain text body (Req 5.5)
    - Test: service throws `DataAccessException` subclass → HTTP 500, plain text body (Req 5.6)
    - Test: malformed/absent body → HTTP 400 (Req 1.3)
    - Verify `Content-Type` is `text/plain` on error responses (Req 5.7)
    - _Requirements: 1.1, 1.2, 1.3, 5.1–5.7, 7.8–7.10_

- [ ] 7. Write @DataJpaTest for UserRepository
  - [ ]* 7.1 Write @DataJpaTest repository tests
    - In `UserRepositoryTest`, annotate `@DataJpaTest`
    - Add `src/test/resources/application-test.properties` configuring H2 dialect if not already present
    - Test: save a `User`, then `existsByUsername(username)` returns `true` (Req 7.7, 4.4)
    - Test: `existsByUsername` returns `false` for a username that was never saved (Req 7.7, 4.4)
    - Test: save a `User`, then `findByUsername(username)` returns non-empty `Optional` with matching entity (Req 4.5)
    - Test: `findByUsername` returns empty `Optional` for a username that was never saved (Req 4.5)
    - _Requirements: 4.4, 4.5, 7.7_

- [ ] 8. Write PasswordValidator unit tests (individual rule coverage)
  - [ ]* 8.1 Write unit tests for each individual PasswordValidator rule violation
    - In `PasswordValidatorTest`, add `@Test` methods (not property-based) for deterministic edge cases
    - Test each of the seven rules with a concrete password violating only that rule; assert the exact message appears in the violations list (Req 7.5)
    - Test: valid password satisfying all rules → empty violations list (Req 7.6)
    - These complement the jqwik property tests from task 2.2
    - _Requirements: 3.2, 3.3, 7.5, 7.6_

- [ ] 9. Final checkpoint — full test suite
  - Run `gradlew.bat test` from `spring-todo-backend/`
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- `UserService.java` already exists with `register()` and `login()` — it is left untouched; the new `RegistrationService` is additive
- H2 is already in the Gradle dependencies (`testImplementation("com.h2database:h2:2.4.240")`), so `@DataJpaTest` will auto-configure it
- The `User.id` field is named `id` (not `userId`) in the entity
- There are NO DTOs (`RegisterRequest`, `RegisterResponse`) — the controller accepts `@RequestBody User` directly
- There is NO `GlobalExceptionHandler` / `@ControllerAdvice` — all exception handlers are local to `RegistrationController`
- The `RegistrationFailure` exception replaces both `IllegalArgumentException` and `DuplicateUsernameException` from the previous design
- Error responses are plain text strings (`Content-Type: text/plain`), NOT JSON objects

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "3.1", "7.1"] },
    { "id": 2, "tasks": ["2.2", "4.1", "8.1"] },
    { "id": 3, "tasks": ["4.2", "6.1"] },
    { "id": 4, "tasks": ["6.2"] }
  ]
}
```
