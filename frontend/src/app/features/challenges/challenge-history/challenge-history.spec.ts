import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Challenge, ChallengeStatus, ChallengesApi, DishLabel } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { expectNoAxeViolations } from '../../../testing/axe';
import { ChallengeHistory } from './challenge-history';

const challenge: Challenge = {
  id: 'chal-1',
  date: '2026-08-01',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.OPEN,
  cookAssignments: [
    { accountId: 'cook-a', name: 'Alice', label: DishLabel.A, colorId: null },
    { accountId: 'cook-b', name: 'Bob', label: DishLabel.B, colorId: null }
  ],
  guestAccountIds: [],
  createdByAccountId: 'organizer-1',
  submittedGuestCount: 0,
  totalGuestCount: 2,
  hasImage: false,
  overallWinnerAccountId: null
};

describe('ChallengeHistory', () => {
  function setup(listChallenges: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [ChallengeHistory],
      providers: [provideRouter([]), { provide: ChallengesApi, useValue: { listChallenges } }]
    });

    const fixture = TestBed.createComponent(ChallengeHistory);
    fixture.detectChanges();
    return { fixture };
  }

  it('renders a card per challenge on load', () => {
    const listChallenges = vi
      .fn()
      .mockReturnValue(of({ data: [challenge], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listChallenges);

    expect(listChallenges).toHaveBeenCalledWith(0, 12);
    expect(fixture.nativeElement.querySelectorAll('app-challenge-card').length).toBe(1);
  });

  it('shows an empty state with a New challenge action when there are none', () => {
    const listChallenges = vi.fn().mockReturnValue(of({ data: [], pagination: { totalElements: 0 }, meta: {} }));
    const { fixture } = setup(listChallenges);

    expect(fixture.nativeElement.querySelector('app-empty-state')).not.toBeNull();
  });

  it('shows a retryable error state on failure', () => {
    const apiError: ApiError = {
      code: 'UNKNOWN_ERROR',
      message: 'Network error',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 0
    };
    const listChallenges = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture } = setup(listChallenges);

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });

  it('navigates to the challenge detail route when a card is opened', () => {
    const listChallenges = vi
      .fn()
      .mockReturnValue(of({ data: [challenge], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listChallenges);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.componentInstance['openChallenge']('chal-1');

    expect(navigateSpy).toHaveBeenCalledWith(['/challenges', 'chal-1']);
  });

  it(
    'has no axe violations',
    async () => {
      const listChallenges = vi
        .fn()
        .mockReturnValue(of({ data: [challenge], pagination: { totalElements: 1 }, meta: {} }));
      const { fixture } = setup(listChallenges);

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
