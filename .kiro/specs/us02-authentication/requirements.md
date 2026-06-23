# Requirements Document

## Introduction

This document specifies the requirements for User Story 2: Authentication of the Todo Management Application. The feature delivers JWT-based login, token validation infrastructure, and route protection within the existing Spring Boot 4.1.0 / Java 21 stack backed by SQLite.

This spec assumes User Story 1 (Registration) has already been implemented, meaning the `users` table is populated with accounts whose passwords are stored as BCrypt hashes. This feature builds on that foundation.

One public endpoint is delivered:
- `POST /api/auth/login` — authenticate credentials and receive a JWT via the `Authorization` response header

JWTs are signed with HMAC-SHA256, carry the `accountId` as the `sub` claim, and expire after 3600 seconds (1 hour). A `JwtFilter` intercepts requests to protected routes (`/api/todos/**`), validates the Bearer token, and populates the Spring `SecurityContext`.

---

## Glossary

- **System**: The Spring Boot 4.1.0 REST API process, collectively referred to as "the System" when no finer-grained component name applies.
- **AuthController**: The Spring `@RestController` mapped to `/api/auth/**`, responsible for authentication endpoints.
- **UserService**: The Spring `@Service` class containing business logic for login (credential verification and JWT issuance).
- **UserRepository**: The Spring Data JPA interface that provides persistence operations for `User` entities against the `users` table (assumed to exist from US01).
- **JwtUtil**: The Spring `@Component` responsible for generating, signing, parsing, and validating JWT tokens using JJWT 0.13.0 and the server-side secret key.
- **JwtFilter**: The `OncePerRequestFilter` component that intercepts every HTTP request, extracts and validates a Bearer token from the `Authorization` header, and populates the Spring `SecurityContext` when the token is valid.
- **SecurityConfig**: The `@Configuration` class that defines the Spring Security filter chain, permits `/api/auth/**` publicly, requires authentication on `/api/todos/**`, registers the `JwtFilter`, and exposes the `PasswordEncoder` bean.
- **PasswordEncoder**: A `BCryptPasswordEncoder` bean used to verify passwords during login.
- **User**: The existing JPA entity (`entity/User.java`) mapped to the `users` table with fields: `id` (Integer, PK AUTOINCREMENT), `username` (String, unique), `email` (String, unique), `passwordHash` (String), `createdAt` (String), `updatedAt` (String).
- **LoginRequest**: The request DTO for login; fields are `username` (String), `password` (String).
- **AuthResponse**: The response DTO for login; fields are `accountId` (Integer) and `username` (String).
- **JWT**: A JSON Web Token signed with HMAC-SHA256, issued by `JwtUtil` upon successful login. The `sub` claim contains the user's `accountId` as a string. Expiry is 3600 seconds from issuance.
- **Bearer_Token**: A JWT value transmitted in the HTTP `Authorization` header using the `Bearer` scheme.
- **InvalidCredentialsException**: A custom `RuntimeException` thrown when login credentials do not match; mapped to HTTP 401.
- **UnauthorizedAccessException**: A custom `RuntimeException` thrown when a valid JWT does not resolve to an existing user or when token validation fails in the filter; mapped to HTTP 401.
- **GlobalExceptionHandler**: The `@RestControllerAdvice` class that maps application exceptions to standardized JSON error responses.
- **Blank**: A field value that is `null`, an empty string (`""`), or a string composed entirely of whitespace characters.
- **Authenticated_User**: The `User` entity resolved from the JWT `sub` claim (accountId) stored in the `SecurityContext`; available to downstream controllers via `SecurityContextHolder`.

---

## Requirements

### Requirement 1: User Login and JWT Issuance

**User Story:** As a registered user, I want to log in with my username and password and receive a token, so that I can securely access my todo items on subsequent requests.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/login` request is received with a `LoginRequest` body containing a `username` and `password` that match a record in the `users` table (password verified via `BCryptPasswordEncoder.matches()`), THE `AuthController` SHALL return HTTP 200 with an `AuthResponse` body containing the matching `accountId` (positive integer) and `username`, and SHALL include a signed JWT in the `Authorization` response header formatted as `Bearer <token>` where `<token>` is a valid JWT whose `sub` claim equals the `accountId` as a string.
2. IF a `POST /api/auth/login` request is received and the submitted `username` does not exist in the `users` table, THEN THE `AuthController` SHALL return HTTP 401 with a JSON body containing a `"message"` key with a non-empty string value that does not reveal whether the username or the password was incorrect.
3. IF a `POST /api/auth/login` request is received and the submitted `password` does not match the BCrypt hash stored in `users.password_hash` for the given username, THEN THE `AuthController` SHALL return HTTP 401 with a JSON body containing a `"message"` key with a non-empty string value that does not reveal whether the username or the password was incorrect.
4. IF a `POST /api/auth/login` request is received with a Blank `username` or a Blank `password`, THEN THE `AuthController` SHALL return HTTP 400 with the JSON body `{"status": 400}`.
5. THE `UserService` SHALL use `BCryptPasswordEncoder.matches(rawPassword, storedHash)` to verify the submitted password against `users.password_hash`; direct plaintext comparison SHALL NOT be used.
6. WHEN a `POST /api/auth/login` request is received with a body that is not valid JSON, has an unsupported `Content-Type`, or is entirely absent, THE `AuthController` SHALL return HTTP 400 with the JSON body `{"status": 400}`.
7. IF a `POST /api/auth/login` request results in HTTP 401 due to a non-existent username or an incorrect password, THEN THE `AuthController` SHALL return identical HTTP status codes and identical response body structures for both cases so that a caller cannot distinguish between a wrong username and a wrong password.

---

### Requirement 2: JWT Generation and Validation (JwtUtil)

**User Story:** As a developer, I want a JWT utility class that generates and validates tokens using JJWT 0.13.0, so that the security filter can authenticate incoming requests on protected routes.

#### Acceptance Criteria

1. WHEN `JwtUtil.generateToken(User)` is called, THE `JwtUtil` SHALL produce a JWT string signed with HMAC-SHA256 using a secret key of at least 256 bits, whose `sub` claim equals the `User`'s `accountId` (the `id` field) as a string.
2. WHEN `JwtUtil.generateToken(User)` is called, THE `JwtUtil` SHALL set a token expiration of exactly 3600 seconds (1 hour) from the time of generation.
3. WHEN `JwtUtil.generateToken(User)` is called, THE `JwtUtil` SHALL embed the `User`'s `username` as an additional claim named `username` in the token payload.
4. WHEN `JwtUtil.extractAccountId(String token)` is called with a valid, non-expired token, THE `JwtUtil` SHALL return the `accountId` string that was embedded as the `sub` claim.
5. IF `JwtUtil.extractAccountId(String token)` is called with a null token string, a malformed token, a token with an invalid signature, or an expired token, THEN THE `JwtUtil` SHALL return `null` without throwing an unchecked exception.
6. WHEN `JwtUtil.isTokenValid(String token)` is called with a valid, non-expired token that has a valid HMAC-SHA256 signature matching the server's secret key and whose `exp` claim is in the future, THE `JwtUtil` SHALL return `true`; this method SHALL NOT query the database or verify that the `sub` claim corresponds to a persisted user.
7. IF `JwtUtil.isTokenValid(String token)` is called with a null token string, an expired token, a malformed token, or a token with an invalid signature, THEN THE `JwtUtil` SHALL return `false` without throwing an unchecked exception.
8. FOR ALL `User` entities with a non-null `id` and non-blank `username`, `JwtUtil.generateToken(user)` SHALL produce a token such that `extractAccountId(token)` returns a value equal to `String.valueOf(user.getId())` (JWT round-trip property).
9. IF `JwtUtil.generateToken(User)` is called with a null `User` argument or a `User` whose `id` is null, THEN THE `JwtUtil` SHALL throw an `IllegalArgumentException` without producing a token.

---

### Requirement 3: JWT Security Filter (JwtFilter)

**User Story:** As a system operator, I want all requests to protected routes to be intercepted by a filter that validates the Bearer token, so that unauthenticated callers cannot access user data.

#### Acceptance Criteria

1. WHEN a request to any path is received, THE `JwtFilter` SHALL execute once per request and SHALL only attempt token extraction and validation if the `Authorization` header is present and its value starts with `Bearer ` (case-sensitive, followed by at least one non-whitespace character); if the header is absent or does not match this prefix, the filter SHALL continue the filter chain without populating the `SecurityContext`.
2. WHEN a request carries a valid Bearer token (non-expired, valid signature, `sub` claim resolves to an existing `users.id`), THE `JwtFilter` SHALL extract the `sub` claim (the user's `accountId`), load the corresponding `User` from the database, and store a `UsernamePasswordAuthenticationToken` in the Spring `SecurityContext` for the duration of the request, with the `User` as the principal, `null` credentials, and a granted authority of `ROLE_USER` so that `SecurityContextHolder.getContext().getAuthentication().isAuthenticated()` returns `true`.
3. WHEN a request to `/api/todos/**` is received without an `Authorization` header or with an empty `Authorization` header value, THE `JwtFilter` SHALL NOT populate the `SecurityContext`, and THE `SecurityConfig` SHALL cause the System to return HTTP 401 before the request reaches any controller method.
4. WHEN a request to `/api/todos/**` is received with a malformed JWT (not a valid three-part Base64url-encoded structure, including a `Bearer ` prefix followed by only whitespace or an empty string), THE `JwtFilter` SHALL NOT populate the `SecurityContext`, and THE System SHALL return HTTP 401.
5. WHEN a request to `/api/todos/**` is received with an expired JWT (current time is past the `exp` claim), THE `JwtFilter` SHALL NOT populate the `SecurityContext`, and THE System SHALL return HTTP 401.
6. WHEN a request carries a JWT with a valid signature and non-expired `exp` claim but the `sub` claim does not correspond to any `users.id` in the database, THE `JwtFilter` SHALL NOT populate the `SecurityContext`, and THE System SHALL return HTTP 401 for protected routes.
7. WHEN a request targets `/api/auth/**`, THE `JwtFilter` SHALL skip token validation and allow the request to proceed without authentication regardless of whether an `Authorization` header is present.
8. IF a JWT presented to the `JwtFilter` has an altered signature or a modified payload (i.e., signature verification fails), THEN THE `JwtFilter` SHALL reject the token (treat as invalid), SHALL NOT populate the `SecurityContext`, and THE System SHALL return HTTP 401 for protected routes.
9. WHILE the `SecurityContext` is populated by the `JwtFilter` for a given request, THE `JwtFilter` SHALL ensure the authentication is scoped to that request only; the `SecurityContext` SHALL be cleared after the response completes so that subsequent requests on the same thread do not inherit a prior authentication state.

---

### Requirement 4: Spring Security Configuration (SecurityConfig)

**User Story:** As a system operator, I want a centralized security configuration that defines which routes are public and which require authentication, so that the filter chain is predictable and maintainable.

#### Acceptance Criteria

1. THE `SecurityConfig` SHALL configure the Spring Security filter chain to permit all requests to `/api/auth/**` (any HTTP method) without requiring a Bearer token.
2. THE `SecurityConfig` SHALL configure the Spring Security filter chain to require authentication (a valid JWT) for all requests to `/api/todos/**`; any request to `/api/todos/**` that does not carry a valid Bearer token SHALL receive HTTP 401.
3. THE `SecurityConfig` SHALL configure the Spring Security filter chain to deny by default all requests that do not match `/api/auth/**` or `/api/todos/**`, returning HTTP 401 for unauthenticated requests to unmatched paths.
4. THE `SecurityConfig` SHALL disable CSRF protection, since the API is stateless and uses Bearer tokens for authentication.
5. THE `SecurityConfig` SHALL set session management to `STATELESS` so that no HTTP session is created or used.
6. THE `SecurityConfig` SHALL register the `JwtFilter` before the `UsernamePasswordAuthenticationFilter` in the Spring Security filter chain.
7. THE `SecurityConfig` SHALL expose a `@Bean PasswordEncoder` that returns a `BCryptPasswordEncoder` instance.
8. WHEN an unauthenticated request is rejected by the filter chain (HTTP 401), THE `SecurityConfig` SHALL configure an `AuthenticationEntryPoint` that returns a response with `Content-Type: application/json` and a JSON body containing `"status": 401`.

---

### Requirement 5: Login DTOs

**User Story:** As a developer, I want dedicated DTO classes for login request and response payloads, so that entity internals are never directly exposed over the HTTP API.

#### Acceptance Criteria

1. THE `LoginRequest` DTO SHALL contain a non-null, non-blank `String username` field (maximum 50 characters) and a non-null, non-blank `String password` field (maximum 128 characters).
2. THE `AuthResponse` DTO SHALL contain a non-null `Integer accountId` field and a non-null `String username` field.
3. THE `AuthController` SHALL accept `LoginRequest` as the `@RequestBody` parameter for `POST /api/auth/login` and return an `AuthResponse` as the response body with the JWT placed in the `Authorization` response header; THE response body SHALL NOT contain the token string.
4. THE `LoginRequest` and `AuthResponse` DTOs SHALL NOT carry JPA annotations (`@Entity`, `@Table`, `@Column`, `@Id`) and SHALL NOT extend or reference the `User` entity class, ensuring complete separation between API payloads and persistence internals.

---

### Requirement 6: Custom Exceptions for Authentication

**User Story:** As a developer, I want domain-specific exception classes for authentication failures, so that error conditions are modeled explicitly and mapped consistently to HTTP status codes.

#### Acceptance Criteria

1. THE `InvalidCredentialsException` SHALL extend `RuntimeException`, SHALL provide a no-argument constructor, and SHALL produce a fixed message (returned by `getMessage()`) that is a non-empty string which does not contain the literal text "username" or "password" (case-insensitive), ensuring callers cannot determine which credential was incorrect.
2. THE `UnauthorizedAccessException` SHALL extend `RuntimeException` and SHALL accept a `String message` parameter in its constructor, producing a message (returned by `getMessage()`) equal to the supplied string.
3. WHEN an `InvalidCredentialsException` or `UnauthorizedAccessException` is instantiated, THE exception instance SHALL be assignable to `RuntimeException` and SHALL not require a checked-exception declaration in method signatures that throw it.

---

### Requirement 7: Global Exception Handling for Authentication

**User Story:** As a developer, I want a centralized exception handler for authentication errors, so that all auth errors are translated to consistent HTTP error responses without duplicating error-mapping logic in controllers.

#### Acceptance Criteria

1. THE `GlobalExceptionHandler` SHALL be annotated with `@RestControllerAdvice` and SHALL handle exceptions thrown from any controller in the `com.revature.todomanagement` package, setting `Content-Type: application/json` on all error responses.
2. WHEN an `InvalidCredentialsException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 401 with a JSON body containing at minimum the field `"message"` as a non-empty string that does not reveal whether the username or the password was incorrect; the response body SHALL NOT include stack traces, internal class names, or exception cause chains.
3. WHEN an `IllegalArgumentException` is handled (from input validation), THE `GlobalExceptionHandler` SHALL return HTTP 400 with a JSON body containing at minimum the field `"status": 400`; the handler SHALL catch all `IllegalArgumentException` instances regardless of their origin within the controller layer.
4. WHEN an `UnauthorizedAccessException` is handled, THE `GlobalExceptionHandler` SHALL return HTTP 401 with a JSON body containing at minimum the field `"status": 401`; the response body SHALL NOT include stack traces, internal class names, or exception cause chains.
5. IF an unhandled exception type propagates from any controller, THEN THE `GlobalExceptionHandler` SHALL return HTTP 500 with a JSON body containing at minimum the field `"status": 500`; the response body SHALL NOT include stack traces, internal class names, or the original exception message.
6. IF multiple `@ExceptionHandler` methods could match a thrown exception (e.g., a subclass of `IllegalArgumentException`), THEN THE `GlobalExceptionHandler` SHALL resolve to the most specific matching handler method so that custom exception types always take precedence over their supertypes.

---

### Requirement 8: Login Input Validation

**User Story:** As an API consumer, I want the server to reject malformed login input with clear error codes, so that client applications can handle validation errors predictably.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/login` request is received with a Blank `username` or a Blank `password`, THE System SHALL return HTTP 400 with the JSON body `{"status": 400}` before any service method is invoked.
2. WHEN a `POST /api/auth/login` request is received with a body that is not valid JSON, has an unsupported `Content-Type`, or is entirely absent, THE System SHALL return HTTP 400 with the JSON body `{"status": 400}` before any service method is invoked.
3. WHEN a request body fails validation on multiple fields simultaneously, THE GlobalExceptionHandler SHALL return a single HTTP 400 response with the JSON body `{"status": 400}`; exactly one response per failing request SHALL be returned regardless of the number of invalid fields.

---

### Requirement 9: Unit and Property-Based Tests

**User Story:** As a developer, I want a suite of JUnit 5 and jqwik property-based tests covering the service, security, and controller layers, so that regressions are caught automatically on every build.

#### Acceptance Criteria

1. THE test suite SHALL include a `UserService` unit test that mocks `UserRepository` and `PasswordEncoder`, stubs `findByUsername` to return a `User` and `matches()` to return `true`, calls `login`, and verifies that the returned `AuthResponse` contains the correct `accountId` and `username` and that `JwtUtil.generateToken` was invoked exactly once.
2. THE test suite SHALL include a `UserService` unit test that verifies calling `login` with a non-existent username (stubbing `findByUsername` to return empty `Optional`) throws `InvalidCredentialsException`.
3. THE test suite SHALL include a `UserService` unit test that verifies calling `login` with a wrong password (stubbing `matches()` to return `false`) throws `InvalidCredentialsException`.
4. THE test suite SHALL include a `JwtUtil` unit test that verifies for a `User` with a positive integer `id` and a non-blank `username`, `extractAccountId(generateToken(user))` returns a value equal to `String.valueOf(user.getId())`.
5. THE test suite SHALL include a `JwtUtil` unit test that verifies `isTokenValid` returns `true` for a freshly generated token and `false` for a garbage string, an expired token, or a token signed with a different key.
6. THE test suite SHALL include a `@WebMvcTest` test class for `AuthController` (with `GlobalExceptionHandler` and `SecurityConfig` included in the test context) that sends a `POST /api/auth/login` request with a valid JSON `LoginRequest` body and `Content-Type: application/json`, mocks `UserService.login` to throw `InvalidCredentialsException`, and verifies the response status is HTTP 401.
7. THE test suite SHALL include a `@WebMvcTest` test class for `AuthController` that sends a `POST /api/auth/login` request with an empty or malformed body and verifies the response status is HTTP 400.
8. THE test suite SHALL include a jqwik `@Property(tries = 100)` test verifying the JWT round-trip property: for any generated `User` with a positive integer `id` (1 to 2,147,483,647) and non-blank `username` of 1–50 characters, `extractAccountId(generateToken(user))` equals `String.valueOf(user.getId())`.
9. THE test suite SHALL include a jqwik `@Property(tries = 100)` test verifying that `JwtUtil.extractAccountId` returns `null` for any randomly generated string of length 1–500 characters drawn from printable ASCII (codes 32–126) that does not contain exactly two period characters (malformed token property).
10. THE test suite SHALL include a jqwik `@Property(tries = 100)` test verifying the blank input guard property: for any Blank value (null, empty string, or whitespace-only string of 1–10 characters) supplied as `username` or `password` in a `LoginRequest`, the `UserService.login` method throws `IllegalArgumentException` without invoking `UserRepository`.
