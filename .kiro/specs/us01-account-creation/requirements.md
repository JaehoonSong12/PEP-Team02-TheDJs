# Requirements Document

## Introduction

This feature implements user registration for the Todo Management Application. It delivers the
`POST /api/auth/register` endpoint, which accepts a `User` entity as the request body, validates
the username and password fields against defined rules, persists a new `User` record to the SQLite
database, and returns HTTP 201 with an empty body on success or a meaningful error status with a
plain text message on failure.

The feature is scoped to registration only. Authentication (login) and JWT issuance are handled
by a separate feature.

---

## Glossary

- **RegistrationController**: The Spring `@RestController` that handles `POST /api/auth/register` requests and contains local `@ExceptionHandler` methods for error mapping.
- **RegistrationService**: The Spring `@Service` class that contains all business logic for user registration.
- **UserRepository**: The Spring Data JPA interface that provides persistence operations for `User` entities.
- **PasswordValidator**: A utility component that checks a plaintext password against all strength rules and returns a list of violation messages.
- **User**: The JPA entity (`entity/User.java`) with fields `UUID userId`, `String username`, `String password`.
- **RegistrationFailure**: A custom `RuntimeException` thrown by `RegistrationService` when any registration validation fails (blank username, bad length, bad password, duplicate username).
- **blank**: A string that is either `null`, empty (`""`), or contains only whitespace characters.
- **special character**: Any character from the set `!@#$%^&*`.

---

## Requirements

### Requirement 1: Registration Endpoint

**User Story:** As a new user, I want to submit my username and password to create an account, so that I can start managing my todo tasks.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/register` request is received with a valid `@RequestBody User` body, THE `RegistrationController` SHALL delegate to `RegistrationService.registerUser(User)` and return HTTP 201 with an empty body (`ResponseEntity<Void>` with null body).
2. THE `RegistrationController` SHALL accept `User` as the `@RequestBody` parameter and return `ResponseEntity<Void>`; THE response body SHALL be empty (null) on success.
3. WHEN a `POST /api/auth/register` request is received with a malformed or absent request body, THE `RegistrationController` SHALL return HTTP 400.

---

### Requirement 2: Username Validation

**User Story:** As a developer, I want username inputs validated before any database access, so that invalid data never reaches the persistence layer.

#### Acceptance Criteria

1. WHEN `RegistrationService.registerUser` is called with a `username` field that is blank, THE `RegistrationService` SHALL throw a `RegistrationFailure` with message `"Username must not be blank."` before any database access occurs.
2. WHEN `RegistrationService.registerUser` is called with a `username` whose length is less than 5 characters or greater than 18 characters, THE `RegistrationService` SHALL throw a `RegistrationFailure` with message `"Username must be between 5 and 18 characters."` before any database access occurs.
3. WHEN `RegistrationService.registerUser` is called with a `username` that already exists in the database (case-sensitive comparison), THE `RegistrationService` SHALL throw a `RegistrationFailure` with message indicating the username is already taken, without persisting a new record.
4. WHEN `RegistrationService.registerUser` is called with a `username` between 5 and 18 characters that does not yet exist in the database, THE `RegistrationService` SHALL proceed to password validation without throwing an exception.

---

### Requirement 3: Password Validation

**User Story:** As a security-conscious developer, I want passwords validated against strength rules before persistence, so that only strong passwords are accepted.

#### Acceptance Criteria

1. WHEN `RegistrationService.registerUser` is called with a `password` field that is blank, THE `RegistrationService` SHALL throw a `RegistrationFailure` with message `"Password must not be blank."` before any database access occurs.
2. WHEN `PasswordValidator.getViolations` is called with a password that violates one or more of the following rules, THE `PasswordValidator` SHALL return a non-empty `List<String>` containing one entry per violated rule:
   - `"Password must be at least 8 characters long."` — minimum length of 8
   - `"Password must be no more than 72 characters long."` — maximum length of 72
   - `"Password must contain at least one uppercase letter."` — at least one character `A–Z`
   - `"Password must contain at least one lowercase letter."` — at least one character `a–z`
   - `"Password must contain at least one digit."` — at least one character `0–9`
   - `"Password must contain at least one special character (!@#$%^&*)."` — at least one from `!@#$%^&*`
   - `"Password must not contain whitespace."` — no space or other whitespace characters
3. WHEN `PasswordValidator.getViolations` is called with a password that satisfies all rules, THE `PasswordValidator` SHALL return an empty `List<String>`.
4. WHEN `RegistrationService.registerUser` is called with a password that violates one or more strength rules, THE `RegistrationService` SHALL throw a `RegistrationFailure` whose message lists each violation before any database access occurs.

---

### Requirement 4: User Persistence

**User Story:** As a developer, I want a new `User` record written to the SQLite database on successful registration, so that the account can be used for subsequent login.

#### Acceptance Criteria

1. WHEN `RegistrationService.registerUser` completes all validations successfully, THE `RegistrationService` SHALL call `UserRepository.save` exactly once with a `User` entity whose `username` equals the submitted username and whose `password` equals the submitted password as-is.
2. WHEN `UserRepository.save` succeeds, THE `RegistrationService` SHALL return void (user is persisted successfully).
3. WHEN `UserRepository.save` throws a `DataAccessException`, THE `RegistrationService` SHALL propagate the exception without swallowing it.
4. THE `UserRepository` SHALL declare `boolean existsByUsername(String username)`, returning `true` when at least one `User` with that username exists in the database and `false` otherwise.
5. THE `UserRepository` SHALL declare `Optional<User> findByUsername(String username)`, returning a non-empty `Optional` when a `User` with the given username exists and an empty `Optional` otherwise.

---

### Requirement 5: HTTP Status Codes and Error Responses

**User Story:** As a client developer, I want the registration API to return meaningful HTTP status codes and plain text error messages for every outcome, so that I can handle each case programmatically.

#### Acceptance Criteria

1. WHEN `RegistrationService.registerUser` succeeds, THE `RegistrationController` SHALL return HTTP 201 with an empty body.
2. IF a `RegistrationFailure` is thrown during registration, THEN THE `RegistrationController` local `@ExceptionHandler(RegistrationFailure.class)` SHALL return HTTP 400 with a plain text body containing the exception message string.
3. IF a `DataIntegrityViolationException` is thrown during registration, THEN THE `RegistrationController` local `@ExceptionHandler(DataIntegrityViolationException.class)` SHALL return HTTP 409 with a plain text body `"Could not complete registration: data conflict"`.
4. IF a `DataAccessResourceFailureException` is thrown during registration, THEN THE `RegistrationController` local `@ExceptionHandler(DataAccessResourceFailureException.class)` SHALL return HTTP 503 with a plain text body `"Service temporarily unavailable, please try again later"`.
5. IF a `QueryTimeoutException` is thrown during registration, THEN THE `RegistrationController` local `@ExceptionHandler(QueryTimeoutException.class)` SHALL return HTTP 503 with a plain text body `"Request timed out, please try again later"`.
6. IF a `DataAccessException` is thrown during registration (catch-all for other data access errors), THEN THE `RegistrationController` local `@ExceptionHandler(DataAccessException.class)` SHALL return HTTP 500 with a plain text body `"An unexpected error occurred during registration"`.
7. THE `RegistrationController` SHALL set `Content-Type: text/plain` on all error responses; error bodies are plain strings, NOT JSON objects.

---

### Requirement 6: Registration Validation Order

**User Story:** As a developer, I want validations applied in a defined order, so that the error messages returned to the client are deterministic and predictable.

#### Acceptance Criteria

1. WHEN `RegistrationService.registerUser` is called, THE `RegistrationService` SHALL apply validations in the following order:
   - Step 1: Validate `username` is not blank → `RegistrationFailure`
   - Step 2: Validate `username` length is 5–18 characters → `RegistrationFailure`
   - Step 3: Validate `password` is not blank → `RegistrationFailure`
   - Step 4: Validate `password` satisfies all strength rules → `RegistrationFailure`
   - Step 5: Check `existsByUsername` for duplicate → `RegistrationFailure`
   - Step 6: Persist → `UserRepository.save`
   - Step 7: Return (void)
2. THE `RegistrationService` SHALL NOT invoke `UserRepository` for any database read or write before all field-level validations (Steps 1–4) have passed.

---

### Requirement 7: Unit and Integration Tests

**User Story:** As a developer, I want a test suite covering registration logic, password validation, persistence, and HTTP behavior, so that regressions are caught automatically on every build.

#### Acceptance Criteria

1. THE test suite SHALL include a `RegistrationService` unit test that mocks `UserRepository` and `PasswordValidator`, calls `registerUser` with a valid `User` object, and verifies that `UserRepository.save` is called exactly once with a `User` whose `username` and `password` fields equal the submitted values.
2. THE test suite SHALL include a `RegistrationService` unit test that stubs `UserRepository.existsByUsername` to return `true` and verifies that calling `registerUser` throws `RegistrationFailure` without calling `UserRepository.save`.
3. THE test suite SHALL include a `RegistrationService` unit test that verifies calling `registerUser` with a blank username throws `RegistrationFailure` before any repository access.
4. THE test suite SHALL include a `RegistrationService` unit test that verifies calling `registerUser` with a username shorter than 5 characters or longer than 18 characters throws `RegistrationFailure`.
5. THE test suite SHALL include a `PasswordValidator` unit test that, for each individual rule violation (blank password, too short, no uppercase, no lowercase, no digit, no special character, contains whitespace), verifies that `getViolations` returns a list containing the corresponding violation message.
6. THE test suite SHALL include a `PasswordValidator` unit test that verifies `getViolations` returns an empty list for a password that satisfies all rules.
7. THE test suite SHALL include a `@DataJpaTest` test class for `UserRepository` using the H2 in-memory database that verifies `existsByUsername` returns `true` for a saved username and `false` for an unsaved username.
8. THE test suite SHALL include a `@WebMvcTest` test class for `RegistrationController` that mocks `RegistrationService` to throw `RegistrationFailure` and verifies that `POST /api/auth/register` returns HTTP 400 with a plain text string body.
9. THE test suite SHALL include a `@WebMvcTest` test class for `RegistrationController` that calls `registerUser` with a valid `User` and verifies that `POST /api/auth/register` returns HTTP 201 with an empty body.
10. THE test suite SHALL include a `@WebMvcTest` test class for `RegistrationController` that mocks `RegistrationService` to throw `DataIntegrityViolationException` and verifies that `POST /api/auth/register` returns HTTP 409 with the expected plain text message.
