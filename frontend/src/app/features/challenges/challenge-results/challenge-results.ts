import { Component, computed, effect, inject, input, signal } from '@angular/core';

import { ChallengeResult, ChallengesApi } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { ResultsTable } from '../../../shared/components/results-table/results-table';

type LoadState = 'loading' | 'loaded' | 'error';

/**
 * `GET /challenges/{id}/results` — the same `results-table` the organizer detail screen
 * uses, minus the unreveal control, see the frontend plan's Phase 6.
 */
@Component({
  selector: 'app-challenge-results',
  imports: [ErrorState, LoadingSkeleton, PageHeader, ResultsTable],
  templateUrl: './challenge-results.html'
})
export class ChallengeResults {
  private readonly challengesApi = inject(ChallengesApi);
  private readonly appConfig = inject(AppConfig);

  readonly id = input.required<string>();

  protected readonly loadState = signal<LoadState>('loading');
  protected readonly loadErrorMessage = signal('');
  protected readonly result = signal<ChallengeResult | null>(null);

  protected readonly plateColorHex = computed<Record<string, string>>(() =>
    Object.fromEntries(this.appConfig.plateColors().map((color) => [color.id, color.hexCode]))
  );

  constructor() {
    effect(() => {
      this.id();
      this.loadResults();
    });
  }

  protected loadResults(): void {
    this.loadState.set('loading');
    this.challengesApi.getChallengeResults(this.id()).subscribe({
      next: (response) => {
        this.result.set(response.data);
        this.loadState.set('loaded');
      },
      error: (error: ApiError) => {
        this.loadErrorMessage.set(error.message);
        this.loadState.set('error');
      }
    });
  }
}
