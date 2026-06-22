# Implementation Plan: User Registration

## Overview

Implement the `POST /api/auth/register` endpoint in the Spring Boot backend. Tasks proceed
bottom-up: DTOs → password validation → security config → service → controller → exception
handler → tests. Each step compiles and integrates with the previous one.

All code lives under `spring-todo-backend/src/` in the base package
`com.revature.todomanagement`.

## Tasks

- [ ] 1. Add jqwik dependency and create DTO classes
  - Add `testImplementation("net.jqwik:jqwik:1.9.3")` to `build.gradle.kts` dependencies block
  - Create `dto/RegisterRequest.java` with Lombok `@Data @NoArgsConstructor @AllArgsConstructor`; fields: `String username`, `String password`
  - Create `dto/RegisterResponse.java` with Lombok `@Data @NoArgsConstructor @AllArgsConstructor`; fields: `UUID userId`, `String username`
  - _Requirements: 1.2, 5.2_

- [ ] 2. Implement PasswordValidator and SecurityConfig
  - [ ] 2.1 Create `security/PasswordValidator.java`
    - Annotate `@Component`
    - Implement `public List<String> getViolations(String password)` that checks each of the seven rules independently and collects violation messages into a `List<String>`
    - Rules and messages match Requirements 3.2 exactly
    - Method never throws; returns empty list when all rules pass
    - Add the regex constant `PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])\\S{8,72}$"` as a `public static final String` for documentation purposes
    - _Requirements: 3.2, 3.3, 3.5_

  - [ ]* 2.2 Write property test for PasswordValidator — Property 5: PasswordValidator returns a violation for each broken rule
    - **Property 5: PasswordValidator returns a violation for each broken rule**
    - **Validates: Requirements 3.2, 3.3**
    - Use jqwik `@Property` methods in `PasswordValidatorTest`
    - For each rule, use `@ForAll @StringLength` / `@CharsList` / `Arbitraries` providers to generate passwords violating only that rule; assert the specific message is present
    - For the valid-password case, generate strings matching all rules; assert violations list is empty
    - Tag: `// Feature: user-registration, Property 5: PasswordValidator rule coverage`

  - [ ] 2.3 Create `security/SecurityConfig.java`
    - Annotate `@Configuration @EnableWebSecurity`
    - Declare `@Bean PasswordEncoder passwordEncoder()` returning `new BCryptPasswordEncoder()`
    - Declare `@Bean SecurityFilterChain securityFilterChain(HttpSecurity http)` that calls `http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())` and returns `http.build()`
    - _Requirements: 4.1, 4.2_

  - [ ]* 2.4 Write property test for BCrypt round trip — Property 6: BCrypt encode–then–matches round trip
    - **Property 6: BCrypt encode–then–matches round trip**
    - **Validates: Requirements 4.2, 4.4**
    - In `BCryptRoundTripTest`, use jqwik to generate arbitrary valid plaintext passwords (8–72 chars, non-whitespace, meeting all rules)
    - For each: `encoder.encode(plain)` → assert `encoder.matches(plain, hash)` is `true`
    - Also assert `encoder.matches(differentPlain, hash)` is `false` for a distinct generated plaintext
    - Tag: `// Feature: user-registration, Property 6: BCrypt round trip`

- [ ] 3. Implement custom exceptions and GlobalExceptionHandler
  - [ ] 3.1 Create `exception/DuplicateUsernameException.java`
    - Extends `RuntimeException`
    - Constructor: `public DuplicateUsernameException(String username)` with message `"Username '" + username + "' is already taken."`
    - _Requirements: 2.3, 6.3_

  - [ ] 3.2 Create `exception/GlobalExceptionHandler.java`
    - Annotate `@RestControllerAdvice`
    - Inner record (or separate class) `ErrorResponse(int status, String message)`
    - `@ExceptionHandler(IllegalArgumentException.class)` → `ResponseEntity` with HTTP 400 and `ErrorResponse(400, ex.getMessage())`
    - `@ExceptionHandler(DuplicateUsernameException.class)` → HTTP 409 and `ErrorResponse(409, ex.getMessage())`
    - `@ExceptionHandler(HttpMessageNotReadableException.class)` → HTTP 400 and `ErrorResponse(400, "Malformed or missing request body.")`
    - `@ExceptionHandler(Exception.class)` → HTTP 500 and `ErrorResponse(500, "Internal server error.")`
    - All handlers set `produces = MediaType.APPLICATION_JSON_VALUE` (via `@RequestMapping` or `produces` on each handler)
    - _Requirements: 6.2, 6.3, 6.4, 6.5_

- [ ] 4. Implement RegistrationService
  - [ ] 4.1 Create `service/RegistrationService.java`
    - Annotate `@Service @RequiredArgsConstructor`
    - Inject: `UserRepository userRepository`, `PasswordEncoder passwordEncoder`, `PasswordValidator passwordValidator`
    - Implement `public RegisterResponse register(RegisterRequest request)` following the eight-step validation and persistence order from Requirements 7.1 exactly
    - Steps 1–4 (field validation) must complete before any `UserRepository` call
    - On success return `new RegisterResponse(savedUser.getId(), savedUser.getUsername())`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.4, 4.1, 4.3, 5.1, 5.2, 5.3, 7.1, 7.2_

  - [ ]* 4.2 Write unit tests for RegistrationService
    - In `RegistrationServiceTest`, mock `UserRepository`, `PasswordEncoder`, `PasswordValidator` with Mockito
    - Test: valid input → `save` called once, returned `RegisterResponse` has correct `userId` and `username`, encoded password not equal to plaintext (Req 8.1)
    - Test: `existsByUsername` returns `true` → `DuplicateUsernameException` thrown, `save` never called (Req 8.2)
    - Test: blank username → `IllegalArgumentException("Username must not be blank.")`, no repo calls (Req 8.3)
    - Test: username length < 5 → `IllegalArgumentException("Username must be between 5 and 18 characters.")`, no repo calls (Req 8.4)
    - Test: username length > 18 → same `IllegalArgumentException`, no repo calls (Req 8.4)
    - Test: blank password → `IllegalArgumentException("Password must not be blank.")`, `PasswordEncoder` and `UserRepository` never called
    - Test: `passwordValidator.getViolations` returns non-empty list → `IllegalArgumentException` whose message contains all returned violations, no repo calls
    - Test: `save` throws `DataAccessException` → exception propagates (Req 5.3)
    - Use `Mockito.verifyNoInteractions(userRepository)` where repo must not be touched
    - _Requirements: 7.1, 7.2, 8.1–8.4_

- [ ] 5. Checkpoint — compile and run unit tests
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement RegistrationController
  - [ ] 6.1 Create `controller/RegistrationController.java`
    - Annotate `@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor`
    - Inject `RegistrationService registrationService`
    - Method: `@PostMapping("/register") public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request)`
    - Return `ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request))`
    - No try/catch — exception handling is delegated to `GlobalExceptionHandler`
    - _Requirements: 1.1, 1.2, 1.3, 6.1_

  - [ ]* 6.2 Write @WebMvcTest tests for RegistrationController
    - In `RegistrationControllerTest`, annotate `@WebMvcTest(RegistrationController.class)`, mock `RegistrationService`
    - Test: valid request → HTTP 201, response body contains `userId` and `username`, no `password` field (Req 8.10, 1.1, 1.2)
    - Test: service throws `DuplicateUsernameException` → HTTP 409, JSON body has `status:409` and non-empty `message` (Req 8.8, 6.3)
    - Test: service throws `IllegalArgumentException` → HTTP 400, JSON body has `status:400` and non-empty `message` (Req 8.9, 6.2)
    - Test: service throws `RuntimeException` → HTTP 500, JSON body has `status:500` (Req 6.4)
    - Test: malformed/absent body → HTTP 400 (Req 1.3)
    - Verify `Content-Type: application/json` on all error responses (Req 6.5)
    - Note: if `@WebMvcTest` loads `SecurityConfig`, add `@Import(SecurityConfig.class)` or use `@WithMockUser` / disable security in test slice
    - _Requirements: 1.1, 1.2, 1.3, 6.1–6.5, 8.8–8.10_

- [ ] 7. Write @DataJpaTest for UserRepository
  - [ ]* 7.1 Write @DataJpaTest repository tests
    - In `UserRepositoryTest`, annotate `@DataJpaTest`
    - Add `src/test/resources/application-test.properties` configuring H2 dialect if not already present
    - Test: save a `User`, then `existsByUsername(username)` returns `true` (Req 8.7, 5.4)
    - Test: `existsByUsername` returns `false` for a username that was never saved (Req 8.7, 5.4)
    - Test: save a `User`, then `findByUsername(username)` returns non-empty `Optional` with matching entity (Req 5.5)
    - Test: `findByUsername` returns empty `Optional` for a username that was never saved (Req 5.5)
    - _Requirements: 5.4, 5.5, 8.7_

- [ ] 8. Write PasswordValidator unit tests (individual rule coverage)
  - [ ]* 8.1 Write unit tests for each individual PasswordValidator rule violation
    - In `PasswordValidatorTest`, add `@Test` methods (not property-based) for deterministic edge cases
    - Test each of the seven rules with a concrete password violating only that rule; assert the exact message appears in the violations list (Req 8.5)
    - Test: valid password satisfying all rules → empty violations list (Req 8.6)
    - These complement the jqwik property tests from task 2.2
    - _Requirements: 3.2, 3.3, 8.5, 8.6_

- [ ] 9. Final checkpoint — full test suite
  - Run `./gradlew test` (or `gradlew.bat test` on Windows) from `spring-todo-backend/`
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- `UserService.java` already exists with `register()` and `login()` — it is left untouched; the new `RegistrationService` is additive
- `SecurityConfig` permits all requests for now; the authentication feature will add the JWT filter chain
- H2 is already in the Gradle dependencies (`testImplementation("com.h2database:h2:2.4.240")`), so `@DataJpaTest` will auto-configure it
- The `User.id` field is named `id` (not `userId`) in the entity — `RegisterResponse.userId` maps to `savedUser.getId()`
