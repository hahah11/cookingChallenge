import { Component, computed, inject, signal } from '@angular/core';
import { form, FormField, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { of, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Account, AccountsApi, Challenge, ChallengesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';

interface NewChallengeFormModel {
  title: string;
  date: string;
  dishName: string;
  cookAAccountId: string;
  cookBAccountId: string;
}

/** Large enough to cover every account without a second page — see the frontend plan's Phase 5. */
const ACCOUNTS_PAGE_SIZE = 200;

/**
 * `POST /challenges` then, only if a photo was picked, `PATCH /challenges/{id}/image` —
 * `CreateChallengeRequest` deliberately has no image field, see the frontend plan's Phase 5.
 * Fetches its own account list fresh on open, per the standing data-loading rule.
 */
@Component({
  selector: 'app-new-challenge-dialog',
  imports: [
    FormField,
    LoadingSkeleton,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './new-challenge-dialog.html',
  styleUrl: './new-challenge-dialog.scss'
})
export class NewChallengeDialog {
  private readonly dialogRef = inject(MatDialogRef<NewChallengeDialog, Challenge | undefined>);
  private readonly accountsApi = inject(AccountsApi);
  private readonly challengesApi = inject(ChallengesApi);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly accountsLoading = signal(true);

  protected readonly model = signal<NewChallengeFormModel>({
    title: '',
    date: '',
    dishName: '',
    cookAAccountId: '',
    cookBAccountId: ''
  });
  protected readonly challengeForm = form(this.model, (path) => {
    required(path.title, { message: 'Title is required.' });
    required(path.date, { message: 'Date is required.' });
    required(path.dishName, { message: 'Dish name is required.' });
    required(path.cookAAccountId, { message: 'Choose Cook A.' });
    required(path.cookBAccountId, { message: 'Choose Cook B.' });
  });

  protected readonly cookBOptions = computed(() =>
    this.accounts().filter((account) => account.id !== this.model().cookAAccountId)
  );

  protected readonly guestIds = signal<ReadonlySet<string>>(new Set());
  protected readonly photoFile = signal<File | null>(null);
  protected readonly photoPreviewUrl = signal<string | null>(null);

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

  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.setPhoto(input.files?.[0] ?? null);
  }

  protected onPhotoDropped(event: DragEvent): void {
    event.preventDefault();
    this.setPhoto(event.dataTransfer?.files?.[0] ?? null);
  }

  protected removePhoto(event: Event): void {
    event.stopPropagation();
    this.setPhoto(null);
  }

  private setPhoto(file: File | null): void {
    const previous = this.photoPreviewUrl();
    if (previous) {
      URL.revokeObjectURL(previous);
    }
    this.photoFile.set(file);
    this.photoPreviewUrl.set(file ? URL.createObjectURL(file) : null);
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
    this.challengeForm().markAsTouched();
    if (this.challengeForm().invalid() || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const { title, date, dishName, cookAAccountId, cookBAccountId } = this.model();
    this.challengesApi
      .createChallenge({
        title,
        date,
        dishName,
        cookAAccountId,
        cookBAccountId,
        guestAccountIds: Array.from(this.guestIds())
      })
      .pipe(
        switchMap((response) => {
          const photoFile = this.photoFile();
          if (!photoFile) {
            return of(response.data);
          }
          return this.challengesApi.updateChallengeImage(response.data.id, photoFile).pipe(switchMap((r) => of(r.data)));
        }),
        catchError((error: ApiError) => {
          this.submitting.set(false);
          this.errorMessage.set(error.message);
          return of(null);
        })
      )
      .subscribe((challenge) => {
        if (challenge) {
          this.dialogRef.close(challenge);
        }
      });
  }

  protected onCancel(): void {
    this.dialogRef.close(undefined);
  }
}
