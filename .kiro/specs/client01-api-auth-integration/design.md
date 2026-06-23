# Design Document: Client01 API Auth Integration

## Overview

This document describes the technical design for the Angular frontend authentication and API integration layer. The feature delivers:

- An `AuthService` managing JWT token state via Angular signals with localStorage persistence
- A functional HTTP interceptor (`HttpInterceptorFn`) that attaches Bearer tokens to `/api/` requests
- A functional route guard (`CanActivateFn`) protecting the dashboard
- Login and registration standalone components with reactive forms and error handling
- Application routing configuration tying all pieces together

The design integrates with the existing backend endpoints:
- `POST /api/auth/login` — returns JWT in `Authorization` response header
- `POST /api/auth/register` — creates account, returns 201 with empty body
- Protected routes require `Authorization: Bearer <token>` header

### Scaffolding Strategy

| Artifact | CLI Command | Hand-Written Logic |
|---|---|---|
| `AuthService` | `ng generate service auth/auth` | Signal state, login/register/logout methods, localStorage persistence |
| `Auth Interceptor` | `ng generate interceptor auth/auth` | Token attachment logic, URL filtering |
| `Auth Guard` | `ng generate guard auth/auth` | isAuthenticated check, UrlTree redirect |
| `Login Component` | `ng generate component auth/login` | Reactive form, error handling, navigation |
| `Register Component` | `ng generate component auth/register` | Reactive form, error handling, navigation |
| `Dashboard Component` | `ng generate component dashboard` | Placeholder (future feature) |

The `ng generate` commands produce the file structure and boilerplate. All business logic, templates, and test files are hand-written after scaffolding.

### Relationship to Existing Code

| File | Status | Notes |
|---|---|---|
| `app.config.ts` | **Modify** | Add `provideHttpClient(withInterceptors([...]))` |
| `app.routes.ts` | **Modify** | Add login, register, dashboard, default, wildcard routes |
| `app.ts` | **No change** | Root component with `RouterOutlet` already in place |

---

## Architecture

```mermaid
graph TD
    subgraph Angular Application
        subgraph Routing
            AR[app.routes.ts]
            AG[authGuard]
        end

        subgraph Auth Module
            AS[AuthService]
            AI[authInterceptor]
            LC[LoginComponent]
            RC[RegisterComponent]
        end

        subgraph Protected
            DC[DashboardComponent]
        end

        subgraph Core
            AC[app.config.ts]
            HC[HttpClient]
        end
    end

    subgraph Backend API
        LOGIN[POST /api/auth/login]
        REGISTER[POST /api/auth/register]
        PROTECTED[Protected /api/* routes]
    end

    AC -->|provideHttpClient + withInterceptors| AI
    AC -->|provideRouter| AR
    AR -->|canActivate| AG
    AR -->|path: dashboard| DC
    AR -->|path: login| LC
    AR -->|path: register| RC

    AG -->|inject| AS
    AI -->|inject| AS
    LC -->|inject| AS
    RC -->|inject| AS

    AS -->|POST| LOGIN
    AS -->|POST| REGISTER
    AI -->|attaches Bearer token| HC
    HC -->|requests| PROTECTED

    AS -->|read/write| LS[localStorage]
```

### Request Flow — Login

```mermaid
sequenceDiagram
    participant User
    participant LoginComponent
    participant AuthService
    participant HttpClient
    participant Interceptor
    participant Backend

    User->>LoginComponent: Submit credentials
    LoginComponent->>AuthService: login(username, password)
    AuthService->>HttpClient: POST /api/auth/login
    Note over Interceptor: No token yet, request passes unchanged
    HttpClient->>Backend: POST /api/auth/login {username, password}
    Backend-->>HttpClient: 200 + Authorization: Bearer <jwt>
    HttpClient-->>AuthService: HttpResponse (observe: 'response')
    AuthService->>AuthService: Strip "Bearer ", store in signal + localStorage
    AuthService-->>LoginComponent: Observable completes
    LoginComponent->>LoginComponent: Router.navigate(['/dashboard'])
```

### Request Flow — Protected Route Access

```mermaid
sequenceDiagram
    participant User
    participant Router
    participant AuthGuard
    participant AuthService
    participant Dashboard
    participant Interceptor
    participant Backend

    User->>Router: Navigate to /dashboard
    Router->>AuthGuard: canActivate?
    AuthGuard->>AuthService: isAuthenticated()
    AuthService-->>AuthGuard: true (token signal non-null)
    AuthGuard-->>Router: true
    Router->>Dashboard: Activate component
    Dashboard->>Interceptor: GET /api/todos
    Interceptor->>AuthService: token()
    AuthService-->>Interceptor: "eyJhbG..."
    Interceptor->>Interceptor: Clone request + add Authorization header
    Interceptor->>Backend: GET /api/todos + Authorization: Bearer eyJhbG...
    Backend-->>Dashboard: 200 + response body
```

---

## Components and Interfaces

### `auth/auth.service.ts` — AuthService

Generated via `ng generate service auth/auth`. Decorated with `@Injectable({ providedIn: 'root' })`.

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(null);
  readonly token: Signal<string | null> = this._token.asReadonly();
  readonly isAuthenticated: Signal<boolean> = computed(() => this._token() !== null);

  constructor() {
    const stored = localStorage.getItem('auth_token');
    if (stored && stored.trim().length > 0) {
      this._token.set(stored);
    }
  }

  login(username: string, password: string): Observable<void>;
  register(username: string, password: string): Observable<string>;
  logout(): void;
}
```

**`login` method:**
1. Send `POST` to `http://localhost:8080/api/auth/login` with body `{ username, password }`
2. Use `observe: 'response'` to access full `HttpResponse`
3. On success: read `Authorization` header, strip `"Bearer "` prefix, store token in signal and `localStorage` under key `auth_token`
4. Return `Observable<void>` via `pipe(map(() => undefined))`
5. On error: propagate without modifying token state

**`register` method:**
1. Send `POST` to `http://localhost:8080/api/auth/register` with body `{ username, password }`
2. Use `responseType: 'text'` to prevent JSON parsing of plain text error bodies
3. Return `Observable<string>` with response body text

**`logout` method:**
1. Set `_token` signal to `null`
2. Remove `auth_token` from `localStorage`
3. Call `this.router.navigate(['/login'])`

---

### `auth/auth.interceptor.ts` — authInterceptor

Generated via `ng generate interceptor auth/auth`. Exported as `HttpInterceptorFn`.

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token();

  if (token && req.url.includes('/api/')) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(cloned);
  }

  return next(req);
};
```

**URL filtering rule:** The header is attached only when the request URL contains `/api/`. This ensures:
- Requests to the backend's protected endpoints get the token
- Requests to external origins or non-API paths are unaffected

**401 handling (error path):**
The interceptor also handles 401 responses from non-auth endpoints by triggering logout:

```typescript
return next(cloned).pipe(
  catchError((error: HttpErrorResponse) => {
    if (error.status === 401 && !req.url.includes('/api/auth/')) {
      authService.logout();
    }
    return throwError(() => error);
  })
);
```

---

### `auth/auth.guard.ts` — authGuard

Generated via `ng generate guard auth/auth` (select `CanActivate`). Exported as `CanActivateFn`.

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
```

**Design decision:** The guard is synchronous. Because `isAuthenticated` is a computed signal derived from the token signal (which is synchronously initialized from localStorage in the constructor), there is no need for async checks or Observable returns.

---

### `auth/login/login.component.ts` — LoginComponent

Generated via `ng generate component auth/login` (standalone). Imports `ReactiveFormsModule` and `RouterLink`.

```typescript
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loginForm = new FormGroup({
    username: new FormControl('', Validators.required),
    password: new FormControl('', Validators.required)
  });

  errorMessage = signal<string | null>(null);
  isLoading = signal(false);

  onSubmit(): void;
}
```

**`onSubmit` logic:**
1. If form is invalid, mark all controls as touched (shows validation messages) and return
2. Clear `errorMessage` signal
3. Set `isLoading` to `true` (disables submit button)
4. Call `authService.login(username, password)` and subscribe
5. On success: navigate to `/dashboard`
6. On error: map status code to message, set `errorMessage`
7. On complete (success or error): set `isLoading` to `false`

**Error mapping:**
| Status | Message |
|---|---|
| 400 | Response body text (truncated to 256 chars) |
| 401 | `"Invalid credentials"` |
| 0 (network) | `"Unable to connect to server. Please check your connection."` |
| 500, 503 | `"Server error. Please try again later."` |
| Other | `"An unexpected error occurred. Please try again."` |

---

### `auth/register/register.component.ts` — RegisterComponent

Generated via `ng generate component auth/register` (standalone). Imports `ReactiveFormsModule` and `RouterLink`.

```typescript
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly registerForm = new FormGroup({
    username: new FormControl('', [Validators.required, Validators.maxLength(50)]),
    password: new FormControl('', [Validators.required, Validators.maxLength(128)])
  });

  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  isLoading = signal(false);

  onSubmit(): void;
}
```

**`onSubmit` logic:**
1. If form is invalid, mark all controls as touched and return
2. Clear `errorMessage` and `successMessage`
3. Set `isLoading` to `true`
4. Call `authService.register(username, password)` and subscribe
5. On success: set success message, navigate to `/login`
6. On error: map status code to message, set `errorMessage`
7. On complete: set `isLoading` to `false`

**Error mapping:**
| Status | Message |
|---|---|
| 400 | Response body text (truncated to 256 chars) |
| 409 | `"Username is already taken"` |
| 0 (network) | `"Unable to connect to server. Please check your connection."` |
| 500, 503 | `"Server error. Please try again later."` |
| Other | `"Registration could not be completed. Please try again."` |

---

### `dashboard/dashboard.component.ts` — DashboardComponent

Generated via `ng generate component dashboard` (standalone). This is a placeholder component for the protected route. It will be expanded in a future feature spec.

```typescript
@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: '<h1>Dashboard</h1><p>Welcome! Your tasks will appear here.</p>'
})
export class DashboardComponent {}
```

---

### `app.routes.ts` — Route Configuration

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];
```

**Design decision:** Lazy loading via `loadComponent` keeps the initial bundle small. The guard runs synchronously before the component chunk is downloaded.

---

### `app.config.ts` — Application Configuration

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
```

---

## Data Models

### Token State

The JWT token is a string stored in two locations for different purposes:

| Location | Purpose | Lifecycle |
|---|---|---|
| `WritableSignal<string \| null>` | Reactive state for components/interceptor/guard | Lives as long as the app is running |
| `localStorage['auth_token']` | Persistence across page reloads/tab closes | Cleared on explicit logout |

### JWT Claims (decoded — for reference only, not decoded client-side)

| Claim | Key | Type | Example |
|---|---|---|---|
| Subject | `sub` | string | `"testuser"` |
| User ID | `userId` | string (UUID) | `"550e8400-e29b-41d4-a716-446655440000"` |
| Issued At | `iat` | number (epoch seconds) | `1700000000` |
| Expiration | `exp` | number (epoch seconds) | `1700086400` |

The frontend does NOT decode or validate the JWT. It treats the token as an opaque string. Expiration is handled server-side (401 response triggers logout).

### HTTP Request/Response Contracts

**Login Request:**
```json
POST /api/auth/login
Content-Type: application/json

{ "username": "string", "password": "string" }
```

**Login Success Response:**
```
HTTP/1.1 200 OK
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```
Body: empty

**Login Error Responses:**
| Status | Content-Type | Body |
|---|---|---|
| 400 | text/plain | Validation error message |
| 401 | text/plain | `"Invalid username or password"` |

**Register Request:**
```json
POST /api/auth/register
Content-Type: application/json

{ "username": "string", "password": "string" }
```

**Register Responses:**
| Status | Content-Type | Body |
|---|---|---|
| 201 | — | Empty |
| 400 | text/plain | Validation error details |
| 409 | text/plain | Conflict message |

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Token storage round-trip

*For any* non-empty string value stored via a successful login (HTTP 200 with `Authorization: Bearer <value>`), constructing a new `AuthService` instance SHALL read the same value from `localStorage` and initialize the token signal with it.

**Validates: Requirements 1.5, 1.9**

### Property 2: isAuthenticated derivation

*For any* token signal value, the `isAuthenticated` computed signal SHALL return `true` if and only if the token value is non-null. For `null` token values, `isAuthenticated` SHALL return `false`.

**Validates: Requirements 1.3**

### Property 3: Interceptor conditional header attachment

*For any* HTTP request and any token state, the interceptor SHALL attach an `Authorization: Bearer <token>` header if and only if the token is a non-null, non-empty string AND the request URL contains `/api/`. For all other combinations (null/empty token OR non-API URL), the request SHALL be forwarded without modification.

**Validates: Requirements 2.2, 2.3, 2.6**

### Property 4: Error body truncation

*For any* HTTP error response body string with length greater than 256 characters, the displayed error message SHALL contain exactly the first 256 characters of that body. For body strings with length less than or equal to 256, the full body SHALL be displayed.

**Validates: Requirements 7.1**

---

## Error Handling

### Error Handling Strategy

Error handling follows a layered approach:

1. **Interceptor layer (401 from protected routes):** The interceptor catches 401 responses from non-auth endpoints (`/api/` but not `/api/auth/`), calls `AuthService.logout()`, and re-throws. The user is redirected to `/login` without the component displaying an error.

2. **Component layer (all other errors):** Each component subscribes to service Observables and maps `HttpErrorResponse.status` to user-friendly messages.

3. **Form validation layer (client-side):** Angular `Validators.required` prevents API calls with blank fields. Validation messages appear inline beneath each field.

### Error Message Display

All error messages are rendered in a `<div data-testid="error-message">` element that:
- Is conditionally rendered (only when `errorMessage()` is non-null)
- Is cleared before each new API call
- Truncates server response bodies to 256 characters maximum

### Status Code → Message Mapping

| Status | Login Component | Register Component |
|---|---|---|
| 400 | Server response body (truncated) | Server response body (truncated) |
| 401 | `"Invalid credentials"` | N/A (register doesn't return 401) |
| 409 | N/A (login doesn't return 409) | `"Username is already taken"` |
| 500, 503 | `"Server error. Please try again later."` | `"Server error. Please try again later."` |
| 0 (network) | `"Unable to connect to server. Please check your connection."` | `"Unable to connect to server. Please check your connection."` |
| Other | `"An unexpected error occurred. Please try again."` | `"Registration could not be completed. Please try again."` |

### Design Decision: Error Mapping Utility

To avoid duplicating status-code-to-message logic across components, a shared helper function is used:

```typescript
// auth/error-mapping.ts
export function mapHttpError(error: HttpErrorResponse, context: 'login' | 'register'): string {
  if (error.status === 0) {
    return 'Unable to connect to server. Please check your connection.';
  }
  if (error.status === 400) {
    const body = typeof error.error === 'string' ? error.error : '';
    return body.length > 256 ? body.substring(0, 256) : body;
  }
  if (error.status === 401 && context === 'login') {
    return 'Invalid credentials';
  }
  if (error.status === 409 && context === 'register') {
    return 'Username is already taken';
  }
  if (error.status === 500 || error.status === 503) {
    return 'Server error. Please try again later.';
  }
  return context === 'login'
    ? 'An unexpected error occurred. Please try again.'
    : 'Registration could not be completed. Please try again.';
}
```

---

## Testing Strategy

### Testing Framework

- **Test runner:** Vitest 4.x (configured via `@angular/build:unit-test` builder in `angular.json`)
- **Angular testing utilities:** `TestBed`, `provideHttpClientTesting`, `HttpTestingController`
- **DOM environment:** jsdom (already in devDependencies)
- **Property-based testing:** [fast-check](https://github.com/dubzzz/fast-check) library for TypeScript PBT

### Dependencies to Add

```json
{
  "devDependencies": {
    "fast-check": "^3.22.0"
  }
}
```

### Dual Testing Approach

**Unit tests** (example-based) cover:
- Service construction and DI wiring
- Login/register HTTP call shape and response handling
- Logout side effects (signal, localStorage, navigation)
- Guard return values for authenticated/unauthenticated states
- Component form rendering, submission, navigation, and error display
- Router spy pattern for navigation verification

**Property tests** verify universal properties across randomized inputs:
- Token storage round-trip (Property 1) — fast-check `fc.string()` with 100+ iterations
- isAuthenticated derivation (Property 2) — fast-check `fc.oneof(fc.constant(null), fc.string())` with 100+ iterations
- Interceptor conditional header attachment (Property 3) — fast-check with random URLs and token states, 100+ iterations
- Error body truncation (Property 4) — fast-check `fc.string()` varying lengths, 100+ iterations

### Property Test Configuration

- Library: fast-check 3.x
- Minimum iterations: 100 per property (`{ numRuns: 100 }`)
- Each property test is tagged with a comment:
  - **Feature: client01-api-auth-integration, Property 1: Token storage round-trip**
  - **Feature: client01-api-auth-integration, Property 2: isAuthenticated derivation**
  - **Feature: client01-api-auth-integration, Property 3: Interceptor conditional header attachment**
  - **Feature: client01-api-auth-integration, Property 4: Error body truncation**

### Test File Organization

```
src/app/
├── auth/
│   ├── auth.service.spec.ts          # AuthService unit + property tests (Reqs 1.2-1.10, 8.2-8.4)
│   ├── auth.interceptor.spec.ts      # Interceptor unit + property tests (Reqs 2.1-2.6, 8.5-8.6)
│   ├── auth.guard.spec.ts            # Guard unit tests (Reqs 3.1-3.5, 8.7-8.8)
│   ├── login/
│   │   └── login.component.spec.ts   # Login component tests (Reqs 4.1-4.10, 8.9-8.10)
│   ├── register/
│   │   └── register.component.spec.ts # Register component tests (Reqs 5.1-5.9, 8.11-8.12)
│   └── error-mapping.spec.ts         # Error mapping property test (Req 7.1, Property 4)
└── app.routes.spec.ts                # Route config smoke tests (Reqs 6.1-6.8)
```

### Testing Patterns

**Router spy pattern** (as specified in Requirement 8.13):
```typescript
const routerSpy = { navigate: vi.fn() };
TestBed.configureTestingModule({
  providers: [{ provide: Router, useValue: routerSpy }]
});
// Assert: expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
```

**HttpTestingController pattern:**
```typescript
TestBed.configureTestingModule({
  providers: [provideHttpClientTesting()]
});
const httpMock = TestBed.inject(HttpTestingController);
// After service call:
const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
expect(req.request.method).toBe('POST');
req.flush(null, { headers: { Authorization: 'Bearer test-token' } });
```

**Signal testing pattern:**
```typescript
// Signals are synchronous - read directly after state changes
service.logout();
expect(service.token()).toBeNull();
expect(service.isAuthenticated()).toBe(false);
```
