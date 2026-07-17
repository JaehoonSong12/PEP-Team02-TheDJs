import { Injectable, inject, signal, computed, Signal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';

/**
 * Handles user authentication (login, register, logout).
 *
 * All HTTP calls use relative paths (`/api/auth/login`, `/api/auth/register`).
 * The compiled JS bundle is identical across all environments. Environment-
 * specific routing is handled by the `authInterceptor`, which reads the
 * runtime `APP_CONFIG` token (loaded from `/config.json` at bootstrap):
 * - Local Docker: `apiUrl` is empty, paths stay relative, Nginx proxies
 *   `/api/*` to `backend:8080` (same origin, no CORS).
 * - Production S3: `apiUrl` is `http://<EC2-IP>:8080`, the interceptor
 *   prepends it to every `/api/*` call, making cross-origin requests to
 *   EC2. Spring Boot CORS policy authorizes the S3 origin.
 */
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

  login(username: string, password: string): Observable<void> {
    return this.http
      .post('/api/auth/login', { username, password }, { observe: 'response' })
      .pipe(
        map((response: HttpResponse<Object>) => {
          const authHeader = response.headers.get('Authorization');
          if (authHeader && authHeader.startsWith('Bearer ')) {
            const token = authHeader.substring(7);
            this._token.set(token);
            localStorage.setItem('auth_token', token);
          }
        })
      );
  }

  register(username: string, password: string): Observable<string> {
    return this.http.post('/api/auth/register', { username, password }, { responseType: 'text' });
  }

  logout(): void {
    this._token.set(null);
    localStorage.removeItem('auth_token');
    this.router.navigate(['/login']);
  }
}
