import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { Account, AccountsApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { EditAccountDialog, EditAccountDialogData } from '../edit-account-dialog/edit-account-dialog';

const PAGE_SIZE = 20;

type LoadState = 'loading' | 'loaded' | 'error';

/** `GET /api/v1/accounts`, paginated — see the frontend plan's Phase 5. */
@Component({
  selector: 'app-accounts-admin',
  imports: [EmptyState, ErrorState, LoadingSkeleton, MatButtonModule, MatIconModule, MatPaginatorModule, PageHeader],
  templateUrl: './accounts-admin.html',
  styleUrl: './accounts-admin.scss'
})
export class AccountsAdmin {
  private readonly accountsApi = inject(AccountsApi);
  private readonly dialog = inject(MatDialog);

  protected readonly state = signal<LoadState>('loading');
  protected readonly accounts = signal<Account[]>([]);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly pageSize = PAGE_SIZE;
  protected readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.accountsApi.listAccounts(this.page(), this.pageSize).subscribe({
      next: (response) => {
        this.accounts.set(response.data);
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

  protected openEditDialog(accountId: string): void {
    const data: EditAccountDialogData = { accountId };
    const ref = this.dialog.open(EditAccountDialog, { data, width: '480px' });
    ref.afterClosed().subscribe((updated) => {
      if (updated) {
        this.load();
      }
    });
  }
}
