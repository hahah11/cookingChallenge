# Stack the blind-scoring grid on phones (no horizontal scroll)

## Context
On `/challenges/:id/score` (BlindScoring, reached from the guest `/home?token=...` link) the ratings grid is
`minmax(80px,1fr) minmax(228px,2fr) minmax(228px,2fr)` inside `overflow-x: auto`. At 375px only the first plate
column is visible; the second (yellow) needs a horizontal scroll. Verified in the browser (375px iframe).

Goal: below 600px, show the plates one under the other instead of side by side:
Red header -> Red's 3 category rows, then Yellow header -> Yellow's 3 category rows. In each row the category
description stays on the left, the stars on the right. Desktop/tablet (>= 600px) is unchanged. Same behaviour
for open, closed (read-only) and revealed states, since it is one template.

## Files
- `frontend/src/app/features/challenges/blind-scoring/blind-scoring.html`
- `frontend/src/app/features/challenges/blind-scoring/blind-scoring.scss`
- `frontend/src/app/features/challenges/blind-scoring/blind-scoring.spec.ts`

No backend or API change. `star-rating` is untouched.

## Approach
The rows use `display: contents`, so every cell is a direct grid item. On mobile we reorder them with CSS `order`
instead of restructuring the DOM (desktop alignment stays as is).

**Template**
- Give the header plate cells `[style.--i]="$index"`.
- Give each rating cell `[style.--i]="$index"` (label index) and `[style.--r]` (category index, from the outer `@for`'s `$index`).
- Inside each rating cell, add `<span class="blind-scoring__cell-label" aria-hidden="true">{{ CATEGORY_LABELS[category] }}</span>`
  before `<app-star-rating>`. It is hidden on desktop and shows the description on the left on mobile.
  `aria-hidden` because the fieldset legend in `star-rating` already names it.
- Keep existing class names (`.blind-scoring__plate`, `__grid`, `__submit`, ...): the spec depends on them.

**SCSS** (new `@media (max-width: 599.98px)` block; the existing 640px star-size block is left alone)
- `.blind-scoring__grid`: `grid-template-columns: 1fr` (removes the 228px minimums, so nothing overflows).
- `.blind-scoring__category` and the empty "Category" header cell: `display: none` (label is now inside each cell).
- `.blind-scoring__plate`: `order: calc(var(--i) * 100)`; extra top margin for `--i` > 0 to separate the two groups.
- Rating cells: `order: calc(var(--i) * 100 + var(--r) + 1)`; `display: flex; align-items: center;
  justify-content: space-between; flex-wrap: wrap` so on very narrow screens (~320px) the label wraps above the stars
  instead of overflowing. Label uses `font: var(--mat-sys-body-medium)`.
- Rating cells need a selector: add `blind-scoring__rating` to that cell (desktop styles unaffected).
- Width check at 375px: content 375 - 32 (shell padding) - 16 (cell padding) = 327px; 5 stars (220px) + "Tellersprache" (~90px) fits.

## Tests
jsdom cannot evaluate media queries, so add to `blind-scoring.spec.ts`: each rating cell renders its category label
(`.blind-scoring__cell-label`) and the `--i` / `--r` bindings are set. Re-check existing tests for `getByText`-style
lookups of category names that could now match twice.

## Verification
1. `npm test` and `npm run lint` in `frontend/` (use IntelliJ MCP for tests if it reconnects; it failed to connect this session).
2. In Chrome: window resize is ignored by the extension, so preview `/challenges/0R9GP8X8P5GWY/score` in a 375px
   (and 320px, 599px, 600px) same-origin iframe: no horizontal scroll, Red block above Yellow block, labels left,
   colors intact. At 600px+ the side-by-side table is unchanged.
3. Check the closed/revealed state (disabled stars) shows the same stacked layout.

## Housekeeping
CLAUDE.md wants plans in `docs/cookingChallenge/plans/`. Plan mode restricts me to this file for now; on approval I'll
copy it to `docs/cookingChallenge/plans/blind-scoring-mobile-stacking-plan.md`.
