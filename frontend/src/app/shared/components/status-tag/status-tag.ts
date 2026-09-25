import { Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';

import { ChallengeStatus } from '../../../core/api/generated';

/**
 * `Open` / `Scoring closed` / `Revealed` tag, one place for both organizer and participant
 * screens. Every state is a highlighted (colored) chip, matching the design's always-tinted
 * status chip: `Open` uses the project's custom success (green) role, `Scoring closed` uses the
 * custom warning (yellow) role — M3 has no built-in success or warning color, see
 * `_overrides.scss` — and `Revealed` is black with white text, like the filled buttons.
 */
@Component({
  selector: 'app-status-tag',
  imports: [MatChipsModule, MatIconModule, TranslocoPipe],
  template: `
    <mat-chip-set>
      <mat-chip
        class="status-tag"
        [class.status-tag--open]="status() === ChallengeStatus.OPEN"
        [class.status-tag--closed]="status() === ChallengeStatus.CLOSED"
        [class.status-tag--revealed]="status() === ChallengeStatus.REVEALED"
        highlighted
        disableRipple
      >
        <mat-icon matChipAvatar aria-hidden="true">check</mat-icon>
        <span class="status-tag__label">{{ 'status.' + status() | transloco }}</span>
      </mat-chip>
    </mat-chip-set>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    mat-chip-set {
      pointer-events: none;
    }

    /* Same black/white as the filled buttons. */
    .status-tag--revealed {
      --mat-chip-elevated-selected-container-color: #1a1a1a;
      --mat-chip-selected-label-text-color: #fff;
      --mat-chip-selected-hover-state-layer-color: #fff;
      --mat-chip-selected-focus-state-layer-color: #fff;
      --mat-chip-with-icon-selected-icon-color: #fff;
      --mat-chip-flat-selected-outline-width: 0;
    }

    .status-tag--closed {
      --mat-chip-elevated-selected-container-color: var(--cc-color-warning-container);
      --mat-chip-selected-label-text-color: var(--cc-color-on-warning-container);
      --mat-chip-selected-hover-state-layer-color: var(--cc-color-on-warning-container);
      --mat-chip-selected-focus-state-layer-color: var(--cc-color-on-warning-container);
      --mat-chip-flat-selected-outline-width: 0;
    }

    .status-tag--open {
      --mat-chip-elevated-selected-container-color: var(--cc-color-success-container);
      --mat-chip-selected-label-text-color: var(--cc-color-on-success-container);
      --mat-chip-selected-hover-state-layer-color: var(--cc-color-on-success-container);
      --mat-chip-selected-focus-state-layer-color: var(--cc-color-on-success-container);
      --mat-chip-flat-selected-outline-width: 0;
    }
  `
})
export class StatusTag {
  readonly status = input.required<ChallengeStatus>();

  protected readonly ChallengeStatus = ChallengeStatus;
}
