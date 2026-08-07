import { Component, input } from '@angular/core';

/** Kicker + h1 + optional action slot, used at the top of every feature screen. */
@Component({
  selector: 'app-page-header',
  template: `
    <header class="page-header">
      @if (kicker()) {
        <p class="page-header__kicker">{{ kicker() }}</p>
      }
      <div class="page-header__row">
        <h1 class="page-header__title">{{ title() }}</h1>
        <div class="page-header__actions">
          <ng-content select="[actions]" />
        </div>
      </div>
    </header>
  `,
  styles: `
    .page-header {
      display: flex;
      flex-direction: column;
      gap: var(--md-sys-spacing-1);
      margin-bottom: var(--md-sys-spacing-6);
    }

    .page-header__kicker {
      margin: 0;
      font: var(--mat-sys-label-medium);
      color: var(--mat-sys-primary);
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .page-header__row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--md-sys-spacing-4);
    }

    .page-header__title {
      margin: 0;
      font: var(--mat-sys-headline-medium);
    }

    .page-header__actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--md-sys-spacing-3);
    }

    .page-header__actions:empty {
      display: none;
    }
  `
})
export class PageHeader {
  readonly kicker = input<string>();
  readonly title = input.required<string>();
}
