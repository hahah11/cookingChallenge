import { Component, inject, signal } from '@angular/core';
import { email as emailValidator, form, FormField, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Account, AccountsApi, SystemRole } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';

export interface EditAccountDialogData {
  accountId: string;
}

interface EditAccountFormModel {
  firstName: string;
  lastName: string;
  email: string;
}

type LoadState = 'loading' | 'loaded' | 'error';

/**
 * Re-fetches the account fresh via `GET /accounts/{id}` on open, per the
 * standing data-loading rule — never reuses the parent table's row data.
 */
@Component({
  selector: 'app-edit-account-dialog',
  imports: [
    ErrorState,
    FormField,
    LoadingSkeleton,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './edit-account-dialog.html',
  styleUrl: './edit-account-dialog.scss'
})
export class EditAccountDialog {
  private readonly data = inject<EditAccountDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditAccountDialog, Account | undefined>);
  private readonly accountsApi = inject(AccountsApi);
  private readonly appConfig = inject(AppConfig);

  protected readonly availableRoles = this.appConfig.availableRoles;
  protected readonly SystemRole = SystemRole;

  protected readonly state = signal<LoadState>('loading');
  protected readonly errorMessage = signal('');

  protected readonly model = signal<EditAccountFormModel>({ firstName: '', lastName: '', email: '' });
  protected readonly accountForm = form(this.model, (path) => {
    required(path.firstName, { message: 'First name is required.' });
    required(path.lastName, { message: 'Last name is required.' });
    required(path.email, { message: 'Email is required.' });
    emailValidator(path.email, { message: 'Enter a valid email address.' });
  });

  protected readonly roles = signal<ReadonlySet<SystemRole>>(new Set([SystemRole.USER]));

  protected readonly submitting = signal(false);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.accountsApi.getAccount(this.data.accountId).subscribe({
      next: (response) => {
        const account = response.data;
        this.model.set({ firstName: account.firstName, lastName: account.lastName, email: account.email });
        this.roles.set(new Set(account.roles));
        this.state.set('loaded');
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected toggleRole(role: SystemRole, checked: boolean): void {
    if (role === SystemRole.USER) {
      return;
    }
    const next = new Set(this.roles());
    if (checked) {
      next.add(role);
    } else {
      next.delete(role);
    }
    this.roles.set(next);
  }

  protected onSubmit(): void {
    this.accountForm().markAsTouched();
    if (this.accountForm().invalid() || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    const { firstName, lastName, email } = this.model();
    this.accountsApi
      .updateAccount(this.data.accountId, { firstName, lastName, email, roles: Array.from(this.roles()) })
      .subscribe({
        next: (response) => this.dialogRef.close(response.data),
        error: (error: ApiError) => {
          this.submitting.set(false);
          this.errorMessage.set(error.message);
        }
      });
  }

  protected onCancel(): void {
    this.dialogRef.close(undefined);
  }
}
