# Implementation Plan: Client01 API Auth Integration

## Overview

This plan implements the Angular frontend authentication and API integration layer. It covers the AuthService (JWT signal state + localStorage persistence), a functional HTTP interceptor, a functional route guard, login and registration standalone components with reactive forms and error handling, and application routing configuration. All code is TypeScript targeting the Angular framework with Vitest for testing and fast-check for property-based tests.

## Tasks

- [x] 1. Set up project structure, dependencies, and core auth service
  - [x] 1.1 Install fast-check and scaffold auth files via Angular CLI
    - Run `ng generate service auth/auth`, `ng generate interceptor auth/auth`, `ng generate guard auth/auth`
    - Run `ng generate component auth/login`, `ng generate component auth/register`, `ng generate component dashboard`
    - Install `fast-check` as a dev dependency: `npm install --save-dev fast-check`
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1_

  - [x] 1.2 Implement AuthService with signal state and localStorage persistence
    - Create `WritableSignal<string | null>` for token, expose read-only `token` signal and computed `isAuthenticated` signal
    - Implement constructor logic to read `auth_token` from localStorage on initialization
    - Implement `login(username, password)` method: POST to `http://localhost:8080/api/auth/login`, observe full response, extract Bearer token from Authorization header, store in signal + localStorage, return `Observable<void>`
    - Implement `register(username, password)` method: POST to `http://localhost:8080/api/auth/register` with `responseType: 'text'`, return `Observable<string>`
    - Implement `logout()` method: set token signal to null, remove from localStorage, navigate to `/login`
    - Use `inject(HttpClient)` and `inject(Router)`
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_

  - [x] 1.3 Implement error mapping utility function
    - Create `auth/error-mapping.ts` with `mapHttpError(error: HttpErrorResponse, context: 'login' | 'register'): string`
    - Map status 0 → network error message, 400 → response body (truncated to 256 chars), 401 (login) → "Invalid credentials", 409 (register) → "Username is already taken", 500/503 → server error message, other → generic message per context
    - _Requirements: 7.1, 7.3, 7.4, 7.5_

- [x] 2. Implement interceptor, guard, and routing configuration
  - [x] 2.1 Implement authInterceptor function
    - Inject `AuthService`, read `token()` signal
    - If token is non-null/non-empty AND request URL contains `/api/`, clone request with `Authorization: Bearer <token>` header
    - Add 401 error handling: catch 401 from non-auth endpoints (`/api/` but not `/api/auth/`), call `authService.logout()`, re-throw
    - Otherwise forward original request unchanged
    - _Requirements: 2.2, 2.3, 2.4, 2.6, 7.2_

  - [x] 2.2 Implement authGuard function
    - Inject `AuthService` and `Router`
    - If `isAuthenticated()` returns true, return `true` synchronously
    - If false, return `router.createUrlTree(['/login'])`
    - _Requirements: 3.2, 3.3, 3.4, 3.5_

  - [x] 2.3 Configure app.routes.ts with all routes
    - Add route `login` → lazy-load LoginComponent
    - Add route `register` → lazy-load RegisterComponent
    - Add route `dashboard` → lazy-load DashboardComponent with `canActivate: [authGuard]`
    - Add default route `''` → redirectTo `/login` with `pathMatch: 'full'`
    - Add wildcard route `**` → redirectTo `/login` (last position)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [x] 2.4 Configure app.config.ts with HttpClient and interceptor
    - Add `provideHttpClient(withInterceptors([authInterceptor]))` to providers
    - Retain existing `provideRouter(routes)` and `provideBrowserGlobalErrorListeners()`
    - _Requirements: 2.5, 6.7, 6.8_

- [ ] 3. Implement login and registration components
  - [ ] 3.1 Implement LoginComponent with reactive form and error handling
    - Create reactive form with `username` (required) and `password` (required) controls
    - Implement `onSubmit()`: validate form, clear error message, set loading state, call `authService.login()`, navigate to `/dashboard` on success, map errors via `mapHttpError`, set error message signal on failure, re-enable button on complete
    - Mark all controls as touched on invalid submit to show validation messages
    - Disable submit button while loading
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10_

  - [ ] 3.2 Create LoginComponent HTML template
    - Render form with username input, password input (type="password"), submit button
    - Add inline validation messages beneath each field when touched and invalid
    - Add error message element with `data-testid="error-message"` conditionally rendered
    - Bind submit button disabled state to `isLoading()` signal
    - _Requirements: 4.2, 4.7, 4.8, 7.6_

  - [ ] 3.3 Implement RegisterComponent with reactive form and error handling
    - Create reactive form with `username` (required, maxLength 50) and `password` (required, maxLength 128) controls
    - Implement `onSubmit()`: validate form, clear messages, set loading state, call `authService.register()`, navigate to `/login` on success with success message, map errors via `mapHttpError`, set error message signal on failure, re-enable button on complete
    - Mark all controls as touched on invalid submit
    - _Requirements: 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

  - [ ] 3.4 Create RegisterComponent HTML template
    - Render form with username input (maxlength="50"), password input (type="password", maxlength="128"), submit button
    - Add inline validation messages beneath each field when touched and invalid
    - Add error message element with `data-testid="error-message"` conditionally rendered
    - Add success message element conditionally rendered
    - Bind submit button disabled state to `isLoading()` signal
    - Add RouterLink to login page
    - _Requirements: 5.2, 5.7, 5.8, 7.6_

  - [x] 3.5 Create DashboardComponent placeholder
    - Standalone component with inline template: `<h1>Dashboard</h1><p>Welcome! Your tasks will appear here.</p>`
    - _Requirements: 6.3_

- [ ] 4. Checkpoint - Verify application compiles and routes work
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Write unit tests for AuthService, interceptor, and guard
  - [ ] 5.1 Write AuthService unit tests
    - Test login sends correct POST request and stores token from Authorization header
    - Test logout sets token signal to null and removes auth_token from localStorage
    - Test constructor reads auth_token from localStorage on initialization
    - Use `provideHttpClientTesting` and `HttpTestingController`
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 5.2 Write property test for token storage round-trip (Property 1)
    - **Property 1: Token storage round-trip**
    - Use fast-check `fc.string({ minLength: 1 })` to generate random token values
    - Verify: store token via login success → new AuthService instance reads same value from localStorage
    - Minimum 100 iterations via `{ numRuns: 100 }`
    - **Validates: Requirements 1.5, 1.9**

  - [ ]* 5.3 Write property test for isAuthenticated derivation (Property 2)
    - **Property 2: isAuthenticated derivation**
    - Use fast-check `fc.oneof(fc.constant(null), fc.string())` to generate token states
    - Verify: isAuthenticated returns true iff token is non-null
    - Minimum 100 iterations via `{ numRuns: 100 }`
    - **Validates: Requirements 1.3**

  - [ ] 5.4 Write Auth Interceptor unit tests
    - Test: when token is non-null, interceptor clones request with Authorization header for `/api/` URLs
    - Test: when token is null, interceptor forwards request unchanged
    - Test: 401 from non-auth endpoint triggers logout
    - _Requirements: 8.5, 8.6_

  - [ ]* 5.5 Write property test for interceptor conditional header attachment (Property 3)
    - **Property 3: Interceptor conditional header attachment**
    - Use fast-check to generate random URLs and token states (null, empty, non-empty strings)
    - Verify: Authorization header attached iff token is non-null/non-empty AND URL contains `/api/`
    - Minimum 100 iterations via `{ numRuns: 100 }`
    - **Validates: Requirements 2.2, 2.3, 2.6**

  - [ ] 5.6 Write Auth Guard unit tests
    - Test: when isAuthenticated returns true, guard returns true
    - Test: when isAuthenticated returns false, guard returns UrlTree to `/login`
    - _Requirements: 8.7, 8.8_

- [ ] 6. Write unit tests for components and error mapping
  - [ ] 6.1 Write LoginComponent unit tests
    - Test: successful login navigates to `/dashboard` (router spy pattern)
    - Test: HTTP 401 error displays "Invalid credentials" in DOM
    - Test: form validation prevents API call when fields are blank
    - Test: submit button disabled during loading
    - _Requirements: 8.9, 8.10, 8.13_

  - [ ] 6.2 Write RegisterComponent unit tests
    - Test: successful registration navigates to `/login` (router spy pattern)
    - Test: HTTP 409 error displays "Username is already taken" in DOM
    - Test: form validation prevents API call when fields are blank
    - Test: submit button disabled during loading
    - _Requirements: 8.11, 8.12, 8.13_

  - [ ]* 6.3 Write property test for error body truncation (Property 4)
    - **Property 4: Error body truncation**
    - Use fast-check `fc.string()` to generate response bodies of varying lengths
    - Verify: if body length > 256, displayed message is first 256 chars; if ≤ 256, full body is displayed
    - Minimum 100 iterations via `{ numRuns: 100 }`
    - **Validates: Requirements 7.1**

  - [ ]* 6.4 Write route configuration smoke tests
    - Verify route definitions exist for login, register, dashboard, default redirect, wildcard
    - Verify dashboard route has canActivate with authGuard
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 6.6_

- [ ] 7. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties defined in the design document
- Unit tests validate specific examples and edge cases
- All components are standalone Angular components using signals for reactive state
- The router spy pattern (`vi.fn()`) is used per Requirement 8.13 for navigation verification
- fast-check library is used for all property-based tests with minimum 100 iterations each

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "2.2", "3.5"] },
    { "id": 3, "tasks": ["2.3", "2.4"] },
    { "id": 4, "tasks": ["3.1", "3.3"] },
    { "id": 5, "tasks": ["3.2", "3.4"] },
    { "id": 6, "tasks": ["5.1", "5.4", "5.6"] },
    { "id": 7, "tasks": ["5.2", "5.3", "5.5", "6.1", "6.2"] },
    { "id": 8, "tasks": ["6.3", "6.4"] }
  ]
}
```
