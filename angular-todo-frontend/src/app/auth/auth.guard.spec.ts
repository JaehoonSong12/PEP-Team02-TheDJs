import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthService } from './auth';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let mockAuthService: { isAuthenticated: ReturnType<typeof signal<boolean>> };
  let router: Router;

  beforeEach(() => {
    mockAuthService = {
      isAuthenticated: signal(false),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('should return true when isAuthenticated returns true', () => {
    mockAuthService.isAuthenticated.set(true);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBe(true);
  });

  it('should return UrlTree to /login when isAuthenticated returns false', () => {
    mockAuthService.isAuthenticated.set(false);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toEqual(router.createUrlTree(['/login']));
  });
});
