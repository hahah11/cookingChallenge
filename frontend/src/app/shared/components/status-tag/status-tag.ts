import { Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

import { ChallengeStatus } from '../../../core/api/generated';

/**
 * `Open` / `Revealed` tag, one place for both organizer and participant screens.
 * `Open` is a plain outlined `mat-chip` (M3's default un-highlighted look);
 * `Revealed` reuses the same chip `highlighted`, repainted with the project's
 * custom success role — M3 has no built-in success color, see `_overrides.scss`.
 */
@Component({
  selector: 'app-status-tag',
  imports: [MatChipsModule, MatIconModule],
  template: `
    <mat-chip-set>
      <mat-chip
        class="status-tag"
        [class.status-tag--revealed]="isRevealed()"
        [highlighted]="isRevealed()"
        disableRipple
      >
        <mat-icon matChipAvatar aria-hidden="true">check_circle</mat-icon>
        <span class="status-tag__label">{{ isRevealed() ? 'Revealed' : 'Open' }}</span>
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

    .status-tag--revealed {
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

  protected readonly isRevealed = () => this.status() === ChallengeStatus.REVEALED;
}
