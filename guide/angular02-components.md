<!-- SUPPLEMENT: Grouped topics for Components & Communication -->

# Component Architecture

## Overview of Components

In Angular, a **Component** is the fundamental building block of the application. If you think of an application as a LEGO set, each component is a single, self-contained brick. You build small, simple bricks (like a button or a search bar) and combine them to create larger, more complex structures (like a navigation bar or a user dashboard).

### The Component Trinity
A component is not just a single file; it is a cohesive unit composed of three distinct layers, ensuring the **Separation of Concerns**:

1.  **The Class (Logic):** The TypeScript (`.ts`) file. This is the "Brain." It manages the data, handles user interactions, and dictates how the component behaves.
2.  **The Template (View):** The HTML (`.html`) file. This is the "Skeleton." It defines the structure of what the user actually sees on the screen.
3.  **The Styles (Presentation):** The CSS/SCSS (`.css`) file. This is the "Skin." It defines the visual appearance, ensuring the component looks correct.

**Conceptual Summary**
| Layer | File Type | Responsibility | Analogy |
| :--- | :--- | :--- | :--- |
| **Logic** | `.ts` | Data & Behavior | The Brain |
| **Template** | `.html` | Structure & UI | The Skeleton |
| **Styles** | `.css` | Appearance | The Skin |

---

## Standalone Components: The Modern Standard

Historically, Angular required components to be "registered" inside a container called an `NgModule`. However, modern Angular has shifted to **Standalone Components** (introduced in v14/15, and established as the default structure in Angular v19+).

### The Concept of Autonomy
In the "Legacy" model, a component was like an employee who couldn't work unless they were part of a specific department (the Module). In the **Standalone** model, the component is an independent professional. It carries its own tools and knows exactly what it needs to function.

*   **Direct Dependencies:** Instead of looking to a Module to find a tool (like `CommonModule` for `*ngIf`), a Standalone component lists its own dependencies directly in its `@Component` decorator.
*   **Reduced Complexity:** This eliminates the need for complex `NgModule` files, making the application easier to navigate, test, and tree-shake (remove unused code).

### Component Anatomy (Code Example)

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common'; // Required for standard directives

@Component({
  selector: 'app-user-profile',      // The custom HTML tag name
  // standalone: true,              // Tells Angular this is a standalone unit (implicit default in v19+)
  imports: [CommonModule],          // The component's own "toolbox"
  templateUrl: './user-profile.html',
  styleUrls: ['./user-profile.css'],
  changeDetection: ChangeDetectionStrategy.OnPush // [Mandated Standard: Performance]
})
export class UserProfileComponent {
  // The Logic Layer
  username: string = 'John Doe';
  
  changeName() {
    this.username = 'Jane Doe';
  }
}
```

### Implementation Context: Component Configuration

The following real-world example demonstrates component configuration. Components are the fundamental building blocks of Angular. HTML elements are not simply added to the app: components are created, and those components produce the structure of the application.

Core configurations:
- **selector:** Dictates what tags to use to reference the component in an HTML template.
- **imports:** Required when placing components in other components and adding services, ensuring the component has access to them.
- **templateUrl:** A reference to the HTML file acting as the component template.
- **styleUrl:** A reference to the external style sheet.

A clear distinction should be maintained between the HTML template and the component class:
- **HTML Template:** Provides the structure of the component.
- **Component Class:** Provides the data for the component. If data is displayed in the HTML, it must be defined here.

```typescript
// File: angular1-example/src/app/app.ts
import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SecondComponent } from './second-component/second-component';
import { DirectiveComponent } from "./directive-component/directive-component";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, FormsModule, SecondComponent, DirectiveComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly message = signal("but you should really do this");
  // Component logic omitted for brevity
}
```

## Lifecycle Hooks: Managing the Component Journey

A component is not static; it is born, it lives, it changes, and eventually, it dies. Angular provides **Lifecycle Hooks**—specialized methods that allow you to "tap into" these specific moments to execute code.

### The Component Lifecycle Timeline

The lifecycle follows a strict chronological order. Understanding this order is critical for managing memory and data initialization.

#### 1. The Creation Phase (Initialization)
*   **`ngOnChanges`**: Called whenever an `@Input` property changes. This is the first hook to run.
*   **`ngOnInit`**: Called once, after the first `ngOnChanges`. This is the best place to perform "setup" tasks, like fetching initial data from a service.

#### 2. The Content & View Phase (Rendering)
*   **`ngAfterContentInit`**: Called after external content (via `<ng-content>`) has been projected into the component.
*   **`ngAfterViewInit`**: Called after the component's own view (and all its children) is fully initialized. This is where you interact with the DOM if necessary.

#### 3. The Change Detection Phase (Maintenance)
*   **`ngDoCheck`**: A custom hook that runs during every change detection cycle. 
    *   *Warning:* Use this sparingly. If you put heavy logic here, your app will become slow, as this runs constantly.

#### 4. The Destruction Phase (Cleanup)
*   **`ngOnDestroy`**: Called just before the component is removed from the DOM.
    *   **Critical Task:** This is where you "clean up" to prevent memory leaks (e.g., unsubscribing from Observables or stopping timers).

### Lifecycle Cheat Sheet

| Hook | Timing | Primary Use Case |
| :--- | :--- | :--- |
| **`ngOnChanges`** | On Input change | Reacting to data passed from a parent. |
| **`ngOnInit`** | On Init | Initializing data/API calls. |
| **`ngAfterViewInit`** | On View Init | Accessing child elements or DOM nodes. |
| **`ngOnDestroy`** | On Destruction | Unsubscribing from streams & cleaning up. |

---

## View Encapsulation: Protecting Styles

One of Angular's most powerful features is **View Encapsulation**. By default, Angular ensures that the CSS you write for `Component A` does not "leak out" and accidentally change the appearance of `Component B`.

### Encapsulation Modes

| Mode | Behavior | Use Case |
| :--- | :--- | :--- |
| **Emulated** (Default) | Angular adds unique attributes (e.g., `_ngcontent-c1`) to your HTML/CSS to scope styles. | The standard for 99% of components. |
| **None** | Styles are applied globally. They will affect every element on the page. | Use only for global theme overrides. |
| **ShadowDom** | Uses the browser's native Shadow DOM API to create a hard boundary. | For building highly isolated Web Components. |

If you need to force a style to reach "inside" a child component, you cannot simply write CSS in the parent. You must use specific selectors like `:host` (to style the component itself) or `@Component` specific styling strategies.

---

---

# Data Binding & Communication

## Overview of Data Flow

In a complex Angular application, data is constantly in motion. Effective communication is the difference between a well-organized application and a "spaghetti code" mess where every component is tightly coupled to every other component.

To maintain a scalable architecture, we categorize communication into two distinct patterns:
1.  **Component-to-Component (Hierarchical):** Moving data up or down the "family tree" (Parent $\leftrightarrow$ Child).
2.  **Cross-Component (Decoupled):** Moving data between "strangers" (Siblings or unrelated components) using a central hub.

## Hierarchical Communication (Parent $\leftrightarrow$ Child)

This is the most common form of communication. It follows a strict "Data Down, Events Up" philosophy, which makes the flow of information predictable and easy to debug.

### 1. Parent to Child: `input()` [Mandated Standard]

> [!WARNING]
> **Project Context:** The legacy `@Input()` and `@Output()` decorators are explicitly **banned** per the `AGENTS.md` guidelines. All new development MUST utilize the modern signal-based `input()` and `output()` functional APIs.

When a parent component wants to pass data down to its child, it uses the `input()` function. This creates a reactive signal that the child can read.

*   **Mechanism:** The parent "binds" a value to the child's property in the template.
*   **Legacy Context:** Older code may use the `@Input()` decorator. This is preserved for historical context but must be refactored.

```typescript
// CHILD COMPONENT (Modern Standard)
import { Component, input } from '@angular/core';

@Component({ ... })
export class ChildComponent {
  userTitle = input<string>(''); // Modern Signal Input
}

// PARENT TEMPLATE
<app-child [userTitle]="'Administrator'"></app-child>
```

### 2. Child to Parent: `output()` [Mandated Standard]
A child component cannot directly change the data in its parent. Instead, it must "emit" an event using the modern `output()` function. The parent uses **Event Binding** `( )` to catch it.

*   **Mechanism:** The child uses `output()` to broadcast a message. The parent uses **Event Binding** `( )` to catch it.
*   **Legacy Context:** Older code may use `@Output()` with an `EventEmitter`.

```typescript
// CHILD COMPONENT (Modern Standard)
import { Component, output } from '@angular/core';

@Component({ ... })
export class ChildComponent {
  taskCompleted = output<string>();

  finishTask() {
    this.taskCompleted.emit('Task #1 is done!');
  }
}

// PARENT TEMPLATE
<app-child (taskCompleted)="handleNotification($event)"></app-child>
```

### 3. Two-Way Binding: `[(ngModel)]` [Deprecated]

> [!WARNING]
> **Project Context:** Template-driven forms (and `[(ngModel)]`) are explicitly **deprecated**. The project mandates the use of **Reactive Forms** (`FormControl`, `FormGroup`) for all user input and synchronization.

#### Implementation Context: Reactive Forms and OnPush
*(Extracted from `@angular3-demo/src/app/login/login.ts`)*

This component uses `ReactiveFormsModule` to bind the template controls to a structured `FormGroup`. It also specifies `ChangeDetectionStrategy.OnPush`, ensuring highly optimized rendering that only triggers when Signals change or @Input bindings update.

- `imports`: Requires `ReactiveFormsModule` to enable `[formGroup]` and `formControlName` in the HTML template.
- `loginForm`: A `FormGroup` that organizes multiple `FormControl` objects into a single unit. Values can be read directly from its structure.

```typescript
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private authService = inject(AuthService);

  loginForm = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  errorMessage = signal('');

  onSubmit(): void {
    const username = this.loginForm.value.username ?? '';
    const password = this.loginForm.value.password ?? '';

    // Delegate to the AuthService (Implementation omitted for brevity)
    // this.authService.login(username, password)...
  }
}
```

## Summary of Hierarchical Patterns

| Direction | Pattern | Tool | Syntax |
| :--- | :--- | :--- | :--- |
| **Down** | Parent $\to$ Child | `@Input()` | `[property]="value"` |
| **Up** | Child $\to$ Parent | `@Output()` | `(event)="handler()"` |
| **Both** | Parent $\leftrightarrow$ Child | `[(ngModel)]` | `[(ngModel)]="value"` |

### Implementation Context: Hierarchical Communication

When rendering a component inside another component (creating a parent-child relationship), the component is referenced by its `selector` value via HTML tags. Anytime data must be passed down from a parent element into a child element, the `@Input` decorator is used to specify to Angular that the value of the field is provided by another resource.

If a signal or data must be passed up to the parent, an `EventEmitter` is utilized. These resources allow event binding on the child component's custom events, to which the parent can respond.

**Child Component:**
```typescript
// File: angular1-example/src/app/second-component/second-component.ts
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-second-component',
  imports: [],
  templateUrl: './second-component.html',
  styleUrl: './second-component.css',
})
export class SecondComponent {
  // Provided by the parent component
  @Input() 
  message = "";

  // Event emitter to signal out
  @Output() 
  childEventEmitter = new EventEmitter()

  @Output()
  childEventEmitterWithData: EventEmitter<string> = new EventEmitter();

  triggerChildEvent(){
    // Trigger a signal emission using the emit function.
    this.childEventEmitter.emit();
  }

  triggerSendingOfStringData(){
    this.childEventEmitterWithData.emit(this.message);
  }
}
```

**Parent Template:**
```html
<!-- File: angular1-example/src/app/app.html -->
<!-- 
  If you want to render a component inside of another component (creating a
  parent - child relationship between them) you reference the component by using its "selector"
  value in tags
-->
<app-second-component 
  [message]="valueForChildElement" 
  (childEventEmitter)="respondToChildEvent()" 
  (childEventEmitterWithData)="respondToChildStringData($event)">
</app-second-component>
```

## Cross-Component Communication (Decoupled)

In many cases, components are not in a parent-child relationship (e.g., a "User Profile" component in the header and a "User Settings" component in the main content area). Communicating through a long chain of `@Input` and `@Output` (known as "Prop Drilling") is fragile and difficult to maintain.

Instead, we use a **Service-Based Approach**.

### The Service as a "Message Hub"
Rather than talking to each other, components talk to a shared **Service**. The Service acts as a central "post office" or "message hub."

#### Implementation Strategies:

**1. The RxJS Subject (The Event Bus)**
The most common way to implement a decoupled pattern is using an RxJS `Subject` or `BehaviorSubject` inside a Service.
*   **The Publisher:** A component calls a method in the Service to "push" a new value into the Subject.
*   **The Subscriber:** Other components "subscribe" to that same Subject in the Service to receive updates whenever they happen.

**2. The Signal (The Reactive State)**
In modern Angular, a Service can hold a `signal()`. 
*   Any component can read the signal to get the current value.
*   Any component can update the signal, and **every other component** reading that signal will update automatically and instantly.

### Comparison: When to use which?

| Scenario | Recommended Method | Why? |
| :--- | :--- | :--- |
| **Direct Parent $\to$ Child** | `@Input()` | Simplest, most explicit, and highest performance. |
| **Direct Child $\to$ Parent** | `@Output()` | Maintains the "Data Down, Events Up" principle. |
| **Deeply Nested Components** | **Service (Signals/RxJS)** | Avoids "Prop Drilling" through middle-man components. |
| **Unrelated Components** | **Service (Signals/RxJS)** | Provides a single source of truth that is easy to access. |

**Key Architectural Rule:**
If you find yourself passing data through more than two levels of components just to get it to a destination, stop. You should move that data into a **Service**.

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/components
> Verified `ChangeDetectionStrategy.OnPush` default behavior and performance characteristics: https://angular.dev/best-practices/skipping-change-detection
> Verified modern `input()` and `output()` Signal APIs: https://angular.dev/guide/signals/inputs and https://angular.dev/guide/components/outputs
> Verified Reactive Forms `FormGroup` and `FormControl` patterns: https://angular.dev/guide/forms/reactive-forms

---

---

