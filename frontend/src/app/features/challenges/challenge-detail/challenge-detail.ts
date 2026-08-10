import { Location } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import {
  Challenge,
  ChallengeResult,
  ChallengesApi,
  ChallengeStatus,
  DishLabel,
  SubmissionStatus
} from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { ChallengePhoto } from '../../../shared/components/challenge-photo/challenge-photo';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { ResultsTable } from '../../../shared/components/results-table/results-table';
import { StatusTag } from '../../../shared/components/status-tag/status-tag';
import { EditParticipantsDialog, EditParticipantsDialogData } from '../edit-participants-dialog/edit-participants-dialog';
import { QrDialog, QrDialogData } from '../qr-dialog/qr-dialog';
import { SendLinksDialog, SendLinksDialogData } from '../send-links-dialog/send-links-dialog';

type LoadState = 'loading' | 'loaded' | 'error';

/** No dedicated single-challenge fetch exists for organizers (only the participant-blind
 * view does) — this covers a hard refresh/direct link when History didn't hand off its
 * already-loaded `Challenge` via router state. See the frontend plan's Phase 5 notes. */
const FALLBACK_LIST_PAGE_SIZE = 100;

const REVEAL_ANIMATION_MS = 1100;
const LINKS_SENT_FLASH_MS = 3000;

/**
 * Organizer challenge detail: `GET /challenges/{id}/status` while OPEN,
 * `GET /challenges/{id}/results` once REVEALED — see the frontend plan's Phase 5.
 */
@Component({
  selector: 'app-challenge-detail',
  imports: [
    ChallengePhoto,
    ErrorState,
    LoadingSkeleton,
    MatButtonModule,
    MatIconModule,
    PageHeader,
    ResultsTable,
    StatusTag
  ],
  templateUrl: './challenge-detail.html',
  styleUrl: './challenge-detail.scss'
})
export class ChallengeDetail {
  private readonly challengesApi = inject(ChallengesApi);
  private readonly appConfig = inject(AppConfig);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(Notification);
  private readonly location = inject(Location);

  readonly id = input.required<string>();

  protected readonly ChallengeStatus = ChallengeStatus;

  protected readonly loadState = signal<LoadState>('loading');
  protected readonly loadErrorMessage = signal('');
  protected readonly challenge = signal<Challenge | null>(null);

  protected readonly submissionState = signal<LoadState>('loading');
  protected readonly submissionErrorMessage = signal('');
  protected readonly submissionStatus = signal<SubmissionStatus | null>(null);

  protected readonly resultState = signal<LoadState>('loading');
  protected readonly resultErrorMessage = signal('');
  protected readonly result = signal<ChallengeResult | null>(null);

  protected readonly linksSentFlash = signal(false);
  protected readonly revealing = signal(false);
  protected readonly revealBusy = signal(false);
  protected readonly unrevealBusy = signal(false);

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

    const navigationState = this.location.getState() as { challenge?: Challenge } | null;
    if (navigationState?.challenge && navigationState.challenge.id === id) {
      this.onChallengeLoaded(navigationState.challenge);
      return;
    }

    this.challengesApi.listChallenges(0, FALLBACK_LIST_PAGE_SIZE).subscribe({
      next: (response) => {
        const found = response.data.find((c) => c.id === id);
        if (found) {
          this.onChallengeLoaded(found);
        } else {
          this.loadErrorMessage.set('Challenge not found.');
          this.loadState.set('error');
        }
      },
      error: (error: ApiError) => {
        this.loadErrorMessage.set(error.message);
        this.loadState.set('error');
      }
    });
  }

  private onChallengeLoaded(challenge: Challenge): void {
    this.challenge.set(challenge);
    this.loadState.set('loaded');
    if (challenge.status === ChallengeStatus.OPEN) {
      this.loadSubmissionStatus(challenge.id);
    } else {
      this.loadResults(challenge.id);
    }
  }

  protected retrySubmissionStatus(): void {
    const challenge = this.challenge();
    if (challenge) {
      this.loadSubmissionStatus(challenge.id);
    }
  }

  protected retryResults(): void {
    const challenge = this.challenge();
    if (challenge) {
      this.loadResults(challenge.id);
    }
  }

  private loadSubmissionStatus(challengeId: string): void {
    this.submissionState.set('loading');
    this.challengesApi.getChallengeStatus(challengeId).subscribe({
      next: (response) => {
        this.submissionStatus.set(response.data);
        this.submissionState.set('loaded');
      },
      error: (error: ApiError) => {
        this.submissionErrorMessage.set(error.message);
        this.submissionState.set('error');
      }
    });
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

    const data: SendLinksDialogData = { challengeId: challenge.id };
    const ref = this.dialog.open(SendLinksDialog, { data, width: '480px' });
    ref.afterClosed().subscribe((sent) => {
      if (sent) {
        this.linksSentFlash.set(true);
        setTimeout(() => this.linksSentFlash.set(false), LINKS_SENT_FLASH_MS);
        this.loadSubmissionStatus(challenge.id);
      }
    });
  }

  protected openEditParticipantsDialog(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    const cookA = challenge.cookAssignments.find((cook) => cook.label === DishLabel.A);
    const cookB = challenge.cookAssignments.find((cook) => cook.label === DishLabel.B);
    const data: EditParticipantsDialogData = {
      challengeId: challenge.id,
      cookAAccountId: cookA?.accountId ?? '',
      cookBAccountId: cookB?.accountId ?? '',
      guestAccountIds: [...challenge.guestAccountIds]
    };
    const ref = this.dialog.open(EditParticipantsDialog, { data, width: '560px' });
    ref.afterClosed().subscribe((updated) => {
      if (updated) {
        this.onChallengeLoaded(updated);
      }
    });
  }

  protected openQrDialog(): void {
    const challenge = this.challenge();
    if (!challenge) return;

    const data: QrDialogData = { challengeId: challenge.id };
    this.dialog.open(QrDialog, { data, width: '360px' });
  }

  protected confirmReveal(): void {
    const data: ConfirmDialogData = {
      title: 'Reveal results?',
      message:
        'Revealing shows cook identities, computes results, and closes scoring. You can reopen it later if you need to.',
      confirmLabel: 'Reveal'
    };
    this.dialog
      .open(ConfirmDialog, { data })
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
    this.challengesApi.revealChallenge(challenge.id).subscribe({
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
      title: 'Reopen scoring?',
      message: 'This hides the results and reopens scoring so guests can resubmit.',
      confirmLabel: 'Reopen scoring'
    };
    this.dialog
      .open(ConfirmDialog, { data })
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
    this.challengesApi.unrevealChallenge(challenge.id).subscribe({
      next: (response) => {
        this.unrevealBusy.set(false);
        this.onChallengeLoaded(response.data);
      },
      error: (error: ApiError) => {
        this.unrevealBusy.set(false);
        this.notification.error(error.message);
      }
    });
  }
}
