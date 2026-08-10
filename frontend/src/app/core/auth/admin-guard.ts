import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { SystemRole } from '../api/generated';
import { Auth } from './auth';

/** `/accounts` is admin-only — stricter than `organizerGuard`, per the frontend plan's Phase 4. */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  if (auth.isAuthenticated() && auth.hasAnyRole(SystemRole.ADMIN)) {
    return true;
  }

  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }

  const redirectTo = auth.isOrganizer() ? '/challenges' : '/home';
  return router.createUrlTree([redirectTo]);
};
