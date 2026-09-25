import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { RivalriesApi, RivalryChallengeSummary, RivalryDetail as RivalryDetailModel } from '../../../core/api/generated';
import { RivalryText } from '../../../core/i18n/rivalry-text';
import { ApiError } from '../../../core/errors/api-error';
import { ChallengePhoto } from '../../../shared/components/challenge-photo/challenge-photo';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { StatusTag } from '../../../shared/components/status-tag/status-tag';

type LoadState = 'loading' | 'loaded' | 'error';

/** `GET /rivalries/{cookA}/{cookB}` — the pair is canonicalized server-side. */
@Component({
  selector: 'app-rivalry-detail',
  imports: [
    ChallengePhoto,
    DatePipe,
    ErrorState,
    LoadingSkeleton,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    PageHeader,
    RouterLink,
    StatusTag,
    TranslocoPipe
  ],
  templateUrl: './rivalry-detail.html',
  styleUrl: './rivalry-detail.scss'
})
export class RivalryDetail {
  private readonly rivalriesApi = inject(RivalriesApi);
  private readonly rivalryText = inject(RivalryText);

  readonly cookA = input.required<string>();
  readonly cookB = input.required<string>();

  protected readonly state = signal<LoadState>('loading');
  protected readonly errorMessage = signal('');
  protected readonly rivalry = signal<RivalryDetailModel | null>(null);

  constructor() {
    effect(() => {
      this.cookA();
      this.cookB();
      this.load();
    });
  }

  protected load(): void {
    this.state.set('loading');
    this.rivalriesApi.getRivalryDetail(this.cookA(), this.cookB()).subscribe({
      next: (response) => {
        this.rivalry.set(response.data);
        this.state.set('loaded');
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected headline(rivalry: RivalryDetailModel): string {
    return this.rivalryText.headline(rivalry);
  }

  protected outcome(rivalry: RivalryDetailModel, challenge: RivalryChallengeSummary): string {
    return this.rivalryText.outcome(
      challenge.status,
      challenge.overallWinnerAccountId,
      { accountId: rivalry.cookAAccountId, name: rivalry.cookAName },
      { accountId: rivalry.cookBAccountId, name: rivalry.cookBName }
    );
  }
}
