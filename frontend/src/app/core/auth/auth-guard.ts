import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Auth } from './auth';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(Auth);
  if (auth.isAuthenticated()) {
    return true;
  }

  // `/home?token=` must reach ParticipantHome unauthenticated — it's the access-link
  // exchange itself, see the frontend plan's Phase 6. The backend still gates every
  // subsequent call on the JWT the exchange produces.
  if (route.queryParamMap.has('token')) {
    return true;
  }

  const router = inject(Router);
  return router.createUrlTree(['/'], { queryParams: { returnUrl: state.url } });
};
