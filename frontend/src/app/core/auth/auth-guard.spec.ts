import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { convertToParamMap, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import type { ActivatedRouteSnapshot, UrlTree } from '@angular/router';

import { authGuard } from './auth-guard';
import { Auth } from './auth';

describe('authGuard', () => {
  function runGuard(authenticated: boolean, queryParams: Record<string, string> = {}) {
    const auth: Partial<Auth> = { isAuthenticated: signal(authenticated) };
    const route = { queryParamMap: convertToParamMap(queryParams) } as ActivatedRouteSnapshot;
    const state = { url: '/home' } as RouterStateSnapshot;

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: auth }]
    });
    return TestBed.runInInjectionContext(() => authGuard(route, state));
  }

  it('allows an authenticated visitor', () => {
    expect(runGuard(true)).toBe(true);
  });

  it('allows an unauthenticated visitor carrying a ?token= (the access-link exchange itself)', () => {
    expect(runGuard(false, { token: 'link-token' })).toBe(true);
  });

  it('redirects an unauthenticated visitor with no token to / with a returnUrl', () => {
    const result = runGuard(false) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/?returnUrl=%2Fhome');
  });
});
