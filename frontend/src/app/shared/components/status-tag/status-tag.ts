import { Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

import { ChallengeStatus } from '../../../core/api/generated';

/**
 * `Open` / `Revealed` tag, one place for both organizer and participant screens.
 * Both states are highlighted (colored) chips, matching the design's always-tinted
 * status chip: `Open` uses the M3 error role (design tints it red, its own hue-10
 * chip color isn't a system token, but error already sits at the same hue), `Revealed`
 * uses the project's custom success role — M3 has no built-in success color, see
 * `_overrides.scss`.
 */
@Component({
  selector: 'app-status-tag',
  imports: [MatChipsModule, MatIconModule],
  template: `
    <mat-chip-set>
      <mat-chip
        class="status-tag"
        [class.status-tag--revealed]="isRevealed()"
        [class.status-tag--open]="!isRevealed()"
        highlighted
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

    .status-tag--open {
      --mat-chip-elevated-selected-container-color: var(--mat-sys-error-container);
      --mat-chip-selected-label-text-color: var(--mat-sys-on-error-container);
      --mat-chip-selected-hover-state-layer-color: var(--mat-sys-on-error-container);
      --mat-chip-selected-focus-state-layer-color: var(--mat-sys-on-error-container);
      --mat-chip-flat-selected-outline-width: 0;
    }
  `
})
export class StatusTag {
  readonly status = input.required<ChallengeStatus>();

  protected readonly isRevealed = () => this.status() === ChallengeStatus.REVEALED;
}
