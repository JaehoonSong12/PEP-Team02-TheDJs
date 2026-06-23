# Requirements Document

## Introduction

This feature implements the Angular frontend authentication and API integration layer for the Todo Management Application. It delivers an `AuthService` for credential management and token storage, a functional HTTP interceptor that attaches Bearer tokens to outgoing requests, a functional route guard that protects the task dashboard from unauthenticated access, login and registration components with form handling and error display, and application routing configuration.

The feature integrates with the existing backend endpoints defined by US01 (Account Creation) and US02 (Authentication):
- `POST /api/auth/register` — create a new user account (HTTP 201 on success)
- `POST /api/auth/login` — authenticate and receive a JWT in the `Authorization` response header (HTTP 200 on success)
- All non-auth routes require `Authorization: Bearer <token>` header

The Angular application uses standalone components, functional interceptors (`HttpInterceptorFn`), functional route guards (`CanActivateFn`), signal-based state management, and Vitest for testing. All scaffolding uses the Angular CLI (`ng generate`).

---

## Glossary

- **AuthService**: An Angular `@Injectable` service responsible for storing the JWT token (as a signal), exposing authentication state, and providing methods for login, registration, and logout API calls.
- **Auth_Interceptor**: A functional `HttpInterceptorFn` that reads the current token from `AuthService` and attaches it as an `Authorization: Bearer <token>` header on outgoing HTTP requests.
- **Auth_Guard**: A functional `CanActivateFn` that checks `AuthService` for a valid token and redirects unauthenticated users to the login page.
- **Login_Component**: A standalone Angular component that renders a login form, submits credentials to the backend via `AuthService`, displays error messages, and redirects authenticated users to the dashboard.
- **Register_Component**: A standalone Angular component that renders a registration form, submits credentials to the backend via `AuthService`, displays validation error messages, and redirects successful registrations to the login page.
- **App_Router**: The Angular routing configuration defined in `app.routes.ts` that maps URL paths to components and applies route guards.
- **App_Config**: The Angular application configuration in `app.config.ts` that registers providers for routing, HTTP client, and interceptors.
- **Token**: A JSON Web Token (JWT) string issued by the backend containing claims `sub` (username), `userId` (UUID string), `iat` (issued-at epoch seconds), and `exp` (expiration epoch seconds, iat + 86,400,000ms).
- **Blank**: A string value that is null, undefined, an empty string (`""`), or composed entirely of whitespace characters.
- **Backend_API**: The Spring Boot REST API running at `http://localhost:8080`.
- **Dashboard**: The protected task management view that requires authentication to access.

---

## Requirements

### Requirement 1: Authentication Service

**User Story:** As a developer, I want a centralized authentication service that manages token storage, login, registration, and logout operations, so that all components and interceptors share a single source of truth for authentication state.

#### Acceptance Criteria

1. THE `AuthService` SHALL be generated using `ng generate service auth/auth` and SHALL be decorated with `@Injectable({ providedIn: 'root' })`.
2. THE `AuthService` SHALL store the JWT token as an Angular `WritableSignal<string | null>` initialized to `null`, and SHALL expose a read-only `Signal<string | null>` named `token` for external consumers.
3. THE `AuthService` SHALL expose a computed `Signal<boolean>` named `isAuthenticated` that returns `true` when the token signal value is non-null and `false` otherwise.
4. WHEN `AuthService.login` is called with a non-blank username and non-blank password (where non-blank means not null, not empty, and not composed entirely of whitespace), THE `AuthService` SHALL send a `POST` request to `http://localhost:8080/api/auth/login` with body `{ "username": "<username>", "password": "<password>" }`, SHALL observe the full `HttpResponse`, and SHALL return an `Observable<void>` that emits once and completes after the token is stored on success.
5. WHEN the login request returns HTTP 200 with an `Authorization` header starting with `Bearer `, THE `AuthService` SHALL strip the `Bearer ` prefix and store the remaining token string in the token signal and in `localStorage` under the key `auth_token`.
6. IF the login request returns a non-200 HTTP status or the response lacks a valid `Authorization` header starting with `Bearer `, THEN THE `AuthService` SHALL propagate the error through the returned Observable without modifying the token signal or `localStorage`.
7. WHEN `AuthService.register` is called with a non-blank username and non-blank password, THE `AuthService` SHALL send a `POST` request to `http://localhost:8080/api/auth/register` with body `{ "username": "<username>", "password": "<password>" }`, SHALL configure `responseType: 'text'` to prevent JSON parsing of plain text responses, and SHALL return an `Observable<string>` containing the response body text.
8. WHEN `AuthService.logout` is called, THE `AuthService` SHALL set the token signal to `null`, SHALL remove the `auth_token` entry from `localStorage`, and SHALL navigate the user to the login route using `Router.navigate`.
9. WHEN the `AuthService` is initialized (constructed), THE `AuthService` SHALL read `localStorage` for a previously stored `auth_token` value and SHALL set the token signal to that value if it exists and has a length greater than zero after trimming.
10. THE `AuthService` SHALL use `inject(HttpClient)` to obtain the HTTP client and `inject(Router)` to obtain the router for programmatic navigation during logout.

---

### Requirement 2: HTTP Auth Interceptor

**User Story:** As a user, I want my authentication token automatically attached to every API request, so that I do not need to manually include credentials on each action.

#### Acceptance Criteria

1. THE `Auth_Interceptor` SHALL be generated using `ng generate interceptor auth/auth` and SHALL be exported as a `const` function with type `HttpInterceptorFn`.
2. WHEN an outgoing HTTP request is intercepted and `AuthService.token()` returns a non-null, non-empty string, THE `Auth_Interceptor` SHALL clone the request with an added header `Authorization: Bearer <token>` and SHALL forward the cloned request to the next handler.
3. WHEN an outgoing HTTP request is intercepted and `AuthService.token()` returns `null` or an empty string, THE `Auth_Interceptor` SHALL forward the original request unchanged to the next handler.
4. THE `Auth_Interceptor` SHALL use `inject(AuthService)` within the function body to access the token signal.
5. THE `Auth_Interceptor` SHALL be registered in `App_Config` via `provideHttpClient(withInterceptors([authInterceptor]))`.
6. THE `Auth_Interceptor` SHALL attach the `Authorization` header only to requests whose URL begins with the application's backend base path (`/api/`); requests to other origins or paths SHALL be forwarded unchanged regardless of token state.

---

### Requirement 3: Route Guard

**User Story:** As a system operator, I want unauthenticated users prevented from accessing the task dashboard, so that user data remains protected.

#### Acceptance Criteria

1. THE `Auth_Guard` SHALL be generated using `ng generate guard auth/auth` and SHALL be exported as a `const` function with type `CanActivateFn`.
2. WHEN the `Auth_Guard` is invoked and `AuthService.isAuthenticated()` returns `true`, THE `Auth_Guard` SHALL return `true` synchronously to allow navigation; it SHALL NOT modify the token state.
3. WHEN the `Auth_Guard` is invoked and `AuthService.isAuthenticated()` returns `false`, THE `Auth_Guard` SHALL return a `UrlTree` pointing to `/login` via `router.createUrlTree(['/login'])` without modifying the token state.
4. THE `Auth_Guard` SHALL use `inject(AuthService)` and `inject(Router)` within the function body.
5. THE `Auth_Guard` SHALL be attached to the route with path `dashboard` via the `canActivate` property in the route configuration.

---

### Requirement 4: Login Component

**User Story:** As a registered user, I want a login form where I can enter my credentials and receive immediate feedback on errors, so that I can authenticate and access my tasks.

#### Acceptance Criteria

1. THE `Login_Component` SHALL be generated using `ng generate component auth/login` as a standalone component.
2. THE `Login_Component` SHALL render a reactive form with an input field for username, an input field for password (type `password`), and a submit button.
3. WHEN the user submits the login form with non-blank username and non-blank password, THE `Login_Component` SHALL call `AuthService.login(username, password)` and subscribe to the returned Observable.
4. WHEN `AuthService.login` completes successfully (HTTP 200), THE `Login_Component` SHALL navigate the user to the `/dashboard` route via `Router.navigate(['/dashboard'])`.
5. WHEN `AuthService.login` returns an HTTP 401 error, THE `Login_Component` SHALL display the text `"Invalid credentials"` in a visible error message element.
6. WHEN `AuthService.login` returns an HTTP 400 error, THE `Login_Component` SHALL display the error response body text in a visible error message element.
7. IF the user submits the login form with a blank username or blank password, THEN THE `Login_Component` SHALL display an inline validation message beneath each invalid field indicating that the field is required, without making an API call.
8. WHILE an API request is in progress, THE `Login_Component` SHALL disable the submit button to prevent duplicate submissions; WHEN the API request completes (success or failure), THE `Login_Component` SHALL re-enable the submit button.
9. IF `AuthService.login` returns an error other than HTTP 400 or HTTP 401 (e.g., HTTP 500 or network failure), THEN THE `Login_Component` SHALL display a visible error message indicating that an unexpected error occurred and the user should try again.
10. WHEN the user submits the login form, THE `Login_Component` SHALL clear any previously displayed error message before initiating the API call.

---

### Requirement 5: Registration Component

**User Story:** As a new user, I want a registration form where I can create an account and see validation errors from the server, so that I can fix issues and successfully register.

#### Acceptance Criteria

1. THE `Register_Component` SHALL be generated using `ng generate component auth/register` as a standalone component.
2. THE `Register_Component` SHALL render a form with an input field for username (maximum 50 characters), an input field for password (type `password`, maximum 128 characters), and a submit button.
3. WHEN the user submits the registration form with non-blank username and non-blank password, THE `Register_Component` SHALL call `AuthService.register(username, password)` and subscribe to the returned Observable.
4. WHEN `AuthService.register` completes successfully (HTTP 201), THE `Register_Component` SHALL navigate the user to the `/login` route and SHALL display a success message indicating the account was created.
5. WHEN `AuthService.register` returns an HTTP 400 error, THE `Register_Component` SHALL clear any previously displayed message and display the error response body text (containing validation failure details) in a visible error message element rendered in the DOM.
6. WHEN `AuthService.register` returns an HTTP 409 error, THE `Register_Component` SHALL clear any previously displayed message and display the text `"Username is already taken"` in a visible error message element rendered in the DOM.
7. IF the user submits the registration form with a Blank username or Blank password (where Blank means null, empty string, or whitespace-only), THEN THE `Register_Component` SHALL display a validation message indicating which field is required, without making an API call.
8. WHILE an API request is in progress, THE `Register_Component` SHALL disable the submit button to prevent duplicate submissions.
9. IF `AuthService.register` returns an HTTP error other than 400 or 409 (including network failures), THEN THE `Register_Component` SHALL clear any previously displayed message and display a generic error message indicating the registration could not be completed, and SHALL re-enable the submit button.

---

### Requirement 6: Application Routing Configuration

**User Story:** As a developer, I want a clear routing configuration that maps paths to components and applies guards to protected routes, so that navigation is predictable and secure.

#### Acceptance Criteria

1. THE `App_Router` SHALL define a route with path `login` that maps to `Login_Component`.
2. THE `App_Router` SHALL define a route with path `register` that maps to `Register_Component`.
3. THE `App_Router` SHALL define a route with path `dashboard` that maps to `Dashboard_Component` and SHALL apply `Auth_Guard` via the `canActivate` property.
4. IF `Auth_Guard` determines that no valid authentication token is stored, THEN THE `Auth_Guard` SHALL reject the navigation and redirect the user to the `/login` route.
5. THE `App_Router` SHALL define a default route (empty path `''`) that redirects to `/login` with `pathMatch: 'full'`.
6. THE `App_Router` SHALL define a wildcard route (path `**`) that redirects to `/login`; this route SHALL be declared after all other routes so that it only matches when no preceding route matches.
7. THE `App_Config` SHALL include `provideHttpClient(withInterceptors([authInterceptor]))` in the providers array so that all outgoing HTTP requests to protected endpoints include the `Authorization: Bearer <token>` header.
8. THE `App_Config` SHALL retain the existing `provideRouter(routes)` and `provideBrowserGlobalErrorListeners()` providers.

---

### Requirement 7: Error Handling

**User Story:** As a user, I want clear and understandable error messages when API operations fail, so that I can take corrective action or understand what went wrong.

#### Acceptance Criteria

1. WHEN an API call returns HTTP 400, THE calling component SHALL display the plain text response body as the error message to the user, truncated to a maximum of 256 characters if the body exceeds that length.
2. WHEN an API call returns HTTP 401 from any route other than `POST /api/auth/login`, THE `Auth_Interceptor` SHALL clear the stored token via `AuthService.logout()` and redirect the user to `/login` without the calling component displaying an error message.
3. WHEN an API call returns HTTP 409, THE calling component SHALL display the response body text as the error message to the user (for example, `"Username is already taken"`).
4. WHEN an API call returns HTTP 500 or HTTP 503, THE calling component SHALL display the text `"Server error. Please try again later."`.
5. WHEN an API call fails due to a network error (status code 0 or no response received), THE calling component SHALL display the text `"Unable to connect to server. Please check your connection."`.
6. THE error messages SHALL be displayed in a dedicated HTML element identified by a `data-testid="error-message"` attribute that enables test selection.
7. WHEN a new API call is initiated from the same component, THE calling component SHALL clear any previously displayed error message before the new request is sent.

---

### Requirement 8: Unit Tests

**User Story:** As a developer, I want a comprehensive test suite covering authentication logic, interceptor behavior, guard logic, and component rendering, so that regressions are detected automatically.

#### Acceptance Criteria

1. THE test suite SHALL use Vitest as the test runner, `provideHttpClientTesting` and `HttpTestingController` from `@angular/common/http/testing` for HTTP mocking, and `TestBed` from `@angular/core/testing` for component and service testing.
2. THE test suite SHALL include an `AuthService` unit test that verifies calling `login("testuser", "testpass")` sends a POST request to `/api/auth/login` with body `{ username: "testuser", password: "testpass" }`, and when the mock response returns HTTP 200 with an `Authorization` header value of `Bearer <token>`, the service stores the raw token string (without the `Bearer ` prefix) into its token signal.
3. THE test suite SHALL include an `AuthService` unit test that verifies calling `logout` sets the token signal to `null` and removes the key `auth_token` from `localStorage`.
4. THE test suite SHALL include an `AuthService` unit test that verifies: when `localStorage` contains a key `auth_token` with a non-empty string value prior to service construction, the service initializes its token signal with that stored value.
5. THE test suite SHALL include an `Auth_Interceptor` unit test that verifies: when `AuthService` exposes a non-null token value, the interceptor clones the outgoing request with an `Authorization` header set to `Bearer <token>` and forwards the cloned request to the next handler.
6. THE test suite SHALL include an `Auth_Interceptor` unit test that verifies: when `AuthService` exposes a `null` token value, the interceptor forwards the original request to the next handler without adding or modifying the `Authorization` header.
7. THE test suite SHALL include an `Auth_Guard` unit test that verifies: when `AuthService.isAuthenticated()` returns `true`, the guard returns `true`.
8. THE test suite SHALL include an `Auth_Guard` unit test that verifies: when `AuthService.isAuthenticated()` returns `false`, the guard returns a `UrlTree` pointing to `/login`.
9. THE test suite SHALL include a `Login_Component` unit test that stubs `AuthService.login` to return a successful Observable (completing without error), and verifies the component calls `Router.navigate` with `['/dashboard']`.
10. THE test suite SHALL include a `Login_Component` unit test that stubs `AuthService.login` to return an Observable that errors with an HTTP 401 response, and verifies that the component renders an element containing the text `"Invalid credentials"` visible in the DOM.
11. THE test suite SHALL include a `Register_Component` unit test that stubs the registration service call to return an Observable completing with HTTP 201 status, and verifies the component calls `Router.navigate` with `['/login']`.
12. THE test suite SHALL include a `Register_Component` unit test that stubs the registration service call to return an Observable that errors with an HTTP 409 response, and verifies that the component renders an element containing the text `"Username is already taken"` visible in the DOM.
13. THE test suite SHALL verify navigation by spying on `Router.navigate` (via Vitest `vi.fn()`) and asserting the spy was called with the expected route array.
