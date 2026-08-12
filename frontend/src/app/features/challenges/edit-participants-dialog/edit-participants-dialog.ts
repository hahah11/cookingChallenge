import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { Account, AccountsApi, Challenge, ChallengesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';

export interface EditParticipantsDialogData {
  challengeId: string;
  cookAAccountId: string;
  cookBAccountId: string;
  guestAccountIds: string[];
}

/** Backend's SizeParam caps at 100 (openapi/cookingchallenge-api.yaml) — see the frontend plan's Phase 5. */
const ACCOUNTS_PAGE_SIZE = 100;

/**
 * Reassigns cooks and/or adds or removes guests on an OPEN challenge.
 * Fetches the account pool fresh on open, per the standing data-loading rule —
 * the current cook/guest identities themselves have no dedicated re-fetch
 * endpoint, so those come from the parent detail screen's own state (which is
 * itself always current, since it's the page the organizer is looking at).
 */
@Component({
  selector: 'app-edit-participants-dialog',
  imports: [
    LoadingSkeleton,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './edit-participants-dialog.html',
  styleUrl: './edit-participants-dialog.scss'
})
export class EditParticipantsDialog {
  private readonly data = inject<EditParticipantsDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditParticipantsDialog, Challenge | undefined>);
  private readonly accountsApi = inject(AccountsApi);
  private readonly challengesApi = inject(ChallengesApi);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly accountsLoading = signal(true);

  protected readonly cookAAccountId = signal(this.data.cookAAccountId);
  protected readonly cookBAccountId = signal(this.data.cookBAccountId);
  protected readonly guestIds = signal<ReadonlySet<string>>(new Set(this.data.guestAccountIds));

  protected readonly cookBOptions = computed(() =>
    this.accounts().filter((account) => account.id !== this.cookAAccountId())
  );

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.accountsApi.listAccounts(0, ACCOUNTS_PAGE_SIZE).subscribe({
      next: (response) => {
        this.accounts.set(response.data);
        this.accountsLoading.set(false);
      },
      error: () => this.accountsLoading.set(false)
    });
  }

  protected toggleGuest(accountId: string, checked: boolean): void {
    const next = new Set(this.guestIds());
    if (checked) {
      next.add(accountId);
    } else {
      next.delete(accountId);
    }
    this.guestIds.set(next);
  }

  protected onSubmit(): void {
    if (this.submitting()) {
      return;
    }

    const originalGuestIds = new Set(this.data.guestAccountIds);
    const selectedGuestIds = this.guestIds();
    const addGuestAccountIds = Array.from(selectedGuestIds).filter((id) => !originalGuestIds.has(id));
    const removeGuestAccountIds = Array.from(originalGuestIds).filter((id) => !selectedGuestIds.has(id));

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.challengesApi
      .updateChallengeParticipants(this.data.challengeId, {
        cookAAccountId: this.cookAAccountId(),
        cookBAccountId: this.cookBAccountId(),
        addGuestAccountIds,
        removeGuestAccountIds
      })
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
