# Requirements Document

## Introduction

This feature implements the complete User authentication vertical slice for the Todo Management Application.
It covers the persistence layer (`UserRepository`), business logic (`UserService`), HTTP layer (`AuthController`),
supporting infrastructure (`JwtUtil`, DTOs, exception classes), and unit/integration tests — all within the
existing Spring Boot 4.1.0 / Spring Data JPA / JJWT 0.13.0 stack.

The two public endpoints delivered are:
- `POST /api/auth/register` — create a new account
- `POST /api/auth/login`    — authenticate and receive a JWT

---

## Glossary

- **AuthController**: The Spring `@RestController` that handles requests to `/api/auth/*`.
- **UserRepository**: The Spring Data JPA interface that provides persistence operations for `User` entities.
- **UserService**: The Spring `@Service` class that contains all business logic for registration and login.
- **JwtUtil**: The Spring `@Component` class that generates and validates JSON Web Tokens using JJWT 0.13.0.
- **PasswordEncoder**: A BCrypt-based `PasswordEncoder` bean used to hash and verify passwords.
- **User**: The existing JPA entity (`entity/User.java`) with fields `UUID id`, `String username`, `String password`.
- **RegisterRequest**: A DTO carrying `username` and `password` fields for the registration endpoint.
- **LoginRequest**: A DTO carrying `username` and `password` fields for the login endpoint.
- **RegisterResponse**: A DTO carrying `accountId` (UUID) and `String username` fields returned after successful registration.
- **LoginResponse**: A DTO carrying `accountId` (UUID), `String username`, and `String token` fields returned after successful login.
- **JWT**: A JSON Web Token signed with HMAC-SHA256, issued by `JwtUtil` upon successful login.
- **DuplicateUsernameException**: A custom exception thrown by `UserService` when a registration username is already taken.
- **InvalidCredentialsException**: A custom exception thrown by `UserService` when login credentials do not match.
- **GlobalExceptionHandler**: A `@ControllerAdvice` class that maps exceptions to HTTP error responses.
- **H2**: The in-memory database used in the test context in place of SQLite.
- **blank**: A string that is either `null`, empty (`""`), or contains only whitespace characters.

---

## Requirements

### Requirement 1: UserRepository Interface

**User Story:** As a developer, I want a proper Spring Data JPA repository for `User` entities, so that the service
layer can perform database operations without writing boilerplate SQL.

#### Acceptance Criteria

1. THE `UserRepository` SHALL extend `JpaRepository<User, UUID>`.
2. THE `UserRepository` SHALL declare a method `Optional<User> findByUsername(String username)` that returns a non-empty `Optional` containing the matching `User` when a record with the given username exists, and returns an empty `Optional` when no such record exists.
3. THE `UserRepository` SHALL declare a method `boolean existsByUsername(String username)` that returns `true` when at least one `User` with the given username exists in the database, and `false` otherwise.
4. WHEN the `UserRepository` is loaded in the H2 test context and a `User` is saved, THE `UserRepository` SHALL retrieve via `findByUsername` a `User` whose `id`, `username`, and `password` fields are equal to those of the saved entity.

---

### Requirement 2: Password Hashing

**User Story:** As a security-conscious developer, I want passwords stored as BCrypt hashes, so that plaintext
credentials are never persisted to the database.

#### Acceptance Criteria

1. WHEN a new `User` is saved during registration, THE `UserService` SHALL store a BCrypt-encoded form of the password in the `password` field rather than the plaintext value.
2. WHEN a login request is received with matching credentials, THE `UserService` SHALL grant access only when the provided plaintext password satisfies BCrypt verification against the stored hash.
3. WHEN a registration request is received with a plaintext password between 1 and 72 characters (inclusive), THE BCrypt-encoded password stored in the database SHALL satisfy BCrypt verification against that original plaintext.
4. THE `UserService` SHALL ensure that a plaintext password encoded with BCrypt SHALL NOT satisfy BCrypt verification against the BCrypt hash of any different plaintext password of length 1–72 characters.
5. WHEN a registration request is received with a `password` field that is blank, THE `UserService` SHALL reject the request with an `IllegalArgumentException` before any encoding or database access occurs.

---

### Requirement 3: User Registration

**User Story:** As a new user, I want to register an account with a username and password, so that I can start
managing my todo tasks.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/register` request is received with a `RegisterRequest` body whose `username` is 1–50 characters and `password` is 1–72 characters, THE `AuthController` SHALL delegate to `UserService.register(RegisterRequest)` and return HTTP 200 with a `RegisterResponse` body containing the new account's UUID and username.
2. WHEN `UserService.register` is called with a username of 1–50 characters that does not yet exist in the database, THE `UserService` SHALL hash the password, persist a new `User` via `UserRepository`, and return a `RegisterResponse` containing the persisted `User`'s UUID and username.
3. WHEN `UserService.register` is called with a `username` field that is blank, THE `UserService` SHALL throw an `IllegalArgumentException` before any database access occurs.
4. WHEN `UserService.register` is called with a `password` field that is blank, THE `UserService` SHALL throw an `IllegalArgumentException` before any database access occurs.
5. WHEN `UserService.register` is called with a username that already exists in the database (case-sensitive comparison), THE `UserService` SHALL throw a `DuplicateUsernameException` without persisting a new record.
6. IF a `DuplicateUsernameException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 409 with a response body `{"status": 409}`.
7. IF an `IllegalArgumentException` is thrown during registration, THEN THE `GlobalExceptionHandler` SHALL return HTTP 400 with a response body `{"status": 400}`.
8. WHEN `UserRepository.save` throws a `DataAccessException` during registration, THE `UserService` SHALL propagate the exception without swallowing it.

---

### Requirement 4: User Login

**User Story:** As a registered user, I want to log in with my username and password and receive a JWT, so that I
can authenticate subsequent API requests.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/login` request is received with a `LoginRequest` body whose `username` is 1–50 characters and `password` is 1–72 characters, THE `AuthController` SHALL delegate to `UserService.login(LoginRequest)` and return HTTP 200 with a `LoginResponse` body containing the user's UUID, username, and a signed JWT string.
2. WHEN `UserService.login` is called with a username that does not exist in the database, THE `UserService` SHALL throw an `InvalidCredentialsException`.
3. WHEN `UserService.login` is called with a username that exists but a password that does not match the stored BCrypt hash, THE `UserService` SHALL throw an `InvalidCredentialsException`.
4. IF an `InvalidCredentialsException` is thrown, THEN THE `GlobalExceptionHandler` SHALL return HTTP 401 with a response body `{"status": 401}`.
5. WHEN `UserService.login` is called with credentials that match a stored `User`, THE `UserService` SHALL call `JwtUtil.generateToken(User)` and include the resulting token string — which expires in 24 hours — in the returned `LoginResponse`.
6. WHEN `UserService.login` is called with a `username` or `password` field that is blank, THE `UserService` SHALL throw an `IllegalArgumentException` before any database access occurs.
7. WHEN a `POST /api/auth/login` request is received with a malformed or absent request body, THE `AuthController` SHALL return HTTP 400.

---

### Requirement 5: JWT Generation and Validation

**User Story:** As a developer, I want a JWT utility class that generates and validates tokens using JJWT 0.13.0,
so that downstream filters can authenticate incoming requests.

#### Acceptance Criteria

1. WHEN `JwtUtil.generateToken(User)` is called, THE `JwtUtil` SHALL produce a JWT string signed with HMAC-SHA256 using a secret key of at least 256 bits, whose subject claim equals the `User`'s username.
2. WHEN `JwtUtil.generateToken(User)` is called, THE `JwtUtil` SHALL embed the `User`'s UUID as an additional claim named `userId` in the token payload.
3. WHEN `JwtUtil.generateToken(User)` is called, THE `JwtUtil` SHALL set a token expiration of exactly 86400 seconds (24 hours) from the time of generation.
4. WHEN `JwtUtil.extractUsername(String token)` is called with a valid, non-expired token, THE `JwtUtil` SHALL return the username that was embedded as the subject claim.
5. WHEN `JwtUtil.extractUsername(String token)` is called with a malformed, invalid-signature, or expired token, THE `JwtUtil` SHALL return `null` without throwing an unchecked exception.
6. WHEN `JwtUtil.isTokenValid(String token, String username)` is called with a valid, non-expired token whose subject matches the given username, THE `JwtUtil` SHALL return `true`.
7. WHEN `JwtUtil.isTokenValid(String token, String username)` is called with a token whose subject does not match the given username, THE `JwtUtil` SHALL return `false`.
8. WHEN `JwtUtil.isTokenValid(String token, String username)` is called with an expired or malformed token, THE `JwtUtil` SHALL return `false` without throwing an unchecked exception.
9. WHEN a `User` with a non-null, non-blank username is provided to `JwtUtil.generateToken`, THE `JwtUtil` SHALL produce a token such that `extractUsername(token)` returns a value equal to the original `User`'s username.

---

### Requirement 6: Request and Response DTOs

**User Story:** As a developer, I want dedicated DTO classes for auth request and response payloads, so that
entity internals are never directly exposed over the HTTP API.

#### Acceptance Criteria

1. THE `RegisterRequest` DTO SHALL contain a non-null, non-blank `String username` field and a non-null, non-blank `String password` field.
2. THE `LoginRequest` DTO SHALL contain a non-null, non-blank `String username` field and a non-null, non-blank `String password` field.
3. THE `RegisterResponse` DTO SHALL contain a non-null `UUID accountId` field and a non-null `String username` field.
4. THE `LoginResponse` DTO SHALL contain a non-null `UUID accountId` field, a non-null `String username` field, and a non-null `String token` field.
5. THE `AuthController` SHALL accept `RegisterRequest` as the `@RequestBody` parameter for `POST /api/auth/register` and return a `RegisterResponse` as the response body; THE response type SHALL be `RegisterResponse` and SHALL NOT be the `User` entity class or any type that exposes `User` fields beyond `accountId` and `username`.
6. THE `AuthController` SHALL accept `LoginRequest` as the `@RequestBody` parameter for `POST /api/auth/login` and return a `LoginResponse` as the response body; THE response type SHALL be `LoginResponse` and SHALL NOT be the `User` entity class or any type that exposes `User` fields beyond `accountId`, `username`, and `token`.

---

### Requirement 7: Global Exception Handling

**User Story:** As a developer, I want a centralized exception handler, so that all auth errors are translated to
consistent HTTP error responses without duplicating error-mapping logic in controllers.

#### Acceptance Criteria

1. THE `GlobalExceptionHandler` SHALL handle exceptions thrown from any controller in the `com.revature.todomanagement` package and SHALL set `Content-Type: application/json` on all error responses.
2. WHEN a `DuplicateUsernameException` is handled, THE `GlobalExceptionHandler` SHALL return an HTTP 409 response whose JSON body contains the integer key `"status"` with value `409` and a non-empty string key `"message"`.
3. WHEN an `InvalidCredentialsException` is handled, THE `GlobalExceptionHandler` SHALL return an HTTP 401 response whose JSON body contains the integer key `"status"` with value `401` and a non-empty string key `"message"`.
4. WHEN an `IllegalArgumentException` is handled, THE `GlobalExceptionHandler` SHALL return an HTTP 400 response whose JSON body contains the integer key `"status"` with value `400` and a non-empty string key `"message"`.
5. WHEN an unhandled exception type propagates from any controller, THE `GlobalExceptionHandler` SHALL return an HTTP 500 response whose JSON body contains the integer key `"status"` with value `500`.

---

### Requirement 8: Unit and Integration Tests

**User Story:** As a developer, I want a suite of JUnit 5 tests covering the repository, service, and controller
layers, so that regressions are caught automatically on every build.

#### Acceptance Criteria

1. THE test suite SHALL include a `@DataJpaTest` test class for `UserRepository` that uses the H2 in-memory database and verifies that `findByUsername` returns a `User` whose `username` field equals the username of the saved `User`.
2. THE test suite SHALL include a `@DataJpaTest` test class for `UserRepository` that verifies `existsByUsername` returns `true` for a saved username and `false` for a username that was never saved.
3. THE test suite SHALL include a `UserService` unit test class that mocks `UserRepository` and `PasswordEncoder`, calls `register` with valid input, and verifies that `UserRepository.save` was called exactly once with a `User` whose `password` field satisfies `PasswordEncoder.matches(rawPassword, savedPassword)`.
4. THE test suite SHALL include a `UserService` unit test class that stubs `UserRepository.existsByUsername` to return `true` and verifies that calling `register` throws `DuplicateUsernameException` without calling `UserRepository.save`.
5. THE test suite SHALL include a `UserService` unit test class that stubs `UserRepository.findByUsername` to return an empty `Optional` and verifies that calling `login` throws `InvalidCredentialsException`.
6. THE test suite SHALL include a `UserService` unit test class that stubs `UserRepository.findByUsername` to return a `User` and stubs `PasswordEncoder.matches` to return `false`, then verifies that calling `login` throws `InvalidCredentialsException`.
7. THE test suite SHALL include a `JwtUtil` unit test that verifies that for a `User` with a non-blank username, `extractUsername(generateToken(user))` returns a value equal to the `User`'s username.
8. THE test suite SHALL include a `@WebMvcTest` test class for `AuthController` that mocks `UserService` to throw `DuplicateUsernameException` and verifies that `POST /api/auth/register` with a valid JSON body returns HTTP 409.
9. THE test suite SHALL include a `@WebMvcTest` test class for `AuthController` that mocks `UserService` to throw `InvalidCredentialsException` and verifies that `POST /api/auth/login` with a valid JSON body returns HTTP 401.
