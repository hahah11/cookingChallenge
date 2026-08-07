import { Component, computed, input } from '@angular/core';

/**
 * Invented in the M3 idiom — the prototype has no loading states at all, see
 * the frontend plan's Phase 3. `prefers-reduced-motion` is honoured globally
 * via `_overrides.scss`, not re-implemented here.
 */
@Component({
  selector: 'app-loading-skeleton',
  template: `
    <div class="loading-skeleton" role="status" aria-live="polite">
      <span class="cc-visually-hidden">Loading…</span>
      @for (line of lineIndexes(); track line) {
        <div class="loading-skeleton__line" [class.loading-skeleton__line--card]="variant() === 'card'" aria-hidden="true"></div>
      }
    </div>
  `,
  styles: `
    .loading-skeleton {
      display: flex;
      flex-direction: column;
      gap: var(--md-sys-spacing-2);
    }

    .loading-skeleton__line {
      height: 16px;
      border-radius: var(--md-sys-shape-corner-small);
      background: linear-gradient(
        90deg,
        var(--mat-sys-surface-container) 25%,
        var(--mat-sys-surface-container-high) 50%,
        var(--mat-sys-surface-container) 75%
      );
      background-size: 200% 100%;
      animation: loading-skeleton-shimmer 1.5s ease-in-out infinite;
    }

    .loading-skeleton__line--card {
      height: 220px;
      border-radius: var(--md-sys-shape-corner-medium);
    }

    @keyframes loading-skeleton-shimmer {
      0% {
        background-position: 200% 0;
      }
      100% {
        background-position: -200% 0;
      }
    }
  `
})
export class LoadingSkeleton {
  readonly lines = input(3);
  readonly variant = input<'text' | 'card'>('text');

  protected readonly lineIndexes = computed(() => Array.from({ length: this.lines() }, (_, i) => i));
}
