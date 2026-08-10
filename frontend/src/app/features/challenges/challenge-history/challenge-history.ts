import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';

import { Challenge, ChallengesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { ChallengeCard } from '../../../shared/components/challenge-card/challenge-card';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { NewChallengeDialog } from '../new-challenge-dialog/new-challenge-dialog';

const PAGE_SIZE = 12;

type LoadState = 'loading' | 'loaded' | 'error';

/** `GET /api/v1/challenges`, paginated — see the frontend plan's Phase 5. */
@Component({
  selector: 'app-challenge-history',
  imports: [
    ChallengeCard,
    EmptyState,
    ErrorState,
    LoadingSkeleton,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    PageHeader
  ],
  templateUrl: './challenge-history.html',
  styleUrl: './challenge-history.scss'
})
export class ChallengeHistory {
  private readonly challengesApi = inject(ChallengesApi);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  protected readonly state = signal<LoadState>('loading');
  protected readonly challenges = signal<Challenge[]>([]);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly pageSize = PAGE_SIZE;
  protected readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.challengesApi.listChallenges(this.page(), this.pageSize).subscribe({
      next: (response) => {
        this.challenges.set(response.data);
        this.totalElements.set(response.pagination.totalElements);
        this.state.set('loaded');
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.load();
  }

  protected openChallenge(id: string): void {
    void this.router.navigate(['/challenges', id]);
  }

  protected openNewChallengeDialog(): void {
    const ref = this.dialog.open(NewChallengeDialog, { width: '560px' });
    ref.afterClosed().subscribe((created) => {
      if (created) {
        this.page.set(0);
        this.load();
      }
    });
  }
}
