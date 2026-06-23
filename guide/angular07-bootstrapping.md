<!-- SUPPLEMENT: Grouped topics for Bootstrapping -->

# Bootstrapping & Application Configuration

## Overview of Bootstrapping

"Bootstrapping" is the formal process of launching the Angular application. It tells the browser which component is the "root" (the absolute top-level parent) and what global configurations and services the application needs to operate.

Historically, this was handled by a massive central file known as `AppModule`. However, in modern Angular architecture (v15+), applications are primarily built using **Standalone Components**, which fundamentally shifts the bootstrapping paradigm.

## The Standalone Bootstrapping Paradigm

Instead of bootstrapping a Module, modern Angular bootstraps a Component directly. This requires two primary files:
1.  **The Application Config:** A file that collects all the global tools (like the Router or the HTTP Client) the application will need.
2.  **The Entry Point:** The programmatic execution file that merges the root component and the application config to ignite the framework.

### Implementation Context: Application Configuration

The `ApplicationConfig` object acts as the central registry for global providers. Rather than importing modules, modern Angular uses `provide*` functions (like `provideRouter`) to equip the application with necessary capabilities.

```typescript
// File: angular1-example/src/app/app.config.ts
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes)
  ]
};
```

### Implementation Context: Application Entry Point

The `main.ts` file acts as the primary ignition switch for the SPA. Similar to how `index.html` is the entry point for the browser, `main.ts` is the entry point for the Angular compiler. It executes the `bootstrapApplication` function, securely launching the specified root component with the established global configuration.

```typescript
// File: angular1-example/src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

/*
  Similiar to how index.html is the webpage entry point, this main.ts is
  the programatic entrypoint. Once again, we don't need to change anything
  that is happening here
*/

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
```

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/components/standalone-components

---

---
