import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

import { ChallengeStatus, ParticipantChallenge, PlateColor } from '../../../core/api/generated';
import { ChallengePhoto } from '../../../shared/components/challenge-photo/challenge-photo';
import { StatusTag } from '../../../shared/components/status-tag/status-tag';

/**
 * One row on `/home`, for both the `open` and `past` buckets — branches purely on
 * `canScore`/`canPickColor`/`status`, never on role, per the frontend plan's Phase 6.
 * Purely presentational: emits, the parent owns every API call.
 */
@Component({
  selector: 'app-participant-challenge-card',
  imports: [ChallengePhoto, DatePipe, MatButtonModule, MatCardModule, StatusTag],
  templateUrl: './participant-challenge-card.html',
  styleUrl: './participant-challenge-card.scss'
})
export class ParticipantChallengeCard {
  protected readonly ChallengeStatus = ChallengeStatus;

  readonly challenge = input.required<ParticipantChallenge>();
  readonly pickableColors = input<PlateColor[]>([]);
  readonly ownColor = input<PlateColor | null>(null);
  readonly colorPickBusy = input(false);

  readonly score = output<string>();
  readonly results = output<string>();
  readonly pickColor = output<PlateColor>();
}
