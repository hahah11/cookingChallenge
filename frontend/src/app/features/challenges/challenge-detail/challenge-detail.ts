import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import {
  ChallengeDetail as ChallengeDetailModel,
  ChallengeResult,
  ChallengesApi,
  ChallengeStatus,
  DishLabel
} from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { ChallengePhoto } from '../../../shared/components/challenge-photo/challenge-photo';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { ResultsTable } from '../../../shared/components/results-table/results-table';
import { StatusTag } from '../../../shared/components/status-tag/status-tag';
import { EditParticipantsDialog, EditParticipantsDialogData } from '../edit-participants-dialog/edit-participants-dialog';
import { QrDialog, QrDialogData } from '../qr-dialog/qr-dialog';
import { SendLinksDialog, SendLinksDialogData } from '../send-links-dialog/send-links-dialog';

type LoadState = 'loading' | 'loaded' | 'error';

const REVEAL_ANIMATION_MS = 1100;
const LINKS_SENT_FLASH_MS = 3000;

/**
 * Organizer challenge detail: `GET /challenges/{id}/status` always (it carries both the
 * challenge's own metadata and the guest submission list), plus `GET /challenges/{id}/results`
 * once REVEALED — see the frontend plan's Phase 5b.
 */
@Component({
  selector: 'app-challenge-detail',
  imports: [
    ChallengePhoto,
    DatePipe,
    ErrorState,
    LoadingSkeleton,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    ResultsTable,
    RouterLink,
    StatusTag,
    TranslocoPipe
  ],
  templateUrl: './challenge-detail.html',
  styleUrl: './challenge-detail.scss'
})
export class ChallengeDetail {
  private readonly transloco = inject(TranslocoService);
  private readonly challengesApi = inject(ChallengesApi);
  private readonly appConfig = inject(AppConfig);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(Notification);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly ChallengeStatus = ChallengeStatus;

  protected readonly loadState = signal<LoadState>('loading');
  protected readonly loadErrorMessage = signal('');
  protected readonly challenge = signal<ChallengeDetailModel | null>(null);

  protected readonly resultState = signal<LoadState>('loading');
  protected readonly resultErrorMessage = signal('');
  protected readonly result = signal<ChallengeResult | null>(null);

  protected readonly linksSentFlash = signal(false);
  protected readonly revealing = signal(false);
  protected readonly revealBusy = signal(false);
  protected readonly unrevealBusy = signal(false);
  protected readonly scoringBusy = signal(false);
  protected readonly deleteBusy = signal(false);

  protected readonly plateColorHex = computed<Record<string, string>>(() =>
    Object.fromEntries(this.appConfig.plateColors().map((color) => [color.id, color.hexCode]))
  );

  constructor() {
    effect(() => {
      this.id();
      this.loadChallenge();
    });
  }

  protected loadChallenge(): void {
    const id = this.id();
    this.loadState.set('loading');

    this.challengesApi.getChallengeStatus(id).subscribe({
      next: (response) => {
        this.challenge.set(response.data);
        this.loadState.set('loaded');
        if (response.data.status === ChallengeStatus.REVEALED) {
          this.loadResults(id);
        }
      },
      error: (error: ApiError) => {
        this.loadErrorMessage.set(error.message);
        this.loadState.set('error');
      }
    });
  }

  protected retryResults(): void {
    const challenge = this.challenge();
    if (challenge) {
      this.loadResults(challenge.challengeId);
    }
  }

  private loadResults(challengeId: string): void {
    this.resultState.set('loading');
    this.challengesApi.getChallengeResults(challengeId).subscribe({
      next: (response) => {
        this.result.set(response.data);
        this.resultState.set('loaded');
      },
      error: (error: ApiError) => {
        this.resultErrorMessage.set(error.message);
        this.resultState.set('error');
      }
    });
  }

  protected openSendLinksDialog(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    const data: SendLinksDialogData = { challengeId: challenge.challengeId };
    const ref = this.dialog.open(SendLinksDialog, { data, width: '400px' });
    ref.afterClosed().subscribe((sent) => {
      if (sent) {
        this.linksSentFlash.set(true);
        setTimeout(() => this.linksSentFlash.set(false), LINKS_SENT_FLASH_MS);
        this.loadChallenge();
      }
    });
  }

  protected openEditParticipantsDialog(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    const cookA = challenge.cookAssignments.find((cook) => cook.label === DishLabel.A);
    const cookB = challenge.cookAssignments.find((cook) => cook.label === DishLabel.B);
    const data: EditParticipantsDialogData = {
      challengeId: challenge.challengeId,
      cookAAccountId: cookA?.accountId ?? '',
      cookBAccountId: cookB?.accountId ?? '',
      guestAccountIds: challenge.guests.map((guest) => guest.accountId)
    };
    const ref = this.dialog.open(EditParticipantsDialog, { data, width: '560px' });
    ref.afterClosed().subscribe((updated) => {
      if (updated) {
        this.loadChallenge();
      }
    });
  }

  protected openQrDialog(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    const data: QrDialogData = { challengeId: challenge.challengeId, challengeName: challenge.dishName };
    this.dialog.open(QrDialog, { data, width: '360px' });
  }

  protected confirmCloseScoring(): void {
    const data: ConfirmDialogData = {
      title: this.transloco.translate('challengeDetail.closeDialog.title'),
      message: this.transloco.translate('challengeDetail.closeDialog.message'),
      confirmLabel: this.transloco.translate('challengeDetail.closeDialog.confirm')
    };
    this.dialog
      .open(ConfirmDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.closeScoring();
        }
      });
  }

  private closeScoring(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    this.scoringBusy.set(true);
    this.challengesApi.closeChallenge(challenge.challengeId).subscribe({
      next: () => {
        this.scoringBusy.set(false);
        this.challenge.set({ ...challenge, status: ChallengeStatus.CLOSED });
      },
      error: (error: ApiError) => {
        this.scoringBusy.set(false);
        this.notification.error(error.message);
      }
    });
  }

  protected reopenScoring(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    this.scoringBusy.set(true);
    this.challengesApi.reopenChallenge(challenge.challengeId).subscribe({
      next: () => {
        this.scoringBusy.set(false);
        this.challenge.set({ ...challenge, status: ChallengeStatus.OPEN });
      },
      error: (error: ApiError) => {
        this.scoringBusy.set(false);
        this.notification.error(error.message);
      }
    });
  }

  protected confirmReveal(): void {
    const data: ConfirmDialogData = {
      title: this.transloco.translate('challengeDetail.revealDialog.title'),
      message: this.transloco.translate('challengeDetail.revealDialog.message'),
      confirmLabel: this.transloco.translate('challengeDetail.revealDialog.confirm')
    };
    this.dialog
      .open(ConfirmDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.reveal();
        }
      });
  }

  private reveal(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    this.revealBusy.set(true);
    this.challengesApi.revealChallenge(challenge.challengeId).subscribe({
      next: (response) => {
        this.revealBusy.set(false);
        this.challenge.set({ ...challenge, status: ChallengeStatus.REVEALED });
        this.result.set(response.data);
        this.resultState.set('loaded');
        this.playRevealAnimation();
      },
      error: (error: ApiError) => {
        this.revealBusy.set(false);
        this.notification.error(error.message);
      }
    });
  }

  private playRevealAnimation(): void {
    this.revealing.set(true);
    setTimeout(() => this.revealing.set(false), REVEAL_ANIMATION_MS);
  }

  protected confirmUnreveal(): void {
    const data: ConfirmDialogData = {
      title: this.transloco.translate('challengeDetail.unrevealDialog.title'),
      message: this.transloco.translate('challengeDetail.unrevealDialog.message'),
      confirmLabel: this.transloco.translate('challengeDetail.unrevealDialog.confirm')
    };
    this.dialog
      .open(ConfirmDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.unreveal();
        }
      });
  }

  private unreveal(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    this.unrevealBusy.set(true);
    this.challengesApi.unrevealChallenge(challenge.challengeId).subscribe({
      next: () => {
        this.unrevealBusy.set(false);
        this.loadChallenge();
      },
      error: (error: ApiError) => {
        this.unrevealBusy.set(false);
        this.notification.error(error.message);
      }
    });
  }

  protected confirmDelete(): void {
    const data: ConfirmDialogData = {
      title: this.transloco.translate('challengeDetail.deleteDialog.title'),
      message: this.transloco.translate('challengeDetail.deleteDialog.message'),
      confirmLabel: this.transloco.translate('challengeDetail.deleteDialog.confirm'),
      danger: true
    };
    this.dialog
      .open(ConfirmDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.deleteChallenge();
        }
      });
  }

  private deleteChallenge(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    this.deleteBusy.set(true);
    this.challengesApi.deleteChallenge(challenge.challengeId).subscribe({
      next: () => {
        this.deleteBusy.set(false);
        void this.router.navigate(['/challenges']);
      },
      error: (error: ApiError) => {
        this.deleteBusy.set(false);
        this.notification.error(error.message);
      }
    });
  }
}
