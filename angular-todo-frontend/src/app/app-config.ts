import { InjectionToken } from '@angular/core';

/**
 * Runtime application configuration loaded from `/config.json` at bootstrap.
 *
 * This interface defines the shape of the configuration file that controls
 * environment-specific behavior without requiring a rebuild. The file is
 * fetched by `provideAppInitializer` in `app.config.ts` before Angular
 * renders any component.
 *
 * Local Docker: `config.json` ships with `apiUrl: ""` (empty string).
 * The empty value causes all `/api/*` requests to use relative paths,
 * which Nginx proxies to `backend:8080`.
 *
 * Production S3: Jenkins overwrites `config.json` with the EC2 Elastic IP
 * (e.g., `"apiUrl": "http://54.81.54.237:8080"`) before uploading the
 * build to S3. The auth interceptor prepends this base URL to every
 * `/api/*` request, sending it cross-origin to EC2 where Spring Boot
 * CORS policy (`CORS_ALLOWED_ORIGINS`) authorizes the S3 origin.
 */
export interface AppConfig {
  /** Base URL for backend API calls. Empty string for relative paths. */
  apiUrl: string;
}

/**
 * DI token for injecting the runtime configuration into services and
 * interceptors. Provided in `app.config.ts` after `config.json` is fetched.
 */
export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
