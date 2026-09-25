import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { TranslocoPipe } from '@jsverse/transloco';

import { ChallengeStatus, ParticipantChallenge, PlateColor } from '../../../core/api/generated';
import { PlateColorNamePipe } from '../../../core/i18n/plate-color-name';
import { ChallengePhoto } from '../../../shared/components/challenge-photo/challenge-photo';

/**
 * One row in the `open` bucket on `/home` — branches purely on
 * `canScore`/`canPickColor`, never on role, per the frontend plan's Phase 6.
 * `HomeService` (backend) puts every OPEN or CLOSED challenge into `open`, whether
 * or not there's still a pending action — an already-submitted score or already-picked
 * color stays visible here, editable while scoring is open — and only REVEALED
 * challenges into `past` (see the `past` compact-row template in `participant-home.html`
 * for that case). No status chip for OPEN (it would be redundant with the "Open scoring
 * requests" heading); a CLOSED challenge swaps the score actions for a "Scoring closed"
 * chip plus a read-only "View my scores" link. Purely presentational: emits, the parent
 * owns every API call.
 */
@Component({
  selector: 'app-participant-challenge-card',
  imports: [ChallengePhoto, DatePipe, MatButtonModule, MatCardModule, MatChipsModule, PlateColorNamePipe, TranslocoPipe],
  templateUrl: './participant-challenge-card.html',
  styleUrl: './participant-challenge-card.scss'
})
export class ParticipantChallengeCard {
  readonly challenge = input.required<ParticipantChallenge>();
  readonly pickableColors = input<PlateColor[]>([]);
  readonly ownColor = input<PlateColor | null>(null);
  readonly colorPickBusy = input(false);

  readonly score = output<string>();
  readonly pickColor = output<PlateColor>();

  /** True for the two cook-specific action states (color picker or already-picked) — the canvas's `isCookHome` card style applies only here. */
  readonly scoringClosed = computed(() => this.challenge().status === ChallengeStatus.CLOSED);

  readonly isCook = computed(() => this.challenge().canPickColor || this.ownColor() !== null);
}
