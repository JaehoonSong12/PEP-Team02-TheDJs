import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
  withInterceptors,
  HttpClient,
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { AuthService } from './auth';
import { authInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let mockAuthService: {
    token: ReturnType<typeof signal<string | null>>;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    mockAuthService = {
      token: signal<string | null>(null),
      logout: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach Authorization header when token is non-null and URL contains /api/', () => {
    mockAuthService.token.set('my-jwt-token');

    httpClient.get('http://localhost:8080/api/todos').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/todos');
    expect(req.request.headers.get('Authorization')).toBe(
      'Bearer my-jwt-token'
    );
    req.flush([]);
  });

  it('should forward request unchanged when token is null', () => {
    mockAuthService.token.set(null);

    httpClient.get('http://localhost:8080/api/todos').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/todos');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('should forward request unchanged when token is empty string', () => {
    mockAuthService.token.set('   ');

    httpClient.get('http://localhost:8080/api/todos').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/todos');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('should call logout on 401 from non-auth /api/ endpoint', () => {
    mockAuthService.token.set('my-jwt-token');

    httpClient.get('http://localhost:8080/api/todos').subscribe({
      error: () => {
        // expected error
      },
    });

    const req = httpMock.expectOne('http://localhost:8080/api/todos');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(mockAuthService.logout).toHaveBeenCalled();
  });

  it('should NOT call logout on 401 from /api/auth/ endpoint', () => {
    mockAuthService.token.set('my-jwt-token');

    httpClient
      .post('http://localhost:8080/api/auth/login', {
        username: 'u',
        password: 'p',
      })
      .subscribe({
        error: () => {
          // expected error
        },
      });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(mockAuthService.logout).not.toHaveBeenCalled();
  });
});
