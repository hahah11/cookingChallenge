import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ChallengesApi, CookAssignment, GuestSubmissionStatus, InvitationsSent } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';

export interface SendLinksDialogData {
  challengeId: string;
}

interface Recipient {
  accountId: string;
  name: string;
  role: 'Cook' | 'Guest';
  submitted: boolean;
}

type LoadState = 'loading' | 'loaded' | 'error';

/**
 * Fetches the cook/guest list fresh via `GET /challenges/{id}/status` on open (never
 * reuses the parent detail screen's already-loaded state, per the standing
 * data-loading rule) and resends/sends access links to the checked recipients. Cooks
 * are addressable too (per `SendInvitationsRequest.cookAccountIds`) but never
 * pre-selected — only unsubmitted guests default to checked.
 */
@Component({
  selector: 'app-send-links-dialog',
  imports: [ErrorState, LoadingSkeleton, MatButtonModule, MatCheckboxModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './send-links-dialog.html',
  styleUrl: './send-links-dialog.scss'
})
export class SendLinksDialog {
  private readonly data = inject<SendLinksDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SendLinksDialog, InvitationsSent | undefined>);
  private readonly challengesApi = inject(ChallengesApi);

  protected readonly state = signal<LoadState>('loading');
  protected readonly cooks = signal<CookAssignment[]>([]);
  protected readonly guests = signal<GuestSubmissionStatus[]>([]);
  protected readonly selectedIds = signal<ReadonlySet<string>>(new Set());
  protected readonly errorMessage = signal('');
  protected readonly submitting = signal(false);

  protected readonly recipients = computed<Recipient[]>(() => [
    ...this.cooks().map((cook) => ({ accountId: cook.accountId, name: cook.name, role: 'Cook' as const, submitted: false })),
    ...this.guests().map((guest) => ({
      accountId: guest.accountId,
      name: guest.name,
      role: 'Guest' as const,
      submitted: guest.submitted
    }))
  ]);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.challengesApi.getChallengeStatus(this.data.challengeId).subscribe({
      next: (response) => {
        this.cooks.set(response.data.cookAssignments);
        this.guests.set(response.data.guests);
        this.selectedIds.set(new Set(response.data.guests.filter((g) => !g.submitted).map((g) => g.accountId)));
        this.state.set('loaded');
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected toggle(accountId: string, checked: boolean): void {
    const next = new Set(this.selectedIds());
    if (checked) {
      next.add(accountId);
    } else {
      next.delete(accountId);
    }
    this.selectedIds.set(next);
  }

  protected onSend(): void {
    if (this.selectedIds().size === 0 || this.submitting()) {
      return;
    }

    const cookIds = new Set(this.cooks().map((cook) => cook.accountId));
    const guestAccountIds: string[] = [];
    const cookAccountIds: string[] = [];
    for (const id of this.selectedIds()) {
      (cookIds.has(id) ? cookAccountIds : guestAccountIds).push(id);
    }

    this.submitting.set(true);
    this.challengesApi.sendInvitations(this.data.challengeId, { guestAccountIds, cookAccountIds }).subscribe({
      next: (response) => this.dialogRef.close(response.data),
      error: (error: ApiError) => {
        this.submitting.set(false);
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected onCancel(): void {
    this.dialogRef.close(undefined);
  }
}
