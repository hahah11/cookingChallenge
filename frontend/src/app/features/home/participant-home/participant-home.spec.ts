import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import {
  ChallengesApi,
  ChallengeStatus,
  Config,
  ConfigApi,
  DishLabel,
  GuestHome,
  HomeApi,
  ParticipantChallenge,
  ParticipantChallengeMyCookLabelEnum
} from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { Auth } from '../../../core/auth/auth';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { expectNoAxeViolations } from '../../../testing/axe';
import { ParticipantHome } from './participant-home';

const openChallenge: ParticipantChallenge = {
  id: 'chal-open',
  date: '2026-08-01',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.OPEN,
  labels: [DishLabel.A, DishLabel.B],
  categories: [],
  participantCookAssignments: [
    { label: DishLabel.A, accountId: null, name: null, colorId: null },
    { label: DishLabel.B, accountId: null, name: null, colorId: null }
  ],
  hasImage: false,
  submitted: false,
  mySubmission: null,
  myCookLabel: ParticipantChallengeMyCookLabelEnum.A,
  canScore: false,
  canPickColor: true
};

const pastChallenge: ParticipantChallenge = {
  ...openChallenge,
  id: 'chal-past',
  dishName: 'Curry',
  status: ChallengeStatus.REVEALED,
  canPickColor: false
};

const home: GuestHome = { displayName: 'Felix', open: [openChallenge], past: [] };
const config: Config = {
  availableRoles: [],
  plateColors: [
    { id: 'red', name: 'Red', hexCode: '#c0392b', sortOrder: 0 },
    { id: 'yellow', name: 'Yellow', hexCode: '#e0b400', sortOrder: 1 }
  ],
  featureFlags: {}
};
const meta = { requestId: 'req-1', timestamp: '2026-01-01T00:00:00Z' };

describe('ParticipantHome', () => {
  function setup(options: {
    homeApi?: Record<string, unknown>;
    auth?: Record<string, unknown>;
    challengesApi?: Record<string, unknown>;
    dialog?: Record<string, unknown>;
  }) {
    const notification = { error: vi.fn(), success: vi.fn(), info: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ParticipantHome],
      providers: [
        provideRouter([]),
        { provide: HomeApi, useValue: options.homeApi ?? { getMyHome: () => of({ data: home, meta }) } },
        { provide: Auth, useValue: options.auth ?? {} },
        {
          provide: ChallengesApi,
          useValue: options.challengesApi ?? { getChallengeImage: () => of(new Blob()) }
        },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta }) } },
        AppConfig,
        { provide: MatDialog, useValue: options.dialog ?? {} },
        { provide: Notification, useValue: notification }
      ]
    });
    TestBed.inject(AppConfig).load().subscribe();

    const fixture = TestBed.createComponent(ParticipantHome);
    return { fixture, notification };
  }

  it('exchanges the access-link token before loading home, then strips it from the URL', () => {
    const accessLinkLogin = vi.fn().mockReturnValue(of({ accessToken: 'jwt', expiresAt: '2026-01-02T00:00:00Z' }));
    const getMyHome = vi.fn().mockReturnValue(of({ data: home, meta }));
    const { fixture } = setup({ auth: { accessLinkLogin }, homeApi: { getMyHome } });
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.componentRef.setInput('token', 'link-token');
    fixture.detectChanges();

    expect(accessLinkLogin).toHaveBeenCalledWith({ token: 'link-token' });
    expect(navigateSpy).toHaveBeenCalledWith([], { replaceUrl: true });
    expect(getMyHome).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Hi, Felix');
  });

  it('shows an expired-link message and never loads home when the token is dead', () => {
    const apiError: ApiError = {
      code: 'INVALID_OR_EXPIRED_LINK',
      message: 'Link expired.',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 401
    };
    const accessLinkLogin = vi.fn().mockReturnValue(throwError(() => apiError));
    const getMyHome = vi.fn();
    const { fixture } = setup({ auth: { accessLinkLogin }, homeApi: { getMyHome } });

    fixture.componentRef.setInput('token', 'dead-token');
    fixture.detectChanges();

    expect(getMyHome).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('This link has expired');
  });

  it('loads home directly when there is no token, already-authenticated case', () => {
    const { fixture } = setup({});

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Hi, Felix');
    expect(fixture.nativeElement.querySelector('app-participant-challenge-card')).not.toBeNull();
  });

  it('shows the empty state when nothing is open', () => {
    const emptyHome: GuestHome = { displayName: 'Felix', open: [], past: [] };
    const { fixture } = setup({ homeApi: { getMyHome: () => of({ data: emptyHome, meta }) } });

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-empty-state')).not.toBeNull();
  });

  it('confirms then submits a color pick, and reloads home on success', () => {
    const getMyHome = vi.fn().mockReturnValue(of({ data: home, meta }));
    const pickChallengeColor = vi.fn().mockReturnValue(of({ data: openChallenge, meta }));
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(true) });
    const { fixture } = setup({
      homeApi: { getMyHome },
      challengesApi: { getChallengeImage: () => of(new Blob()), pickChallengeColor },
      dialog: { open: dialogOpen }
    });

    fixture.detectChanges();
    fixture.nativeElement.querySelector('.participant-challenge-card__swatch').click();

    expect(dialogOpen).toHaveBeenCalled();
    expect(pickChallengeColor).toHaveBeenCalledWith('chal-open', { colorId: 'red' });
    expect(getMyHome).toHaveBeenCalledTimes(2);
  });

  it('refreshes without a notification when a color pick 409s (the other cook picked first)', () => {
    const getMyHome = vi.fn().mockReturnValue(of({ data: home, meta }));
    const conflict: ApiError = {
      code: 'CHALLENGE_NOT_OPEN',
      message: 'Already picked.',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 409
    };
    const pickChallengeColor = vi.fn().mockReturnValue(throwError(() => conflict));
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(true) });
    const { fixture, notification } = setup({
      homeApi: { getMyHome },
      challengesApi: { getChallengeImage: () => of(new Blob()), pickChallengeColor },
      dialog: { open: dialogOpen }
    });

    fixture.detectChanges();
    fixture.nativeElement.querySelector('.participant-challenge-card__swatch').click();

    expect(getMyHome).toHaveBeenCalledTimes(2);
    expect(notification.error).not.toHaveBeenCalled();
  });

  it('renders past challenges as a compact row, not a photo card, and navigates to results on click', () => {
    const pastHome: GuestHome = { displayName: 'Felix', open: [], past: [pastChallenge] };
    const { fixture } = setup({ homeApi: { getMyHome: () => of({ data: pastHome, meta }) } });
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.detectChanges();

    const row = fixture.nativeElement.querySelector('.participant-home__past-row');
    expect(row.textContent).toContain('Curry');
    expect(fixture.nativeElement.querySelector('app-participant-challenge-card')).toBeNull();

    row.click();
    expect(navigateSpy).toHaveBeenCalledWith(['/challenges', 'chal-past', 'results']);
  });

  it(
    'has no axe violations',
    async () => {
      const { fixture } = setup({});

      fixture.detectChanges();

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
