import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';

import { Rivalry, RivalriesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';

const PAGE_SIZE = 12;

type LoadState = 'loading' | 'loaded' | 'error';

/** `GET /api/v1/rivalries`, paginated — see the frontend plan's Phase 5. */
@Component({
  selector: 'app-rivalry-list',
  imports: [EmptyState, ErrorState, LoadingSkeleton, MatCardModule, MatPaginatorModule, PageHeader],
  templateUrl: './rivalry-list.html',
  styleUrl: './rivalry-list.scss'
})
export class RivalryList {
  private readonly rivalriesApi = inject(RivalriesApi);
  private readonly router = inject(Router);

  protected readonly state = signal<LoadState>('loading');
  protected readonly rivalries = signal<Rivalry[]>([]);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly pageSize = PAGE_SIZE;
  protected readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.rivalriesApi.listRivalries(this.page(), this.pageSize).subscribe({
      next: (response) => {
        this.rivalries.set(response.data);
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

  protected openRivalry(rivalry: Rivalry): void {
    void this.router.navigate(['/rivalries', rivalry.cookAAccountId, rivalry.cookBAccountId]);
  }
}
