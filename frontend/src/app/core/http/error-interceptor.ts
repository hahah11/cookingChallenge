import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
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
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);

  return next(req).pipe(
    catchError((httpError: HttpErrorResponse) => {
      const apiError = toApiError(httpError);

      if (apiError.code === 'UNAUTHENTICATED') {
        const wasOrganizer = auth.isOrganizer();
        auth.logout();
        void router.navigateByUrl(wasOrganizer ? '/login' : '/home');
      }

      return throwError(() => apiError);
    })
  );
};
