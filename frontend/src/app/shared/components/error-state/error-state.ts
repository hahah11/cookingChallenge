import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Invented in the M3 idiom — the prototype has no error states at all, see
 * the frontend plan's Phase 3.
 */
@Component({
  selector: 'app-error-state',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="error-state" role="alert">
      <mat-icon class="error-state__icon" aria-hidden="true">error</mat-icon>
      <p class="error-state__message">{{ message() }}</p>
      @if (retryable()) {
        <button mat-tonal-button type="button" (click)="retry.emit()">Try again</button>
      }
    </div>
  `,
  styles: `
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: var(--md-sys-spacing-3);
      padding: var(--md-sys-spacing-12) var(--md-sys-spacing-4);
    }

    .error-state__icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: var(--mat-sys-error);
    }

    .error-state__message {
      margin: 0;
      font: var(--mat-sys-body-large);
      color: var(--mat-sys-on-surface-variant);
      max-width: 40ch;
    }
  `
})
export class ErrorState {
  readonly message = input.required<string>();
  readonly retryable = input(false);
  readonly retry = output<void>();
}
