import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { Challenge, ChallengesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { ChallengeCard } from '../../../shared/components/challenge-card/challenge-card';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { NewChallengeDialog } from '../new-challenge-dialog/new-challenge-dialog';

const FETCH_PAGE_SIZE = 50;

type LoadState = 'loading' | 'loaded' | 'error';

/** `GET /api/v1/challenges` — fetches every page and renders one unpaginated grid, see the frontend plan's Phase 5. */
@Component({
  selector: 'app-challenge-history',
  imports: [ChallengeCard, EmptyState, ErrorState, LoadingSkeleton, MatButtonModule, MatIconModule, PageHeader],
  templateUrl: './challenge-history.html',
  styleUrl: './challenge-history.scss'
})
export class ChallengeHistory {
  private readonly challengesApi = inject(ChallengesApi);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  protected readonly state = signal<LoadState>('loading');
  protected readonly challenges = signal<Challenge[]>([]);
  protected readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.challenges.set([]);
    this.loadPage(0);
  }

  private loadPage(page: number): void {
    this.challengesApi.listChallenges(page, FETCH_PAGE_SIZE).subscribe({
      next: (response) => {
        this.challenges.update((current) => [...current, ...response.data]);
        if (page + 1 < response.pagination.totalPages) {
          this.loadPage(page + 1);
        } else {
          this.state.set('loaded');
        }
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected openChallenge(id: string): void {
    void this.router.navigate(['/challenges', id]);
  }

  protected openNewChallengeDialog(): void {
    const ref = this.dialog.open(NewChallengeDialog, { width: '560px' });
    ref.afterClosed().subscribe((created) => {
      if (created) {
        this.load();
      }
    });
  }
}
