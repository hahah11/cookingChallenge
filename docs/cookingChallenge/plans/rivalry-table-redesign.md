# Redesign head-to-head as a separate "Rivalry" table

## Context
The results table (`app-results-table`, used on the challenge detail page and the participant results page) currently shows head-to-head wins as a last `tfoot` row of the score table, followed by a headline sentence. The user wants that removed from the score table and shown as its own borderless "Rivalry" block below it: three label/value rows (cook 1, cook 2, draws), no column headers, wins shown as crown icons, draws as scale icons.

Decisions (confirmed with user):
- Headline sentence ("Alice leads Bob 3-1") is **removed**.
- Draws: **one scale icon per draw**, mirroring the crowns.

Frontend only; no API change (`RivalrySummary` already has `cookAWins`, `cookBWins`, `draws`).

## Changes

### `frontend/src/app/shared/components/results-table/results-table.html`
- Delete the head-to-head `<tr>` in `<tfoot>` (the `results-table__crowns` row).
- Delete `<p class="results-table__headline">`.
- Add below the score table a second table, e.g. `<table class="rivalry-table">`:
  - `<caption>` = visually hidden accessible name; a visible heading `<h3 class="rivalry-table__title">{{ 'resultsTable.rivalry' | transloco }}</h3>` above it (the "header 'Rivalry'"). No `<thead>`.
  - Two rows via `@for (cook of cookAssignments(); ...)` in the same order as the score-table columns: `<th scope="row">{{ cook.name }}</th>` + `<td>` with the crowns (`👑` repeated `winsFor(cook)`, `aria-hidden`) and a visually-hidden text reusing existing `resultsTable.winsOne/winsOther`.
  - Third row: `<th scope="row">{{ 'resultsTable.draws' | transloco }}</th>` + `<td>` with `<mat-icon>balance</mat-icon>` repeated `rivalry().draws` times (`aria-hidden`) + visually-hidden count text (`resultsTable.drawsOne/drawsOther`).
  - Crown emoji is the same `👑` already used in the score-table header, so the icons stay consistent (previous change).
- Icon choice: `balance` — the Material Icons scale icon. `index.html` loads the classic "Material Icons" font, which includes `balance`.

### `results-table.ts`
- Keep `winsFor()` and `crownsFor()` (already tested).
- Add `drawsRange = computed(() => Array.from({ length: this.rivalry().draws }))` (or a small helper) to drive the `@for` of scale icons.
- Remove the `headline` computed and the `RivalryText` injection/import — no longer used in this component (`RivalryText` itself stays; the rivalry pages use it — verify with grep before deleting nothing else).
- Update the class doc comment ("head-to-head row" → "separate Rivalry table").

### `results-table.scss`
- Remove `.results-table__crowns` and `.results-table__headline`.
- Add rivalry block styles: `border-collapse: collapse`, no borders on `th`/`td` (explicitly `border: none`, since the shared `.results-table th, td` border rule is scoped to `.results-table` and must not leak), left-aligned label, value cell with `font-size: 20px` for crowns, `letter-spacing: 1px`; scale icons sized ~20px, `vertical-align: middle`, colour `var(--mat-sys-on-surface-variant)`. Allow icons to wrap (`flex-wrap`/normal wrapping) so many wins don't overflow on phones. Spacing via existing `--md-sys-spacing-*` tokens. Match own layout only, never override Material internals (per fidelity feedback memory).

### i18n — `frontend/public/i18n/en.json` and `de.json` (`resultsTable` block)
- Add `rivalry`: "Rivalry" / "Rivalität" (DE: "Rivalität" — the rivalry pages already use this wording; check `rivalries.*` keys for the existing German term and reuse the same word).
- Add `draws`: "Draws" / "Unentschieden".
- Add `drawsOne`/`drawsOther`: "{{count}} draw" / "{{count}} draws"; DE "{{count}} Unentschieden" (both forms).
- Remove now-unused `headToHead` key (keep `winsOne/winsOther`, reused for screen readers; reword to drop "head-to-head" only if it reads wrong: "{{count}} win").
- Keep en/de key sets in sync.

### Tests — `results-table.spec.ts`
- Keep the crowns/winsFor test.
- Add: renders a "Rivalry" heading and a table with exactly three rows and no `thead`; row labels are Alice, Bob, Draws; Alice row has 3 crowns, Bob 1; draws row has N `mat-icon` `balance` (set `draws: 2` in a variant).
- Add: with `draws: 0`, draws row shows no scale icons.
- Remove/adjust any assertion on the headline (`Alice leads Bob 3-1` test) and the removed tfoot row.
- Check `challenge-detail.spec.ts` / `challenge-results` spec for assertions on the headline or head-to-head row and update.

## Process notes
- CLAUDE.md requires plans in `docs/cookingChallenge/plans/`; plan mode only allows this file, so on approval copy this plan there as `docs/cookingChallenge/plans/rivalry-table-redesign.md` before implementing.
- Prettier not enforced; only new files would be formatted (none here).

## Verification
1. `cd frontend && npx ng test --watch=false --include='src/app/shared/components/results-table/**/*.spec.ts'`, then the challenge-detail and challenge-results specs.
2. `npx ng build` (type check, unused-import errors).
3. Visual check against the running dev server (:4200, already running): open a revealed/closed challenge detail page in EN and DE; confirm the score table ends at the Total row, "Rivalry" block below with 3 borderless rows, crowns/scale icons, correct language, wrapping OK at phone width. Use the scratch-copy setup from memory only if backend data is needed; do not run DML on the `cookoff` DB.
