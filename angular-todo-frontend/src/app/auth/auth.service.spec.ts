import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    router = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('constructor', () => {
    it('should read auth_token from localStorage on initialization', () => {
      localStorage.setItem('auth_token', 'stored-jwt-token');

      // Create a new TestBed to test constructor behavior with pre-set localStorage
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: router },
        ],
      });

      const newService = TestBed.inject(AuthService);
      expect(newService.token()).toBe('stored-jwt-token');
    });

    it('should not set token if localStorage auth_token is empty', () => {
      localStorage.setItem('auth_token', '');

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: router },
        ],
      });

      const newService = TestBed.inject(AuthService);
      expect(newService.token()).toBeNull();
    });

    it('should not set token if localStorage auth_token is whitespace only', () => {
      localStorage.setItem('auth_token', '   ');

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: router },
        ],
      });

      const newService = TestBed.inject(AuthService);
      expect(newService.token()).toBeNull();
    });
  });

  describe('login', () => {
    it('should send POST to correct URL with credentials and store token from Authorization header', () => {
      service.login('testuser', 'testpass').subscribe();

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'testuser', password: 'testpass' });

      req.flush(null, {
        status: 200,
        statusText: 'OK',
        headers: { Authorization: 'Bearer my-jwt-token-123' },
      });

      expect(service.token()).toBe('my-jwt-token-123');
      expect(localStorage.getItem('auth_token')).toBe('my-jwt-token-123');
    });

    it('should not store token if Authorization header is missing', () => {
      service.login('testuser', 'testpass').subscribe();

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush(null, { status: 200, statusText: 'OK' });

      expect(service.token()).toBeNull();
      expect(localStorage.getItem('auth_token')).toBeNull();
    });

    it('should not store token if Authorization header does not start with Bearer', () => {
      service.login('testuser', 'testpass').subscribe();

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush(null, {
        status: 200,
        statusText: 'OK',
        headers: { Authorization: 'Basic abc123' },
      });

      expect(service.token()).toBeNull();
      expect(localStorage.getItem('auth_token')).toBeNull();
    });
  });

  describe('logout', () => {
    it('should set token signal to null, remove from localStorage, and navigate to /login', () => {
      // First login to set a token
      service.login('user', 'pass').subscribe();
      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush(null, {
        status: 200,
        statusText: 'OK',
        headers: { Authorization: 'Bearer active-token' },
      });

      expect(service.token()).toBe('active-token');
      expect(localStorage.getItem('auth_token')).toBe('active-token');

      // Now logout
      service.logout();

      expect(service.token()).toBeNull();
      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('register', () => {
    it('should send POST to correct URL and return text response', () => {
      let result: string | undefined;
      service.register('newuser', 'newpass').subscribe((res) => {
        result = res;
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'newuser', password: 'newpass' });
      expect(req.request.responseType).toBe('text');

      req.flush('User registered successfully', { status: 201, statusText: 'Created' });

      expect(result).toBe('User registered successfully');
    });
  });

  describe('isAuthenticated', () => {
    it('should return false when token is null', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should return true when token is set', () => {
      service.login('user', 'pass').subscribe();
      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush(null, {
        status: 200,
        statusText: 'OK',
        headers: { Authorization: 'Bearer some-token' },
      });

      expect(service.isAuthenticated()).toBe(true);
    });
  });
});
