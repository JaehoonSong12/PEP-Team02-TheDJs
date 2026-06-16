# Implementation Plan: user-auth-repository

## Overview

This plan delivers the complete user authentication vertical slice: persistence layer (`UserRepository`),
JWT infrastructure (`JwtUtil`), security configuration (`SecurityConfig`), DTOs, custom exceptions,
global exception handling, business logic (`UserService`), HTTP layer (`AuthController`), and the full
test suite (repository, service, JWT, and controller layers). Tasks are ordered so each step builds
on the previous ones, with no orphaned code left unwired.

---

## Tasks

- [ ] 1. Add missing dependencies to `build.gradle.kts`
  - Add `implementation("org.springframework.boot:spring-boot-starter-security")` for `PasswordEncoder` and `SecurityFilterChain`
  - Add `testImplementation("net.jqwik:jqwik:1.9.2")` for property-based tests
  - Verify JJWT 0.13.0 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) and H2 2.4.240 are already declared (they are — no change needed)
  - Run `./gradlew dependencies` to confirm the dependency tree resolves without conflicts
  - _Requirements: 2.1, 5.1, 8.1_

- [ ] 2. Create H2 test configuration
  - [ ] 2.1 Create `src/test/resources/application-test.properties`
    - Set `spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`
    - Set `spring.datasource.driver-class-name=org.h2.Driver`
    - Set `spring.jpa.database-platform=org.hibernate.dialect.H2Dialect`
    - Set `spring.jpa.hibernate.ddl-auto=create-drop`
    - This profile is activated via `@ActiveProfiles("test")` on `@DataJpaTest` and `@WebMvcTest` classes
    - _Requirements: 8.1, 8.2_

- [ ] 3. Create DTOs in the `dto/` package
  - [ ] 3.1 Create `dto/RegisterRequest.java`
    - Fields: `String username`, `String password`
    - Annotate with Lombok `@Data`
    - _Requirements: 6.1_
  - [ ] 3.2 Create `dto/RegisterResponse.java`
    - Fields: `UUID accountId`, `String username`
    - Annotate with Lombok `@Data @AllArgsConstructor`
    - _Requirements: 6.3_
  - [ ] 3.3 Create `dto/LoginRequest.java`
    - Fields: `String username`, `String password`
    - Annotate with Lombok `@Data`
    - _Requirements: 6.2_
  - [ ] 3.4 Create `dto/LoginResponse.java`
    - Fields: `UUID accountId`, `String username`, `String token`
    - Annotate with Lombok `@Data @AllArgsConstructor`
    - _Requirements: 6.4_

- [ ] 4. Create custom exception classes in the `exception/` package
  - [ ] 4.1 Create `exception/DuplicateUsernameException.java`
    - Extends `RuntimeException`
    - Constructor: `DuplicateUsernameException(String username)` — message: `"Username already taken: " + username`
    - _Requirements: 3.5, 3.6_
  - [ ] 4.2 Create `exception/InvalidCredentialsException.java`
    - Extends `RuntimeException`
    - No-arg constructor — message: `"Invalid username or password"`
    - _Requirements: 4.2, 4.3, 4.4_

- [ ] 5. Create `GlobalExceptionHandler` in the `exception/` package
  - [ ] 5.1 Create `exception/GlobalExceptionHandler.java`
    - Annotate with `@RestControllerAdvice`
    - Handler for `DuplicateUsernameException` → HTTP 409, body `{"status": 409, "message": "..."}`
    - Handler for `InvalidCredentialsException` → HTTP 401, body `{"status": 401, "message": "..."}`
    - Handler for `IllegalArgumentException` → HTTP 400, body `{"status": 400, "message": "..."}`
    - Handler for `Exception` (catch-all) → HTTP 500, body `{"status": 500}`
    - Use `ResponseEntity<Map<String, Object>>` as the return type; set `Content-Type: application/json` via `produces`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 6. Create `UserRepository` in the `repository/` package
  - [ ] 6.1 Create `repository/UserRepository.java`
    - Extend `JpaRepository<User, UUID>`
    - Declare `Optional<User> findByUsername(String username)`
    - Declare `boolean existsByUsername(String username)`
    - _Requirements: 1.1, 1.2, 1.3_

- [ ] 7. Create `SecurityConfig` in the `security/` package
  - [ ] 7.1 Create `security/SecurityConfig.java`
    - Annotate with `@Configuration`
    - Expose a `@Bean PasswordEncoder passwordEncoder()` returning `new BCryptPasswordEncoder()`
    - Expose a `@Bean SecurityFilterChain filterChain(HttpSecurity http)` that disables CSRF, permits all requests (`anyRequest().permitAll()`), and sets session management to `STATELESS`
    - This prevents Spring Boot 4.x auto-configuration from blocking `/api/auth/**` endpoints
    - _Requirements: 2.1, 3.1, 4.1_

- [ ] 8. Create `JwtUtil` in the `security/` package
  - [ ] 8.1 Create `security/JwtUtil.java`
    - Annotate with `@Component`
    - Inject `${jwt.secret}` via `@Value`; derive `SecretKey` using `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`
    - Implement `String generateToken(User user)`: subject = `user.getUsername()`, claim `userId` = `user.getId().toString()`, expiry = `new Date(System.currentTimeMillis() + 86_400_000L)`, signed with `Jwts.builder().signWith(key, Jwts.SIG.HS256)`
    - Implement `String extractUsername(String token)`: parse token with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`, return subject; catch `JwtException` and `IllegalArgumentException`, return `null`
    - Implement `boolean isTokenValid(String token, String username)`: call `extractUsername(token)`, return `username.equals(extracted)` (returns `false` if `extracted` is `null`)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

- [ ] 9. Add `jwt.secret` property to `application.properties`
  - Append `jwt.secret=<minimum-32-character-secret-key-value>` to `src/main/resources/application.properties`
  - The value must be at least 32 characters (256 bits) to satisfy HMAC-SHA256 key requirements
  - _Requirements: 5.1_

- [ ] 10. Implement `UserService` in the `service/` package
  - [ ] 10.1 Implement `service/UserService.java` — fill in the existing empty shell
    - Add `@Service @RequiredArgsConstructor` annotations to the class
    - Inject `UserRepository`, `PasswordEncoder`, and `JwtUtil` via final fields
    - Implement `RegisterResponse register(RegisterRequest request)`:
      1. Throw `IllegalArgumentException` if `username` is blank (`username == null || username.isBlank()`)
      2. Throw `IllegalArgumentException` if `password` is blank
      3. Throw `DuplicateUsernameException` if `userRepository.existsByUsername(username)` returns `true`
      4. Encode password: `passwordEncoder.encode(request.getPassword())`
      5. Persist: `userRepository.save(new User(null, username, hashedPassword))`
      6. Return `new RegisterResponse(saved.getId(), saved.getUsername())`
    - Implement `LoginResponse login(LoginRequest request)`:
      1. Throw `IllegalArgumentException` if `username` is blank
      2. Throw `IllegalArgumentException` if `password` is blank
      3. Load user: `userRepository.findByUsername(username).orElseThrow(InvalidCredentialsException::new)`
      4. Throw `InvalidCredentialsException` if `!passwordEncoder.matches(request.getPassword(), user.getPassword())`
      5. Generate token: `jwtUtil.generateToken(user)`
      6. Return `new LoginResponse(user.getId(), user.getUsername(), token)`
    - _Requirements: 2.1, 2.2, 2.5, 3.2, 3.3, 3.4, 3.5, 3.8, 4.2, 4.3, 4.5, 4.6_

- [ ] 11. Create `AuthController` in the `controller/` package
  - [ ] 11.1 Create `controller/AuthController.java`
    - Annotate with `@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor`
    - Inject `UserService` via a final field
    - Implement `@PostMapping("/register") ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request)`:
      - Delegate to `userService.register(request)`, return `ResponseEntity.ok(response)`
    - Implement `@PostMapping("/login") ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request)`:
      - Delegate to `userService.login(request)`, return `ResponseEntity.ok(response)`
    - _Requirements: 3.1, 4.1, 6.5, 6.6_

- [ ] 12. Checkpoint — wire check
  - Run `./gradlew build -x test` and confirm the project compiles without errors.
  - Ensure all packages (`dto/`, `exception/`, `repository/`, `security/`, `service/`, `controller/`) are present and imports resolve.
  - Ask the user if questions arise before proceeding to tests.

- [ ] 13. Write `UserRepositoryTest`
  - [ ] 13.1 Create `test/.../UserRepositoryTest.java`
    - Annotate with `@DataJpaTest @ActiveProfiles("test")`
    - Inject `UserRepository` via `@Autowired`
    - Example test: save a `User`, call `findByUsername`, assert returned `Optional` is present and all three fields (`id`, `username`, `password`) match the saved entity
    - Example test: save a `User`, assert `existsByUsername` returns `true` for the saved name and `false` for an unsaved name
    - _Requirements: 1.2, 1.3, 1.4, 8.1, 8.2_
  - [ ]* 13.2 Write property test for repository round-trip (Property 1)
    - **Property 1: Repository Round-Trip**
    - **Validates: Requirements 1.2, 1.3, 1.4**
    - Use `@Property(tries = 100)` with `@ForAll @StringLength(min = 1, max = 50)` for username and `@ForAll @StringLength(min = 1, max = 72)` for password
    - For each generated pair: save a `User`, assert `findByUsername` returns the same entity and `existsByUsername` returns `true`; assert `existsByUsername` returns `false` for a never-saved username
    - Tag with comment: `// Feature: user-auth-repository, Property 1: Repository Round-Trip`

- [ ] 14. Write `BCryptPropertyTest`
  - [ ]* 14.1 Write property test for BCrypt encode/verify round-trip (Property 2)
    - Create `test/.../BCryptPropertyTest.java` with `@ExtendWith(JqwikExtension.class)` (or use `@RunWith` if needed — jqwik auto-discovers via JUnit 5 platform)
    - **Property 2: BCrypt Encode/Verify Round-Trip**
    - **Validates: Requirements 2.3**
    - `@Property(tries = 100) void bcryptRoundTrip(@ForAll @StringLength(min = 1, max = 72) String password)`: encode, then assert `encoder.matches(password, encoded)` is `true`
    - Tag with comment: `// Feature: user-auth-repository, Property 2: BCrypt Encode/Verify Round-Trip`
  - [ ]* 14.2 Write property test for BCrypt non-collision (Property 3)
    - **Property 3: BCrypt Non-Collision**
    - **Validates: Requirements 2.4**
    - `@Property(tries = 100) void bcryptNonCollision(@ForAll @StringLength(min=1,max=72) String p, @ForAll @StringLength(min=1,max=72) String q)`: use `Assume.that(!p.equals(q))`, encode `q`, assert `encoder.matches(p, encodeQ)` is `false`
    - Tag with comment: `// Feature: user-auth-repository, Property 3: BCrypt Non-Collision`

- [ ] 15. Write `UserServiceTest`
  - [ ] 15.1 Create `test/.../UserServiceTest.java` with example-based Mockito tests
    - Use `@ExtendWith(MockitoExtension.class)`; mock `UserRepository`, `PasswordEncoder`, `JwtUtil`
    - Test: `register` with valid input → `userRepository.save` called once with a `User` whose `password` satisfies `passwordEncoder.matches(rawPassword, savedPassword)` (Req 8.3)
    - Test: `existsByUsername` stubbed to `true` → `register` throws `DuplicateUsernameException`, `save` never called (Req 8.4)
    - Test: `findByUsername` stubbed to `Optional.empty()` → `login` throws `InvalidCredentialsException` (Req 8.5)
    - Test: `findByUsername` returns a `User`, `passwordEncoder.matches` returns `false` → `login` throws `InvalidCredentialsException` (Req 8.6)
    - Test: valid credentials → `jwtUtil.generateToken` called, returned `LoginResponse` contains the token
    - Test: `userRepository.save` throws `DataAccessException` → exception propagates from `register` (Req 3.8)
    - _Requirements: 2.1, 2.2, 3.2, 3.5, 4.2, 4.3, 4.5_
  - [ ]* 15.2 Write property test for blank input guard (Property 4)
    - **Property 4: Blank Input Guard**
    - **Validates: Requirements 2.5, 3.3, 3.4, 4.6**
    - Generate blank strings (`null`, `""`, whitespace-only) using a custom `@Provide` method or `@ForAll @Whitespace`
    - Assert that both `register` and `login` throw `IllegalArgumentException` for any blank `username` or `password` without touching the mocked repository or encoder
    - Tag with comment: `// Feature: user-auth-repository, Property 4: Blank Input Guard`
  - [ ]* 15.3 Write property test for registration persists BCrypt hash (Property 5)
    - **Property 5: Registration Persists BCrypt Hash**
    - **Validates: Requirements 2.1, 3.2**
    - Use `@Property(tries = 100)` with generated non-blank username and password
    - Stub `existsByUsername` to `false`; capture the `User` passed to `save` via `ArgumentCaptor`
    - Assert `passwordEncoder.matches(originalPassword, capturedUser.getPassword())` is `true` and `RegisterResponse` carries the same `id` and `username`
    - Tag with comment: `// Feature: user-auth-repository, Property 5: Registration Persists BCrypt Hash`
  - [ ]* 15.4 Write property test for duplicate username rejection (Property 6)
    - **Property 6: Duplicate Username Rejection**
    - **Validates: Requirements 3.5**
    - `@Property(tries = 100)`: for any non-blank username, stub `existsByUsername(username)` to `true`; assert `register` throws `DuplicateUsernameException` and `save` was never invoked
    - Tag with comment: `// Feature: user-auth-repository, Property 6: Duplicate Username Rejection`
  - [ ]* 15.5 Write property test for missing user throws `InvalidCredentialsException` (Property 7)
    - **Property 7: Missing User Causes InvalidCredentialsException**
    - **Validates: Requirements 4.2**
    - `@Property(tries = 100)`: stub `findByUsername` to return `Optional.empty()`; assert `login` throws `InvalidCredentialsException` without invoking `PasswordEncoder` or `JwtUtil`
    - Tag with comment: `// Feature: user-auth-repository, Property 7: Missing User Causes InvalidCredentialsException`
  - [ ]* 15.6 Write property test for password mismatch throws `InvalidCredentialsException` (Property 8)
    - **Property 8: Password Mismatch Causes InvalidCredentialsException**
    - **Validates: Requirements 4.3**
    - `@Property(tries = 100)`: stub `findByUsername` to return a present `User`; stub `passwordEncoder.matches` to return `false`; assert `login` throws `InvalidCredentialsException` without invoking `JwtUtil`
    - Tag with comment: `// Feature: user-auth-repository, Property 8: Password Mismatch Causes InvalidCredentialsException`

- [ ] 16. Write `JwtUtilTest`
  - [ ] 16.1 Create `test/.../JwtUtilTest.java` with example-based tests
    - Instantiate `JwtUtil` directly with a 32-char test secret (e.g., `"test-secret-key-32-chars-minimum!!"`); no Spring context needed
    - Test: `extractUsername(generateToken(user))` equals `user.getUsername()` (Req 8.7)
    - _Requirements: 5.4, 5.9_
  - [ ]* 16.2 Write property test for JWT generation correctness (Property 9)
    - **Property 9: JWT Generation Correctness**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.9**
    - `@Property(tries = 100)` with `@ForAll @StringLength(min=1, max=50) String username` and a generated `UUID id`
    - Construct a `User`, generate a token, parse claims directly via `Jwts.parser()`, assert subject equals username, `userId` claim equals `id.toString()`, and `exp - iat == 86400`
    - Tag with comment: `// Feature: user-auth-repository, Property 9: JWT Generation Correctness`
  - [ ]* 16.3 Write property test for `extractUsername` returns `null` on bad tokens (Property 10)
    - **Property 10: JWT extractUsername Returns null for Bad Tokens**
    - **Validates: Requirements 5.5, 5.8**
    - `@Property(tries = 100)` with `@ForAll String garbage`: assert `extractUsername(garbage)` returns `null` without throwing
    - Also test with a token signed by a different key and an expired token (manually constructed with `new Date(0)` expiry)
    - Tag with comment: `// Feature: user-auth-repository, Property 10: extractUsername Returns null for Bad Tokens`
  - [ ]* 16.4 Write property test for `isTokenValid` correctness (Property 11)
    - **Property 11: isTokenValid Correctness**
    - **Validates: Requirements 5.6, 5.7, 5.8**
    - `@Property(tries = 100)`: for a valid freshly generated token, assert `isTokenValid(token, user.getUsername())` is `true`; for a different username, assert `false`; for a garbage token, assert `false` without throwing
    - Tag with comment: `// Feature: user-auth-repository, Property 11: isTokenValid Correctness`

- [ ] 17. Write `AuthControllerTest`
  - [ ] 17.1 Create `test/.../AuthControllerTest.java`
    - Annotate with `@WebMvcTest(AuthController.class) @ActiveProfiles("test")`
    - Import `GlobalExceptionHandler` with `@Import(GlobalExceptionHandler.class)` so `@ControllerAdvice` is active
    - Declare `@MockBean UserService userService` and `@MockBean JwtUtil jwtUtil`
    - Inject `MockMvc` via `@Autowired`
    - Test: mock `userService.register` to throw `DuplicateUsernameException` → `POST /api/auth/register` with valid JSON body returns HTTP 409 (Req 8.8)
    - Test: mock `userService.login` to throw `InvalidCredentialsException` → `POST /api/auth/login` with valid JSON body returns HTTP 401 (Req 8.9)
    - Test: `POST /api/auth/login` with empty/missing body → HTTP 400 (Req 4.7)
    - Test: mock `userService.register` to throw `IllegalArgumentException` → HTTP 400 (Req 3.7)
    - _Requirements: 3.6, 3.7, 4.4, 4.7, 7.2, 7.3, 7.4_
  - [ ]* 17.2 Write property test for exception-to-HTTP status mapping (Property 12)
    - **Property 12: Exception-to-HTTP Status Mapping**
    - **Validates: Requirements 3.6, 3.7, 4.4, 7.1, 7.2, 7.3, 7.4, 7.5**
    - `@Property(tries = 100)` with `@ForAll` generated exception messages for `DuplicateUsernameException`, `InvalidCredentialsException`, and `IllegalArgumentException`
    - For each exception type, stub `userService.register` or `userService.login` to throw the generated instance and assert the correct HTTP status and JSON structure
    - Tag with comment: `// Feature: user-auth-repository, Property 12: Exception-to-HTTP Status Mapping`

- [ ] 18. Final Checkpoint — full test suite
  - Run `./gradlew test` and confirm all tests pass.
  - Ensure all property tests execute at least 100 tries each.
  - Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP. All non-starred sub-tasks must be implemented.
- `spring-boot-starter-security` is required for the `PasswordEncoder` and `SecurityFilterChain` beans — the `SecurityFilterChain` must be declared to prevent Spring Boot 4.x from blocking all endpoints by default.
- H2 (`testImplementation`) and JJWT 0.13.0 (`runtimeOnly` for impl/jackson) are already in `build.gradle.kts`; only `spring-boot-starter-security` and `jqwik:1.9.2` need to be added.
- The `User` entity (`entity/User.java`) is complete and must not be modified.
- `UserService.java` exists as an empty class shell — Task 10 fills it in rather than creating a new file.
- Each property test references its design property number via a comment tag for traceability.
- All property tests are configured with `@Property(tries = 100)` per the design testing strategy.
- `@WebMvcTest` requires `@Import(GlobalExceptionHandler.class)` so the `@ControllerAdvice` bean is included in the slice context.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1"] },
    { "id": 1, "tasks": ["3.1", "3.2", "3.3", "3.4", "4.1", "4.2"] },
    { "id": 2, "tasks": ["5.1", "6.1", "7.1"] },
    { "id": 3, "tasks": ["8.1"] },
    { "id": 4, "tasks": ["10.1"] },
    { "id": 5, "tasks": ["11.1"] },
    { "id": 6, "tasks": ["13.1", "14.1", "14.2", "16.1"] },
    { "id": 7, "tasks": ["13.2", "15.1", "15.2", "15.3", "15.4", "15.5", "15.6", "16.2", "16.3", "16.4"] },
    { "id": 8, "tasks": ["17.1", "17.2"] }
  ]
}
```
