import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatRippleModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Challenge, ChallengeStatus, CookAssignment } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ChallengePhoto } from '../challenge-photo/challenge-photo';
import { StatusTag } from '../status-tag/status-tag';

/**
 * History grid card. Photo (4:3), date kicker, dish title, event title, cook
 * line, status tag, progress — see the frontend plan's Phase 3/5. Purely
 * presentational: emits `open` with the challenge id, the caller navigates.
 */
@Component({
  selector: 'app-challenge-card',
  imports: [DatePipe, MatCardModule, MatIconModule, MatProgressBarModule, MatRippleModule, ChallengePhoto, StatusTag],
  templateUrl: './challenge-card.html',
  styleUrl: './challenge-card.scss'
})
export class ChallengeCard {
  private readonly appConfig = inject(AppConfig);

  protected readonly ChallengeStatus = ChallengeStatus;

  readonly challenge = input.required<Challenge>();
  readonly open = output<string>();

  protected readonly progressPercent = computed(() => {
    const challenge = this.challenge();
    if (challenge.totalGuestCount === 0) return 0;
    return (challenge.submittedGuestCount / challenge.totalGuestCount) * 100;
  });

  protected hexFor(cook: CookAssignment): string | null {
    return cook.colorId ? (this.appConfig.plateColors().find((color) => color.id === cook.colorId)?.hexCode ?? null) : null;
  }

  protected onOpen(): void {
    this.open.emit(this.challenge().id);
  }
}
