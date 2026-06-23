<!-- SUPPLEMENT: Grouped topics for Core Concepts & Setup -->

# Angular Overview & Setup

## Overview of Angular

Angular is a professional-grade, TypeScript-based framework designed for building complex, scalable **Single Page Applications (SPAs)**. While standard HTML/CSS/JS provides the building blocks of the web, Angular provides the **architectural blueprint** required to manage large-scale applications.

### The SPA Paradigm
In a traditional multi-page website, every time a user clicks a link, the browser requests a brand-new HTML document from the server, causing a "blink" or a full page reload. 

An **Angular SPA** changes this behavior:
*   **Single Load:** The browser loads the core application logic only once.
*   **Dynamic Updates:** When a user navigates, Angular doesn't request a new page; it simply swaps out a piece of the existing page (a Component) and updates the view.
*   **Seamless Experience:** This results in an application that feels as fast and fluid as a desktop or mobile app.

### The Angular Ecosystem: Separation of Concerns
Just as HTML, CSS, and JS have distinct roles, Angular enforces a strict architectural "Separation of Concerns" to ensure code remains maintainable as it grows:

1.  **The Component (Logic & Structure):** The "Brain." A TypeScript class that holds the data and the business logic.
2.  **The Template (View):** The "Skeleton." An HTML file that defines how the data from the Component is displayed to the user.
3.  **The Styles (Presentation):** The "Skin." CSS that defines the visual look of that specific component.
4.  **The Service (Data & Behavior):** The "Nervous System." Shared logic and data that lives outside of components and can be "injected" wherever needed.

**Key Conceptual Takeaways:**
*   **Component-Based:** Everything in Angular is a self-contained, reusable piece (a component).
*   **TypeScript-First:** Angular leverages TypeScript to catch errors during development rather than at runtime.
*   **Declarative UI:** You describe *what* the UI should look like based on the state, and Angular handles the heavy lifting of updating the DOM.

## Angular Setup & Environment

To begin developing with Angular, you must establish a specific development environment consisting of a runtime, a package manager, and a specialized command-line tool.

### The Development Stack

| Tool | Role | Analogy |
| :--- | :--- | :--- |
| **Node.js** | The Runtime Environment | The "Engine" that allows JavaScript to run on your computer. |
| **npm** | The Package Manager | The "Warehouse" where all your external libraries and dependencies are stored. |
| **Angular CLI** | The Command Line Interface | The "Architect" that generates files, builds your app, and runs your server. |

### 1. The Foundation: Node.js & npm
Before touching Angular, your machine requires **Node.js**. Node provides the environment necessary to execute the build tools that Angular uses. Included with Node is **npm (Node Package Manager)**, which is used to install the Angular framework itself and any third-party libraries (like Bootstrap or RxJS) your project might need.

### 2. The Architect: Angular CLI
The **Angular CLI (Command Line Interface)** is the most critical tool in a developer's workflow. Instead of manually creating dozens of files and folders, you use the CLI to "scaffold" (automatically generate) the standard Angular structure.

**Core Installation Command:**
```bash
# Installs the CLI globally on your machine so you can use it anywhere
npm install -g @angular/cli
```

### 3. The Lifecycle of a Project
Starting a new application follows a standardized three-step workflow:

#### Phase A: Creation
You use the CLI to create the initial directory structure and configuration files.
```bash
ng new my-app
```
*This command sets up the TypeScript configuration, the testing environment, and the initial component structure.*

#### Phase B: Development
Once the project is created, you launch a local development server.
```bash
ng serve
```
*This command compiles your code in real-time. Every time you save a file, the CLI detects the change and updates your browser instantly (Live Reloading).*

#### Phase C: Production
When the application is ready for the real world, you transform the code into highly optimized, minified files.
```bash
ng build
```
*This command takes your TypeScript and complex components and "distills" them into plain HTML, CSS, and JavaScript that any web server can host.*

**Summary of Workflow**
| Step | Command | Result |
| :--- | :--- | :--- |
| **Initialize** | `ng new` | A fresh, structured project folder. |
| **Develop** | `ng serve` | A local, live-reloading preview of your app. |
| **Deploy** | `ng build` | A "dist" folder ready to be uploaded to a server. |

### Implementation Context: Package Dependencies & Scripts

The `package.json` file dictates the required dependencies and command-line scripts utilized to manage the application lifecycle. Execution of CLI tools (e.g., `ng serve`) maps directly to these defined scripts. The project relies on explicit configurations of core libraries to function appropriately.

```json
// File: angular1-example/package.json
{
  "scripts": {
    "ng": "ng",
    "start": "ng serve",
    "build": "ng build",
    "watch": "ng build --watch --configuration development",
    "test": "ng test"
  },
  "dependencies": {
    "@angular/common": "^21.2.0",
    "@angular/core": "^21.2.0",
    "rxjs": "~7.8.0"
  }
}
```

---

---

# Angular CLI & Build Pipeline

## The Angular CLI: Your Development Engine

The **Angular CLI (Command Line Interface)** is more than just a shortcut for commands; it is a sophisticated automation engine. It enforces the "Angular Way" by ensuring that every developer on a team follows the same file structure, naming conventions, and architectural patterns.

### The Concept of Scaffolding
In traditional development, creating a new feature might involve manually creating a `.ts` file, a `.html` file, a `.css` file, and a test file, and then manually registering them in a module. 

Angular uses **Scaffolding**. When you run a "generate" command, the CLI doesn't just create files; it performs "Context-Aware Injection." It knows where the new files should live and automatically updates the surrounding architecture to include them.

### Core Command Reference

The CLI commands can be categorized by their role in the development lifecycle.

#### 1. Project Management
Commands used to manage the existence and state of the application itself.

| Command | Purpose | Use Case |
| :--- | :--- | :--- |
| `ng new <name>` | **Initialization** | Create a brand new project from scratch. |
| `ng serve` | **Execution** | Spin up a local development server with live-reloading. |
| `ng build` | **Compilation** | Transform source code into optimized production assets. |
| `ng add <package>` | **Integration** | Installs a library and automatically configures it (e.g., `ng add @angular/material`). |

#### 2. The Generator (`ng generate`)
The most used part of the CLI. It automates the creation of "Blueprints" (boilerplate code).

**Syntax:** `ng generate <type> <name>`  
**Shortform:** `ng g <type> <name>`

| Type | Shortform | Description |
| :--- | :--- | :--- |
| `component` | `c` | A UI building block (HTML, CSS, TS, Spec). |
| `service` | `s` | A class for business logic and data sharing. |
| `directive` | `d` | A tool to change the behavior/appearance of DOM elements. |
| `pipe` | `p` | A tool to transform data within a template. |
| `guard` | `g` | A security layer for protecting routes. |
| `interface` | `intf` | A TypeScript contract for defining data shapes. |

---

## The Build Pipeline: From Source to Browser

A browser cannot natively understand TypeScript, specialized Angular decorators, or advanced SCSS. The **Build Pipeline** is the series of automated transformations that converts your "Developer-Friendly" code into "Browser-Friendly" code.

### The Transformation Process

The pipeline follows a logical flow of "Distillation":

1.  **Transpilation:** Converting TypeScript (`.ts`) into standard JavaScript (`.js`) so the browser can execute the logic.
2.  **Template Compilation:** Converting Angular HTML templates into highly efficient JavaScript instructions that can update the DOM instantly.
3.  **Bundling:** Gathering hundreds of small files and "bundling" them into a few large files. This reduces the number of HTTP requests the browser has to make.
4.  **Minification & Uglification:** Removing all whitespace, comments, and shortening variable names (e.g., changing `isLoggedIn` to `a`) to reduce file size.
5.  **Tree Shaking:** This is a critical optimization step. The builder "shakes" the dependency tree and removes any code that is imported but never actually used, ensuring the user doesn't download "dead weight."

### Implementation Context: TypeScript Compilation

The transpilation phase is governed strictly by the rules delineated in `tsconfig.json`. The configuration ensures adherence to precise ECMAScript standards and strict dependency typing.

```json
// File: angular1-example/tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "isolatedModules": true,
    "experimentalDecorators": true,
    "target": "ES2022"
  },
  "angularCompilerOptions": {
    "strictTemplates": true
  }
}
```

### Builders vs. Bundlers

To understand the modern Angular build process, you must distinguish between the **Task** and the **Engine**.

#### The Builder (The "Project Manager")
In Angular, a **Builder** is a tool defined in your `angular.json` file that tells the CLI *what task* to perform (e.g., `build`, `serve`, `test`). 
*   The Builder is the high-level instruction: *"Hey, I want to build this project for production."*

#### Implementation Context: Angular Builder Configuration

The `angular.json` file specifies the exact routing and setup logic applied by the CLI builders during compilation. It outlines the browser entry points, expected assets, styling constraints, and optimization parameters.

```json
// File: angular1-example/angular.json
{
  "architect": {
    "build": {
      "builder": "@angular/build:application",
      "options": {
        "browser": "src/main.ts",
        "tsConfig": "tsconfig.app.json",
        "assets": [
          {
            "glob": "**/*",
            "input": "public"
          }
        ],
        "styles": [
          "src/styles.css"
        ]
      }
    }
  }
}
```

#### The Bundler (The "Heavy Machinery")
The **Bundler** is the actual engine that does the heavy lifting of combining files. 
*   **Webpack:** The traditional, highly configurable industry standard.
*   **Esbuild / Vite:** The modern, high-speed engines used by newer Angular versions to provide near-instant build times during development.

**Conceptual Summary**

| Feature | Developer Mode (`ng serve`) | Production Mode (`ng build`) |
| :--- | :--- | :--- |
| **Goal** | Speed of development. | Speed of the end-user. |
| **Code Quality** | Unminified (easy to debug). | Minified/Uglified (hard to read, tiny size). |
| **Optimization** | Minimal (focus on fast rebuilds). | Maximum (Tree-shaking, heavy compression). |
| **Output** | Served from memory. | Written to a physical `/dist` folder. |

---

---

# Modules vs. Standalone

## Overview of the Architectural Shift

For many years, the `NgModule` was the mandatory way to organize Angular applications. However, as applications grew in complexity, the "Module" system became a source of friction, making it difficult to track dependencies and increasing the "mental overhead" for developers.

With the introduction of **Standalone Components** (Angular 14/15+), Angular moved toward a more streamlined, lightweight, and intuitive architecture. As of Angular v19, standalone components are the default configuration for all new structures.

---

## The Legacy Model: NgModules

In the traditional model, a component cannot exist in isolation. It must be "declared" as part of an `NgModule`. This module acts as a container that groups related components, directives, and pipes together.

### How NgModules Work
*   **Declarations:** A list of all components/directives/pipes that "belong" to this module.
*   **Imports:** A list of *other* modules that this module needs to function (e.g., `CommonModule`).
*   **Exports:** A list of components that this module makes available to *other* modules.
*   **Providers:** A list of services that should be available within this module's scope.

### The "Complexity Tax" of Modules
While modules provided organization, they introduced several challenges:
1.  **The "Boilerplate" Problem:** Even a tiny component required an entire module file just to exist.
2.  **Dependency Confusion:** It was often unclear where a component's dependency was coming from (Was it in the module? Was it imported from another module?).
3.  **Difficult Tree-Shaking:** Because components were bundled into large modules, it was harder for build tools to remove unused code, leading to larger bundle sizes.

---

## The Modern Model: Standalone Components

**Standalone Components** eliminate the need for `NgModules` by allowing components to manage their own dependencies directly.

### How Standalone Works
A component is marked as a standalone unit by simply existing; in Angular v19+, `standalone: true` is the implicit default. Instead of being "declared" in a module, it "imports" exactly what it needs directly in its `@Component` decorator.

> **Project Context:** The `AGENTS.md` guidelines explicitly **ban** the manual inclusion of `standalone: true` inside `@Component` or `@Directive` decorators. Since it is the default in modern Angular (v20+), explicitly declaring it is considered unnecessary boilerplate.

*   **Self-Contained:** The component is a complete, independent unit.
*   **Explicit Dependencies:** You can look at a single component file and see every tool it uses (e.g., `imports: [CommonModule, MyButtonComponent]`).
*   **Granular Tree-Shaking:** Since dependencies are explicitly linked to the component, the build tool can easily identify and remove unused code.

### Comparison: Code Comparison

**Legacy (NgModule Style):**
```typescript
// user.module.ts
@NgModule({
  declarations: [UserComponent],
  imports: [CommonModule],
  exports: [UserComponent]
})
export class UserModule {}

// user.component.ts
@Component({
  selector: 'app-user',
  template: `<div *ngIf="isActive">...</div>` // Relies on CommonModule via the Module
})
export class UserComponent { ... }
```

**Modern (Standalone Style):**
```typescript
// user.component.ts
@Component({
  selector: 'app-user-profile',
  // standalone: true,              // <--- Banned per AGENTS.md (Implicit default)
  imports: [CommonModule],          // <--- Direct dependency management
  template: `<div *ngIf="isActive">...</div>`
})
export class UserComponent { ... }
```

---

## Summary: Which One to Use?

In the current Angular ecosystem, the decision is largely made for you by the version of Angular you are using and the project's age.

| Feature | NgModules (Legacy) | Standalone (Modern) |
| :--- | :--- | :--- |
| **Organization** | Grouped into "buckets" (Modules). | Individual, independent units. |
| **Dependency Management** | Centralized in the Module file. | Decentralized in the Component file. |
| **Complexity** | High (requires managing declarations/exports). | Low (direct and explicit). |
| **Boilerplate** | Heavy. | Minimal. |
| **Tree-Shaking** | Less efficient. | Highly efficient. |
| **Project Status** | Use for maintaining legacy apps. | **The standard for all new development.** |

**Architectural Rule of Thumb:**
If you are starting a new project, **always use Standalone Components** (which is the default behavior in Angular v19+). If you are working in an existing codebase, follow the existing pattern, but look for opportunities to migrate individual components to Standalone as you refactor.

> [!NOTE]
> Verified against official documentation: https://angular.dev/reference/migrations/standalone

---

---

