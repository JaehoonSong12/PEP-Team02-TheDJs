<!-- SUPPLEMENT: Grouped topics for Services & Dependency Injection -->

# Services

## Overview of Services

In the Angular architecture, a **Service** is a class designed to perform a specific, repeatable task. While Components are responsible for the **User Interface** (what the user sees), Services are responsible for the **Business Logic** (how the data works).

### The Role of a Service
A service acts as a specialized worker that exists independently of any single component. Its primary responsibilities include:

1.  **Data Fetching:** Communicating with an external API via `HttpClient`.
2.  **Business Logic:** Performing complex calculations, data transformations, or validations.
3.  **State Management:** Holding and sharing data between multiple components.
4.  **Cross-Cutting Concerns:** Handling tasks that affect the whole app, such as logging, authentication, or error handling.

**The Golden Rule of Angular Architecture:**
> *Components should be "thin." They should only handle UI logic. If you find yourself writing complex math, API calls, or heavy data manipulation inside a component, that logic belongs in a Service.*

## Implementing a Service

A service is a standard TypeScript class decorated with `@Injectable()`. This decorator tells Angular that this class can be managed by the Dependency Injection (DI) system.

```typescript
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root' // This makes the service a global singleton
})
export class DataService {
  private data: string[] = ['Apple', 'Banana', 'Cherry'];

  getData() {
    return this.data;
  }

  addItem(newItem: string) {
    this.data.push(newItem);
  }
}
```

### Implementation Context: Service Implementation

In Angular, services are classes decorated with the `@Injectable` decorator. This indicates the class is a resource expected to be utilized in other parts of the application. Typical use cases include shared state management and shared functionality.

Beyond these examples, any business logic should be handled by one or more services, rather than components directly. Components should build the visual aspects of the application (and manage slight data or signaling between them), while heavy logic and actions are left to services.

By default, all utilized services are singletons. A service can be configured to act as a prototype bean (where each reference gets a new version), but this is uncommon.

```typescript
// File: angular1-example/src/app/service/example-service.ts
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ExampleService {
  private myServiceField = "Some private data"

  getMyServiceFieldData(){
    return this.myServiceField;
  }
}
```

## Provider Scopes: Controlling Lifetime & Visibility

One of the most important decisions a developer makes is **where** to provide a service. This decision dictates how many instances of the service exist and who has permission to use them.

### 1. Root Scope (The Singleton Pattern)
By using `providedIn: 'root'` inside the `@Injectable` decorator, you register the service at the application's root level.

*   **Visibility:** The service is available to every single component and service in the entire application.
*   **Lifetime:** A **Singleton**. Angular creates exactly **one instance** when the app starts and keeps it alive until the app is closed.
*   **Benefit (Tree-shaking):** If your application never actually uses the service, the Angular compiler is smart enough to "shake it off" and exclude it from the final production bundle, reducing your app's size.

### 2. Component Scope (The Isolated Pattern)
You can bypass the root level and provide a service directly within a component's `@Component` decorator.

*   **Visibility:** The service is only available to that specific component and any of its **child components**. It is invisible to the rest of the app.
*   **Lifetime:** A **New Instance** is created every time the component is initialized. When the component is destroyed, the service instance is destroyed along with it.
*   **Use Case:** Use this when you need a "private" instance of a service. For example, a `TimerService` that should only exist while a specific "Dashboard" component is on the screen.

### 3. Summary of Scopes

| Scope | Declaration Method | Instance Count | Visibility | Best Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Root** | `@Injectable({ providedIn: 'root' })` | **One (Singleton)** | Global | Shared data, Auth, API calls. |
| **Component** | `@Component({ providers: [...] })` | **One per component instance** | Component + Children | Isolated state, private logic. |

## Service-Based Communication

Services are the primary mechanism for **decoupled communication**. Instead of components talking to each other directly, they use a service as a "Message Hub."

### The "Pub/Sub" Pattern in Services
When two components are unrelated (e.g., a Sidebar and a Footer), they communicate by "publishing" and "subscribing" to data within a service.

1.  **The Publisher:** Component A calls a method in `SharedService` to update a value.
2.  **The Hub:** `SharedService` holds that value (usually inside an RxJS `Subject` or an Angular `Signal`).
3.  **The Subscriber:** Component B is "listening" to the service and automatically updates its view when the value changes.

**Conceptual Flow:**
`Component A` $\rightarrow$ `Service Method` $\rightarrow$ `Service State` $\rightarrow$ `Component B (Automatic Update)`

## Best Practices for Services

*   **Single Responsibility:** A service should do one thing well. Don't create a `GlobalService` that handles Auth, Logging, and API calls. Create `AuthService`, `LogService`, and `ApiService`.
*   **Keep Components Thin:** If a component is more than 100 lines of code, check if you can move some of its logic into a service.
*   **Prefer `providedIn: 'root'`:** Always start with root-level providers unless you have a specific reason to isolate a service to a single component.
*   **Use Signals/RxJS for State:** For any data that needs to be "shared" and "reactive," do not use simple variables. Use **Signals** (for state) or **RxJS Subjects** (for events) to ensure components react to changes automatically.

---

---

# Dependency Injection (DI)

## Overview of Dependency Injection

**Dependency Injection** is a design pattern where a class requests the tools it needs (dependencies) from an external source rather than creating them itself. 

In a standard class, if you need a `UserService`, you might write `this.userService = new UserService()`. In Angular, you don't do this. Instead, you tell Angular: *"I need a UserService,"* and Angular's **Injector** finds the instance and "injects" it into your class.

### Why use DI?
*   **Decoupling:** Components don't need to know how to configure or instantiate services. They only need to know how to use them.
*   **Testability:** You can easily swap a real `ApiService` for a "MockApiService" during unit testing.
*   **Singleton Pattern:** DI makes it easy to share a single instance of a service across many different components.

## The DI Mechanics: How it Works

The DI system relies on three core concepts: the **Dependency**, the **Provider**, and the **Injector**.

### 1. The Dependency (The "What")
This is the object or service that is being requested (e.g., `HttpClient`, `AuthService`, or a custom `LoggingService`).

### 2. The Provider (The "Recipe")
A provider tells the Injector **how** to create the dependency. It is the instruction manual.
*   When you use `@Injectable({ providedIn: 'root' })`, you are telling Angular: *"If anyone asks for this service, create one instance and make it available everywhere."*

### 3. The Injector (The "Factory")
The Injector is the engine that looks at your request, checks its "warehouse" of providers, and either hands you an existing instance or creates a new one based on the provider's recipe.

## Injection Patterns: Classic vs. Modern

Angular has evolved its syntax for requesting dependencies. You will encounter both in professional environments.

### 1. Constructor Injection (Legacy)
This has been the standard for years. You define your dependencies as parameters in the class `constructor`.

> **Project Context:** The "Constructor Injection" pattern is explicitly **banned** per the `AGENTS.md` guidelines. All new development MUST utilize the modern `inject()` functional API. The following is preserved for historical context.

```typescript
@Component({ ... })
export class UserProfileComponent {
  // The dependency is requested via the constructor
  constructor(private userService: UserService) {}

  ngOnInit() {
    this.user = this.userService.getUser();
  }
}
```
*   **Pros:** Very explicit; easy to see dependencies at a glance.
*   **Cons:** Can lead to "Constructor Bloat" in large components with many dependencies.

### 2. The `inject()` Function (Modern Standard)
Introduced in recent versions, the `inject()` function allows you to request dependencies outside of the constructor. This is the **mandated** method in this project.

```typescript
import { Component, inject } from '@angular/core';

@Component({ ... })
export class UserProfileComponent {
  // The dependency is requested using the inject() function
  private userService = inject(UserService);

  user = this.userService.getUser();
}
```
*   **Pros:** Cleaner syntax; works better with functional programming patterns; easier to use in "factory" functions or route guards.
*   **Cons:** Requires a slightly different mental model for where variables are initialized.

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/di

---

---

