import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError, Subject } from 'rxjs';
import { Register } from './register';
import { AuthService } from '../auth';

describe('RegisterComponent', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let mockAuthService: { register: ReturnType<typeof vi.fn>; token: any; isAuthenticated: any };
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      register: vi.fn(),
      token: vi.fn(() => null),
      isAuthenticated: vi.fn(() => false),
    };

    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should navigate to /login on successful registration', () => {
    mockAuthService.register.mockReturnValue(of('User registered successfully'));

    component.registerForm.controls.username.setValue('testuser');
    component.registerForm.controls.password.setValue('testpass');
    component.onSubmit();

    expect(mockAuthService.register).toHaveBeenCalledWith('testuser', 'testpass');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should display "Username is already taken" when HTTP 409 error occurs', () => {
    const errorResponse = new HttpErrorResponse({
      status: 409,
      statusText: 'Conflict',
    });
    mockAuthService.register.mockReturnValue(throwError(() => errorResponse));

    component.registerForm.controls.username.setValue('existinguser');
    component.registerForm.controls.password.setValue('password123');
    component.onSubmit();
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('[data-testid="error-message"]');
    expect(errorEl).not.toBeNull();
    expect(errorEl.textContent).toContain('Username is already taken');
  });

  it('should not call register when form fields are blank', () => {
    component.onSubmit();

    expect(mockAuthService.register).not.toHaveBeenCalled();
    expect(component.registerForm.controls.username.touched).toBe(true);
    expect(component.registerForm.controls.password.touched).toBe(true);
  });

  it('should disable submit button while loading', () => {
    const subject = new Subject<string>();
    mockAuthService.register.mockReturnValue(subject.asObservable());

    component.registerForm.controls.username.setValue('testuser');
    component.registerForm.controls.password.setValue('testpass');
    component.onSubmit();
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    subject.next('success');
    subject.complete();
    fixture.detectChanges();

    expect(button.disabled).toBe(false);
  });
});
