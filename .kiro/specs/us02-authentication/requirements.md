# Requirements Document

## Introduction

This feature implements User Story 2 (Authentication) for the Todo Management Application. It delivers
the `POST /api/auth/login` endpoint, a `HandlerInterceptor` that protects routes by validating JWTs,
a `WebConfig` class that registers the interceptor and configures CORS for the Angular frontend, and
the necessary application properties for JWT signing and cross-origin access.

This spec builds on the infrastructure established by US01 (Account Creation):
- `User` entity (`UUID id`, `String username`, `String password`) persisted to the `users` table
- `UserRepository` with `findByUsername(String)` and `existsByUsername(String)`
- `UserService` with `findByUsername(String)` returning `Optional<User>`
- `JwtUtil` with `generateToken(User)` (subject = username, claim "userId" = UUID, 24-hour expiry),
  `extractUsername(String)`, and `isTokenValid(String, String)`
- `jwt.secret` property injected via `@Value("${jwt.secret}")` into `JwtUtil` constructor
- Passwords stored as plaintext strings (matching US01's persistence behavior)

One public endpoint is delivered:
- `POST /api/auth/login` — verify credentials and return a signed JWT in the `Authorization` header

Route protection follows the project guide pattern: a `HandlerInterceptor` registered via
`WebMvcConfigurer`, NOT Spring Security's filter chain. No `spring-boot-starter-security` dependency
is introduced.

---

## Glossary

- **System**: The Spring Boot 4.1.0 REST API process.
- **LoginController**: The Spring `@RestController` that handles `POST /api/auth/login` and contains local `@ExceptionHandler` methods for authentication error mapping.
- **AuthInterceptor**: A Spring `@Component` implementing `HandlerInterceptor` that intercepts incoming requests, validates the Bearer token from the `Authorization` header via `JwtUtil`, and attaches `userId` and `username` as request attributes for downstream controllers.
- **WebConfig**: A `@Configuration` class implementing `WebMvcConfigurer` that registers `AuthInterceptor` on all paths (excluding `/api/auth/**`) and configures global CORS mappings for the Angular frontend.
- **JwtUtil**: The existing Spring `@Component` that generates and validates JWTs (subject = username, "userId" claim = UUID string, 24-hour expiry). Enhanced in this feature with an `extractUserId` method.
- **UserService**: The existing Spring `@Service` that provides `findByUsername(String)` for credential lookup.
- **User**: The existing JPA entity with fields `UUID id`, `String username`, `String password`.
- **UserRepository**: The existing Spring Data JPA interface providing `findByUsername(String)` and `existsByUsername(String)`.
- **InvalidCredentialsException**: A new custom `RuntimeException` thrown when login credentials do not match any record.
- **Blank**: A field value that is `null`, an empty string (`""`), or a string composed entirely of whitespace characters.
- **Property Injection**: The use of `@Value("${property.name}")` to inject configuration values from `application.properties` or OS environment variables into Spring beans.

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

### Requirement 2: Auth Interceptor

**User Story:** As a system operator, I want all requests to protected routes intercepted and validated for a Bearer token, so that unauthenticated callers cannot access user data.

#### Acceptance Criteria

1. THE `AuthInterceptor` SHALL implement `HandlerInterceptor` and be annotated with `@Component`.
2. WHEN a request carries an `Authorization` header whose value starts with `Bearer ` (followed by a non-empty token string), THE `AuthInterceptor` SHALL strip the `Bearer ` prefix, validate the token via `JwtUtil.isTokenValid(token, extractedUsername)`, and on success attach the user's `userId` (from the token's "userId" claim) and `username` (from the token's subject) as request attributes via `HttpServletRequest.setAttribute`.
3. WHEN a request does not carry an `Authorization` header or the header does not start with `Bearer `, THE `AuthInterceptor` SHALL set the response status to HTTP 401, write a plain text message `"Missing or malformed Authorization header"` to the response, and return `false` to halt the request.
4. WHEN the `Authorization` header is present but `JwtUtil` reports the token as invalid (expired, tampered, or malformed), THE `AuthInterceptor` SHALL set the response status to HTTP 401, write a plain text message `"Invalid or expired token"` to the response, and return `false`.
5. WHEN the HTTP method is `OPTIONS` (CORS preflight), THE `AuthInterceptor` SHALL return `true` immediately without validating the token, allowing the preflight to complete.
6. THE `AuthInterceptor` SHALL set request attributes with keys `"userId"` (the UUID string from the token's "userId" claim) and `"username"` (the string from the token's subject claim) so downstream controllers can access user identity via `request.getAttribute("userId")` and `request.getAttribute("username")`.

---

### Requirement 3: Web Configuration

**User Story:** As a developer, I want a centralized configuration that registers the auth interceptor on protected routes and enables CORS for the Angular frontend, so that both security and cross-origin access are handled in one place.

#### Acceptance Criteria

1. THE `WebConfig` SHALL be annotated with `@Configuration` and implement `WebMvcConfigurer`.
2. THE `WebConfig` SHALL register `AuthInterceptor` via `addInterceptors` on all paths (`/**`) and exclude `/api/auth/register` and `/api/auth/login` from interception.
3. THE `WebConfig` SHALL inject the `cors.allowed-origins` property via `@Value("${cors.allowed-origins}")` and use it in `addCorsMappings` to configure allowed origins.
4. THE `WebConfig` CORS configuration SHALL apply to all endpoints (`/**`), allow methods `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, allow all headers (`*`), and allow credentials (`true`).
5. THE `cors.allowed-origins` property SHALL default to `http://localhost:4200` in `application.properties` for local Angular development.

---

### Requirement 4: JwtUtil Enhancement

**User Story:** As a developer, I want the existing JwtUtil to expose a method for extracting the userId claim, so that the auth interceptor can attach the user's UUID to the request.

#### Acceptance Criteria

1. THE `JwtUtil` SHALL expose a method `extractUserId(String token)` that parses the token and returns the value of the `"userId"` claim as a `String`.
2. IF `extractUserId` is called with a malformed, expired, or invalid-signature token, THE `JwtUtil` SHALL return `null` without throwing an unchecked exception.
3. THE existing `generateToken`, `extractUsername`, and `isTokenValid` methods SHALL remain unchanged.

---

### Requirement 5: Application Properties

**User Story:** As a developer, I want all environment-specific configuration externalized to application.properties with environment variable overrides, so that the application is portable across development and production environments.

#### Acceptance Criteria

1. THE `application.properties` SHALL include a `jwt.secret` property with a development fallback value of at least 32 characters; the comment SHALL note that the OS environment variable `JWT_SECRET` overrides this value in production.
2. THE `application.properties` SHALL include a `cors.allowed-origins` property with a default value of `http://localhost:4200`; the comment SHALL note that the OS environment variable `CORS_ALLOWED_ORIGINS` overrides this value in production.

---

### Requirement 6: Authentication Error Handling

**User Story:** As a client developer, I want authentication errors returned as plain text with appropriate HTTP status codes, matching the existing registration error pattern.

#### Acceptance Criteria

1. THE `LoginController` SHALL declare a local `@ExceptionHandler(InvalidCredentialsException.class)` that returns HTTP 401 with `Content-Type: text/plain` and a plain text body containing the exception message.
2. THE `InvalidCredentialsException` SHALL extend `RuntimeException` and produce a generic message that does not reveal whether the username or password was incorrect.
3. IF a `POST /api/auth/login` request results in HTTP 401 due to a non-existent username or an incorrect password, THE `LoginController` SHALL return identical HTTP status and identical response body text for both cases.
4. THE error handling pattern SHALL follow the existing `RegistrationController` convention: local `@ExceptionHandler` methods in the controller class, plain text content type, no JSON error bodies.

---

### Requirement 7: Tests

**User Story:** As a developer, I want a test suite covering login logic, interceptor behavior, and controller responses, so that regressions are caught automatically.

#### Acceptance Criteria

1. THE test suite SHALL include a unit test that mocks `UserService.findByUsername` to return a `User` with matching password, calls the login logic, and verifies that `JwtUtil.generateToken` is invoked and the response includes the token in the `Authorization` header.
2. THE test suite SHALL include a unit test that mocks `UserService.findByUsername` to return an empty `Optional` and verifies that `InvalidCredentialsException` is thrown.
3. THE test suite SHALL include a unit test that mocks `UserService.findByUsername` to return a `User` with a non-matching password and verifies that `InvalidCredentialsException` is thrown.
4. THE test suite SHALL include a unit test for `AuthInterceptor` that provides a valid token (generated by `JwtUtil`), verifies `preHandle` returns `true`, and verifies request attributes `"userId"` and `"username"` are set.
5. THE test suite SHALL include a unit test for `AuthInterceptor` that provides an invalid token string and verifies `preHandle` returns `false` with HTTP 401 status on the response.
6. THE test suite SHALL include a unit test for `AuthInterceptor` that sends an `OPTIONS` request and verifies `preHandle` returns `true` without checking the token.
7. THE test suite SHALL include a `@WebMvcTest` test for `LoginController` that mocks `UserService` to return a valid `User` and verifies `POST /api/auth/login` with correct credentials returns HTTP 200 with an `Authorization` header starting with `Bearer `.
8. THE test suite SHALL include a `@WebMvcTest` test for `LoginController` that mocks `UserService` to return an empty `Optional` and verifies `POST /api/auth/login` returns HTTP 401 with plain text body.
9. THE test suite SHALL include a unit test for `JwtUtil.extractUserId` that generates a token for a known `User` and verifies the returned string equals `user.getId().toString()`.
10. THE test suite SHALL include a jqwik `@Property(tries = 100)` test verifying: for any Blank value (null, empty, or whitespace-only) supplied as `username` or `password`, the login logic throws an exception without invoking `UserService.findByUsername`.
