import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth';
import { APP_CONFIG } from '../app-config';
import { catchError, throwError } from 'rxjs';

/**
 * HTTP interceptor that attaches JWT Bearer tokens to outgoing API requests
 * and prepends the runtime API base URL for cross-origin production calls.
 *
 * Responsibilities:
 * 1. Reads the `apiUrl` from the runtime `APP_CONFIG` token (loaded from
 *    `/config.json` at bootstrap). If non-empty, prepends it to any
 *    request whose URL starts with `/api/`, converting a relative path
 *    into an absolute cross-origin URL.
 * 2. Adds `Authorization: Bearer <token>` header to any `/api/` request
 *    (but only if a non-empty token exists in AuthService).
 * 3. Catches 401 responses on protected routes and triggers logout.
 *
 * URL routing by environment:
 * - Local Docker: `config.json` has `apiUrl: ""`. Relative paths stay
 *   relative. Nginx proxies `/api/*` to `backend:8080` (same origin).
 * - Production S3: `config.json` has `apiUrl: "http://<EC2-IP>:8080"`.
 *   This interceptor rewrites `/api/todos` to
 *   `http://<EC2-IP>:8080/api/todos`, a cross-origin request. The
 *   Spring Boot CORS policy (`CORS_ALLOWED_ORIGINS`) authorizes the
 *   S3 website origin.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const config = inject(APP_CONFIG);
  const token = authService.token();

  let request = req;

  // [URL rewrite]
  //
  // When apiUrl is non-empty (production S3), prepend it to relative
  // API paths so the browser sends the request to the EC2 backend
  // instead of the S3 domain.
  if (config.apiUrl && req.url.startsWith('/api/')) {
    request = req.clone({ url: config.apiUrl + req.url });
  }

  // [JWT attachment]
  if (token && token.trim().length > 0 && request.url.includes('/api/')) {
    request = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && req.url.includes('/api/') && !req.url.includes('/api/auth/')) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
