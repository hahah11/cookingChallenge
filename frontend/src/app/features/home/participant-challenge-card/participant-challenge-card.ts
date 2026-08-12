import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';

import { ParticipantChallenge, PlateColor } from '../../../core/api/generated';
import { ChallengePhoto } from '../../../shared/components/challenge-photo/challenge-photo';

/**
 * One row in the `open` bucket on `/home` — branches purely on
 * `canScore`/`canPickColor`, never on role, per the frontend plan's Phase 6.
 * `HomeService` (backend) only ever puts OPEN challenges with a pending action into
 * `open`, so this card never needs to handle a revealed challenge — see the
 * `past` compact-row template in `participant-home.html` for that case. No status
 * chip either: every card here is provably `OPEN` for the same reason, so the chip
 * would always read "Open" — redundant with the "Open scoring requests" heading.
 * Purely presentational: emits, the parent owns every API call.
 */
@Component({
  selector: 'app-participant-challenge-card',
  imports: [ChallengePhoto, DatePipe, MatButtonModule, MatCardModule, MatChipsModule],
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
}
