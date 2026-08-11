import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { ChallengesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { QrCode } from '../../../shared/components/qr-code/qr-code';

export interface QrDialogData {
  challengeId: string;
  challengeName: string;
}

type LoadState = 'loading' | 'loaded' | 'error';

/**
 * Generates a fresh self-registration QR on every open via
 * `POST /challenges/{id}/registration-invites` — reusable until expiry, so
 * re-opening this dialog is cheap and always current.
 */
@Component({
  selector: 'app-qr-dialog',
  imports: [ErrorState, LoadingSkeleton, MatButtonModule, MatDialogModule, QrCode],
  template: `
    <h2 mat-dialog-title>Scan to register</h2>
    <mat-dialog-content class="qr-dialog__content">
      @switch (state()) {
        @case ('loading') {
          <app-loading-skeleton [lines]="3" />
        }
        @case ('error') {
          <app-error-state [message]="errorMessage()" [retryable]="true" (retry)="load()" />
        }
        @case ('loaded') {
          <app-qr-code [value]="registrationUrl()" />
          <p class="qr-dialog__hint">Guests scan this to register for the app and join {{ data.challengeName }}.</p>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: `
    .qr-dialog__content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--md-sys-spacing-3);
      min-width: min(320px, 90vw);
      text-align: center;
    }

    .qr-dialog__hint {
      margin: 0;
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
    }
  `
})
export class QrDialog {
  protected readonly data = inject<QrDialogData>(MAT_DIALOG_DATA);
  private readonly challengesApi = inject(ChallengesApi);

  protected readonly state = signal<LoadState>('loading');
  protected readonly registrationUrl = signal('');
  protected readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.challengesApi.createRegistrationInvite(this.data.challengeId).subscribe({
      next: (response) => {
        this.registrationUrl.set(response.data.registrationUrl);
        this.state.set('loaded');
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }
}
