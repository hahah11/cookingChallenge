import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { catchError, throwError } from 'rxjs';

import { Auth } from '../auth/auth';
import { toApiError } from '../errors/api-error';

/**
 * Maps the `{error:{code,message,details,requestId,timestamp}}` envelope to a typed `ApiError`
 * and rethrows it, so callers can `catchError` on `.code` instead of parsing raw HTTP bodies.
 *
 * The backend generates a fresh RSA keypair on every restart, so every token dies with the
 * server — an `UNAUTHENTICATED` response always means the session is dead, never just this one
 * request, so it's handled globally here rather than per-feature.
 *
 * `ApiError.message` is translated here from the stable `code`, so every caller that shows
 * `error.message` shows the user's language without knowing about i18n.
 *
 * Organizers always re-authenticate with a password, so `/login` is a real next step for them.
 * Guests only ever get in via a QR code or an emailed access link — they have no credentials to
 * log in with, so sending them to `/login` is a dead end. They go to `/link-expired` instead.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const transloco = inject(TranslocoService);

  return next(req).pipe(
    catchError((httpError: HttpErrorResponse) => {
      const apiError = toApiError(httpError, (key) => transloco.translate(key));
      if (apiError.code === 'UNAUTHENTICATED' || httpError.status === 401) {
        const wasOrganizer = auth.isOrganizer();
        auth.logout();
        void router.navigateByUrl(wasOrganizer ? '/login' : '/link-expired?kind=link');
      }

      return throwError(() => apiError);
    })
  );
};
