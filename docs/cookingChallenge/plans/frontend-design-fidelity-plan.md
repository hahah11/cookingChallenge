# Bring the frontend in line with the CookOff Material 3 design, and retire the old design-doc citation

## Status (2026-08-12 — Part F implemented)

The first fully **live** click-through audit against the running app (previous passes were all
code-comparison only, for lack of an organizer password and seed data — both now available). Found
and fixed one high-severity production bug (`/link-expired` threw and rendered blank in its one real
use case), three medium-severity UI bugs (a collapsed photo drop-zone, a dead-end Results page, an
unstyled Registration page), one intermittent visual bug (tab-nav ink bar), and one layout fidelity
gap (Challenge Detail header). Also *ruled out* a scary-looking empty-Rivalries-page finding as a
seed-data artifact rather than a live defect. **Part F below is now implemented** — see its own status
line and verification section.

## Status (2026-08-12 — Part E implemented)

A user report on 2026-08-12 surfaced two more issues on `/home` (shared by both guest and cook
roles): (1) still-`OPEN` challenges show up in the "Past challenges" list, and (2) the cook-facing
card doesn't match the canvas's `isCookHome` styling, most visibly right after a cook picks their
plate color. Investigation traced both to a single backend bucketing bug plus the already-flagged
Part C "out of scope" item. **Part E below is now implemented** — see its own verification section.

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

## Part E. Home bucketing bug + cook-card fidelity (2026-08-12) — **status: implemented 2026-08-12**

Reported by the user: (1) on both the guest and cook `/home` view, the "Past challenges" table
shows challenges that are still `OPEN`, not just `REVEALED` ones; (2) the cook home doesn't look
like the mockups, and (3) the state right after a cook picks their plate color doesn't match the
mockups either. All three trace back to one root cause plus the styling gap Part C already flagged
as out of scope.

### Root cause

`HomeService.execute()` (`backend/.../application/service/HomeService.java:46-51`) buckets by
**`status == OPEN && pendingAction`**, not by status alone:

```java
boolean pendingAction = (view.getCanScore() && !view.getSubmitted()) || view.getCanPickColor();
if (challenge.getStatus() == ChallengeStatus.OPEN && pendingAction) {
    open.add(view);
} else {
    past.add(view);
}
```

Any `OPEN` challenge with **no pending action** — a guest who already submitted, or a cook who
already picked their color — falls into `past`, mixed in with genuinely `REVEALED` challenges.
`participant-home.html:36-46`'s past list renders every row identically and unconditionally routes
clicks to `/challenges/:id/results` (`participant-home.ts:110-112`), but
`GetChallengeResultsService.execute()` (`backend/.../GetChallengeResultsService.java:57-58`) throws
a 409 (`ChallengeNotRevealedException`) for anything not actually `REVEALED`. That's issue (1).

This is also issue (3), confirmed against the domain model: a plain cook's `canScore` is
`isGuest(accountId) || accountId.equals(createdBy)` (`Challenge.java:185-187`) — never true for an
ordinary cook, independent of color-pick state. So the instant a cook picks their color,
`canPickColor` flips false, `canScore` was already false, `pendingAction` becomes false, and
`loadHome()`'s refetch (`participant-home.ts:131-148`, called right after a successful pick) drops
the challenge straight into `past` — losing the "You're plating **{color}**" swatch card
(`participant-challenge-card.html:22-26`) in favor of a bare text row, while the challenge is still
`OPEN` and scoring hasn't even started.

The canvas confirms the intended shape: `isCookHome`'s `cookOpen` loop (`CookingChallenge.dc.html`)
explicitly includes both the `needsColor` (picker) *and* `colorChosen` ("You're plating…") states
inside "Your open challenges" — a color-picked-but-still-open cook card never leaves the open
section. Likewise `isGuestHome`'s `guestOpen` keeps a `submitted` guest card (with the "Submitted —
editable until reveal" chip) in the open section, matching the already-built but currently
unreachable `canScore && submitted` branch in `participant-challenge-card.html:29-38`. Both
`cookPast`/`guestPast` rows only ever show a green "Revealed" chip — confirming `past` is meant to
be `REVEALED`-only.

### E1. Backend — bucket `open`/`past` by `status` alone

`backend/.../application/service/HomeService.java`:

```java
for (Challenge challenge : challengeRepository.findByParticipant(accountId)) {
    ScoreSubmission mySubmission = ...;
    ParticipantChallengeRestDto view = ChallengeModelMapper.toParticipantChallenge(...);
    if (challenge.getStatus() == ChallengeStatus.OPEN) {
        open.add(view);
    } else {
        past.add(view);
    }
}
```

Drop the now-unused `pendingAction` local. Update the class doc comment (lines 20-26), which
currently documents the buggy semantics as intentional ("`open` holds challenges with a pending
action... `past` holds every other challenge... already actioned but still OPEN, or REVEALED") —
rewrite to: `open` = every `OPEN` challenge the requester participates in (whether or not there's a
pending action); `past` = every `REVEALED` challenge.

`backend/.../HomeServiceTest.java`:
- `should_bucketUnsubmittedOpenChallengeAsOpen_andSubmittedOpenChallengeAsPast` (lines 57-79) locks
  in the old behavior — rename (e.g.
  `should_bucketAllOpenChallengesAsOpen_regardlessOfSubmission`) and change the assertions so both
  `notYetSubmitted` and `alreadySubmitted` land in `home.getOpen()` (size 2) and `home.getPast()` is
  empty.
- `should_bucketRevealedChallengeAsPast_evenWithoutASubmission` (lines 81-96) and
  `should_hideCookMapping_when_challengeNotRevealed` (lines 98-112) are unaffected — no change.
- Add a new case for the color-pick regression: a cook with `canPickColor` already exercised
  (color picked, challenge still `OPEN`) must still land in `home.open` — this is the scenario that
  was actually broken and had no test coverage.

### E2. Frontend — verify the already-built branches, fix stale doc comment

No template change needed: `participant-challenge-card.html`'s `canScore && submitted` ("Edit
scores", lines 29-38) and `ownColor()` ("You're plating…", lines 22-26) branches are already
correct and already have test coverage (`participant-challenge-card.spec.ts:60`,` :95`) — they were
simply unreachable until E1 ships. After E1, confirm both render in the `open` section as expected.

`participant-challenge-card.ts:10-19`'s doc comment currently asserts `HomeService` "only ever puts
OPEN challenges **with a pending action** into `open`" and that "every card here is provably OPEN
[so no status chip needed]" — the OPEN-only part stays true post-fix, but the "with a pending
action" qualifier is now wrong; update the comment to describe the corrected invariant.

### E3. Frontend — past list is now results-safe by construction

Once E1 ships, `home.past` is guaranteed `REVEALED`-only, so `participant-home.html:40`'s
unconditional `openResults(challenge.id)` click handler needs no extra guard — matches the
project's "don't add handling for scenarios that can't happen" rule. No code change required here;
`participant-home.spec.ts:192-206`'s existing REVEALED-past-row test keeps covering it.

Optional, low-priority polish (not required to fix the reported bug): the canvas's past-row chip
reads "Revealed — view results" (`CookingChallenge.dc.html`'s `guestPast`/`cookPast` `md-chip
md-chip--green`), not the generic `<app-status-tag>` currently used (`participant-home.html:42`,
which renders plain "Revealed"). Could swap in a static labeled chip for closer fidelity, but this
is cosmetic and separate from the visibility bug.

### E4. Cook-facing card style parity (issue 2, and the visual half of issue 3)

Picks up Part C's explicitly-flagged "out of scope" item: `participant-challenge-card` is shared
between guest and cook, but Part C's C1/C2 fixes (filled card + photo split) were applied
unconditionally, so cook cards currently render identically to guest cards. The canvas's
`isCookHome` card (`cookOpen` loop) is `md-card md-card--outlined`, `background:#f0f0f0`, **no
photo**, with color-picker swatch buttons `56×56` at `gap:16px` (current: `40×40` /
`--md-sys-spacing-3` ≈ 12px) and a `colorChosen` state showing a small color dot + the same
"You're plating **{color}**" copy already implemented.

Plan:
1. `participant-challenge-card.ts` — add `readonly isCook = computed(() => this.challenge().canPickColor || this.ownColor() !== null);`.
   Backend-confirmed disjoint from guest state: `canPickColor` is only ever true when the requester
   is one of the two assigned cooks (`ChallengeModelMapper` — `myCookLabel != null`), never for a
   guest.
2. `participant-challenge-card.html` — bind `[appearance]="isCook() ? 'outlined' : 'filled'"` and
   `[class.participant-challenge-card--cook]="isCook()"` on the `mat-card`; wrap
   `<app-challenge-photo>` in `@if (!isCook())`.
3. `participant-challenge-card.scss` — add
   `.participant-challenge-card--cook { background: var(--mat-sys-surface-variant); }` (a real M3
   token standing in for the canvas's raw prototype hex `#f0f0f0`, same reasoning as the "Open"
   status-chip color decision earlier in this doc — confirm visually which surface-role token reads
   closest to `#f0f0f0` before finalizing). Bump `.participant-challenge-card__swatch` (the
   color-picker variant only — leave the `.participant-challenge-card__own-color .participant-challenge-card__swatch`
   24px override alone) to `56px` and its parent `.participant-challenge-card__swatches` gap to
   `var(--md-sys-spacing-4)` (16px).
4. `participant-challenge-card.spec.ts` — add cases asserting a `canPickColor`/`ownColor` card gets
   `appearance="outlined"` and renders no `<app-challenge-photo>`, and a plain guest card keeps
   `appearance="filled"` with a photo present.

**Confirmed out of scope, not part of this fix:** the canvas gives cook-home and guest-home
different page-level headings ("Your open challenges" vs. "Open scoring requests") and kickers
(`cookName` vs `guestName`/"Hi, {name}" wording is the same, only the section title differs) — the
app deliberately merged these into one shared screen per Part C/`participant-home.ts:21-25`'s own
doc comment, and un-merging the heading text per role is a separate decision this fix doesn't make.

### Verification — Part E, done 2026-08-12
1. Applied E1 to `HomeService.java` (bucket by `ChallengeStatus` alone, dropped `pendingAction`,
   rewrote the class doc comment) and `HomeServiceTest.java` (renamed
   `should_bucketUnsubmittedOpenChallengeAsOpen_andSubmittedOpenChallengeAsPast` →
   `should_bucketAllOpenChallengesAsOpen_regardlessOfSubmission`, now asserting both land in
   `open`/`past` is empty; added `should_bucketOpenChallengeAsOpen_evenAfterCookAlreadyPickedColor`
   reproducing the color-pick regression with `Challenge.pickColor(...)`).
2. Applied E2 (doc comment fix only — the "Edit scores"/"You're plating…" branches needed no
   template changes, they just became reachable) and E4 (`isCook` computed signal on
   `participant-challenge-card.ts`; `[appearance]`/`[class.participant-challenge-card--cook]`
   binding and `@if (!isCook())`-wrapped photo in the template; `--cook` gray-background rule via
   the already-used-elsewhere `--mat-sys-surface-variant` token, and swatch size bumped
   40px→56px/gap 12px→16px in the scss). E3 needed no code change, confirmed by inspection.
3. Added 3 new cases to `participant-challenge-card.spec.ts` covering the outlined/no-photo cook
   style (picker and already-picked states) and the unchanged filled/photo guest style.
4. `cd backend && ./gradlew test` — all tests green, including the 2 rewritten/new `HomeServiceTest`
   cases.
5. `cd frontend && npx ng build` — clean (pre-existing `qrcode` CommonJS warning only, unrelated).
   `npx ng test --watch=false` — 129/130 passing; the one failure
   (`error-interceptor.spec.ts`, non-UNAUTHENTICATED 401 handling) is the same pre-existing,
   unrelated failure documented in Parts C/D's verification sections. `npm run lint` clean.
6. Live check: not run — same missing-credentials gap noted throughout this document (no seeded
   cook/organizer account with in-progress challenge data in the dev DB). Verified instead by
   tracing the exact regression through the domain model (`Challenge.canScore`/`pickColor`) and by
   new test coverage on both ends of the stack.

---

## Part F. Live click-through audit against the mockup — **status: implemented 2026-08-12**

Every prior pass in this document (A–E) was verified by *reading* the mockup markup against the
Angular templates, because there was no seeded cook/guest/challenge data and no known organizer
password — this doc says so explicitly in at least five places above ("Live check: not run…"). That
gap is closed here: the user supplied the organizer password (`claude@claude.com`) and approved
creating throwaway test data. Both `ng serve` (4200) and the backend (8080, Postgres via Docker) were
already running.

This pass clicked through every route and dialog live in Chrome, cross-checked sizes/spacing/copy
against the mockup source (`CookingChallenge.dc.html` + its M3 tokens, pulled fresh via `DesignSync`),
and — where the UI made a claim that could be verified independently — went one level deeper via
direct Postgres queries (`docker exec backend-postgres-1 psql -U cookoff -d cookoff`) and
console/network inspection. That extra step caught two real bugs pure code-reading would have missed
(a CSS flexbox collapse, and an Angular Router input-binding gap that throws in production), and let
one scary-looking finding (an empty Rivalries page) be *ruled out* as a seed-data artifact rather than
a live defect — a distinction a code-only review can't make.

Created throwaway test data along the way: a new "Test Plate" / "Color Test Duel" challenge (Guest1
vs. Guest2, used to exercise the cook color-pick flow with no prior color assigned), and revealed the
previously-open "Schnitzel" challenge (Daniel Daniel now leads Michael Holzer 27-18, 1-0). Left in
place intentionally — this is exactly the cook/guest/challenge data every prior pass above complained
about missing, useful for future manual QA.

### F1. HIGH — `/link-expired` threw and rendered blank in its one real use case

`error-interceptor.ts`'s only real call site did `router.navigateByUrl(wasOrganizer ? '/login' :
'/link-expired')` with no `?kind=` query param — and nothing else in the codebase ever passed one
either (the `kind=qr` copy branch was unreachable in production). `LinkExpired.kind` is a signal
`input<'link'|'qr'>('link')`, but the app enables `provideRouter(routes, withComponentInputBinding())`
(`app.config.ts`), which sets `kind` to `undefined` — not the declared default — when the route has no
matching query param. `copy = computed(() => COPY[this.kind()])` then evaluated `COPY[undefined]`, and
`{{ copy().kicker }}` threw `TypeError: Cannot read properties of undefined (reading 'kicker')`
(confirmed live in the browser console). Result: only the `link_off` icon and "Back to start" button
rendered — kicker/headline/body were all empty, for every guest whose session died mid-visit.
`/link-expired?kind=qr` (param supplied explicitly) rendered pixel-perfect against the mockup, so only
the missing-default case was broken — and `link-expired.spec.ts`'s existing "shows the access-link
copy by default" test passed because it instantiates the component directly via `TestBed`, never
exercising the router's input-binding path.

**Fix:** `copy` now falls back explicitly — `computed(() => COPY[this.kind() ?? 'link'])` — and
`error-interceptor.ts` navigates with an explicit `?kind=link` for clarity. Added a new
`link-expired.spec.ts` test that navigates via `RouterTestingHarness` with
`withComponentInputBinding()` enabled (the same real path `errorInterceptor` uses) instead of
instantiating the component directly, to catch this class of bug in future.
Files: `frontend/src/app/features/auth/link-expired/link-expired.ts`,
`frontend/src/app/core/http/error-interceptor.ts` (+ both `.spec.ts`).

### F2. MEDIUM — New Challenge dialog's photo drop-zone collapsed to ~4px tall

`.new-challenge-dialog__photo` set `aspect-ratio: 4/3` but was a flex item inside
`.new-challenge-dialog__content { display:flex; flex-direction:column }` with the default
`flex-shrink:1` and no `min-height` — it collapsed to a 4px sliver (just the dashed border),
completely hiding the `add_a_photo` icon and "Drop a 4:3 photo…" prompt. Confirmed via
`getBoundingClientRect` (`height:0`) and confirmed the fix live: `flex-shrink:0` restores the correct
~385px height with the icon and prompt visible.
File: `frontend/src/app/features/challenges/new-challenge-dialog/new-challenge-dialog.scss`.

### F3. MEDIUM — Challenge Results page had no way back

`challenge-results.html` had zero `<a>`/`<button>` elements besides the status chip — confirmed via
DOM query. The mockup's `isGuestResults` block has a "← Home" link at the top. A guest/cook tapping a
past-challenge row landed here with no in-app way back except the browser's own back button.

**Fix:** added the same back-link pattern already used in `blind-scoring.html` (Part D) and
`challenge-detail.html` — `<a mat-button routerLink="/home"><mat-icon>arrow_back</mat-icon>Home</a>`.
Verified live: click navigates to `/home`. Added `.spec.ts` coverage and `provideRouter([])` to the
test module (now required since the template uses `routerLink`).
(Confirmed still-open, not new: the header shows generic "CookOff"/"Results" instead of dish
name/date — documented API-shape gap, `frontend-implementation-plan.md:396`, no frontend-only fix.)
File: `frontend/src/app/features/challenges/challenge-results/challenge-results.{ts,html,spec.ts}`.

### F4. MEDIUM — Public Registration page was missing the centered-card shell

The mockup wraps `isLogin`, `isRegister`, and `isExpired` all in the same
`min-height:100vh;display:flex;align-items:center;justify-content:center` + `md-card md-card--elevated`
"landing card" treatment. Organizer-login and link-expired both had it; Public Registration used the
generic in-app `<app-page-header kicker="CookOff" title="Register">` pattern instead, with bare,
un-carded form fields near the top-left of the page and a small `align-self:flex-start` submit button
— the page real guests land on straight from a scanned QR code read as unstyled next to the rest of
the app.

**Fix:** reused the centered-card shell pattern from `organizer-login.html`/`.scss` (host
`display:flex;align-items:center;justify-content:center;min-height:100dvh`, form wrapped in
`mat-card appearance="raised"`, 360px max-width) and made the submit button full-width. Verified live
against `/register?token=…` — now matches the login page's card treatment exactly.
Files: `frontend/src/app/features/register/public-registration/{public-registration.html,.scss}`.

### F5. LOW/MEDIUM — Organizer tab-nav ink bar intermittently lagged behind the active tab

`organizer-shell.html`'s `mat-tab-nav-bar` + `mat-tab-link [active]="rla.isActive"` is standard,
correctly-wired Angular Material — `aria-selected`/the `active` class moved to the right tab
immediately on navigation (confirmed via DOM query), but the visual ink-bar underline sometimes stayed
under the *previous* tab (reproduced 2 of 3 sequential tab-link clicks in one session; correct on a
fresh single click in another). Root cause not pinned down with certainty (the underline itself is a
per-tab CSS-class-driven MDC element with no separately-computed pixel position, so this may partly
have been transition-timing rather than a persistent desync) — applied a low-risk mitigation rather
than a deep Material-internals dig: on every `NavigationEnd`, dispatch a `resize` event (deferred via
`queueMicrotask`) to nudge Material's own ink-bar recalculation. Stress-tested live with 3+ rapid
sequential tab switches (including several fired in the same JS tick, no explicit waits) after the fix
— the underline landed under the correct tab every time.
File: `frontend/src/app/layout/organizer-shell/organizer-shell.ts`.

### F6. MEDIUM — Challenge Detail header: chip position + photo/text order

Mockup: text column (kicker, dish name **with the status chip inline right next to it**, subtitle) on
the left, photo on the right, ~even split. Live rendered the status chip via `<app-page-header>`'s
`actions` slot, which places it at the far top-right of the page — not next to the dish name — and
`<app-challenge-photo>` came *before* `.challenge-detail__meta` in the DOM with uneven flex ratios
(`flex: 1 1 240px` photo vs. `flex: 2 1 280px` meta), so the photo rendered on the left and text on
the right, reversed from the mockup.

**Fix (user confirmed: match the mockup over keeping the shared-PageHeader shortcut):** stopped
routing the status chip through `PageHeader`; it now renders inline next to the dish-name `<h1>`
inside a hand-built header block (kicker, title-row with chip, event subtitle, cook names), with the
photo swapped to come after the text column in the DOM and an ~even 1:1 flex split
(`flex: 1 1 280px` both sides). `PageHeader` import removed from `challenge-detail.ts` (no longer
used). Verified live on both an Open and a Revealed challenge — chip sits inline next to the dish
name, photo renders on the right.
Files: `frontend/src/app/features/challenges/challenge-detail/challenge-detail.{ts,html,scss}`.

### Already-known or intentional — checked live, no action taken

- **Organizer name missing from the top bar** — `organizer-shell.ts`'s own doc comment explains why
  (JWT has no name claim, no `/me` endpoint yet). Real gap vs. the mockup's `{{organizerName}}`, but a
  scoped decision, not an oversight.
- **Cook Home color-pick swatches are circles, not rounded squares** — the shared `.plate-swatch`
  utility (`styles/_plate-color.scss`, `border-radius: full`) is used consistently everywhere a plate
  color renders. A deliberate shared design-system choice; changing it means auditing every
  plate-color indicator in the app. Cook/guest merged-home-card styling itself (Part E's fix)
  reconfirmed correct live, both `needsColor` and `colorChosen` states, including the
  `colorConfirmOpen` dialog's copy matching the mockup verbatim.
- **New Challenge / Edit-participants dialogs render Cook A and Cook B side-by-side**, not stacked as
  in the mockup — space-saving, reads as intentional.
- **New/Edit Account dialog has an extra Password field** the mockup never modeled (it has no auth
  story at all) — functionally necessary.
- **Send-links dialog pre-checks only unsubmitted guests**, not "everyone" like the mockup — already
  called out in the component's own doc comment as a deliberate choice.
- **Rivalries page was empty despite an existing REVEALED challenge — turned out NOT to be a bug.**
  Revealing a *fresh* challenge live through the real UI correctly created a `cook_rivalries` row
  (confirmed via direct Postgres query) and the Rivalries list/detail pages rendered it exactly per
  the mockup's card structure. The one empty case traced to `test dish`, whose seed data almost
  certainly set `status=REVEALED` directly rather than going through the real reveal flow that fires
  the domain event — a seed-data quirk, not a live defect. No code change needed. Still true and worth
  a future product call: OPEN challenges never contribute to a rivalry pair at all (no `openCount`
  concept anywhere in the backend model), unlike the mockup which shows "X revealed · Y open".
- Everything already itemized as resolved/intentional in Parts A–E (trophy vs. crown emoji, guest list
  as a `<table>`, plate-color bars with text instead of bare color, etc.) — reconfirmed live wherever
  the relevant screen was visited this pass (History cards, Challenge Detail, Blind Scoring, Accounts,
  Rivalries, dialogs), no regressions found.

### Not fully covered this pass

- **Responsive/mobile reflow** — attempted via the browser tool's `resize_window` to 390×844; the tool
  changed `window.outerWidth` but the page's actual `innerWidth` never followed (a tab-group/
  automation quirk, not an app issue) — inconclusive. Recommend a manual follow-up (real window resize
  or Chrome DevTools device toolbar) on: the challenge-history grid
  (`repeat(auto-fill,minmax(260px,1fr))`), the rivalries grid, and the accounts-admin table (a
  4-column table with no documented narrow-viewport treatment).

### Verification — Part F, done 2026-08-12

1. Applied F1–F6 as described above.
2. `cd frontend && npx ng build` — clean (pre-existing `qrcode` CommonJS warning only, unrelated).
3. `npx ng test --watch=false` — 131/132 passing (up from 129/130: added the router-navigation test
   for F1 and the back-link test for F3). The one failure (`error-interceptor.spec.ts`,
   non-UNAUTHENTICATED 401 handling) is the same pre-existing, unrelated failure documented in every
   prior part's verification section.
4. `npm run lint` clean.
5. Live check: run, for the first time in this document's history. Logged in as organizer
   (`claude@claude.com`), created and revealed test data, and walked every route and every dialog —
   History, New Challenge, Challenge Detail (Open + Revealed) and all five of its dialogs, Accounts +
   New/Edit Account, Rivalries + Rivalry Detail, cook Home (`needsColor`/`colorChosen`), guest Home
   (open/past), Blind Scoring (submit flow), Challenge Results, Public Registration, Link Expired
   (`link`/`qr`), 404. Each of F1–F6 individually re-verified live after its fix (see each section
   above).

---

## Verification

1. `cd frontend && npx ng build` — stays under budget.
2. `npx ng test` — 120/120 passing as of this audit (down from 122 after deleting the two dead-branch tests for the removed "Revealed — view results" code path; one `qr-code` canvas test is occasionally flaky under jsdom, unrelated to design fidelity).
3. `npm run lint` clean.
4. Manual pass: logged in live as an organizer (`claude@claude.com`) and walked History → Accounts → Rivalries, confirming the navbar and button-color fixes render correctly. Did not click through Challenge Detail/dialogs live (no seed data in the dev DB) — those fixes are verified by code comparison against the canvas plus a clean build/lint/test run, not a live screenshot.
