<!-- SUPPLEMENT: Grouped topics for Routing & Networking -->

# Routing & Navigation

## Overview of Routing

In a Single Page Application (SPA), the browser does not actually reload a new HTML file when you navigate. Instead, the **Router** intercepts the URL change, looks at a "map" you have created, and swaps the current component out for a new one.

### The Concept of the "Router Map"
Think of routing as a directory in a large building. 
*   The **URL Path** is the room number (e.g., `/settings`).
*   The **Route Configuration** is the directory that says "Room 101 is the Settings Room."
*   The **Router Outlet** is the actual room where the content is displayed.

## Modern Routing (Standalone API)

In modern Angular, routing is configured using functional providers rather than the legacy `RouterModule`. This is more lightweight and aligns with the Standalone component architecture.

### 1. Defining the Routes
Routes are defined as an array of objects. Each object maps a `path` (the URL string) to a `component` (the view to show).

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { UserComponent } from './user/user.component';
import { NotFoundComponent } from './not-found/not-found.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },            // The default/home path
  { path: 'user', component: UserComponent },        // Navigates to /user
  { path: '**', component: NotFoundComponent },      // Wildcard: catches all invalid URLs
];
```

### Implementation Context: Advanced Routing Configuration

The routing map is defined using an array of `Route` objects. In a complex standalone application, this configuration handles redirection strategies, nested (child) routes, and security guards.

*(Extracted from `@angular2-pokedex/src/app/app.routes.ts`)*
```typescript
import { Routes } from '@angular/router';
import { PokeHome } from './components/poke-home/poke-home';
import { PokeName } from './components/poke-name/poke-name';
import { PokeSprite } from './components/poke-sprite/poke-sprite';
import { PokeLogin } from './components/poke-login/poke-login';
import { loginGuard } from './guards/login-guard';

export const routes: Routes = [
    {
        // 1. Nested Routing: Child routes render inside the parent's RouterOutlet
        path: 'home',
        component: PokeHome,
        children: [
            { path: 'name', component: PokeName },
            { path: 'sprite', component: PokeSprite }
        ],
        // 2. Security: Ensure user is logged in before accessing home
        canActivate: [loginGuard]
    },
    {
        // 3. Redirection Strategy: Catch-all redirect to login
        path: '',
        redirectTo: 'login',
        pathMatch: 'full' // Ensures the redirect only happens on the exact empty path
    },
    {
        path: 'login',
        component: PokeLogin
    }
];
```

### 2. Providing the Router
In a standalone application, you register these routes in your `app.config.ts` file using `provideRouter`.

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes) // This "activates" the routing system
  ]
};
```

## Navigation: Moving Between Views

Once the routes are defined, you need a way for users to actually move between them.

### 1. Declarative Navigation (`routerLink`)
In your HTML templates, you should **never** use `<a href="/user">`. Using `href` causes the browser to perform a full page reload, which breaks the SPA experience. Instead, use the `routerLink` directive.

#### Implementation Context: Absolute vs. Relative Paths
*(Extracted from `@angular2-pokedex/src/app/components/poke-home/poke-home.html`)*

When using `routerLink`, the presence or absence of a leading slash (`/`) completely changes how the router resolves the destination:
- **Absolute:** Paths starting with `/` override the entire URL.
- **Relative:** Paths without `/` are appended to the *current* active route.

```html
<nav>
    <!-- 
        ABSOLUTE: "/home" is an absolute navigation path.
        Clicking it navigates directly to "localhost:4200/home", replacing whatever the current route is.
    -->
    <a routerLink="/home">Pokemon Home </a>
    
    <!-- 
        RELATIVE: Without the "/", these paths are relative. 
        Because we are currently at the "/home" endpoint, clicking these links dynamically appends 
        to the current url, navigating to "/home/name" and "/home/sprite" respectively.
    -->
    <a routerLink="name">Pokemon Name </a>
    <a routerLink="sprite">Pokemon Sprites </a>
</nav>

<!-- The <router-outlet> is the placeholder where the child components (name/sprite) will appear -->
<main>
  <router-outlet></router-outlet>
</main>
```

> [!NOTE] 
> Verified against official documentation: https://angular.dev/guide/routing (A `routerLink` path starting with `/` is absolute and resolves from the application root. A path without a `/` is relative and resolves from the currently activated route).

### 2. Programmatic Navigation (`Router.navigate`)
Sometimes you need to move a user automatically (e.g., redirecting them to `/login` after they click "Logout"). For this, you use the `Router` service in your TypeScript code.

```typescript
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({ ... })
export class LogoutButtonComponent {
  private router = inject(Router);

  onLogout() {
    // Perform logout logic...
    // Then, programmatically move the user to the home page
    this.router.navigate(['/']);
  }
}
```

## Advanced Routing Concepts

### 1. Route Parameters (Dynamic Routes)
Often, you don't want a route for every single user. Instead, you want a single `UserComponent` that can display *any* user based on an ID in the URL.

**Definition:**
```typescript
{ path: 'user/:id', component: UserComponent }
```

**Accessing the Parameter:**
In the component, you use the `ActivatedRoute` service to "read" the ID from the URL.

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({ ... })
export class UserComponent implements OnInit {
  private route = inject(ActivatedRoute);
  userId: string | null = null;

  ngOnInit() {
    // Use the paramMap observable to react to URL changes
    this.route.paramMap.subscribe(params => {
      this.userId = params.get('id');
    });
  }
}
```

### 2. Route Guards (Security)
Route Guards are functions that decide if a user is **allowed** to navigate to a specific route. They are used for authentication (is the user logged in?) or authorization (is the user an admin?).

#### Implementation Context: The `CanActivateFn` Guard
*(Extracted from `@angular2-pokedex/src/app/guards/login-guard.ts`)*

A modern route guard is a simple function that leverages `inject()` to interact with application services. If `true` is returned, the navigation proceeds. Otherwise, programmatic redirection is triggered.

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { LoginService } from '../services/login-service';

export const loginGuard: CanActivateFn = (route, state) => {
  // 1. Inject the necessary services directly into the functional guard
  const loginService = inject(LoginService);
  const router = inject(Router);

  // 2. Perform the security check
  if (loginService.checkIfUserIsLoggedIn()) {
    return true; // Authorized
  }
  
  // 3. Fallback: Programmatic redirect if unauthorized
  return router.navigate(["/login"]);
};
```

> [!NOTE] 
> Verified against official documentation: https://angular.dev/guide/routing (Functional Route Guards execute in an injection context, allowing the direct use of `inject(Router)`).

#### Implementation Context: Functional Route Guard
*(Extracted from `@angular3-demo/src/app/auth/auth.guard.ts`)*

This example demonstrates how a guard checks an authentication service and uses the `UrlTree` object to declaratively redirect unauthorized users. It is attached to a route's `canActivate` array in the route configuration.

How it works:
1. Checks if the user is currently authenticated via `AuthService`.
2. If authenticated, allows navigation by returning `true`.
3. If not authenticated, redirects the user to the `/login` page by returning a `UrlTree` (Angular's way of declaratively redirecting).

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  // Inject dependencies — AuthService for auth state, Router for redirects
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check if the user has a valid session/token
  if (authService.isAuthenticated()) {
    // User is logged in — allow access to the route
    return true;
  }

  // User is NOT authenticated — redirect to the login page.
  // Returning a UrlTree tells the router to navigate there instead.
  return router.createUrlTree(['/login']);
};
```

#### Implementation Context: Dependency Resolution (`LoginService`)
*(Extracted from `@angular2-pokedex/src/app/services/login-service.ts`)*

The above guard relies on the `LoginService` to determine authorization. This service encapsulates the application's authentication state and demonstrates how to use the `Router` service to programmatically navigate the user *after* an action (such as logging in).

```typescript
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  // Injecting the Router service to control navigation programmatically
  private router = inject(Router)

  // Internal placeholder for a real authentication token/state
  private isLoggedIn = false;

  checkIfUserIsLoggedIn(){
    return this.isLoggedIn;
  }

  attemptLogin(){
    // In a real app, you would validate credentials against a server here
    this.isLoggedIn = true;
    
    // Programmatically navigate the user back to the protected route upon success
    this.router.navigate(["home"]);
  }
}
```

## Summary Table of Navigation

| Concept | Tool | Purpose |
| :--- | :--- | :--- |
| **The Map** | `Routes` array | Defines which URL leads to which component. |
| **The Placeholder** | `<router-outlet>` | The spot in the HTML where components are swapped in. |
| **User Navigation** | `routerLink` | The HTML directive for clicking links without reloading. |
| **Code Navigation** | `router.navigate()` | The TypeScript method for moving users via logic. |
| **Dynamic Data** | `:id` (Params) | Allows one component to represent many different data items. |
| **Security** | `canActivate` | Guards that prevent unauthorized access to routes. |

---

---

# HTTP & Data Fetching

## Overview of HTTP in Angular

In a modern web application, the frontend is rarely a closed system; it is a consumer of data from a backend server or a third-party API. Angular provides the `HttpClient` service, a powerful, built-in tool designed to handle these communication needs using **Observables**.

### The "Reactive" Request
Unlike the standard JavaScript `fetch` API, which uses Promises, Angular's `HttpClient` is built on **RxJS**. This means every HTTP request is a stream. This allows you to use powerful operators to retry failed requests, transform the data as it arrives, or cancel a request if the user navigates away before it finishes.

## The HttpClient Workflow

To use HTTP, you must first enable it in your application configuration using `provideHttpClient()`.

### 1. The Request Lifecycle
An HTTP request in Angular follows a very specific path:

1.  **The Trigger:** A component or service calls a method like `.get()` or `.post()`.
2.  **The Interceptor Layer:** Before the request leaves the app, it passes through any configured **Interceptors** (e.g., to add an Auth Token).
3.  **The Backend:** The request is sent to the server; the server processes it and sends a response.
4.  **The Interceptor Layer (Response):** The response passes back through the interceptors (e.g., to catch a 401 error).
5.  **The Subscriber:** The data finally arrives at the `.subscribe()` block or the `async` pipe in your component.

### 2. Basic Implementation Example

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = 'https://api.example.com';

  // GET: Fetching data
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }

  // POST: Sending data
  createUser(newUser: User): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/users`, newUser);
  }
}
```

#### Implementation Context: Service with CRUD Operations
*(Extracted from `@angular3-demo/src/app/book/book.service.ts`)*

This service encapsulates all the standard HTTP operations required for a RESTful entity, demonstrating `.get()`, `.post()`, `.put()`, and `.delete()`. 

Each method returns an Observable — the caller subscribes to trigger the request and receive the response. Note how `authInterceptor` automatically attaches the token, simplifying the service logic so this service doesn't need to handle authentication headers manually.

```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Book } from './book.model';

@Injectable({ providedIn: 'root' })
export class BookService {
  private http = inject(HttpClient);

  // Base URL for the books REST endpoint
  private baseUrl = 'http://localhost:8080/books';

  /** Fetch all books (that belong to the user) from the API */
  getAll(): Observable<Book[]> {
    return this.http.get<Book[]>(this.baseUrl);
  }

  /** Fetch a single book by its ID */
  getById(id: string): Observable<Book> {
    return this.http.get<Book>(`${this.baseUrl}/${id}`);
  }

  /**
   * Create a new book.
   * Uses Omit<Book, 'id'> because the backend generates the ID.
   */
  create(book: Omit<Book, 'id'>): Observable<Book> {
    return this.http.post<Book>(this.baseUrl, book);
  }

  /**
   * Replace an existing book's data.
   * Uses PUT for a full replacement — all fields must be provided.
   */
  update(id: string, book: Omit<Book, 'id'>): Observable<Book> {
    return this.http.put<Book>(`${this.baseUrl}/${id}`, book);
  }

  /** Delete a book by ID. Returns void — no response body expected. */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

### Implementation Context: Advanced HTTP POST Responses
*(Extracted from `@angular3-demo/src/app/register/register.ts`)*

When dealing with legacy or specific backend setups (like an endpoint returning a `201 Created` with a raw text message instead of a JSON object), the `HttpClient` requires explicit configuration to prevent JSON parsing errors.

```typescript
import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export class Register {
  private http = inject(HttpClient);

  onSubmit(body: any): void {
    // POST to the backend registration endpoint
    // 1st arg: URL
    // 2nd arg: Request Body
    // 3rd arg: Options Configuration:
    //   - observe: 'response' -> Returns the full HttpResponse object (so we can check status 201)
    //   - responseType: 'text' -> Tells Angular NOT to run JSON.parse() on the text payload
    this.http.post('http://localhost:8080/register', body, { observe: 'response', responseType: 'text' }).subscribe({
      next: (response) => {
        // We have access to the exact HTTP Status code because we used observe: 'response'
        if (response.status === 201) {
          console.log('Registration successful!');
        }
      },
      error: (err) => {
        // HttpClient automatically routes non-2xx responses to the error callback.
        const message = typeof err.error === 'string' ? err.error : (err.error?.message ?? 'Unknown error');
        console.error(message);
      },
    });
  }
}
```

> [!NOTE] 
> Verified against official documentation: https://angular.dev/guide/http/making-requests (Passing `observe: 'response'` successfully changes the return type to `Observable<HttpResponse<T>>`, and `responseType: 'text'` explicitly prevents the default JSON parsing).

## HTTP Interceptors: The "Middleware"

An **Interceptor** is a powerful tool that allows you to "intercept" every single outgoing request and every incoming response. Instead of manually adding an Authorization header to every single API call in your app, you do it **once** in an interceptor.

### Common Use Cases
*   **Authentication:** Automatically attaching a `Bearer <token>` to the `Authorization` header.
*   **Error Handling:** Catching global errors (like a 500 Internal Server Error) and showing a notification.
*   **Logging:** Measuring how long requests take for performance monitoring.
*   **Loading Spinners:** Turning on a global loading spinner when a request starts and off when it ends.

### Functional Interceptor Example

```typescript
// auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authToken = 'my-secret-token'; // Usually retrieved from an AuthService

  // We MUST clone the request because the original request is immutable
  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${authToken}`
    }
  });

// Pass the cloned request to the next handler in the chain
  return next(authReq);
};
```

#### Implementation Context: Functional HTTP Interceptor
*(Extracted from `@angular3-demo/src/app/auth/auth.interceptor.ts`)*

This interceptor attaches a Bearer token to outgoing requests by reading the current token value from a Signal in the `AuthService`. 

How it works:
1. Reads the current auth token from `AuthService` (a signal).
2. If a token exists, clones the request and adds an `Authorization` header.
3. If no token is available, forwards the original request unchanged.

Cloning is necessary because Angular `HttpRequest` objects are immutable — you cannot modify them directly.

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Inject AuthService to access the stored JWT token
  const authService = inject(AuthService);

  // Read the current token value from the signal
  const token = authService.token();

  if (token) {
    // Clone the request and attach the Authorization header with a Bearer token
    const clonedReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });

    // Forward the modified request to the next handler in the chain
    return next(clonedReq);
  }

  // No token available — forward the original request as-is
  return next(req);
};
```

## Error Handling Strategies

In a distributed system, things will fail: the internet goes down, the server crashes, or the user's session expires. A professional application must handle these gracefully.

### 1. Local Error Handling
You can handle errors on a specific request using the `catchError` operator. This is best when you want to provide a specific fallback for a single call.

```typescript
import { catchError, throwError } from 'rxjs';

this.http.get('/api/data').pipe(
  catchError(error => {
    console.error('Individual request failed', error);
    return throwError(() => new Error('Something went wrong!'));
  })
).subscribe();
```

### 2. Global Error Handling
For errors that should affect the whole app (like a 401 Unauthorized), an Interceptor is the best place to catch them.

```typescript
// error.interceptor.ts
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401) {
        // Redirect to login page
      }
      return throwError(() => error);
    })
  );
};
```

### 3. Registering Interceptors: Connecting the Middleware

Defining an interceptor function is only half the battle; you must explicitly tell Angular to include that function in its HTTP processing pipeline. In modern Angular (v15+), this is handled via the **Functional Interceptor** pattern within your application configuration.

#### The Modern Way: `withInterceptors()`
In a standalone application, you register interceptors during the initialization of the `HttpClient` inside your `app.config.ts` file. You use the `provideHttpClient()` function combined with the `withInterceptors()` feature.

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

// Import your custom interceptor functions
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Registering the HttpClient with our middleware chain
    provideHttpClient(
      withInterceptors([
        authInterceptor,   // Interceptor #1
        errorInterceptor   // Interceptor #2
      ])
    )
  ]
};
```

#### The Execution Chain (The "Onion" Model)
It is critical to understand that interceptors do not run in isolation; they form a **chain**. The order in which you list them in the `withInterceptors` array determines the sequence of execution.

**For Outgoing Requests (App $\to$ Server):**
The interceptors execute in the **order they are listed** (Top to Bottom).
1.  `authInterceptor` runs (e.g., adds the token).
2.  `errorInterceptor` runs (e.g., logs the outgoing request).
3.  The request is finally sent to the server.

**For Incoming Responses (Server $\to$ App):**
The interceptors execute in **reverse order** (Bottom to Top).
1.  The response hits `errorInterceptor` first (to check for status codes like 401 or 500).
2.  The response then passes through `authInterceptor`.
3.  The response finally reaches your Service/Component.

#### Summary: Registration Checklist

| Requirement | Implementation |
| :--- | :--- |
| **Registration Site** | `app.config.ts` (inside `appConfig` providers). |
| **Function** | `provideHttpClient(withInterceptors([...]))` |
| **Order Matters?** | **YES**. Top $\to$ Bottom for requests, Bottom $\to$ Top for responses. |

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/http/interceptors
> Verified Functional Route Guards (`CanActivateFn`): https://angular.dev/guide/routing/common-router-tasks#preventing-unauthorized-access
> Verified `HttpClient` and Observable usage: https://angular.dev/guide/http/making-requests
| **Dependency Rule** | Ensure interceptors are exported as `const` functions. |

## Summary: HTTP Best Practices

| Concept | Best Practice | Why? |
| :--- | :--- | :--- |
| **Data Typing** | Always use Generics: `http.get<User[]>(...)` | Ensures type safety and prevents runtime errors. |
| **Immutability** | Always `.clone()` requests in interceptors. | Original requests are immutable; cloning is required to modify them. |
| **Location** | Keep HTTP calls in **Services**, never in Components. | Promotes reusability and keeps components "thin." |
| **Consumption** | Use the `async` pipe in templates. | Automatically handles subscriptions and prevents memory leaks. |
| **Security** | Use Interceptors for Auth headers. | Centralizes security logic and prevents code duplication. |

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/routing and https://angular.dev/guide/http

---

---

