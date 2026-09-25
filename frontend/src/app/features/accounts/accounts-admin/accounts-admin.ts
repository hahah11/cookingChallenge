import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { Account, AccountsApi, SystemRole } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';
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
  imports: [
    EmptyState,
    ErrorState,
    LoadingSkeleton,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatPaginatorModule,
    PageHeader,
    TranslocoPipe
  ],
  templateUrl: './accounts-admin.html',
  styleUrl: './accounts-admin.scss'
})
export class AccountsAdmin {
  private readonly transloco = inject(TranslocoService);
  protected readonly SystemRole = SystemRole;

  private readonly accountsApi = inject(AccountsApi);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(Notification);

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
    const ref = this.dialog.open(EditAccountDialog, { data, width: '360px' });
    ref.afterClosed().subscribe((updated) => {
      if (updated) {
        this.load();
      }
    });
  }

  /** Only accounts that hold a password; guests (USER only) log in through access links. */
  protected canResetPassword(account: Account): boolean {
    return account.roles.includes(SystemRole.ADMIN) || account.roles.includes(SystemRole.ORGANIZER);
  }

  protected confirmPasswordReset(account: Account): void {
    const data: ConfirmDialogData = {
      title: this.transloco.translate('accounts.resetDialog.title'),
      message: this.transloco.translate('accounts.resetDialog.message', { name: account.name }),
      confirmLabel: this.transloco.translate('accounts.resetDialog.confirm')
    };
    this.dialog
      .open(ConfirmDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.resetPassword(account);
        }
      });
  }

  /** Nothing on the page changes, so the snackbar carries all the feedback. */
  private resetPassword(account: Account): void {
    this.accountsApi.triggerPasswordReset(account.id).subscribe({
      next: () => this.notification.success(this.transloco.translate('accounts.resetSent', { email: account.email })),
      error: (error: ApiError) => this.notification.error(error.message)
    });
  }

  protected openCreateDialog(): void {
    const data: EditAccountDialogData = { accountId: null };
    const ref = this.dialog.open(EditAccountDialog, { data, width: '360px' });
    ref.afterClosed().subscribe((created) => {
      if (created) {
        this.load();
      }
    });
  }
}
