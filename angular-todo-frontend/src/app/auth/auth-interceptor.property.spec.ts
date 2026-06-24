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
import { describe, it, expect, vi } from 'vitest';
import * as fc from 'fast-check';
import { AuthService } from './auth';
import { authInterceptor } from './auth-interceptor';

/**
 * Feature: client01-api-auth-integration, Property 3: Interceptor conditional header attachment
 *
 * Validates: Requirements 2.2, 2.3, 2.6
 *
 * For any HTTP request and any token state, the interceptor SHALL attach an
 * Authorization: Bearer <token> header if and only if the token is a non-null,
 * non-empty string AND the request URL contains /api/. For all other combinations
 * (null/empty token OR non-API URL), the request SHALL be forwarded without modification.
 */
describe('authInterceptor Property Tests', () => {
  it('Property 3: Authorization header attached iff token is non-null/non-empty AND URL contains /api/', () => {
    const tokenArb = fc.oneof(
      fc.constant(null as string | null),
      fc.constant(''),
      fc.constant('   '),
      fc.string({ minLength: 1 }).filter((s) => s.trim().length > 0)
    );

    const urlArb = fc.oneof(
      fc.constant('http://localhost:8080/api/todos'),
      fc.constant('http://localhost:8080/api/auth/login'),
      fc.constant('http://localhost:8080/api/users/me'),
      fc.constant('http://localhost:8080/other/path'),
      fc.constant('http://example.com/no-api-here'),
      fc.constant('http://localhost:8080/health')
    );

    fc.assert(
      fc.property(tokenArb, urlArb, (token, url) => {
        const mockAuthService = {
          token: signal<string | null>(token),
          logout: vi.fn(),
        };

        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
          providers: [
            provideHttpClient(withInterceptors([authInterceptor])),
            provideHttpClientTesting(),
            { provide: AuthService, useValue: mockAuthService },
          ],
        });

        const httpClient = TestBed.inject(HttpClient);
        const httpMock = TestBed.inject(HttpTestingController);

        httpClient.get(url).subscribe({ error: () => {} });

        const req = httpMock.expectOne(url);
        const shouldHaveAuth =
          token !== null &&
          token.trim().length > 0 &&
          url.includes('/api/');

        if (shouldHaveAuth) {
          expect(req.request.headers.has('Authorization')).toBe(true);
          expect(req.request.headers.get('Authorization')).toBe(
            `Bearer ${token}`
          );
        } else {
          expect(req.request.headers.has('Authorization')).toBe(false);
        }

        req.flush({});
        httpMock.verify();
      }),
      { numRuns: 100 }
    );
  });
});
