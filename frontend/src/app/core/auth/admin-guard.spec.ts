import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { adminGuard } from './admin-guard';
import { Auth } from './auth';

describe('adminGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/accounts' } as RouterStateSnapshot;

  function runGuard(options: { authenticated: boolean; organizer: boolean; admin: boolean }) {
    const auth: Partial<Auth> = {
      isAuthenticated: signal(options.authenticated),
      isOrganizer: signal(options.organizer),
      hasAnyRole: () => options.admin
    };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: auth }]
    });
    return TestBed.runInInjectionContext(() => adminGuard(route, state));
  }

  it('allows an authenticated admin', () => {
    const result = runGuard({ authenticated: true, organizer: false, admin: true });
    expect(result).toBe(true);
  });

  it('redirects an unauthenticated visitor to /login with a returnUrl', () => {
    const result = runGuard({ authenticated: false, organizer: false, admin: false }) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login?returnUrl=%2Faccounts');
  });

  it('redirects an authenticated non-admin organizer to /challenges', () => {
    const result = runGuard({ authenticated: true, organizer: true, admin: false }) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/challenges');
  });

  it('redirects an authenticated non-admin non-organizer to /home', () => {
    const result = runGuard({ authenticated: true, organizer: false, admin: false }) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/home');
  });
});
