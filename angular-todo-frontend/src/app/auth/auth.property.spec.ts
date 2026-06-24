import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import * as fc from 'fast-check';
import { AuthService } from './auth';

/**
 * Feature: client01-api-auth-integration, Property 1: Token storage round-trip
 *
 * For any non-empty string value stored via a successful login (HTTP 200 with
 * Authorization: Bearer <value>), constructing a new AuthService instance SHALL
 * read the same value from localStorage and initialize the token signal with it.
 *
 * Validates: Requirements 1.5, 1.9
 */
describe('AuthService Property Tests', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('Property 1: token storage round-trip', () => {
    fc.assert(
      fc.property(fc.string({ minLength: 1 }).filter(s => s.trim().length > 0), (token) => {
        localStorage.clear();

        // Set up TestBed for login
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
          providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: Router, useValue: { navigate: vi.fn() } },
          ],
        });

        const service = TestBed.inject(AuthService);
        const httpMock = TestBed.inject(HttpTestingController);

        // Perform login — this should store the token
        service.login('user', 'pass').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
        req.flush(null, {
          status: 200,
          statusText: 'OK',
          headers: { Authorization: `Bearer ${token}` },
        });

        // Verify token was stored in localStorage
        expect(localStorage.getItem('auth_token')).toBe(token);

        // Now create a new AuthService instance to verify localStorage persistence
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
          providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: Router, useValue: { navigate: vi.fn() } },
          ],
        });

        const newService = TestBed.inject(AuthService);
        expect(newService.token()).toBe(token);
      }),
      { numRuns: 100 }
    );
  });
});
