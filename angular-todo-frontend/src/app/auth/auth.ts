import { Injectable, inject, signal, computed, Signal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';

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
