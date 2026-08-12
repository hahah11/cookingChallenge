# Bring the frontend in line with the CookOff Material 3 design, and retire the old design-doc citation

## Status (2026-08-12 — Parts C and D implemented)

Part B7 (Participant Home) was marked resolved on 2026-08-11, but a live comparison against
`http://localhost:4200/home?token=...` on 2026-08-12 found the guest-facing card layout itself
was never actually brought in line with the canvas — only copy/section-heading issues were
caught in the original pass, not the card's structure/style. **Part C (C1–C6) is now implemented**
— see its own status line below for what changed and how it was verified.

Same day, a follow-up audit of the blind-scoring screen (B8 in the historical record below had
only checked copy/success-state, not structure) found four more gaps against the canvas's
`isScoring` block. **Part D (D1–D4) is now implemented** — see its own section below.

## Status (2026-08-11 re-audit)

Part A (doc citation cleanup) and nearly all of Part B's findings below are **done** — verified by re-reading every changed template against the live canvas markup (`DesignSync get_file`, `CookingChallenge.dc.html`, project `eddd583d-a944-4319-a1a6-c853a5f2fe57`) rather than trusting the original audit's notes, which had already gone stale in places (e.g. B5 #21 claimed the "New account" flow didn't exist; it does, and works). Sections B1–B11 below are kept as a historical record of what was found and fixed, each item marked with its current status.

Three things came out of *this* pass, all now resolved:

1. **Fixed**: filled buttons were brand-red instead of the design's black (`cc-black-btn` promoted from an opt-in class to the global `mat-flat-button` default), and several secondary actions used `mat-tonal-button` where the design specifies outlined (`mat-stroked-button`) — Send links, Edit cooks & guests, Registration QR code, Unreveal challenge, Edit scores, plus New account was tonal instead of filled.
2. **Fixed**: the organizer top nav was a single `mat-toolbar` row; the design has two stacked bars — a plain top-app-bar (brand + Log out) and a separate full-width tabs row underneath with a primary-color underline on the active tab. Rebuilt `organizer-shell` on `mat-tab-nav-bar`.
3. **Fixed**: the "Open" status chip had no color at all (design tints it red) — `status-tag.ts` now highlights both states, "Open" via the M3 error-container role.

Two more were resolved by decision rather than by code:

4. Organizer-login's two design-only preview links ("Preview a guest's/cook's personalized link →") — **decided: mockup-only, not building**. These are the design canvas's own internal navigation aid for a static prototype with no real access-link flow; production cooks/guests only ever arrive via an emailed link.
5. `participant-challenge-card`'s "Revealed — view results" tonal button — **confirmed dead code, removed**. `HomeService.java` only ever puts `OPEN` + pending-action challenges into `home.open` (everything else, including all `REVEALED` challenges, lands in `.past`), and this component is only ever rendered from `home.open`, so the branch could never fire. Deleted the branch, its now-unused `results` output, the `ChallengeStatus` import, and the two tests that covered it.

## Resolved-today details

### "Open" status chip color

`shared/components/status-tag/status-tag.ts` previously only styled the `isRevealed()` branch (green, via `--cc-color-success-container`); `!isRevealed()` ("Open") was a bare, unstyled `mat-chip` — M3's neutral outlined default. The design's `statusChipCls(revealed)` helper (`CookingChallenge.dc.html:721`) returns `md-chip--red` for Open — a raw hex (`#fbe0dc`/`#8a2a10`) that isn't a real design token but sits at the same hue (10°) the app's own M3 error role already uses. Rather than invent a one-off custom color to pixel-match a hand-picked prototype hex, both chip states are now `highlighted` and colored via CSS custom properties: `.status-tag--revealed` keeps the existing success-green treatment, `.status-tag--open` uses `--mat-sys-error-container`/`--mat-sys-on-error-container`. Since `<app-status-tag>` is shared, this fixes the chip everywhere it appears: challenge-history cards, challenge-detail, rivalry-detail, participant-home.

### Dead "Revealed — view results" branch

Design shows this as a `md-chip md-chip--green` inside a clickable row (`CookingChallenge.dc.html:452`, cook-home past list), not a button — the current code rendered it as `mat-tonal-button`. Traced the reachability question from the earlier report: `HomeService.execute()` puts a challenge in `open` only when `status == OPEN && pendingAction`; every `REVEALED` challenge always lands in `past`. `participant-challenge-card` is only ever instantiated from `home.open` in `participant-home.html`. So the branch was unreachable in production — removed rather than restyled, per "don't add handling for scenarios that can't happen."

---

## Historical record — original findings, verified status as of 2026-08-11

Findings from the original 6-item pass are marked **[core]**. Everything else was from a larger recheck against the actual canvas markup. Status column added in this re-audit.

### B1. Organizer Login (`features/auth/organizer-login/`) — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 1 | Kicker read "CookingChallenge" instead of "CookOff" | ✅ Fixed |
| 2 | No card surface, not centered | ✅ Fixed — `mat-card appearance="raised"`, centered via host flex |
| 3 | Note rendered before the form fields instead of after | ✅ Fixed — order now matches |
| 4 | No placeholders on email/password | ✅ Fixed |

### B2. Challenge History (`features/challenges/challenge-history/`, `shared/components/challenge-card/`) — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 5 | `font-weight: 600` on `.challenge-card__cook--winner` **[core]** | ✅ Fixed → `500` |
| 6 | Missing hover/ripple state layer on the clickable card **[core]** | ✅ Fixed — `matRipple` present |
| 7 | ⚠ Trophy/bold winner name shown on the History grid card, which the design doesn't do until the detail view | Kept intentionally, documented in `design-reference.md`'s deviations table — no action |
| 8 | Missing per-cook plate-color tint on cook names | ✅ Fixed — `plate-tint` class + `--plate-color` applied |
| 9 | Status chip missing leading check icon | ✅ Fixed — `check_circle` present in `status-tag` |

### B3. Challenge Detail (`features/challenges/challenge-detail/`) — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 10 | No back button | ✅ Fixed — "Back to challenges" |
| 11 | Icon mismatches (Send links, QR, Unreveal missing icon/label) | ✅ Fixed — `forward_to_inbox`, `qr_code_2`, `undo` + "Unreveal challenge" all correct |
| 12 | Section order inverted (actions before guest list) | ✅ Fixed — guest list now first |
| 13 | Missing "Guest list" heading | ✅ Fixed |
| 14 | Guest status was a plain `<span>` in a `<table>`, not a chip | ✅ Fixed (chip-style with icon) — still a `<table>` not a flex-row-list, kept intentionally as an accessibility improvement, documented in `design-reference.md` |
| 15 | "Links sent ✓" used a raw glyph and success-green color **[core + nuance]** | ✅ Fixed — `check_circle` icon, own line, `--mat-sys-primary` color |
| 16 | "Revealed!" not all-caps | ✅ Fixed — `text-transform: uppercase` |
| 17 | Reveal overlay missing the pill background | ✅ Fixed |
| 18 | Trophy icon vs. design's 👑 crown emoji | Kept trophy intentionally, documented in `design-reference.md` — no action |
| 19 | Missing head-to-head wins crown row **[core]** | ✅ Fixed — present in `results-table` |

### B4. Dialogs — ✅ all resolved

- **New Challenge**: visible "Rivalry photo" label ✅, placeholder text now matches design verbatim ✅.
- **Edit cooks & guests**: matches design; no photo-replace capability, which is a real gap but has no design source to copy from either (PRD-only requirement) — no action possible from this audit.
- **Send links**: title "Send / resend links" ✅, hint copy matches ✅, recipient list now includes cooks with a role label (`Cook`/`Guest`) *and* an "already submitted" badge — resolves the previous ⚠ needs-product-call by doing both.
- **Registration QR**: title "Scan to register" ✅, body copy with challenge-name interpolation ✅.
- **Reveal confirm**: title "Reveal this challenge?" ✅, confirm label "Yes, reveal" ✅. Body text intentionally still differs (documented Phase 5 fix, not reverted) — correct as-is.
- **Unreveal confirm**: title "Unreveal this challenge?" ✅, body now includes both previously-missing clauses ✅, button "Yes, unreveal" ✅.
- **Color-pick confirm**: title "Plate under {color}?" ✅, confirm label "Yes, choose {color}" ✅.
- **Account create/edit**: create mode now exists (`isCreate`, dynamic title/submit label matching design's `acctDialogTitle`/`acctDialogSubmitLabel` logic exactly) ✅ — resolves the ⚠ needs-product-call; a real "New account" flow was in fact wanted and is now built.

### B5. Accounts Admin (`features/accounts/accounts-admin/`) — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 20 | Missing "Admin" kicker | ✅ Fixed |
| 21 | ⚠ Missing "New account" button/flow entirely | ✅ Resolved — flow now exists and works (verified live) |
| 22 | Edit control icon-only | ✅ Fixed — visible "Edit" text label |
| 23 | Roles rendered as comma-joined string | ✅ Fixed — chips |

### B6. Rivalries (`features/rivalries/rivalry-list/`, `rivalry-detail/`) — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 24 | Title "Rivalries" vs. design's "Cook rivalries" | ✅ Fixed |
| 25 | Missing challenges-count kicker line on list cards | ✅ Fixed |
| 26 | Grid `minmax(260px,…)` vs. design's `280px` | ✅ Fixed |
| 27 | Missing "← All rivalries" back button | ✅ Fixed |
| 28 | ⚠ Flat `<ul>/<li>` challenge list instead of elevated photo cards | ✅ Resolved — now `mat-card` cards with photo, dish name, status chip, outcome label, matching the design's layout |

### B7. Participant Home (`features/home/participant-home/`, `participant-challenge-card/`) — ✅ resolved except one open item

| # | Finding | Status |
|---|---|---|
| 29 | Shortened section headings ("Open" / "Past") | ✅ Fixed — full text restored |
| 30 | "Past" rendered as full photo cards instead of compact rows | ✅ Fixed — dedicated compact-row template |
| — | "Revealed — view results" chip-vs-button on `participant-challenge-card` | Open — see "Open findings" #3 above |

### B8. Blind Scoring (`features/challenges/blind-scoring/`) — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 31 | No success state at all | ✅ Fixed — full `scoringSubmitted` branch (🎉, headline, body, Back to home) |
| 32 | Submit button label hardcoded | ✅ Fixed — dynamic `submitButtonLabel()` |
| 33 | Missing instructional paragraph | ✅ Fixed |

### B9. Challenge Results (`features/challenges/challenge-results/`) — ✅ resolved (except a known, unfixable API gap)

| # | Finding | Status |
|---|---|---|
| 34 | Missing dish name/date in header | Still open — already-documented API-shape gap (`frontend-implementation-plan.md:396`), no frontend-only fix possible |
| 35 | Missing "Revealed" chip | ✅ Fixed |
| 36 | Missing challenge photo | ✅ Fixed |

### B10. Public Registration (`features/register/public-registration/`) — ✅ resolved

| # | Finding | Status |
|---|---|---|
| 37 | Success state was a plain `<p>` instead of a card with 🎉/headline | ✅ Fixed |

### B11. Shared components — ✅ all resolved

| # | Finding | Status |
|---|---|---|
| 38 | `font-weight: 600` on organizer-shell active link **[core]** | ✅ Fixed (superseded — organizer-shell was rebuilt on `mat-tab-nav-bar` today, which uses Material's own active-tab treatment) |
| 39 | `star-rating` used a raw `★` glyph **[core]** | ✅ Fixed — Material Symbols `star` with `FILL` toggle |
| 40 | organizer-shell nav links missing ripple **[core]** | ✅ Fixed (superseded — `mat-tab-link` has its own ripple) |

---

## Part C. Guest/Participant Home card fidelity (2026-08-12) — **status: implemented 2026-08-12**

Re-audited by comparing the live app (`http://localhost:4200/home?token=...`, screenshot taken
2026-08-12) against the canvas's `isGuestHome` block (`CookingChallenge.dc.html`, project
`eddd583d-a944-4319-a1a6-c853a5f2fe57` — `<sc-if value="{{ isGuestHome }}">…</sc-if>`) side by
side. Current files: `features/home/participant-home/participant-home.html`/`.scss`,
`features/home/participant-challenge-card/participant-challenge-card.html`/`.scss`.

The screen-level structure (header, "Open scoring requests" / "Past challenges" sections, empty
state copy) already matches — B7's original pass got that right. What it missed is the **open
challenge card** itself, which currently looks nothing like the canvas:

| # | Canvas (`isGuestHome`) | Live app (was) | Fix | Status |
|---|---|---|---|---|
| C1 | One card per row, full width, **horizontal split**: content on the left (~50%), photo on the right (~50%, `aspect-ratio:4/3`) | `participant-home__grid` is a `repeat(auto-fill, minmax(260px,1fr))` **grid** of narrow cards; each card stacks photo-on-top, content-below (`app-challenge-photo` precedes `mat-card-content` in the DOM, no row layout) | Change `.participant-home__grid` to a single-column vertical list (`flex-direction: column`); restructure `participant-challenge-card.html` into a flex row with content first, `app-challenge-photo` second, each `flex:1`, photo `aspect-ratio: 4/3` | ✅ Fixed |
| C2 | `md-card md-card--filled` | `<mat-card appearance="outlined">` | Change to `appearance="filled"` | ✅ Fixed |
| C3 | No status chip at all on open cards — section heading ("Open scoring requests") already conveys it | `<app-status-tag [status]="challenge().status" />` unconditionally rendered, always showing "Open" | Remove — per B7/design-reference.md's own established finding, `HomeService` only ever puts `status === OPEN` challenges into `home.open`, and this component is only ever instantiated from `home.open`, so the chip is provably always redundant here, same "unreachable variance" reasoning already used to remove the dead "Revealed — view results" branch | ✅ Fixed — `StatusTag` import dropped too |
| C4 | `cc-kicker` reads **"Your personalized link"** | `<app-page-header kicker="CookOff" .../>` — the generic brand kicker used elsewhere | Add a page-specific kicker override, `kicker="Your personalized link"`, on `participant-home.html`'s `<app-page-header>` | ✅ Fixed |
| C5 | "Submitted — editable until reveal" is a real chip (`md-chip md-chip--selected`) on its own line, **above** the score/edit button | Plain `<span class="participant-challenge-card__submitted-tag">` (colored text, not a chip), sitting **inline beside** "Edit scores" in a `flex-wrap` row | Render as a `mat-chip` (`highlighted disableRipple`, no custom color override — same treatment the app already gives generic "selected" chips elsewhere, e.g. accounts-admin role chips), stacked above the button in a column | ✅ Fixed |
| C6 | Section title `md-typescale-title-small` | `.participant-home__section-title { font: var(--mat-sys-title-medium); }` | Change to `--mat-sys-title-small` | ✅ Fixed |

**Confirmed intentional, not a fix:** the canvas's "← Organizer login" back-link at the top of
`isGuestHome` is the same class of prototype-internal screen-switcher already decided
mockup-only for organizer-login's two "Preview a guest's/cook's personalized link" buttons (see
item #4 in the "Resolved-today" section above) — a guest arriving via a real emailed link has no
"organizer login" to go back to in production. No action.

**Out of scope for this pass:** `participant-challenge-card` is also used for the cook-facing
case (`canPickColor` swatch branch) — a deliberate merge of the canvas's separate `isGuestHome`/
`isCookHome` cards into one adaptive component, not something to unmerge. The canvas's
`isCookHome` card style differs again (`md-card--outlined`, gray background, no photo at all) —
whether the merged component should visually flex between the two canvas styles per-role, or
settle on one, wasn't audited here and would need its own pass with a cook-role test account.

### Verification — Part C, done 2026-08-12
1. Applied C1–C6 to `participant-home.html`/`.scss` and `participant-challenge-card.html`/`.scss`.
2. `participant-home.spec.ts` / `participant-challenge-card.spec.ts` needed no assertion changes —
   neither had a test tied to the removed status chip or the old photo-then-content DOM order, and
   the `.participant-challenge-card__submitted-tag` selector still resolves (kept on the new
   `mat-chip`).
3. `npx ng test` — 127/128 passing; the one failure (`error-interceptor.spec.ts`, non-UNAUTHENTICATED
   401 handling) reproduces identically on `main` before this change (confirmed via `git stash`) —
   pre-existing, unrelated to design fidelity. `npx ng build` and `npm run lint` both clean.
4. Live check: not run. No known credentials for an organizer account with seed challenge/guest
   data in the dev DB (same gap already noted for Challenge Detail/dialogs in the "Verification"
   section below) — guessing a password to obtain one is out of bounds. Verified instead by close
   structural comparison against the canvas's `isGuestHome` markup (`CookingChallenge.dc.html`)
   pulled directly via `DesignSync get_file`, matching flex layout, spacing tokens (`gap:8px` →
   `--md-sys-spacing-2`, `gap:12px`/`margin-bottom:24px` → `--md-sys-spacing-3`/`-6`), and the
   `aspect-ratio:4/3` photo split verbatim.

---

## Part D. Blind Scoring screen fidelity (2026-08-12) — **status: implemented 2026-08-12**

B8's original pass (see historical record above) only checked the success state, the submit
button label, and the instructional paragraph's presence — not the screen's structure. Re-audited
by comparing `features/challenges/blind-scoring/blind-scoring.html`/`.scss` against the canvas's
`isScoring` → `scoringNotSubmitted` block (`CookingChallenge.dc.html`, pulled fresh via
`DesignSync get_file` against project `eddd583d-a944-4319-a1a6-c853a5f2fe57`).

| # | Canvas (`scoringNotSubmitted`) | Live app (was) | Fix |
|---|---|---|---|
| D1 | "← Home" text button (`arrow_back` + "Home") above the kicker/title | No back link anywhere in the scoring form state | Added `<a mat-button routerLink="/home">` with `arrow_back` `mat-icon`, matching the existing `challenge-detail__back`/`rivalry-detail__back` convention used elsewhere in the app |
| D2 | `<hr class="md-divider">` between the instructions paragraph and the scoring grid | No divider | Added `<mat-divider>` (same component `organizer-login` already uses), with matching `margin-bottom:20px` (`--md-sys-spacing-5`) on both the paragraph above it and itself, mirroring the canvas's inline styles |
| D3 | Grid `gap:12px 16px` (row-gap/col-gap) | Uniform `gap: var(--md-sys-spacing-2)` (8px) | Changed to `gap: var(--md-sys-spacing-3) var(--md-sys-spacing-4)` (12px/16px) |
| D4 | Submit button `width:100%;margin-top:24px` | No `.blind-scoring__submit` rule existed at all — button sized to content, no top margin | Added the missing rule: `width: 100%; margin-top: var(--md-sys-spacing-6)` |

**Also tightened, minor:** `.blind-scoring__category` was `label-large` (bold-ish), design and the
sibling `results-table` component both use plain `body-medium` for row-header text — changed to
match.

**Confirmed intentional, not a fix:** the canvas's plate-column header is a bare color bar with no
text (`background:{{ colorHex }};height:8px`); the real app instead renders a solid color box with
the plate's name ("Red"/"Yellow") — required by this codebase's own accessibility rule in
`styles/_plate-color.scss` ("Never conveys meaning by color alone — pair every tint with text or an
icon (WCAG AA)"), and explicitly locked in by an existing test
(`blind-scoring.spec.ts`: "renders a plate-color bar per dish, with a visible name — never color
alone"). Matching the canvas here would regress accessibility, so left as-is — same class of
deliberate deviation as the trophy-icon-vs-crown-emoji and table-vs-flex-list decisions already
logged in `design-reference.md`.

### Verification — Part D, done 2026-08-12
1. Applied D1–D4 to `blind-scoring.html`/`.ts`/`.scss`.
2. `blind-scoring.spec.ts` needed no changes — none of its 8 tests assert on the back link,
   divider, or submit-button CSS, and the plate-color-with-name test still passes untouched.
3. `npx ng test` — 127/128 passing, same single pre-existing failure as Part C
   (`error-interceptor.spec.ts`, confirmed unrelated via `git stash`). `npx ng build` and
   `npm run lint` both clean.
4. Live check: not run, same missing-credentials gap as Part C. Verified by structural comparison
   against the canvas's `isScoring` markup pulled directly via `DesignSync get_file`.

---

## Verification

1. `cd frontend && npx ng build` — stays under budget.
2. `npx ng test` — 120/120 passing as of this audit (down from 122 after deleting the two dead-branch tests for the removed "Revealed — view results" code path; one `qr-code` canvas test is occasionally flaky under jsdom, unrelated to design fidelity).
3. `npm run lint` clean.
4. Manual pass: logged in live as an organizer (`claude@claude.com`) and walked History → Accounts → Rivalries, confirming the navbar and button-color fixes render correctly. Did not click through Challenge Detail/dialogs live (no seed data in the dev DB) — those fixes are verified by code comparison against the canvas plus a clean build/lint/test run, not a live screenshot.
