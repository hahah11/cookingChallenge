import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { AccessLinkLoginRequest, AuthApi, AuthToken, LoginRequest, SystemRole } from '../api/generated';
import { detectApiLocale } from '../i18n/api-locale';
import { JwtClaims, decodeJwtClaims, isJwtExpired } from './jwt-claims';

const STORAGE_KEY = 'cookoff.accessToken';

/**
 * Both login paths (`login`, `accessLinkLogin`) return the same `{ accessToken, expiresAt }`.
 * There is no `/me` endpoint, so `sub`/`roles` come from decoding the JWT payload client-side.
 */
@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly authApi = inject(AuthApi);

  private readonly token = signal<string | null>(sessionStorage.getItem(STORAGE_KEY));

  readonly accessToken = this.token.asReadonly();

  readonly claims = computed<JwtClaims | null>(() => {
    const token = this.token();
    return token ? decodeJwtClaims(token) : null;
  });

  readonly accountId = computed(() => this.claims()?.sub ?? null);
  readonly roles = computed(() => this.claims()?.roles ?? []);

  readonly isAuthenticated = computed(() => {
    const claims = this.claims();
    return claims !== null && !isJwtExpired(claims);
  });

  readonly isOrganizer = computed(() => this.hasAnyRole(SystemRole.ORGANIZER, SystemRole.ADMIN));

  hasAnyRole(...roles: SystemRole[]): boolean {
    const accountRoles = this.roles();
    return roles.some((role) => accountRoles.includes(role));
  }

  login(request: LoginRequest): Observable<AuthToken> {
    return this.authApi.login(request).pipe(
      map((response) => response.data),
      tap((token) => this.storeToken(token.accessToken)),
      tap(() => this.syncLocale())
    );
  }

  accessLinkLogin(request: AccessLinkLoginRequest): Observable<AuthToken> {
    return this.authApi.accessLinkLogin(request).pipe(
      map((response) => response.data),
      tap((token) => this.storeToken(token.accessToken)),
      tap(() => this.syncLocale())
    );
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.token.set(null);
  }

  /**
   * Tells the backend which language this browser prefers, so emails to this account (invitations,
   * results, password resets — all sent by someone else) come in that language. Login returns only
   * a token, so there is no stored value to compare against: it is simply sent every time.
   * Best effort — a failure must never disturb the login itself.
   */
  private syncLocale(): void {
    this.authApi.updateMyLocale({ locale: detectApiLocale() }).subscribe({ error: () => undefined });
  }

  private storeToken(accessToken: string): void {
    sessionStorage.setItem(STORAGE_KEY, accessToken);
    this.token.set(accessToken);
  }
}
