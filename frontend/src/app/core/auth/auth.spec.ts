import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthApi, Locale } from '../api/generated';
import { Auth } from './auth';

const meta = { requestId: 'r', timestamp: '2026-01-01T00:00:00Z' };
const token = { accessToken: 'header.payload.signature', expiresAt: '2026-01-02T00:00:00Z' };

describe('Auth locale sync', () => {
  function setup(updateMyLocale = vi.fn().mockReturnValue(of(undefined))) {
    const authApi = {
      login: vi.fn().mockReturnValue(of({ data: token, meta })),
      accessLinkLogin: vi.fn().mockReturnValue(of({ data: token, meta })),
      updateMyLocale,
    };
    TestBed.configureTestingModule({ providers: [{ provide: AuthApi, useValue: authApi }] });
    return { auth: TestBed.inject(Auth), authApi };
  }

  function browserLanguages(languages: string[]) {
    return vi.spyOn(navigator, 'languages', 'get').mockReturnValue(languages);
  }

  afterEach(() => {
    vi.restoreAllMocks();
    sessionStorage.clear();
  });

  it("tells the backend the browser's language after a password login", () => {
    browserLanguages(['de-AT', 'en']);
    const { auth, authApi } = setup();

    auth.login({ email: 'a@b.com', password: 'secret123' }).subscribe();

    expect(authApi.updateMyLocale).toHaveBeenCalledWith({ locale: Locale.DE });
  });

  it("tells the backend the browser's language after an access-link login", () => {
    browserLanguages(['de']);
    const { auth, authApi } = setup();

    auth.accessLinkLogin({ token: 'link-token' }).subscribe();

    expect(authApi.updateMyLocale).toHaveBeenCalledWith({ locale: Locale.DE });
  });

  it('sends English for a browser language that is not supported', () => {
    browserLanguages(['fr-FR']);
    const { auth, authApi } = setup();

    auth.login({ email: 'a@b.com', password: 'secret123' }).subscribe();

    expect(authApi.updateMyLocale).toHaveBeenCalledWith({ locale: Locale.EN });
  });

  it('stores the token before syncing, so the request is authenticated', () => {
    const order: string[] = [];
    const { auth } = setup(
      vi.fn().mockImplementation(() => {
        order.push(`sync:${sessionStorage.getItem('cookoff.accessToken')}`);
        return of(undefined);
      }),
    );

    auth.login({ email: 'a@b.com', password: 'secret123' }).subscribe();

    expect(order).toEqual(['sync:header.payload.signature']);
  });

  it('still completes the login when the sync request fails', () => {
    const { auth } = setup(vi.fn().mockReturnValue(throwError(() => new Error('offline'))));
    const next = vi.fn();
    const error = vi.fn();

    auth.login({ email: 'a@b.com', password: 'secret123' }).subscribe({ next, error });

    expect(next).toHaveBeenCalledWith(token);
    expect(error).not.toHaveBeenCalled();
    expect(auth.accessToken()).toBe(token.accessToken);
  });
});
