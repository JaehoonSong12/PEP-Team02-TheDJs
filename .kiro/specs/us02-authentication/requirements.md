# Requirements Document

## Introduction

This feature implements User Story 2 (Authentication) for the Todo Management Application. It delivers
the `POST /api/auth/login` endpoint, a JWT security filter that protects routes, and Spring Security
configuration — all building on the infrastructure established by US01 (Account Creation).

This spec assumes the following prerequisites from US01 are in place:
- `User` entity (`UUID id`, `String username`, `String password`) persisted to the `users` table
- `UserRepository` with `findByUsername(String)` and `existsByUsername(String)`
- `UserService` with `findByUsername(String)` returning `Optional<User>`
- `JwtUtil` with `generateToken(User)`, `extractUsername(String)`, and `isTokenValid(String, String)`
- Passwords stored as plaintext strings (matching US01's persistence behavior)

One public endpoint is delivered:
- `POST /api/auth/login` — verify credentials and return a signed JWT

The existing `JwtUtil` signs tokens with HMAC-SHA256, uses the username as the `sub` claim,
embeds the user's UUID as a `userId` claim, and sets a 24-hour expiry.

---

## Glossary

- **System**: The Spring Boot 4.1.0 REST API process.
- **LoginController**: The Spring `@RestController` that handles `POST /api/auth/login` and contains local `@ExceptionHandler` methods for authentication error mapping.
- **UserService**: The existing Spring `@Service` that provides `findByUsername(String)` for credential lookup.
- **JwtUtil**: The existing Spring `@Component` that generates and validates JWTs (subject = username, userId claim = UUID, 24-hour expiry).
- **JwtFilter**: A new `OncePerRequestFilter` that intercepts HTTP requests, extracts the Bearer token from the `Authorization` header, validates it via `JwtUtil`, loads the `User` from the database, and populates the Spring `SecurityContext`.
- **SecurityConfig**: A new `@Configuration` class that defines the Spring Security filter chain, permits `/api/auth/**` publicly, requires authentication on `/api/todos/**`, registers `JwtFilter`, and configures stateless sessions.
- **User**: The existing JPA entity with fields `UUID id`, `String username`, `String password`.
- **UserRepository**: The existing Spring Data JPA interface providing `findByUsername(String)` and `existsByUsername(String)`.
- **InvalidCredentialsException**: A new custom `RuntimeException` thrown when login credentials do not match any record.
- **Blank**: A field value that is `null`, an empty string (`""`), or a string composed entirely of whitespace characters.
- **Authenticated_User**: The `User` entity resolved from the JWT and stored in the `SecurityContext`; available to downstream controllers via `SecurityContextHolder`.

---

## Requirements

### Requirement 1: Login Endpoint

**User Story:** As a registered user, I want to log in with my username and password and receive a JWT, so that I can securely access my todo items on subsequent requests.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/login` request is received with a `@RequestBody User` body containing a `username` and `password` that match a record in the `users` table (exact string comparison on password), THE `LoginController` SHALL return HTTP 200 with the signed JWT string in the `Authorization` response header formatted as `Bearer <token>`.
2. IF a `POST /api/auth/login` request is received and no `User` with the submitted `username` exists in the database, THEN THE `LoginController` SHALL return HTTP 401 with a plain text body containing a generic error message that does not reveal whether the username or password was incorrect.
3. IF a `POST /api/auth/login` request is received and a `User` with the submitted `username` exists but the submitted `password` does not equal the stored `password` value, THEN THE `LoginController` SHALL return HTTP 401 with a plain text body containing the same generic error message as criterion 2.
4. IF a `POST /api/auth/login` request is received with a `username` that is Blank or a `password` that is Blank, THEN THE `LoginController` SHALL return HTTP 400 with a plain text body describing the validation failure.
5. WHEN a `POST /api/auth/login` request is received with a malformed or absent request body, THE `LoginController` SHALL return HTTP 400.
6. THE `LoginController` SHALL use `UserService.findByUsername` to load the user and `JwtUtil.generateToken(User)` to produce the token; it SHALL NOT re-implement token generation logic.
7. THE `LoginController` SHALL set `Content-Type: text/plain` on all error responses, following the same pattern as `RegistrationController`.

---

### Requirement 2: JWT Security Filter (JwtFilter)

**User Story:** As a system operator, I want all requests to protected routes intercepted by a filter that validates the Bearer token, so that unauthenticated callers cannot access user data.

#### Acceptance Criteria

1. THE `JwtFilter` SHALL extend `OncePerRequestFilter` and SHALL execute once per request.
2. WHEN a request carries an `Authorization` header whose value starts with `Bearer ` (case-sensitive, followed by a non-empty token string), THE `JwtFilter` SHALL extract the token portion and call `JwtUtil.extractUsername(token)` to retrieve the username.
3. WHEN `JwtUtil.extractUsername` returns a non-null username and the `SecurityContext` does not already contain an authentication, THE `JwtFilter` SHALL load the `User` via `UserRepository.findByUsername`, verify the token via `JwtUtil.isTokenValid(token, user.getUsername())`, and on success store a `UsernamePasswordAuthenticationToken` in the `SecurityContext` with the `User` as principal.
4. WHEN a request to `/api/todos/**` is received without an `Authorization` header, with an empty token, or with an invalid/expired token, THE System SHALL return HTTP 401 before the request reaches any controller method.
5. WHEN a request targets `/api/auth/**`, THE `JwtFilter` SHALL allow the request to proceed regardless of whether a valid token is present.
6. IF `JwtUtil.extractUsername` returns `null` (malformed, expired, or tampered token), THE `JwtFilter` SHALL NOT populate the `SecurityContext` and SHALL continue the filter chain (letting Spring Security deny the request for protected routes).
7. IF the `sub` claim in the token does not correspond to any username in the database, THE `JwtFilter` SHALL NOT populate the `SecurityContext`.

---

### Requirement 3: Spring Security Configuration

**User Story:** As a system operator, I want a centralized security configuration that defines which routes are public and which require authentication, so that the filter chain is predictable.

#### Acceptance Criteria

1. THE `SecurityConfig` SHALL add `spring-boot-starter-security` as a project dependency.
2. THE `SecurityConfig` SHALL configure the Spring Security filter chain to permit all requests to `/api/auth/**` without requiring authentication.
3. THE `SecurityConfig` SHALL configure the Spring Security filter chain to require authentication for all requests to `/api/todos/**`.
4. THE `SecurityConfig` SHALL disable CSRF protection.
5. THE `SecurityConfig` SHALL set session management to `STATELESS`.
6. THE `SecurityConfig` SHALL register the `JwtFilter` before `UsernamePasswordAuthenticationFilter` in the filter chain.
7. THE `SecurityConfig` SHALL add the `jwt.secret` property to `application.properties` with a value of at least 32 characters (256 bits) for HMAC-SHA256 key derivation.
8. WHEN an unauthenticated request is rejected by the filter chain, THE System SHALL return HTTP 401.

---

### Requirement 4: Authentication Error Handling

**User Story:** As a client developer, I want authentication errors returned as plain text with appropriate HTTP status codes, so that I can handle each case programmatically.

#### Acceptance Criteria

1. THE `LoginController` SHALL declare a local `@ExceptionHandler(InvalidCredentialsException.class)` that returns HTTP 401 with `Content-Type: text/plain` and a plain text body containing the exception message.
2. THE `InvalidCredentialsException` SHALL extend `RuntimeException` and produce a generic message that does not reveal whether the username or password was incorrect.
3. IF a `POST /api/auth/login` request results in HTTP 401 due to a non-existent username or an incorrect password, THE `LoginController` SHALL return identical HTTP status and identical response body text for both cases.
4. THE error handling pattern SHALL follow the existing `RegistrationController` convention: local `@ExceptionHandler` methods in the controller class, plain text content type, no JSON error bodies.

---

### Requirement 5: Tests

**User Story:** As a developer, I want a test suite covering login logic, JWT filter behavior, and controller responses, so that regressions are caught automatically.

#### Acceptance Criteria

1. THE test suite SHALL include a unit test that mocks `UserService.findByUsername` to return a `User` with matching password, calls the login logic, and verifies that `JwtUtil.generateToken` is invoked and the response includes the token in the `Authorization` header.
2. THE test suite SHALL include a unit test that mocks `UserService.findByUsername` to return an empty `Optional` and verifies that `InvalidCredentialsException` is thrown.
3. THE test suite SHALL include a unit test that mocks `UserService.findByUsername` to return a `User` with a non-matching password and verifies that `InvalidCredentialsException` is thrown.
4. THE test suite SHALL include a unit test for `JwtFilter` that provides a valid token (generated by `JwtUtil`), verifies the `SecurityContext` is populated with the correct `User` principal.
5. THE test suite SHALL include a unit test for `JwtFilter` that provides an invalid token string and verifies the `SecurityContext` remains empty.
6. THE test suite SHALL include a `@WebMvcTest` test for `LoginController` that mocks `UserService` to return a valid `User` and verifies `POST /api/auth/login` with correct credentials returns HTTP 200 with an `Authorization` header starting with `Bearer `.
7. THE test suite SHALL include a `@WebMvcTest` test for `LoginController` that mocks `UserService` to return an empty `Optional` and verifies `POST /api/auth/login` returns HTTP 401.
8. THE test suite SHALL include a jqwik `@Property(tries = 100)` test verifying: for any Blank value (null, empty, or whitespace-only) supplied as `username` or `password`, the login logic throws an exception without invoking `UserService.findByUsername`.
