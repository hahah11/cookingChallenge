import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin-guard';
import { Auth } from './core/auth/auth';
import { authGuard } from './core/auth/auth-guard';
import { organizerGuard } from './core/auth/organizer-guard';

/**
 * The literal `''` visit (bare domain). Unauthenticated visitors go to
 * `/login` — sending them to `/home` instead would bounce straight back
 * here via `authGuard`'s own redirect-to-`/` fallback, looping forever.
 */
function rootRedirect(): string {
  const auth = inject(Auth);
  if (!auth.isAuthenticated()) {
    return '/login';
  }
  return auth.isOrganizer() ? '/challenges' : '/home';
}

export const routes: Routes = [
  {
    path: 'login',
    title: 'Organizer log in',
    loadComponent: () => import('./features/auth/organizer-login/organizer-login').then((m) => m.OrganizerLogin)
  },
  {
    path: 'register',
    title: 'Register',
    loadComponent: () =>
      import('./features/register/public-registration/public-registration').then((m) => m.PublicRegistration)
  },
  {
    path: 'link-expired',
    title: 'Link expired',
    loadComponent: () => import('./features/auth/link-expired/link-expired').then((m) => m.LinkExpired)
  },
  { path: '', pathMatch: 'full', redirectTo: rootRedirect },
  {
    path: '',
    loadComponent: () => import('./layout/organizer-shell/organizer-shell').then((m) => m.OrganizerShell),
    children: [
      {
        path: 'challenges',
        title: 'Challenges',
        canActivate: [organizerGuard],
        loadComponent: () =>
          import('./features/challenges/challenge-history/challenge-history').then((m) => m.ChallengeHistory)
      },
      {
        path: 'challenges/:id',
        title: 'Challenge',
        canActivate: [organizerGuard],
        loadComponent: () =>
          import('./features/challenges/challenge-detail/challenge-detail').then((m) => m.ChallengeDetail)
      },
      {
        path: 'accounts',
        title: 'Accounts',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/accounts/accounts-admin/accounts-admin').then((m) => m.AccountsAdmin)
      },
      {
        path: 'rivalries',
        title: 'Rivalries',
        canActivate: [organizerGuard],
        loadComponent: () => import('./features/rivalries/rivalry-list/rivalry-list').then((m) => m.RivalryList)
      },
      {
        path: 'rivalries/:cookA/:cookB',
        title: 'Rivalry',
        canActivate: [organizerGuard],
        loadComponent: () => import('./features/rivalries/rivalry-detail/rivalry-detail').then((m) => m.RivalryDetail)
      }
    ]
  },
  {
    path: '',
    loadComponent: () => import('./layout/participant-shell/participant-shell').then((m) => m.ParticipantShell),
    children: [
      {
        path: 'home',
        title: 'Home',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/home/participant-home/participant-home').then((m) => m.ParticipantHome)
      },
      {
        path: 'challenges/:id/score',
        title: 'Score',
        canActivate: [authGuard],
        loadComponent: () => import('./features/challenges/blind-scoring/blind-scoring').then((m) => m.BlindScoring)
      },
      {
        path: 'challenges/:id/results',
        title: 'Results',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/challenges/challenge-results/challenge-results').then((m) => m.ChallengeResults)
      }
    ]
  },
  {
    path: '**',
    title: 'Not found',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound)
  }
];
