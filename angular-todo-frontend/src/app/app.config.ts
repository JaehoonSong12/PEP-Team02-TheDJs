import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, HttpBackend, HttpRequest, HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './auth/auth-interceptor';
import { APP_CONFIG, AppConfig } from './app-config';

/**
 * [Runtime Config Bootstrap]
 *
 * The `provideAppInitializer` callback runs before Angular renders any
 * component. It fetches `/config.json` via `HttpBackend` (not `HttpClient`)
 * so the request bypasses the interceptor chain entirely. This is necessary
 * because the `authInterceptor` depends on `AuthService`, which may not be
 * ready during bootstrap. The loaded config is then provided via the
 * `APP_CONFIG` injection token for use in interceptors and services.
 *
 * If the fetch fails (e.g., file missing during `ng serve`), the fallback
 * is an empty `apiUrl`, preserving the existing relative-path behavior.
 */

// [Mutable config holder]
//
// Populated by the app initializer before Angular renders. The `useFactory`
// provider below captures this reference, so every injection of APP_CONFIG
// returns the loaded values. This pattern avoids a circular dependency
// between the config service and the HTTP client.
let loadedConfig: AppConfig = { apiUrl: '' };

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),

    // [App initializer]
    //
    // Blocks rendering until config.json is fetched. Uses HttpBackend
    // directly (via a fresh HttpClient) to avoid the authInterceptor
    // (which would try to attach a JWT token to a config file request).
    provideAppInitializer(() => {
      const httpBackend = inject(HttpBackend);
      // Create a raw client without interceptors
      const http = new HttpClient(httpBackend);
      return firstValueFrom(http.get<AppConfig>('/config.json'))
        .then(config => {
          if (config) {
            loadedConfig = config;
          }
        })
        .catch(() => {
          // Fallback: keep empty apiUrl (relative paths for local dev)
        });
    }),

    // [APP_CONFIG provider]
    //
    // The factory runs after the initializer completes, so `loadedConfig`
    // is guaranteed to hold the fetched values (or the fallback).
    { provide: APP_CONFIG, useFactory: () => loadedConfig }
  ]
};
