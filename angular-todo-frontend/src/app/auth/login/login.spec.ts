import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError, Subject } from 'rxjs';
import { Login } from './login';
import { AuthService } from '../auth';

describe('LoginComponent', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let mockAuthService: { login: ReturnType<typeof vi.fn>; token: any; isAuthenticated: any };
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      login: vi.fn(),
      token: vi.fn(() => null),
      isAuthenticated: vi.fn(() => false),
    };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should navigate to /dashboard on successful login', () => {
    mockAuthService.login.mockReturnValue(of(undefined));

    component.loginForm.controls.username.setValue('testuser');
    component.loginForm.controls.password.setValue('testpass');
    component.onSubmit();

    expect(mockAuthService.login).toHaveBeenCalledWith('testuser', 'testpass');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should display "Invalid credentials" when login returns 401', () => {
    const errorResponse = new HttpErrorResponse({
      status: 401,
      statusText: 'Unauthorized',
    });
    mockAuthService.login.mockReturnValue(throwError(() => errorResponse));

    component.loginForm.controls.username.setValue('testuser');
    component.loginForm.controls.password.setValue('wrongpass');
    component.onSubmit();
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('[data-testid="error-message"]');
    expect(errorEl).toBeTruthy();
    expect(errorEl.textContent).toContain('Invalid credentials');
  });

  it('should not call login when form fields are blank', () => {
    component.onSubmit();

    expect(mockAuthService.login).not.toHaveBeenCalled();
    expect(component.loginForm.controls.username.touched).toBe(true);
    expect(component.loginForm.controls.password.touched).toBe(true);
  });

  it('should disable submit button while loading', () => {
    const loginSubject = new Subject<void>();
    mockAuthService.login.mockReturnValue(loginSubject.asObservable());

    component.loginForm.controls.username.setValue('testuser');
    component.loginForm.controls.password.setValue('testpass');
    component.onSubmit();
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    loginSubject.next(undefined);
    loginSubject.complete();
    fixture.detectChanges();

    expect(button.disabled).toBe(false);
  });
});
