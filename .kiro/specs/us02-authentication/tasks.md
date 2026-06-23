# Implementation Plan: US02 Authentication

## Overview

This plan implements user authentication for the Todo Management Application: a login endpoint that validates credentials and returns a signed JWT, a `HandlerInterceptor` that guards protected routes, a `WebConfig` for interceptor registration and CORS, and the supporting `JwtUtil.extractUserId` enhancement. All code is Java 21 / Spring Boot 4.1.0 targeting the existing `spring-todo-backend/` project.

## Tasks

- [ ] 1. Add application properties and create exception class
  - [ ] 1.1 Update application.properties with jwt.secret and cors.allowed-origins
    - Add `jwt.secret` property with a development fallback value of at least 32 characters and a comment noting `JWT_SECRET` env var override
    - Add `cors.allowed-origins` property defaulting to `http://localhost:4200` with a comment noting `CORS_ALLOWED_ORIGINS` env var override
    - _Requirements: 5.1, 5.2_

  - [ ] 1.2 Create InvalidCredentialsException class
    - Create `com.revature.todomanagement.exception.InvalidCredentialsException` extending `RuntimeException`
    - Constructor sets a generic message `"Invalid username or password"` that does not reveal whether username or password was wrong
    - _Requirements: 6.2, 6.3_

- [ ] 2. Enhance JwtUtil and implement AuthInterceptor
  - [ ] 2.1 Add extractUserId method to JwtUtil
    - Add `extractUserId(String token)` method to the existing `JwtUtil` class
    - Parse token and return the `"userId"` claim as a `String`
    - Return `null` on any parse failure (expired, tampered, malformed) without throwing
    - Existing `generateToken`, `extractUsername`, and `isTokenValid` methods remain unchanged
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ] 2.2 Create AuthInterceptor class
    - Create `com.revature.todomanagement.security.AuthInterceptor` implementing `HandlerInterceptor`, annotated with `@Component`
    - Inject `JwtUtil` via constructor (use `@RequiredArgsConstructor`)
    - Implement `preHandle` logic: bypass OPTIONS requests immediately; check for `Authorization` header starting with `Bearer `; extract and validate token via `JwtUtil`; set `"userId"` and `"username"` request attributes on success
    - Return HTTP 401 with plain text `"Missing or malformed Authorization header"` when header absent/malformed
    - Return HTTP 401 with plain text `"Invalid or expired token"` when token validation fails
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ]* 2.3 Write property test for extractUserId round-trip
    - **Property 4: extractUserId round-trip**
    - **Validates: Requirements 4.1**
    - jqwik `@Property(tries = 100)`: for any `User` with a non-null UUID, `generateToken(user)` followed by `extractUserId(token)` returns `user.getId().toString()`

- [ ] 3. Implement WebConfig and LoginController
  - [ ] 3.1 Create WebConfig class
    - Create `com.revature.todomanagement.security.WebConfig` annotated with `@Configuration`, implementing `WebMvcConfigurer`
    - Inject `AuthInterceptor` and `@Value("${cors.allowed-origins}") String allowedOrigins`
    - Register `AuthInterceptor` on `/**` excluding `/api/auth/register` and `/api/auth/login`
    - Configure CORS mappings on `/**` with allowed origins from property, methods GET/POST/PUT/PATCH/DELETE/OPTIONS, all headers, credentials allowed
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 3.2 Create LoginController class
    - Create `com.revature.todomanagement.controller.LoginController` annotated with `@RestController` and `@RequestMapping("/api/auth")`
    - Inject `UserService` and `JwtUtil` via constructor
    - Implement `@PostMapping("/login")` that accepts `@RequestBody User loginRequest`
    - Validate username and password are not blank; return HTTP 400 with plain text if blank
    - Call `userService.findByUsername(username)`; throw `InvalidCredentialsException` if empty
    - Compare passwords via `.equals()`; throw `InvalidCredentialsException` on mismatch
    - Call `jwtUtil.generateToken(user)` and return HTTP 200 with `Authorization: Bearer <token>` header
    - Add local `@ExceptionHandler(InvalidCredentialsException.class)` returning HTTP 401 with `Content-Type: text/plain` and the exception message
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 6.1, 6.3, 6.4_

- [ ] 4. Checkpoint - Verify core implementation compiles
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Create test infrastructure and unit tests
  - [ ] 5.1 Create test application.properties for H2
    - Create `src/test/resources/application.properties` with H2 in-memory DB config, test JWT secret (32+ chars), and `cors.allowed-origins=http://localhost:4200`
    - _Requirements: 7.1_

  - [ ] 5.2 Write JwtUtil.extractUserId unit test
    - Create `src/test/java/com/revature/todomanagement/security/JwtUtilTest.java`
    - Generate a token for a known `User` and verify `extractUserId` returns `user.getId().toString()`
    - _Requirements: 7.9_

  - [ ] 5.3 Write AuthInterceptor unit tests
    - Create `src/test/java/com/revature/todomanagement/security/AuthInterceptorTest.java`
    - Test valid token: generate a real token via `JwtUtil`, verify `preHandle` returns `true` and request attributes `"userId"` and `"username"` are set correctly
    - Test invalid token: provide a garbage string, verify `preHandle` returns `false` with HTTP 401
    - Test OPTIONS request: verify `preHandle` returns `true` without checking the token
    - _Requirements: 7.4, 7.5, 7.6_

  - [ ] 5.4 Write LoginController @WebMvcTest tests
    - Create `src/test/java/com/revature/todomanagement/controller/LoginControllerTest.java`
    - Mock `UserService` and `JwtUtil`; use `@WebMvcTest(LoginController.class)` with `AuthInterceptor` excluded or mocked
    - Test valid credentials: mock `UserService.findByUsername` to return a user with matching password, verify HTTP 200 with `Authorization` header starting with `Bearer `
    - Test invalid credentials (user not found): mock `findByUsername` returning empty Optional, verify HTTP 401 with plain text body
    - _Requirements: 7.7, 7.8_

- [ ] 6. Write login logic unit tests and property-based tests
  - [ ] 6.1 Write login logic unit tests
    - Create `src/test/java/com/revature/todomanagement/service/LoginServiceTest.java` (or test login logic inline in controller test)
    - Mock `UserService.findByUsername` returning a valid user with matching password, verify `JwtUtil.generateToken` is invoked and response includes token
    - Mock `findByUsername` returning empty Optional, verify `InvalidCredentialsException` is thrown
    - Mock `findByUsername` returning user with non-matching password, verify `InvalidCredentialsException` is thrown
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ]* 6.2 Write property test for blank credential rejection
    - **Property 2: Blank credential rejection without DB access**
    - **Validates: Requirements 1.4, 7.10**
    - jqwik `@Property(tries = 100)`: for any blank value (null, empty, or whitespace-only) supplied as username or password, login logic rejects with an exception without invoking `UserService.findByUsername`

  - [ ]* 6.3 Write property test for login token round-trip
    - **Property 1: Login token round-trip**
    - **Validates: Requirements 1.1, 4.1**
    - jqwik `@Property(tries = 100)`: for any valid User (non-blank username, non-blank password), performing login and decoding the returned JWT yields the same username as subject and same `id.toString()` as userId claim

  - [ ]* 6.4 Write property test for interceptor attribute extraction
    - **Property 3: Interceptor attribute extraction preserves token claims**
    - **Validates: Requirements 2.2, 2.6**
    - jqwik `@Property(tries = 100)`: for any valid User, generating a token and passing it through `AuthInterceptor.preHandle` results in request attributes matching user's id and username

- [ ] 7. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The `LoginController` login logic is tested directly in controller tests (Reqs 7.1-7.3) since the controller contains the validation and credential-checking inline (no separate login service class)
- All property tests go in `AuthPropertyTest.java` as specified by the design's test organization

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "5.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["3.1", "3.2"] },
    { "id": 4, "tasks": ["5.2", "5.3"] },
    { "id": 5, "tasks": ["5.4", "6.1"] },
    { "id": 6, "tasks": ["6.2", "6.3", "6.4"] }
  ]
}
```
