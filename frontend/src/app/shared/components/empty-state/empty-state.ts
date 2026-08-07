import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Invented in the M3 idiom — the prototype has no empty states at all, see
 * the frontend plan's Phase 3. Content projection carries an optional
 * primary action (e.g. "New challenge").
 */
@Component({
  selector: 'app-empty-state',
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon class="empty-state__icon" aria-hidden="true">{{ icon() }}</mat-icon>
      <p class="empty-state__message">{{ message() }}</p>
      <div class="empty-state__actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: var(--md-sys-spacing-3);
      padding: var(--md-sys-spacing-12) var(--md-sys-spacing-4);
      color: var(--mat-sys-on-surface-variant);
    }

    .empty-state__icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: var(--mat-sys-outline);
    }

    .empty-state__message {
      margin: 0;
      font: var(--mat-sys-body-large);
      max-width: 40ch;
    }

    .empty-state__actions:empty {
      display: none;
    }
  `
})
export class EmptyState {
  readonly icon = input('inbox');
  readonly message = input.required<string>();
}
