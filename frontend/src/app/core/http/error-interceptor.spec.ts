import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { throwError } from 'rxjs';
import { vi } from 'vitest';

import { Auth } from '../auth/auth';
import { errorInterceptor } from './error-interceptor';

describe('errorInterceptor', () => {
  function setup(auth: Partial<Auth>) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: auth }]
    });
    return { router: TestBed.inject(Router) };
  }

  function run(errorBody: unknown, status: number) {
    const req = new HttpRequest('GET', '/api/v1/me/home');
    const httpError = new HttpErrorResponse({ error: errorBody, status, url: req.url });
    const next = () => throwError(() => httpError);
    return TestBed.runInInjectionContext(() => errorInterceptor(req, next));
  }

  it('logs out and redirects organizers to /login on UNAUTHENTICATED', () => {
    const logout = vi.fn();
    const { router } = setup({ isOrganizer: signal(true), logout });
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    run({ error: { code: 'UNAUTHENTICATED', message: 'Authentication is required' } }, 401).subscribe({
      error: () => undefined
    });

    expect(logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith('/login');
  });

  it('logs out and redirects guests to /link-expired on UNAUTHENTICATED, never /login', () => {
    const logout = vi.fn();
    const { router } = setup({ isOrganizer: signal(false), logout });
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    run({ error: { code: 'UNAUTHENTICATED', message: 'Authentication is required' } }, 401).subscribe({
      error: () => undefined
    });

    expect(logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith('/link-expired');
  });

  it('leaves non-UNAUTHENTICATED 401s alone (e.g. wrong organizer password)', () => {
    const logout = vi.fn();
    const { router } = setup({ isOrganizer: signal(false), logout });
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    run({ error: { code: 'INVALID_CREDENTIALS', message: 'Bad password' } }, 401).subscribe({ error: () => undefined });

    expect(logout).not.toHaveBeenCalled();
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
