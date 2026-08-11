import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';

import { ChallengesApi, GuestHome, HomeApi, ParticipantChallenge, PlateColor } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { Auth } from '../../../core/auth/auth';
import { Notification } from '../../../core/notifications/notification';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { StatusTag } from '../../../shared/components/status-tag/status-tag';
import { ParticipantChallengeCard } from '../participant-challenge-card/participant-challenge-card';

type LoadState = 'loading' | 'loaded' | 'link-expired' | 'error';

/**
 * `/home?token=` — the access-link exchange itself, then `GET /me/home`. One screen for both
 * guest and cook, branching purely on the per-challenge flags (`canScore`/`canPickColor`/
 * `status`), never on role, see the frontend plan's Phase 6.
 */
@Component({
  selector: 'app-participant-home',
  imports: [DatePipe, EmptyState, ErrorState, LoadingSkeleton, PageHeader, ParticipantChallengeCard, StatusTag],
  templateUrl: './participant-home.html',
  styleUrl: './participant-home.scss'
})
export class ParticipantHome {
  private readonly auth = inject(Auth);
  private readonly homeApi = inject(HomeApi);
  private readonly challengesApi = inject(ChallengesApi);
  private readonly appConfig = inject(AppConfig);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(Notification);
  private readonly router = inject(Router);

  readonly token = input<string>();

  protected readonly state = signal<LoadState>('loading');
  protected readonly errorMessage = signal('');
  protected readonly home = signal<GuestHome | null>(null);
  protected readonly colorPickBusyId = signal<string | null>(null);

  protected readonly pickableColors = computed(() => this.appConfig.plateColors().slice(0, 2));

  constructor() {
    effect(() => {
      const token = this.token();
      if (token) {
        this.exchangeAndLoad(token);
      } else {
        this.loadHome();
      }
    });
  }

  private exchangeAndLoad(token: string): void {
    this.state.set('loading');
    this.auth.accessLinkLogin({ token }).subscribe({
      next: () => {
        void this.router.navigate([], { replaceUrl: true });
        this.loadHome();
      },
      error: (error: ApiError) => {
        if (error.code === 'INVALID_OR_EXPIRED_LINK') {
          this.state.set('link-expired');
        } else {
          this.errorMessage.set(error.message);
          this.state.set('error');
        }
      }
    });
  }

  protected loadHome(): void {
    this.state.set('loading');
    this.homeApi.getMyHome().subscribe({
      next: (response) => {
        this.home.set(response.data);
        this.state.set('loaded');
      },
      error: (error: ApiError) => {
        this.errorMessage.set(error.message);
        this.state.set('error');
      }
    });
  }

  protected colorFor(colorId: string | null): PlateColor | null {
    if (!colorId) return null;
    return this.appConfig.plateColors().find((color) => color.id === colorId) ?? null;
  }

  protected ownAssignment(challenge: ParticipantChallenge) {
    // `myCookLabel` is a distinct generated enum from `DishLabel` (an OpenAPI-generator inline-enum
    // quirk) even though the two share the same string values — compare as strings.
    return challenge.participantCookAssignments.find(
      (assignment) => (assignment.label as string) === (challenge.myCookLabel as string)
    );
  }

  protected openScore(challengeId: string): void {
    void this.router.navigate(['/challenges', challengeId, 'score']);
  }

  protected openResults(challengeId: string): void {
    void this.router.navigate(['/challenges', challengeId, 'results']);
  }

  protected confirmPickColor(challenge: ParticipantChallenge, color: PlateColor): void {
    const data: ConfirmDialogData = {
      title: `Plate under ${color.name}?`,
      message:
        "This locks your plate color for this challenge and assigns the other cook the remaining color automatically. This can't be changed afterward.",
      confirmLabel: `Yes, choose ${color.name}`
    };
    this.dialog
      .open(ConfirmDialog, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.pickColor(challenge, color);
        }
      });
  }

  private pickColor(challenge: ParticipantChallenge, color: PlateColor): void {
    this.colorPickBusyId.set(challenge.id);
    this.challengesApi.pickChallengeColor(challenge.id, { colorId: color.id }).subscribe({
      next: () => {
        this.colorPickBusyId.set(null);
        this.loadHome();
      },
      error: (error: ApiError) => {
        this.colorPickBusyId.set(null);
        if (error.status === 409) {
          // The other cook picked first — refresh, don't error out.
          this.loadHome();
        } else {
          this.notification.error(error.message);
        }
      }
    });
  }
}
