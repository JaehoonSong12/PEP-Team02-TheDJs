<!-- SUPPLEMENT: Grouped topics for Templates & Directives -->

# Templates & Control Flow

## Overview of Templates

An Angular **Template** is a specialized version of HTML that allows you to embed dynamic logic directly into your markup. While standard HTML is static (it displays exactly what is written), an Angular template is **reactive**—it changes automatically whenever the underlying component data changes.

### The Role of the Template
The template acts as the bridge between the **Logic Layer** (the TypeScript class) and the **User Interface**. It doesn't just display data; it provides the structure for how that data is interpreted, looped, and conditionally shown.

## Template Syntax & Data Binding

To make a template dynamic, Angular uses specific syntax to "bind" the component's properties to the HTML elements.

### 1. Interpolation: The "Output"
Interpolation is the simplest form of data binding. It allows you to pull a value from the TypeScript class and display it as text within your HTML.
* **Syntax:** `{{ value }}`
* **Example:** `<p>Welcome, {{ username }}!</p>`

### 2. Property Binding: The "Input"
Property binding allows you to pass data from the component to an HTML attribute or a component property. This is used to control things like `disabled`, `href`, or custom component inputs.
* **Syntax:** `[property]="value"`
* **Example:** `<button [disabled]="isFormInvalid">`

### 2a. Image Optimization: `NgOptimizedImage` [Mandated Standard]
> [!WARNING]
> **Project Context:** The standard `[src]` attribute is explicitly **banned** for static images per `AGENTS.md`. The project mandates the `NgOptimizedImage` directive for all static images.

* **Syntax:** `ngSrc="url"` (instead of `src`)
* **Example:** `<img [ngSrc]="userImageUrl" width="100" height="100">`
* **Requirement:** Requires the `NgOptimizedImage` module to be imported.

### 3. Event Binding: The "Action"
Event binding allows the template to communicate back to the component. It listens for user actions (clicks, typing, hovering) and triggers a method defined in the TypeScript class.
* **Syntax:** `(event)="handler()"`
* **Example:** `<button (click)="saveData()">Save</button>`

### 4. Two-Way Binding: The "Sync"
Two-way binding creates a continuous, synchronized loop between the view and the logic. If the user changes an input value, the component data updates; if the component data updates, the input value changes.
* **Syntax:** `[(ngModel)]="value"`
* **Requirement:** Requires the `FormsModule` to be imported.

**Binding Summary Table**

| Type | Syntax | Direction | Purpose |
| :--- | :--- | :--- | :--- |
| **Interpolation** | `{{ }}` | Component $\to$ View | Displaying text content. |
| **Property** | `[ ]` | Component $\to$ View | Setting element attributes/properties. |
| **Event** | `( )` | View $\to$ Component | Handling user interactions. |
| **Two-Way** | `[( )]` | Both Ways | Synchronizing form inputs with data. |

### Implementation Context: Data Binding

In Angular, there are several ways to reference data from a component in an HTML template via one-way data binding (property and event binding) or two-way data binding.

- **Interpolation:** Double curly brackets indicate a reference from the component (field or function) to render in the viewport.
- **Property Binding:** The attribute to augment is enclosed in square brackets, and the value references the component property.
- **Event Binding:** The event is enclosed in parentheses, and its value is set to the function called when the event triggers.
- **Two-Way Binding:** Utilizes the `ngModel` attribute to facilitate simultaneous sync between the UI and logic (e.g., passing user input back to the component while keeping the UI updated).

```typescript
// File: angular1-example/src/app/app.ts
import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-root',
  // ...
})
export class App {
  readonly message = signal("but you should really do this");
  isDisabled = signal(true);
  twoWayData = "";

  toggleDisabledButton(){
    this.isDisabled.set(!this.isDisabled());
  }
}
```

```html
<!-- File: angular1-example/src/app/app.html -->
<!-- Interpolation -->
<h1>{{ message() }}</h1>

<!-- Property Binding -->
<button [disabled]="isDisabled()">This button should be disabled</button>

<!-- Event Binding -->
<button (click)="toggleDisabledButton()">toggle disabled button</button>

<!-- Two-Way Binding -->
<input type="text" placeholder="Enter some value" [(ngModel)]="twoWayData">
<p>{{ twoWayData }}</p>
```

## Control Flow: Managing Logic in HTML

Control flow determines which parts of a template should be rendered based on certain conditions. Angular has recently undergone a major evolution in how this is handled.

### 1. Modern Built-in Control Flow (Angular 17+)
The new "Block Syntax" is the current standard. It is faster, more readable, and doesn't require importing extra modules like `CommonModule`.

#### **Conditional Logic: `@if`**
Used to show or hide elements based on a boolean condition.
```html
@if (isLoggedIn) {
  <button>Logout</button>
} @else {
  <button>Login</button>
}
```

#### **Iterative Logic: `@for`**
Used to loop through a collection (array) and render a template for each item. 
*Note: The `track` property is mandatory and essential for performance.*
```html
<ul>
  @for (item of items; track item.id) {
    <li>{{ item.name }}</li>
  } @empty {
    <li>No items found in the list.</li>
  }
</ul>
```

#### **Switch Logic: `@switch`**
Used to choose between multiple possible outcomes based on a single value.
```html
@switch (userRole) {
  @case ('admin') { <admin-dashboard /> }
  @case ('editor') { <editor-panel /> }
  @default { <user-view /> }
}
```

### 2. Legacy Structural Directives (The "Old" Way)
Before Angular 17, control flow was managed via "Structural Directives" using an asterisk (`*`) prefix. You will still encounter these in older codebases, but they are being phased out in favor of the `@` syntax.

| Feature | Legacy Syntax (`*`) | Modern Syntax (`@`) |
| :--- | :--- | :--- |
| **Conditionals** | `*ngIf="condition"` | `@if (condition)` |
| **Loops** | `*ngFor="let item of items"` | `@for (item of items; track ...)` |
| **Switch** | `*ngSwitch="value"` | `@switch (value)` |

**Key Differences to Remember:**
*   **Performance:** The new `@` syntax is built directly into the compiler, making it significantly faster than the legacy `*` directives.
*   **Complexity:** The new syntax handles the "Else" and "Empty" states (`@else`, `@empty`) much more cleanly than the old directives.
*   **Imports:** Legacy directives require `CommonModule` to be imported; Modern control flow works automatically.

### Implementation Context: Modern Control Flow

In the past, special directives were utilized for control-flow syntax directly in HTML templates. In modern Angular, a specialized template syntax is utilized to handle logic more cleanly without explicit imports.

```html
<!-- File: angular1-example/src/app/app.html -->
<!-- Conditional Logic -->
@if (isVisible) {
  <p>{{conditionalText}}</p>
}
<button (click)="toggleIsVisible()">toggle isvisible</button>

<!-- Iterative Logic -->
<h3>My Pets</h3>
<ul>
  @for (petName of myPetNames; track $index) {
    <li>{{petName}}</li>
  }
</ul>

<!-- Switch Logic -->
@switch (switchInput) {
  @case (1) {
    <p>first case matched</p>
  }
  @case (2) {
    <p>second case matched</p>
  }
  @default {
    <p>no case matched</p>
  }
}
```

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/templates/control-flow

---

---

# Directives & Pipes

## Overview of Directives

In Angular, a **Directive** is a class that adds extra behavior to elements in your template. While a Component is a directive with its own template, "pure" directives are used to manipulate existing DOM elements or change their appearance without creating new components.

Directives are categorized into two distinct types based on their impact on the DOM.

---

## 1. Attribute Directives (The "Stylists")

Attribute directives change the **appearance or behavior** of an existing element without altering the structure of the DOM. They act as "decorators," allowing you to apply logic-driven visual changes to your HTML.

While you can use standard HTML attributes (like `class` or `style`), Angular provides two built-in attribute directives to handle **dynamic** styling: `[ngClass]` and `[ngStyle]`.

> [!WARNING]
> **Project Context:** The `[ngClass]` and `[ngStyle]` directives are explicitly **banned** per the `AGENTS.md` guidelines. All new development MUST utilize native `[class.name]="condition"` and `[style.prop]="value"` bindings. The following documentation on `ngClass` and `ngStyle` is preserved for legacy maintenance only.

### The Engineering Decision: `[ngClass]` vs. `[ngStyle]`

The most important skill in mastering these directives is knowing which one to choose. Using the wrong one leads to "code smell"—either bloated CSS files or messy, unmaintainable inline styles.

#### **A. `[ngClass]` (The Class Manager)**
`[ngClass]` is used to toggle one or more **pre-defined CSS classes**.

*   **When to use it:** Use this for **state-based styling**. If you are changing the look of an element based on a status (e.g., `is-error`, `is-loading`, `is-active`), you should always use `ngClass`.
*   **Why:** It preserves the **Separation of Concerns**. Your logic stays in TypeScript, but your visual definitions (colors, padding, fonts) stay in your CSS file.

**Implementation Patterns:**

| Pattern | Syntax | Best Use Case |
| :--- | :--- | :--- |
| **Object Syntax** | `[ngClass]="{ 'class-name': condition }"` | Toggling multiple specific classes based on booleans. |
| **String Syntax** | `[ngClass]="condition ? 'class-a' : 'class-b'"` | Switching between two distinct visual states. |
| **Array Syntax** | `[ngClass]="['class-one', 'class-two']"` | Applying a static set of multiple classes dynamically. |

**Example: State-Driven Styling**
```html
<!-- The 'warning-text' class is applied only if shouldWarnUser is true -->
<p [ngClass]="{ 'warning-text': shouldWarnUser, 'success-text': isSuccess }">
  System Status Message
</p>
```

---

#### **B. `[ngStyle]` (The Inline Stylist)**
`[ngStyle]` is used to apply **specific, dynamic CSS properties** directly to an element's `style` attribute.

*   **When to use it:** Use this for **value-driven styling**. If the style value is a number or a color that is calculated at runtime (e.g., a progress bar width, a user-selected color, or a dynamic font size), you must use `ngStyle`.
*   **Why:** You cannot write a CSS class for every possible percentage or color code. `ngStyle` allows the math to happen in your logic and the result to be applied to the UI.

**Implementation Patterns:**

| Pattern | Syntax | Best Use Case |
| :--- | :--- | :--- |
| **Object Syntax** | `[ngStyle]="{ 'property': value }"` | Applying multiple dynamic properties at once. |
| **Function Syntax** | `[ngStyle]="getDynamicStyles()"` | Complex logic that is too messy for a single line. |

**Example: Calculation-Driven Styling**
```html
<!-- The width is calculated based on the progress variable -->
<div class="progress-bar"
     [ngStyle]="{ 'width.%': progressPercentage, 'background-color': statusColor }">
</div>
```

### Summary: The Rule of Thumb

To maintain a clean, professional codebase, follow this hierarchy of decision-making:

1.  **Can I define this look in my CSS file?**
    *   *Yes* $\rightarrow$ Use **`[ngClass]`**. (Preferred)
2.  **Is the value a calculation or a variable (like a number or a hex code)?**
    *   *Yes* $\rightarrow$ Use **`[ngStyle]`**.
3.  **Am I only changing ONE single property?**
    *   *Yes* $\rightarrow$ Use **Property Binding** (e.g., `[style.color]="'red'"` or `[class.active]="true"`). This is more performant than the directives.

| Feature | `[ngClass]` | `[ngStyle]` |
| :--- | :--- | :--- |
| **Primary Target** | CSS Classes | Inline CSS Properties |
| **Logical Driver** | Boolean States (On/Off) | Dynamic Values (Numbers/Colors) |
| **Separation of Concerns** | **High** (Styles stay in CSS) | **Low** (Styles live in Template/TS) |
| **Complexity** | Best for complex "themes" | Best for "math-based" layouts |

### Deep Dive: Creating Custom Attribute Directives

While built-in directives like `ngClass` handle common tasks, **Custom Attribute Directives** allow you to encapsulate reusable, complex DOM behaviors into a single, declarative instruction.

#### 1. The Anatomy of a Custom Directive
To create a directive, you use the `@Directive` decorator. The three most important pieces of a custom directive are:
1.  **The Selector:** The "name" of your directive (usually in square brackets, e.g., `[appHighlight]`). This is how you apply it to an element in HTML.
2.  **`ElementRef`:** A service that gives the directive direct access to the DOM element it is attached to.
3.  **`Renderer2`:** A service used to manipulate the element. 
    *   *Crucial:* You should **always** use `Renderer2` instead of direct DOM manipulation (like `el.style.color = 'red'`) to ensure your app remains safe for Server-Side Rendering (SSR).

#### 2. Practical Implementation Example: The "Hover Highlight"
Below is a classic implementation of a directive that changes an element's background color when a user hovers over it.

> [!WARNING]
> **Project Context:** The `@HostListener` and `@HostBinding` decorators are explicitly **banned**. Events and bindings must be defined within the `host` object of the `@Directive` decorator. The `@Input` decorator is also deprecated in favor of the `input()` function.

```typescript
import { Directive, ElementRef, Renderer2, input } from '@angular/core';

@Directive({
  selector: '[appHighlight]', 
  // [Mandated Standard] Host events are declared in the decorator metadata
  host: {
    '(mouseenter)': 'onMouseEnter()',
    '(mouseleave)': 'onMouseLeave()'
  }
})
export class HighlightDirective {
  // [Mandated Standard] Modern signal-based input
  appHighlight = input<string>('yellow'); 

  constructor(private el: ElementRef, private renderer: Renderer2) {}

  onMouseEnter() {
    this.setBgColor(this.appHighlight());
  }

  onMouseLeave() {
    this.setBgColor(null);
  }

  private setBgColor(color: string | null) {
    // Use Renderer2 for safe DOM manipulation
    this.renderer.setStyle(this.el.nativeElement, 'background-color', color);
  }
}
```

**How to use it in a template:**
```html
<!-- Using the default color defined in the class -->
<p appHighlight>Hover over me!</p>

<!-- Passing a custom color via property binding -->
<p [appHighlight]="'cyan'">I will turn cyan on hover!</p>
```

#### 3. Essential Tools for Directive Authors

| Tool | Role | Why use it? |
| :--- | :--- | :--- |
| **`host` object** | **Event/Property Linker** | [Mandated] Allows the directive to listen to events or bind properties natively without decorator overhead. |
| **`@HostListener`** | **Event Listener** | [Deprecated] Listens to events (click, mouseover, keyup) happening on its host element. |
| **`@HostBinding`** | **Property Linker** | [Deprecated] Automatically links a class property to a property of the host element. |
| **`ElementRef`** | **The Target** | Provides a reference to the actual HTML element the directive is sitting on. |
| **`Renderer2`** | **The Hand** | The safe way to change styles, classes, or attributes without breaking the Angular abstraction layer. |

### Implementation Context: Custom Directives

Anytime styling or content transformation must be shared across multiple elements, a custom directive can be applied directly to the resource instead of injecting a service or updating CSS across multiple components.

To access the element, a reference to the `ElementRef` is required. To ensure the change is set safely across different environments (like SSR), a `Renderer2` must be utilized.

```typescript
// File: angular1-example/src/app/directive/custom-directive.ts
import { Directive, ElementRef, OnInit, Renderer2 } from '@angular/core';

@Directive({
  selector: '[appCustomDirective]',
})
export class CustomDirective implements OnInit {
  constructor(private element: ElementRef, private renderer: Renderer2) {}
  
  // This function triggers after the element is initialized
  ngOnInit(): void {
    const newColor = "red";
    /*
      Using the renderer's setStyle function, styling is changed via three arguments:
      1. A reference to the nativeElement.
      2. A string reference to the style property.
      3. The new value of the style.
    */
    this.renderer.setStyle(this.element.nativeElement, 'color', newColor);
  }
}
```

#### 4. When to Build a Custom Directive
Don't create a directive just because you can. Use them when a behavior meets these criteria:
*   **Reusability:** The behavior (like a tooltip, a mask for an input, or a drag-and-drop capability) is needed in multiple different components.
*   **Declarative Intent:** You want to make your HTML more readable. ` <input appPhoneMask>` is much more readable than a complex set of event listeners and regex logic inside a component.
*   **Separation of Concerns:** The logic is purely "DOM-focused" (e.g., "When this is clicked, make it shake") rather than "Data-focused."

---

## 2. Structural Directives (The "Architects")
Structural directives are the heavy hitters. They change the **layout of the DOM** by adding, removing, or replacing elements.

*   **Behavior:** They effectively "rewrite" the HTML structure at runtime. 
*   **Identification:** In the legacy syntax, these are always identified by an asterisk (`*`) prefix.
*   **Modern Evolution:** As covered in the *Templates & Control Flow* section, the modern standard is to use the **Built-in Control Flow** (`@if`, `@for`, `@switch`), which performs these structural changes more efficiently than the legacy directives.

### Comparison: Legacy vs. Modern

> **Project Context:** The legacy Structural Directives (`*ngIf`, `*ngFor`, `*ngSwitch`) are explicitly **banned** per the `AGENTS.md` guidelines. All new development MUST utilize the modern, built-in Native Control Flow (`@if`, `@for`, `@switch`). The legacy syntaxes are preserved in the table below for historical maintenance purposes only.

| Goal | Legacy Structural Directive (Banned) | Native Control Flow (Mandated) |
| :--- | :--- | :--- |
| **Conditional Rendering** | `*ngIf="condition"` | `@if (condition) { ... }` |
| **List Rendering** | `*ngFor="let item of list"` | `@for (item of list; track item.id) { ... }` |
| **Switch/Case** | `*ngSwitch="value"` | `@switch (value) { @case (x) { ... } }` |

**Note:** While the `@` syntax is preferred for new development, understanding the `*` syntax is essential for maintaining existing Angular applications.

---

## Pipes: The "Data Transformers"

A **Pipe** is a lightweight tool used to transform a piece of data directly within a template. Pipes allow you to keep your component logic "clean" by offloading purely visual formatting to the template layer.

### The Concept of Transformation
A pipe takes an input value, performs a transformation, and returns a formatted output. Crucially, **pipes do not change the original data in the component**; they only change how that data is *rendered* to the user.

*   **Analogy:** Think of a pipe as a "lens." The object behind the lens (the data) remains the same, but the lens changes how it looks to the observer (the user).

### Common Built-in Pipes

| Pipe | Input | Output Example | Use Case |
| :--- | :--- | :--- | :--- |
| `uppercase` | `'hello'` | `HELLO` | Standardizing headers. |
| `lowercase` | `'HELLO'` | `hello` | Normalizing user input. |
| `date` | `Date Object` | `Oct 24, 2023` | Formatting timestamps. |
| `currency` | `100` | `$100.00` | Displaying monetary values. |
| `percent` | `0.5` | `50%` | Converting decimals to percentages. |
| `async` | `Observable` | (The emitted value) | Automatically subscribing to async data. |
| `json` | `Object` | `{"id": 1, "name": "..."}` | Debugging objects in the template. |

### Pipe Syntax
Pipes are applied using the "pipe" character (`|`) within interpolation or property binding.

```html
<!-- Basic usage -->
<p>The date is {{ today | date:'shortDate' }}</p>

<!-- Usage with parameters (passing arguments to the pipe) -->
<p>Price: {{ amount | currency:'EUR' }}</p>

<!-- Chaining multiple pipes -->
<p>{{ username | lowercase | trim }}</p>
```

### Custom Pipes
When built-in pipes aren't enough, you can create your own using the `@Pipe` decorator. This is useful for domain-specific formatting, such as converting a complex business code into a human-readable string. While built-in pipes handle standard formatting, **Custom Pipes** allow you to implement domain-specific transformations. However, because pipes run during the change detection cycle, understanding how they execute is vital to preventing performance bottlenecks.

### Creating a Custom Pipe
A custom pipe is a class decorated with `@Pipe`. It must implement the `PipeTransform` interface, which requires a `transform` method.

### Implementation Example: The "Weight Converter"
Imagine an application that displays data in kilograms but needs to show imperial units based on a user preference.

```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'kgToLbs'
})
export class KgToLbsPipe implements PipeTransform {
  /**
   * @param value The weight in KG (the input)
   * @param ratio The multiplier (optional argument, defaults to 2.2)
   */
  transform(value: number, ratio: number = 2.2): number {
    if (isNaN(value)) return 0;
    return +(value * ratio).toFixed(2); // Returns a rounded number
  }
}
```

**How to use it in a template:**
```html
<!-- Basic usage -->
<p>Weight: {{ weightKg | kgToLbs }} lbs</p>

<!-- Usage with a custom argument (e.g., a different conversion ratio) -->
<p>Weight: {{ weightKg | kgToLbs:2.5 }} lbs</p>
```

### Pure vs. Impure Pipes (The Performance Key)

This is the most critical technical distinction in Angular pipes. It determines how often the `transform` method is executed.

### Pure Pipes (The Default & High Performance)
By default, all pipes are **Pure**. 
* **Behavior:** A pure pipe is only called when Angular detects a **change to the input value itself**. 
* **Primitive Types:** If the input is a `string`, `number`, or `boolean`, the pipe runs only when the value changes.
* **Object/Array Types:** If the input is an `Object` or `Array`, the pipe runs **only when the reference changes**. 
    * *Crucial:* If you push a new item into an array, the array reference remains the same, and a **Pure Pipe will NOT re-run**. You must replace the array (e.g., `this.items = [...this.items, newItem]`) to trigger the pipe.

### Impure Pipes (The "Heavy" Option)
An impure pipe is configured by setting `pure: false` in the decorator.
* **Behavior:** An impure pipe runs during **every single change detection cycle**, regardless of whether the input changed.
* **Use Case:** Used when you need to react to internal changes within an object or array (e.s., filtering a list where items are added/removed without changing the array reference).
* **Danger:** Because they run constantly, impure pipes can cause massive performance degradation if they contain complex logic.

### Comparison Summary

| Feature | Pure Pipe (`pure: true`) | Impure Pipe (`pure: false`) |
| :--- | :--- | :--- |
| **Execution Frequency** | Only when input reference/value changes. | On every change detection cycle. |
| **Performance** | High (Optimized). | Low (Expensive). |
| **Array/Object Handling** | Does not detect internal mutations. | Detects internal mutations. |
| **Best Practice** | **Default choice.** | Use only when absolutely necessary. |

**When to use a Pipe vs. Component Logic:**
*   **Use a Pipe** if the transformation is purely **visual** (e.g., formatting a date, rounding a number).
*   **Use Component Logic** if the transformation involves **business rules** (e.g., calculating a user's total tax based on their location).

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/pipes

---

---

# Angular Forms

Angular provides two distinct approaches to handling user input: **Template-Driven Forms** and **Reactive Forms**. 

Modern Angular applications (and specifically this project's guidelines) heavily mandate the use of **Reactive Forms**. Reactive forms are more robust: they provide direct, programmatic access to the underlying form's object model in the TypeScript class, making them highly scalable, reusable, and testable.

## Reactive Forms Architecture
To use Reactive Forms, you must import the `ReactiveFormsModule` into your component.

The architecture relies on two core building blocks:
1. `FormControl`: Tracks the value and validation status of an individual form control (like a single text input).
2. `FormGroup`: Tracks the same values and status for a collection of form controls, acting as a single unit (like the entire `<form>` element).

### Implementation Context: Reactive Form Binding
*(Extracted from `@angular3-demo/src/app/register/register`)*

This implementation demonstrates how to build the model in TypeScript and bind it directly to the HTML template using `[formGroup]` and `formControlName`.

**1. The Component (Logic Layer)**
```typescript
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule],
  templateUrl: './register.html'
})
export class Register {
  // A FormGroup tracks the value and state of the entire form as a single unit.
  // Each FormControl inside holds the value for one specific input field (initialized to '').
  registerForm = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  onSubmit(): void {
    // Access the current values directly from the Reactive model
    const username = this.registerForm.value.username;
    const password = this.registerForm.value.password;
    console.log(`Submitting: ${username}`);
  }
}
```

**2. The Template (View Layer)**
```html
<!-- 
  [formGroup]="registerForm" connects this <form> element to the FormGroup instance in the class.
  (ngSubmit)="onSubmit()" prevents the default browser reload and executes our logic. 
-->
<form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
  <div>
    <label for="username">Username</label>
    <!-- 
      formControlName="username" strictly binds this input to the 'username' FormControl
      inside the parent FormGroup. Angular syncs the values automatically. 
    -->
    <input id="username" type="text" formControlName="username" />
  </div>

  <div>
    <label for="password">Password</label>
    <input id="password" type="password" formControlName="password" />
  </div>

  <button type="submit">Register</button>
</form>
```

> [!NOTE] 
> Verified against official documentation: https://angular.dev/guide/forms/reactive-forms (The `ReactiveFormsModule`, `FormGroup`, `FormControl`, and `formControlName` directive are the official, required primitives for establishing model-driven reactive forms).

