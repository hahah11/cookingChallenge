import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Category, ChallengesApi, Config, ConfigApi, DishLabel } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { expectNoAxeViolations } from '../../../testing/axe';
import { ChallengeResults } from './challenge-results';

const result = {
  challengeId: 'chal-1',
  categoryWinners: { [Category.GESCHMACK]: 'cook-a' },
  categoryTotals: [],
  overallWinnerAccountId: 'cook-a',
  cookAssignments: [
    { accountId: 'cook-a', name: 'Alice', label: DishLabel.A, colorId: 'red' },
    { accountId: 'cook-b', name: 'Bob', label: DishLabel.B, colorId: 'yellow' }
  ],
  rivalry: {
    cookAAccountId: 'cook-a',
    cookBAccountId: 'cook-b',
    cookAWins: 1,
    cookBWins: 0,
    draws: 0,
    totalChallenges: 1,
    headline: 'Alice leads Bob 1-0'
  }
};

const config: Config = { availableRoles: [], plateColors: [], featureFlags: {} };
const meta = { requestId: 'req-1', timestamp: '2026-01-01T00:00:00Z' };

describe('ChallengeResults', () => {
  function setup(challengesApi: Record<string, unknown>) {
    TestBed.configureTestingModule({
      imports: [ChallengeResults],
      providers: [
        { provide: ChallengesApi, useValue: challengesApi },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta }) } },
        AppConfig
      ]
    });
    TestBed.inject(AppConfig).load().subscribe();

    const fixture = TestBed.createComponent(ChallengeResults);
    fixture.componentRef.setInput('id', 'chal-1');
    fixture.detectChanges();
    return { fixture };
  }

  it('fetches results for the given id and renders the shared results table, with no unreveal control', () => {
    const getChallengeResults = vi.fn().mockReturnValue(of({ data: result, meta }));
    const { fixture } = setup({ getChallengeResults });

    expect(getChallengeResults).toHaveBeenCalledWith('chal-1');
    expect(fixture.nativeElement.querySelector('app-results-table')).not.toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Unreveal');
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
    const getChallengeResults = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture } = setup({ getChallengeResults });

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });

  it(
    'has no axe violations',
    async () => {
      const getChallengeResults = vi.fn().mockReturnValue(of({ data: result, meta }));
      const { fixture } = setup({ getChallengeResults });

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
