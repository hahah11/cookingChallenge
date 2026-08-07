import { Component, computed, input, model } from '@angular/core';

/**
 * Native `<input type="radio">` in a `<fieldset>`, visually hidden, stars as
 * labels — keyboard and screen-reader support for free. The prototype's
 * mouse-only `<span>★</span>` is not shippable, see the frontend plan's Phase 3.
 * Fill color is the plate color via `--plate-color`.
 */
@Component({
  selector: 'app-star-rating',
  templateUrl: './star-rating.html',
  styleUrl: './star-rating.scss'
})
export class StarRating {
  private static nextId = 0;

  readonly name = input<string>(`star-rating-${StarRating.nextId++}`);
  readonly label = input.required<string>();
  readonly max = input(5);
  readonly colorHex = input<string>();
  readonly disabled = input(false);
  readonly value = model<number | null>(null);

  // Descending so the DOM order is 5,4,3,2,1 — combined with row-reverse in
  // CSS, this lets the `~` general-sibling selector fill "this star and every
  // lower one" without JavaScript.
  protected readonly stars = computed(() =>
    Array.from({ length: this.max() }, (_, i) => this.max() - i)
  );

  protected select(v: number): void {
    if (this.disabled()) return;
    this.value.set(v);
  }
}
