# Requirements Document

## Introduction

This document specifies the requirements for completing the Todo Management Backend, a Spring Boot 4.1.0 REST API backed by SQLite. A partial implementation already exists under `spring-todo-backend/` with controllers, services, repositories, entities, and DTOs in place. Four critical gaps must be closed: the authentication layer is mocked and must be replaced with real JWT-based Spring Security; password handling consists of TODO comments and must be replaced with BCrypt hashing and verification; input validation is absent from DTOs and controllers; and one subtask endpoint (GET list) is missing. All four user stories -- account registration, authentication, task management, and subtask organization -- must be fully operational against the database schema defined in the LaTeX documentation.

## Glossary

- **System**: The Spring Boot 4.1.0 REST API process, collectively referred to as "the System" when no finer-grained component name applies.
- **AuthController**: The Spring `@RestController` mapped to `/api/auth/**`, responsible for registration and login endpoints.
- **TodoController**: The Spring `@RestController` mapped to `/api/todos/**`, responsible for todo and subtask CRUD endpoints.
- **SecurityFilter**: The `JwtFilter` component that intercepts every HTTP request, extracts and validates a Bearer token from the `Authorization` header, and populates the Spring `SecurityContext`.
- **JwtUtil**: The component responsible for generating, signing, and verifying JWT tokens using the server-side secret key.
- **SecurityConfig**: The `@Configuration` class that defines the Spring Security filter chain, permitting `/api/auth/**` publicly and requiring authentication on all other paths.
- **UserService**: The Spring `@Service` responsible for user registration (including BCrypt hashing) and login (including BCrypt verification).
- **TaskService**: The Spring `@Service` responsible for CRUD operations on `todos` rows, including ownership enforcement.
- **SubtaskService**: The Spring `@Service` responsible for CRUD operations on `subtasks` rows, including parent-todo ownership enforcement.
- **GlobalExceptionHandler**: The `@RestControllerAdvice` class that maps application exceptions to standardized HTTP error responses.
- **TodoRequest**: The request DTO for todo create and update operations; fields are `title` (String, 1-255 characters), `description` (String, 0-1000 characters), `completed` (boolean).
- **TodoResponse**: The response DTO for todo operations; fields are `todoId` (Integer), `accountId` (Integer), `title` (String), `description` (String), `completed` (boolean).
- **SubtaskRequest**: The request DTO for subtask create and update operations; fields are `title` (String, 1-255 characters), `completed` (boolean).
- **SubtaskResponse**: The response DTO for subtask operations; fields are `id` (Integer), `todoId` (Integer), `accountId` (Integer), `title` (String), `completed` (boolean).
- **AuthResponse**: The response DTO for registration and login; fields are `accountId` (Integer), `username` (String).
- **LoginRequest**: The request DTO for login; fields are `username` (String), `password` (String).
- **UserRegistrationRequest**: The request DTO for registration; fields are `username` (String, 1-50 characters), `email` (String, 1-254 characters), `password` (String, 8-128 characters).
- **BCrypt**: The adaptive password hashing algorithm used to store and verify user passwords; the plaintext password must never be persisted.
- **JWT**: JSON Web Token -- a signed, compact token issued at login and submitted on protected requests as `Authorization: Bearer <token>`. A valid JWT is non-expired, structurally well-formed, and carries a verifiable HMAC-SHA256 signature produced with the server-side secret key.
- **Bearer_Token**: A JWT value transmitted in the HTTP `Authorization` header using the `Bearer` scheme.
- **ResourceNotFoundException**: The application exception mapped to HTTP 404.
- **ResourceConflictException**: The application exception mapped to HTTP 409.
- **UnauthorizedAccessException**: The application exception mapped to HTTP 403, replacing the current `RuntimeException` thrown on ownership failure.
- **Authenticated_User**: The `User` entity resolved from the JWT in the `SecurityContext`; replaces the current hardcoded `userService.findById(1L)` call.
- **Ownership**: The invariant that a `todos` row's `user_id` column equals the `id` of the Authenticated_User performing the operation.
- **Blank**: A field value that is null, an empty string (`""`), or a string composed entirely of whitespace characters.

## Requirements

---

### Requirement 1: User Account Registration

**User Story:** As a new user, I want to register an account with a unique username and password, so that I can start tracking my todo tasks.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/register` request is received with a `UserRegistrationRequest` body containing a `username` of 1-50 characters, an `email` of 1-254 characters in valid email format, and a `password` of 8-128 characters, THE `AuthController` SHALL persist a new `users` row and return `HTTP 200 OK` with an `AuthResponse` body containing the generated `accountId` (a positive integer) and the registered `username`.
2. IF a `POST /api/auth/register` request is received and the submitted `username` already exists in the `users` table (case-sensitive comparison), THEN THE `AuthController` SHALL return `HTTP 409 Conflict` with the JSON body `{"status": 409}` and no new `users` row SHALL be created.
3. IF a `POST /api/auth/register` request is received and the submitted `email` already exists in the `users` table (case-insensitive comparison), THEN THE `AuthController` SHALL return `HTTP 409 Conflict` with the JSON body `{"status": 409}` and no new `users` row SHALL be created.
4. IF a `POST /api/auth/register` request is received with a Blank `username`, a `username` exceeding 50 characters, a Blank `password`, a `password` shorter than 8 characters, a `password` exceeding 128 characters, or a Blank `email`, THEN THE `AuthController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}`.
5. WHEN a valid registration request is processed, THE `UserService` SHALL hash the submitted plaintext password using BCrypt (strength >= 10) and persist only the resulting hash in the `users.password_hash` column; the value stored in `users.password_hash` SHALL NOT equal the plaintext password and SHALL be verifiable by `BCryptPasswordEncoder.matches()`.
6. WHEN a valid registration request is processed, THE `UserService` SHALL populate `users.created_at` and `users.updated_at` with the current UTC instant formatted as an ISO-8601 string (e.g., `2024-01-15T10:30:00Z`), accurate to within 2 seconds of the actual request time.
7. FOR ALL valid `username` values u, a `POST /api/auth/register` request with `username` equal to u SHALL succeed exactly once; every subsequent request carrying the same `username` SHALL return `HTTP 409 Conflict` regardless of the values of other fields (registration idempotency property).

---

### Requirement 2: User Authentication and JWT Issuance

**User Story:** As a registered user, I want to log in with my username and password and receive a token, so that I can securely access my todo items on subsequent requests.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/login` request is received with a `LoginRequest` body containing a `username` and `password` that match a record in the `users` table, THE `AuthController` SHALL return `HTTP 200 OK` with an `AuthResponse` body containing the matching `accountId` and `username`, and SHALL include a signed JWT in the `Authorization` response header as `Bearer <token>`.
2. WHEN a `POST /api/auth/login` request is received and the submitted `username` does not exist in the `users` table, THE `AuthController` SHALL return `HTTP 401 Unauthorized` with the JSON body `{"message": "<descriptive message>"}`.
3. WHEN a `POST /api/auth/login` request is received and the submitted `password` does not match the BCrypt hash stored in `users.password_hash` for the given username, THE `AuthController` SHALL return `HTTP 401 Unauthorized` with the JSON body `{"message": "<descriptive message>"}`.
4. WHEN a JWT is issued by `POST /api/auth/login`, THE `JwtUtil` SHALL sign the token with HMAC-SHA256 using the server-side secret key, set the `sub` claim to the user's `accountId` as a string, and set the expiry to 3600 seconds from the issuance time; the token SHALL be decodable and verifiable by `JwtUtil` using that same key (JWT authenticity property).
5. IF a JWT is presented to the `SecurityFilter` with an altered signature, a modified payload, a missing `Authorization` header, or an empty `Authorization` header value, THEN THE `SecurityFilter` SHALL reject the request and THE `System` SHALL return `HTTP 401 Unauthorized` (JWT authenticity property).
6. IF a `POST /api/auth/login` request is received with a Blank `username` or a Blank `password`, THEN THE `AuthController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}`.
7. THE `UserService` SHALL use `BCryptPasswordEncoder.matches(rawPassword, storedHash)` to verify the submitted password against `users.password_hash`; direct plaintext comparison SHALL NOT be used.

---

### Requirement 3: Spring Security Configuration

**User Story:** As a system operator, I want all protected routes to require a valid JWT, so that unauthenticated callers cannot access user data.

#### Acceptance Criteria

1. THE `SecurityConfig` SHALL configure the Spring Security filter chain to permit all requests to `/api/auth/**` without requiring a Bearer token.
2. THE `SecurityConfig` SHALL configure the Spring Security filter chain to require a valid JWT for all requests to `/api/todos/**`.
3. WHEN a request to `/api/todos/**` is received without an `Authorization` header, THE `SecurityFilter` SHALL return `HTTP 401 Unauthorized` before the request reaches any controller method.
4. WHEN a request to `/api/todos/**` is received with a malformed JWT (not a valid three-part Base64url-encoded structure), THE `SecurityFilter` SHALL return `HTTP 401 Unauthorized`.
5. WHEN a request to `/api/todos/**` is received with an expired JWT (current time is past the `exp` claim), THE `SecurityFilter` SHALL return `HTTP 401 Unauthorized`.
6. WHEN a request carries a valid JWT, THE `SecurityFilter` SHALL extract the `sub` claim (the user's `accountId`) from the token payload and store a populated `UsernamePasswordAuthenticationToken` in the Spring `SecurityContext` for the duration of the request.
7. WHEN a request carries a valid JWT and the `sub` claim does not correspond to any `users.id` in the database, THE `SecurityFilter` SHALL return `HTTP 401 Unauthorized`.
8. THE `TodoController` SHALL retrieve the `Authenticated_User` by reading the principal from `SecurityContextHolder.getContext().getAuthentication()`; the hardcoded call `userService.findById(1L)` SHALL be removed and SHALL NOT appear anywhere in the codebase.

---

### Requirement 4: Input Validation and Global Error Handling

**User Story:** As an API consumer, I want the server to return consistent, well-formed error responses for invalid input, so that client applications can handle errors predictably.

#### Acceptance Criteria

1. WHEN a `POST /api/auth/register` request is received with a `username` that is Blank or exceeds 50 characters, or a `password` that is Blank, shorter than 8 characters, or exceeds 128 characters, THE `AuthController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}` before any service method is invoked.
2. WHEN a `POST /api/auth/login` request is received with a Blank `username` or a Blank `password`, THE `AuthController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}` before any service method is invoked.
3. WHEN a `POST /api/todos` or `PUT /api/todos/{id}` request is received with a Blank `title` or a `title` exceeding 255 characters, or a `description` exceeding 1000 characters, THE `TodoController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}` before any service method is invoked.
4. WHEN a `POST /api/todos/{id}/subtask` or `PUT /api/todos/{id}/{subtaskId}` request is received with a Blank `title` or a `title` exceeding 255 characters, THE `TodoController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}` before any service method is invoked.
5. WHEN a request body fails validation on multiple fields simultaneously, THE `GlobalExceptionHandler` SHALL return a single `HTTP 400 Bad Request` response with the JSON body `{"status": 400}`; one response per failing request SHALL be returned.
6. THE `GlobalExceptionHandler` SHALL handle `ResourceNotFoundException` and return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
7. THE `GlobalExceptionHandler` SHALL handle `ResourceConflictException` and return `HTTP 409 Conflict` with the JSON body `{"status": 409}`.
8. THE `GlobalExceptionHandler` SHALL handle `UnauthorizedAccessException` and return `HTTP 403 Forbidden` with the JSON body `{"status": 403}`; a generic `RuntimeException` SHALL NOT be used for ownership violations.
9. IF an unhandled exception propagates to the `GlobalExceptionHandler`, THEN THE `GlobalExceptionHandler` SHALL return `HTTP 500 Internal Server Error` with the JSON body `{"status": 500}`.

---

### Requirement 5: Todo (Task) CRUD Operations

**User Story:** As an authenticated user, I want to create, read, update, and delete my todo items, so that I can manage my primary tasks.

#### Acceptance Criteria

1. WHEN a `POST /api/todos` request is received with a valid `TodoRequest` body (non-Blank `title` of 1-255 characters, `description` of 0-1000 characters, and a `completed` boolean) and a valid Bearer token, THE `TaskService` SHALL create a new `todos` row with `user_id` set to the `Authenticated_User`'s id and return `HTTP 200 OK` with a `TodoResponse` containing the generated `todoId`, `accountId`, `title`, `description`, and `completed` fields.
2. IF a `POST /api/todos` request is received with a Blank `title`, a `title` exceeding 255 characters, or a `description` exceeding 1000 characters, THEN THE `TodoController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}`.
3. WHEN a `GET /api/todos` request is received with a valid Bearer token, THE `TaskService` SHALL return `HTTP 200 OK` with a JSON array containing only the `TodoResponse` objects for `todos` rows where `user_id` equals the `Authenticated_User`'s id; rows belonging to any other user SHALL NOT appear in the array.
4. WHEN a `GET /api/todos/{id}` request is received and a `todos` row with the given `id` exists and `user_id` equals the `Authenticated_User`'s id, THE `TodoController` SHALL return `HTTP 200 OK` with the corresponding `TodoResponse`.
5. IF a `GET /api/todos/{id}` request is received and no `todos` row with the given `id` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
6. IF a `GET /api/todos/{id}` request is received and the `todos` row with the given `id` exists but `user_id` does not equal the `Authenticated_User`'s id, THEN THE `TaskService` SHALL throw `UnauthorizedAccessException` and THE `GlobalExceptionHandler` SHALL return `HTTP 403 Forbidden` with the JSON body `{"status": 403}`.
7. WHEN a `PUT /api/todos/{id}` request is received with a valid `TodoRequest` body, a valid Bearer token, and the `todos` row's `user_id` equals the `Authenticated_User`'s id, THE `TaskService` SHALL overwrite the `title`, `description`, and `completed` fields of the matching `todos` row and return `HTTP 200 OK` with the updated `TodoResponse`.
8. IF a `PUT /api/todos/{id}` request is received and no `todos` row with the given `id` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
9. IF a `PUT /api/todos/{id}` request is received and the `todos` row exists but `user_id` does not equal the `Authenticated_User`'s id, THEN THE `TaskService` SHALL throw `UnauthorizedAccessException` and THE `GlobalExceptionHandler` SHALL return `HTTP 403 Forbidden` with the JSON body `{"status": 403}`.
10. WHEN a `DELETE /api/todos/{id}` request is received with a valid Bearer token and the `todos` row's `user_id` equals the `Authenticated_User`'s id, THE `TaskService` SHALL delete the matching `todos` row and return `HTTP 204 No Content` with no response body.
11. IF a `DELETE /api/todos/{id}` request is received and no `todos` row with the given `id` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
12. WHEN a `todos` row with N associated `subtasks` rows is deleted, THE `System` SHALL remove exactly those N `subtasks` rows from the `subtasks` table via the database `ON DELETE CASCADE` constraint, leaving no orphaned subtask rows (cascade integrity property).
13. FOR ALL Authenticated_Users A and B where A's id does not equal B's id, user A SHALL NOT receive a `2xx` response when attempting to read, update, or delete any `todos` row whose `user_id` equals B's id; such attempts SHALL return `HTTP 403 Forbidden` (ownership isolation property).

---

### Requirement 6: Subtask CRUD Operations

**User Story:** As an authenticated user, I want to create, list, update, and delete subtasks nested under a parent todo, so that I can break down my tasks into smaller steps.

#### Acceptance Criteria

1. WHEN a `GET /api/todos/{id}/subtask` request is received with a valid Bearer token and the `todos` row with `{id}` exists and `user_id` equals the `Authenticated_User`'s id, THE `SubtaskService` SHALL return `HTTP 200 OK` with a JSON array of `SubtaskResponse` objects for all `subtasks` rows where `todo_id` equals `{id}`; this endpoint is currently absent from `TodoController` and SHALL be added.
2. IF a `GET /api/todos/{id}/subtask` request is received and no `todos` row with the given `{id}` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
3. IF a `GET /api/todos/{id}/subtask` request is received and the `todos` row exists but `user_id` does not equal the `Authenticated_User`'s id, THEN THE `SubtaskService` SHALL throw `UnauthorizedAccessException` and THE `GlobalExceptionHandler` SHALL return `HTTP 403 Forbidden` with the JSON body `{"status": 403}`.
4. IF a `GET /api/todos/{id}/subtask` request is received without an `Authorization` header, THEN THE `SecurityFilter` SHALL return `HTTP 401 Unauthorized` before the request reaches the controller.
5. WHEN a `POST /api/todos/{id}/subtask` request is received with a valid `SubtaskRequest` body (non-Blank `title` of 1-255 characters and a `completed` boolean), a valid Bearer token, and the `todos` row's `user_id` equals the `Authenticated_User`'s id, THE `SubtaskService` SHALL create a new `subtasks` row with `todo_id` set to `{id}` and return `HTTP 200 OK` with a `SubtaskResponse` containing `id`, `todoId`, `accountId` (populated from the parent `todos.user_id`), `title`, and `completed`.
5. IF a `POST /api/todos/{id}/subtask` request is received with a Blank `title` or a `title` exceeding 255 characters, THEN THE `TodoController` SHALL return `HTTP 400 Bad Request` with the JSON body `{"status": 400}`.
6. IF a `POST /api/todos/{id}/subtask` request is received and no `todos` row with the given `{id}` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
7. WHEN a `PUT /api/todos/{id}/{subtaskId}` request is received with a valid `SubtaskRequest` body, a valid Bearer token, and the parent `todos` row's `user_id` equals the `Authenticated_User`'s id, THE `SubtaskService` SHALL overwrite the `title` and `completed` fields of the `subtasks` row with `id` equal to `{subtaskId}` and return `HTTP 200 OK` with the updated `SubtaskResponse`.
8. IF a `PUT /api/todos/{id}/{subtaskId}` request is received and no `subtasks` row with `id` equal to `{subtaskId}` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
9. IF a `PUT /api/todos/{id}/{subtaskId}` request is received and the parent `todos` row exists but `user_id` does not equal the `Authenticated_User`'s id, THEN THE `SubtaskService` SHALL throw `UnauthorizedAccessException` and THE `GlobalExceptionHandler` SHALL return `HTTP 403 Forbidden`.
10. WHEN a `DELETE /api/todos/{id}/{subtaskId}` request is received with a valid Bearer token and the parent `todos` row's `user_id` equals the `Authenticated_User`'s id, THE `SubtaskService` SHALL delete the `subtasks` row with `id` equal to `{subtaskId}` and return `HTTP 204 No Content` with no response body.
11. IF a `DELETE /api/todos/{id}/{subtaskId}` request is received and no `subtasks` row with `id` equal to `{subtaskId}` exists, THEN THE `TodoController` SHALL return `HTTP 404 Not Found` with the JSON body `{"status": 404}`.
12. IF a `DELETE /api/todos/{id}/{subtaskId}` request is received and the parent `todos` row exists but `user_id` does not equal the `Authenticated_User`'s id, THEN THE `SubtaskService` SHALL throw `UnauthorizedAccessException` and THE `GlobalExceptionHandler` SHALL return `HTTP 403 Forbidden`.
13. FOR ALL subtasks created under todo T, THE `SubtaskService` SHALL persist `todo_id` equal to T's `id`; IF a request is received where the `{id}` path variable does not equal the subtask's `todo_id`, THEN THE `System` SHALL return `HTTP 404 Not Found` (subtask parent binding property).
14. THE `SubtaskResponse` DTO SHALL include the `accountId` field populated from the parent `todos.user_id`; the current `SubtaskResponse` which omits `accountId` SHALL be updated to conform to the API contract.

---

### Requirement 7: Database Schema and Persistence Configuration

**User Story:** As a system operator, I want the database schema to match the canonical data dictionary, so that the application data remains consistent and upgradeable.

#### Acceptance Criteria

1. THE `users` table SHALL contain the columns `id` (INTEGER PRIMARY KEY AUTOINCREMENT), `username` (TEXT NOT NULL UNIQUE), `email` (TEXT NOT NULL UNIQUE), `password_hash` (TEXT NOT NULL), `created_at` (TEXT NOT NULL), `updated_at` (TEXT NOT NULL); all timestamps SHALL be stored as ISO-8601 strings in UTC format.
2. THE `todos` table SHALL contain the columns `id` (INTEGER PRIMARY KEY AUTOINCREMENT), `user_id` (INTEGER NOT NULL, REFERENCES users(id) ON DELETE CASCADE), `title` (TEXT NOT NULL), `description` (TEXT), `completed` (INTEGER NOT NULL DEFAULT 0), `created_at` (TEXT NOT NULL), `updated_at` (TEXT NOT NULL); all timestamps SHALL be stored as ISO-8601 strings in UTC format.
3. THE `subtasks` table SHALL contain the columns `id` (INTEGER PRIMARY KEY AUTOINCREMENT), `todo_id` (INTEGER NOT NULL, REFERENCES todos(id) ON DELETE CASCADE), `title` (TEXT NOT NULL), `completed` (INTEGER NOT NULL DEFAULT 0), `created_at` (TEXT NOT NULL), `updated_at` (TEXT NOT NULL); all timestamps SHALL be stored as ISO-8601 strings in UTC format.
4. THE `spring.jpa.hibernate.ddl-auto` property SHALL be set to `update` for all non-production Spring profiles; the current value of `create-drop` SHALL be replaced so that data is not discarded on application restart.
5. WHEN the application starts, THE `System` SHALL connect to the SQLite file at the path specified in `spring.datasource.url`; IF the connection fails, THE `System` SHALL log the error and fail to start rather than starting in a degraded state.
6. THE SQLite database SHALL enforce foreign-key constraints at runtime; the application configuration SHALL execute `PRAGMA foreign_keys = ON` upon establishing each connection so that `ON DELETE CASCADE` rules are honored by the SQLite engine.
