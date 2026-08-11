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
  /** `null` opens the dialog in create mode instead of editing an existing account. */
  accountId: string | null;
}

interface EditAccountFormModel {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

type LoadState = 'loading' | 'loaded' | 'error';

/**
 * In edit mode, re-fetches the account fresh via `GET /accounts/{id}` on open, per
 * the standing data-loading rule — never reuses the parent table's row data. In
 * create mode (`accountId: null`) there's nothing to fetch; the same form and
 * dialog just submit to `POST /accounts` instead, per the design's single
 * New/Edit dialog with a dynamic title and submit label.
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

  protected readonly isCreate = this.data.accountId === null;
  protected readonly dialogTitle = this.isCreate ? 'New account' : 'Edit account';
  protected readonly submitLabel = this.isCreate ? 'Create account' : 'Save changes';

  protected readonly state = signal<LoadState>(this.isCreate ? 'loaded' : 'loading');
  protected readonly errorMessage = signal('');

  protected readonly model = signal<EditAccountFormModel>({
    firstName: '',
    lastName: '',
    email: '',
    password: ''
  });
  protected readonly accountForm = form(this.model, (path) => {
    required(path.firstName, { message: 'First name is required.' });
    required(path.lastName, { message: 'Last name is required.' });
    required(path.email, { message: 'Email is required.' });
    emailValidator(path.email, { message: 'Enter a valid email address.' });
  });

  protected readonly roles = signal<ReadonlySet<SystemRole>>(new Set([SystemRole.USER]));

  protected readonly submitting = signal(false);

  constructor() {
    if (!this.isCreate) {
      this.load();
    }
  }

  protected load(): void {
    const accountId = this.data.accountId;
    if (accountId === null) return;

    this.state.set('loading');
    this.accountsApi.getAccount(accountId).subscribe({
      next: (response) => {
        const account = response.data;
        this.model.set({
          firstName: account.firstName,
          lastName: account.lastName,
          email: account.email,
          password: ''
        });
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
    const { firstName, lastName, email, password } = this.model();
    const roles = Array.from(this.roles());
    const accountId = this.data.accountId;
    const request = accountId === null
      ? this.accountsApi.createAccount({ firstName, lastName, email, roles, password: password || undefined })
      : this.accountsApi.updateAccount(accountId, { firstName, lastName, email, roles });

    request.subscribe({
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
