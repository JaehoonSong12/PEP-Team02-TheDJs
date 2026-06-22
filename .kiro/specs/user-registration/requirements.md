# Requirements Document

## Introduction

This feature implements user registration for the Todo Management Application. It delivers the
`POST /api/auth/register` endpoint, which accepts a username and password, validates both fields
against defined rules, persists a new `User` record with a BCrypt-hashed password to the SQLite
database, and returns HTTP 201 on success or a meaningful error status on failure.

The feature is scoped to registration only. Authentication (login) and JWT issuance are handled
by a separate feature.

---

## Glossary

- **RegistrationController**: The Spring `@RestController` that handles `POST /api/auth/register` requests.
- **RegistrationService**: The Spring `@Service` class that contains all business logic for user registration.
- **UserRepository**: The Spring Data JPA interface that provides persistence operations for `User` entities.
- **PasswordEncoder**: A BCrypt-based `PasswordEncoder` bean used to hash passwords before persistence.
- **PasswordValidator**: A utility component that checks a plaintext password against all strength rules and returns a list of violation messages.
- **User**: The JPA entity (`entity/User.java`) with fields `UUID userId`, `String username`, `String password`.
- **RegisterRequest**: A DTO carrying `username` and `password` fields submitted by the client.
- **RegisterResponse**: A DTO carrying the new account's `UUID userId` and `String username` returned on successful registration.
- **DuplicateUsernameException**: A custom `RuntimeException` thrown when the submitted username already exists in the database.
- **GlobalExceptionHandler**: A `@ControllerAdvice` class that maps exceptions to HTTP error responses.
- **blank**: A string that is either `null`, empty (`""`), or contains only whitespace characters.
- **special character**: Any character from the set `!@#$%^&*`.

---

## Requirements

### Requirement 1: Registration Endpoint

**User Story:** As a new user, I want to submit my username and password to create an account, so that I can start managing my todo tasks.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/register` request is received with a valid `RegisterRequest` body, THE `RegistrationController` SHALL delegate to `RegistrationService.register(RegisterRequest)` and return HTTP 201 with a `RegisterResponse` body containing the new account's `UUID userId` and `String username`.
2. THE `RegistrationController` SHALL accept `RegisterRequest` as the `@RequestBody` parameter and return `RegisterResponse` as the response body; THE response body SHALL NOT expose the `User` entity directly or include the hashed password field.
3. WHEN a `POST /api/auth/register` request is received with a malformed or absent request body, THE `RegistrationController` SHALL return HTTP 400.

---

### Requirement 2: Username Validation

**User Story:** As a developer, I want username inputs validated before any database access, so that invalid data never reaches the persistence layer.

#### Acceptance Criteria

1. WHEN `RegistrationService.register` is called with a `username` field that is blank, THE `RegistrationService` SHALL throw an `IllegalArgumentException` with message `"Username must not be blank."` before any database access occurs.
2. WHEN `RegistrationService.register` is called with a `username` whose length is less than 5 characters or greater than 18 characters, THE `RegistrationService` SHALL throw an `IllegalArgumentException` with message `"Username must be between 5 and 18 characters."` before any database access occurs.
3. WHEN `RegistrationService.register` is called with a `username` that already exists in the database (case-sensitive comparison), THE `RegistrationService` SHALL throw a `DuplicateUsernameException` without persisting a new record.
4. WHEN `RegistrationService.register` is called with a `username` between 5 and 18 characters that does not yet exist in the database, THE `RegistrationService` SHALL proceed to password validation without throwing an exception.

---

### Requirement 3: Password Validation

**User Story:** As a security-conscious developer, I want passwords validated against strength rules before hashing and persistence, so that only strong passwords are accepted.

#### Acceptance Criteria

1. WHEN `RegistrationService.register` is called with a `password` field that is blank, THE `RegistrationService` SHALL throw an `IllegalArgumentException` with message `"Password must not be blank."` before any encoding or database access occurs.
2. WHEN `PasswordValidator.getViolations` is called with a password that violates one or more of the following rules, THE `PasswordValidator` SHALL return a non-empty `List<String>` containing one entry per violated rule:
   - `"Password must be at least 8 characters long."` — minimum length of 8
   - `"Password must be no more than 72 characters long."` — maximum length of 72
   - `"Password must contain at least one uppercase letter."` — at least one character `A–Z`
   - `"Password must contain at least one lowercase letter."` — at least one character `a–z`
   - `"Password must contain at least one digit."` — at least one character `0–9`
   - `"Password must contain at least one special character (!@#$%^&*)."` — at least one from `!@#$%^&*`
   - `"Password must not contain whitespace."` — no space or other whitespace characters
3. WHEN `PasswordValidator.getViolations` is called with a password that satisfies all rules, THE `PasswordValidator` SHALL return an empty `List<String>`.
4. WHEN `RegistrationService.register` is called with a password that violates one or more strength rules, THE `RegistrationService` SHALL throw an `IllegalArgumentException` whose message lists each violation before any encoding or database access occurs.
5. THE full combined regex pattern that enforces all password strength rules simultaneously SHALL be `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*])\S{8,72}$`.

---

### Requirement 4: Password Hashing

**User Story:** As a security-conscious developer, I want passwords stored as BCrypt hashes, so that plaintext credentials are never persisted to the database.

#### Acceptance Criteria

1. WHEN `RegistrationService.register` is called with a valid username and a password that passes all strength rules, THE `RegistrationService` SHALL encode the password using `PasswordEncoder` before passing it to `UserRepository.save`.
2. WHEN a new `User` is saved during registration, THE `User` entity persisted to the database SHALL contain a BCrypt-encoded `password` field, and `PasswordEncoder.matches(originalPlaintext, savedPassword)` SHALL return `true`.
3. THE `RegistrationService` SHALL NOT store the plaintext password in the `User` entity or include it in the `RegisterResponse`.
4. WHEN a registration request is received with a plaintext password that satisfies all strength rules, THE BCrypt-encoded password stored in the database SHALL NOT satisfy `PasswordEncoder.matches` when called with any different plaintext password of length 8–72 characters.

---

### Requirement 5: User Persistence

**User Story:** As a developer, I want a new `User` record written to the SQLite database on successful registration, so that the account can be used for subsequent login.

#### Acceptance Criteria

1. WHEN `RegistrationService.register` completes all validations successfully, THE `RegistrationService` SHALL call `UserRepository.save` exactly once with a `User` entity whose `username` equals the submitted username and whose `password` is a BCrypt-encoded form of the submitted password.
2. WHEN `UserRepository.save` succeeds, THE `RegistrationService` SHALL return a `RegisterResponse` containing the persisted `User`'s `UUID userId` and `String username`.
3. WHEN `UserRepository.save` throws a `DataAccessException`, THE `RegistrationService` SHALL propagate the exception without swallowing it.
4. THE `UserRepository` SHALL declare `boolean existsByUsername(String username)`, returning `true` when at least one `User` with that username exists in the database and `false` otherwise.
5. THE `UserRepository` SHALL declare `Optional<User> findByUsername(String username)`, returning a non-empty `Optional` when a `User` with the given username exists and an empty `Optional` otherwise.

---

### Requirement 6: HTTP Status Codes and Error Responses

**User Story:** As a client developer, I want the registration API to return meaningful HTTP status codes and structured error bodies for every outcome, so that I can handle each case programmatically.

#### Acceptance Criteria

1. WHEN `RegistrationService.register` succeeds, THE `RegistrationController` SHALL return HTTP 201 with the `RegisterResponse` body.
2. IF an `IllegalArgumentException` is thrown during registration, THEN THE `GlobalExceptionHandler` SHALL return HTTP 400 with a JSON body containing `"status": 400` and a non-empty `"message"` string describing the validation failure.
3. IF a `DuplicateUsernameException` is thrown during registration, THEN THE `GlobalExceptionHandler` SHALL return HTTP 409 with a JSON body containing `"status": 409` and a non-empty `"message"` string.
4. WHEN an unhandled exception propagates from any controller, THE `GlobalExceptionHandler` SHALL return HTTP 500 with a JSON body containing `"status": 500`.
5. THE `GlobalExceptionHandler` SHALL set `Content-Type: application/json` on all error responses.

---

### Requirement 7: Registration Validation Order

**User Story:** As a developer, I want validations applied in a defined order, so that the error messages returned to the client are deterministic and predictable.

#### Acceptance Criteria

1. WHEN `RegistrationService.register` is called, THE `RegistrationService` SHALL apply validations in the following order:
   - Step 1: Validate `username` is not blank → `IllegalArgumentException`
   - Step 2: Validate `username` length is 5–18 characters → `IllegalArgumentException`
   - Step 3: Validate `password` is not blank → `IllegalArgumentException`
   - Step 4: Validate `password` satisfies all strength rules → `IllegalArgumentException`
   - Step 5: Check `existsByUsername` for duplicate → `DuplicateUsernameException`
   - Step 6: Encode password → `PasswordEncoder.encode`
   - Step 7: Persist → `UserRepository.save`
   - Step 8: Return `RegisterResponse`
2. THE `RegistrationService` SHALL NOT invoke `UserRepository` for any database read or write before all field-level validations (Steps 1–4) have passed.

---

### Requirement 8: Unit and Integration Tests

**User Story:** As a developer, I want a test suite covering registration logic, password validation, persistence, and HTTP behavior, so that regressions are caught automatically on every build.

#### Acceptance Criteria

1. THE test suite SHALL include a `RegistrationService` unit test that mocks `UserRepository` and `PasswordEncoder`, calls `register` with a valid `RegisterRequest`, and verifies that `UserRepository.save` is called exactly once with a `User` whose `password` field satisfies `PasswordEncoder.matches(rawPassword, savedPassword)`.
2. THE test suite SHALL include a `RegistrationService` unit test that stubs `UserRepository.existsByUsername` to return `true` and verifies that calling `register` throws `DuplicateUsernameException` without calling `UserRepository.save`.
3. THE test suite SHALL include a `RegistrationService` unit test that verifies calling `register` with a blank username throws `IllegalArgumentException` before any repository access.
4. THE test suite SHALL include a `RegistrationService` unit test that verifies calling `register` with a username shorter than 5 characters or longer than 18 characters throws `IllegalArgumentException`.
5. THE test suite SHALL include a `PasswordValidator` unit test that, for each individual rule violation (blank password, too short, no uppercase, no lowercase, no digit, no special character, contains whitespace), verifies that `getViolations` returns a list containing the corresponding violation message.
6. THE test suite SHALL include a `PasswordValidator` unit test that verifies `getViolations` returns an empty list for a password that satisfies all rules.
7. THE test suite SHALL include a `@DataJpaTest` test class for `UserRepository` using the H2 in-memory database that verifies `existsByUsername` returns `true` for a saved username and `false` for an unsaved username.
8. THE test suite SHALL include a `@WebMvcTest` test class for `RegistrationController` that mocks `RegistrationService` to throw `DuplicateUsernameException` and verifies that `POST /api/auth/register` returns HTTP 409.
9. THE test suite SHALL include a `@WebMvcTest` test class for `RegistrationController` that mocks `RegistrationService` to throw `IllegalArgumentException` and verifies that `POST /api/auth/register` returns HTTP 400.
10. THE test suite SHALL include a `@WebMvcTest` test class for `RegistrationController` that calls `register` with a valid `RegisterRequest` and verifies that `POST /api/auth/register` returns HTTP 201.
