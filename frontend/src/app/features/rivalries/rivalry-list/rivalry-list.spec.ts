import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { RivalriesApi, Rivalry } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { expectNoAxeViolations } from '../../../testing/axe';
import { RivalryList } from './rivalry-list';

const rivalry: Rivalry = {
  cookAAccountId: 'cook-a',
  cookAName: 'Alice',
  cookBAccountId: 'cook-b',
  cookBName: 'Bob',
  cookAWins: 3,
  cookBWins: 1,
  draws: 1,
  totalChallenges: 5,
  headline: 'Alice leads Bob 3-1 (1 draw)'
};

describe('RivalryList', () => {
  function setup(listRivalries: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [RivalryList],
      providers: [provideRouter([]), { provide: RivalriesApi, useValue: { listRivalries } }]
    });

    const fixture = TestBed.createComponent(RivalryList);
    fixture.detectChanges();
    return { fixture };
  }

  it('renders the server headline verbatim', () => {
    const listRivalries = vi.fn().mockReturnValue(of({ data: [rivalry], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listRivalries);

    expect(fixture.nativeElement.querySelector('.rivalry-list__headline').textContent.trim()).toBe(
      'Alice leads Bob 3-1 (1 draw)'
    );
  });

  it('shows the total-challenges kicker line on each card', () => {
    const listRivalries = vi.fn().mockReturnValue(of({ data: [rivalry], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listRivalries);

    expect(fixture.nativeElement.querySelector('.rivalry-list__kicker').textContent.trim()).toBe('5 challenges');
  });

  it('shows the no-rivalries empty state copy', () => {
    const listRivalries = vi.fn().mockReturnValue(of({ data: [], pagination: { totalElements: 0 }, meta: {} }));
    const { fixture } = setup(listRivalries);

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
    const listRivalries = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture } = setup(listRivalries);

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });

  it('navigates to the rivalry detail route when a card is opened', () => {
    const listRivalries = vi.fn().mockReturnValue(of({ data: [rivalry], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listRivalries);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.componentInstance['openRivalry'](rivalry);

    expect(navigateSpy).toHaveBeenCalledWith(['/rivalries', 'cook-a', 'cook-b']);
  });

  it(
    'has no axe violations',
    async () => {
      const listRivalries = vi
        .fn()
        .mockReturnValue(of({ data: [rivalry], pagination: { totalElements: 1 }, meta: {} }));
      const { fixture } = setup(listRivalries);

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
