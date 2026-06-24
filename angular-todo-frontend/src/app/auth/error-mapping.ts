import { HttpErrorResponse } from '@angular/common/http';

export function mapHttpError(error: HttpErrorResponse, context: 'login' | 'register'): string {
  if (error.status === 0) {
    return 'Unable to connect to server. Please check your connection.';
  }
  if (error.status === 400) {
    const body = typeof error.error === 'string' ? error.error : '';
    return body.length > 256 ? body.substring(0, 256) : body;
  }
  if (error.status === 401 && context === 'login') {
    return 'Invalid credentials';
  }
  if (error.status === 409 && context === 'register') {
    return 'Username is already taken';
  }
  if (error.status === 500 || error.status === 503) {
    return 'Server error. Please try again later.';
  }
  return context === 'login'
    ? 'An unexpected error occurred. Please try again.'
    : 'Registration could not be completed. Please try again.';
}
