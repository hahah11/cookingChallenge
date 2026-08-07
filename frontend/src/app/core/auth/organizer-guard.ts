import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Auth } from './auth';

export const organizerGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  if (auth.isAuthenticated() && auth.isOrganizer()) {
    return true;
  }

  const router = inject(Router);
  const redirectTo = auth.isAuthenticated() ? '/home' : '/login';
  return router.createUrlTree([redirectTo], { queryParams: { returnUrl: state.url } });
};
