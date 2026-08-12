# Frontend Pixel-Fidelity Sweep — Plan

**Status:** Part 0 (recon) done 2026-08-12 — see its section for the navigation model and gotchas.
Screen checklist (items 1–26) not started. This is a checklist plan, meant to be worked one item
at a time (by separate agent sessions if useful) rather than in one pass. Each numbered item under
"Screen checklist" is self-contained enough to hand to a fresh agent with just a pointer to this
file — they no longer need to redo Part 0's discovery work, just read it.

**Relationship to other docs:**
- [`frontend-design-fidelity-plan.md`](frontend-design-fidelity-plan.md) is the running *record*
  of every fidelity finding and fix to date (Parts A–F), including several **deliberate,
  documented deviations** from the mockup that must not be reverted while doing this sweep (see
  "Ground rules" below). This new plan is the *forward-looking task list* for finishing the job;
  completed items here should be logged there too, as a new "Part G" (see "Logging completion").
- [`design-reference.md`](../design-reference.md) points at the mockup sources.

## Why this plan exists

Prior fidelity passes (Parts A–F of the other doc) mostly compared the mockup's HTML/CSS *source*
against the Angular templates by reading both and reasoning about whether they'd render the same.
That catches structural bugs (missing chip, wrong section order) well, but is too imprecise for
"the layout/size/arrangement doesn't quite match" complaints — it can't catch "gap is 8px instead
of 12px" or "this button is a different width than the mockup's" without actually rendering both
and measuring. This plan's methodology is **render both, then measure both** — screenshots plus
`getComputedStyle()`/`getBoundingClientRect()` diffs — not read-and-guess.

## Scope & fidelity-depth definition (decided with the user 2026-08-12)

**Match exactly:**
- Layout **arrangement** — flex/grid direction, DOM/visual order of elements, which elements are
  siblings vs. nested, column/row structure.
- **Size** of text — which type-scale step is used for each piece of copy (headline vs. title vs.
  body vs. label, and large/medium/small within that), explicit font-size/line-height where the
  mockup sets one directly.
- **Size** of buttons, cards, images, icons, swatches, and other elements *as arranged by the
  app's own CSS* — width/height rules, aspect ratios, grid-template-columns, explicit gaps set in
  our own `.scss` files.

**Do not touch:** Angular Material's own internal per-component CSS — the padding, min-height,
internal spacing baked into `mat-button`, `mat-card`, `mat-chip`, etc.'s own Material Design
Components (MDC) implementation. If a button's height comes from Material's own `.mdc-button`
rules rather than anything in our `.scss`, leave it as Material renders it, even if it doesn't
match the mockup's hand-authored px value exactly.

**In short:** fix everything in the app's own templates/`.scss` (arrangement, our own explicit
sizes, which Material variant/typescale class is used), leave Material's internal component
chrome alone. Most of the time these should already agree — the theme is wired to the mockup's
exact M3 tokens (see `_theme.scss`) and both implement the same M3 button/card spec — so the real
work is almost entirely in our own layout code, not in fighting Material.

**If a mismatch turns out to be genuinely un-fixable without overriding Material internals** (i.e.
it's baked into the component, not our layout), log it as a new "confirmed out of scope" entry in
`frontend-design-fidelity-plan.md`, the same way earlier deliberate deviations were logged — don't
silently skip it, and don't override Material to force it either, per the decision above.

## Mockup source

Use the self-contained offline bundle: `docs/cookingChallenge/CookOff Frontend (offline).html`.
Open it as a local `file://` URL in Chrome — it's a single file (fonts/images inlined), no
dependency on the `_ds/` folder or a running server. It's still plain-text/greppable for CSS class
rules and `--md-sys-*` token values if you need to read source directly instead of/alongside
rendering it.

The unbundled set (`CookingChallenge.dc.html` + `support.js` + `image-slot.js` + `_ds/`, all in
`docs/cookingChallenge/CookoffFrontendMockups/`) is the fallback if you need the token files
broken out individually (`tokens/colors.css`, `tokens/typography.css`, etc.) rather than inlined.

## Part 0 — Recon — **done 2026-08-12**

### Opening the file

The `claude-in-chrome` browser extension refuses `file://` URLs outright ("Can't interact with
browser-internal or unparseable URLs"). Serve the docs folder over local HTTP instead:

```
cd "docs/cookingChallenge" && python3 -m http.server 8765 --bind 127.0.0.1 &
```

then navigate to `http://127.0.0.1:8765/CookOff%20Frontend%20(offline).html` (URL-encode the
space). Takes ~5–8s to finish its "Unpacking..." step before content appears — `wait` before the
first screenshot/JS call or it'll error with a script-injection timeout. Leave the server running
in the background for the rest of the sweep; it's a `127.0.0.1`-bound static file server, harmless
to leave up.

### Navigation model

It's a **real interactive click-through prototype**, not a static per-state switcher — same
interaction model as the live app. Two navigation layers:

1. **Dev-nav bar**, fixed bottom-right, always present, 7 entries that jump to a top-level
   screen/persona: **Login, History, Accounts, Guest home, Cook home, Register, Expired**.
   - *Guest home* / *Cook home* land on a specific seeded persona's personalized-link view
     (Felix Wagner / Marco Huber in the current seed data), same page the real app's `/home`
     renders for a guest vs. a cook.
   - *Expired* lands directly on the link-expired screen (only saw one copy variant this pass —
     confirm whether qr vs. link variants are reachable separately, or if that's a query-param
     thing the dev-nav doesn't expose).
2. **Real in-page navigation**, same as production: once on *History*, the top row switches
   between **History / Accounts / Rivalries** tabs (organizer shell); clicking a challenge card
   opens its **Challenge Detail** (Open or Revealed, depending which card); clicking a rivalry
   card opens **Rivalry Detail**; dialog trigger buttons (New challenge, Send links, Edit cooks &
   guests, Registration QR code, Reveal/Unreveal, New/Edit account) open the same as in the real
   app. Use the "← All challenges" / "← All rivalries" / "← Organizer login" back-links to return.

### Known seed data (useful for reaching specific states)

- Organizer: Anna Novak (also ADMIN).
- Challenges: **Gazpacho** (Jul 12, Revealed, Marco won 12–10 vs. Lea), **Risotto** (Jun 14,
  Revealed, Marco vs. Petra), **Pulled Pork Burger** (Jul 26, Open, 2/3 guests submitted — Felix
  pending, Sophie/Tobias submitted), **Tiramisu** (Aug 9, Open — "Dessert Duell", Marco vs.
  someone, color not yet picked as of the Cook-home view).
- Cook-home (Marco) showed the **colorChosen** state already (red/yellow swatches under "Your open
  challenges" → Tiramisu). Didn't find **needsColor** (the picker-not-yet-clicked state) this pass
  — look for a challenge with an unassigned cook color, or check if Tiramisu itself is actually
  still `needsColor` and the swatches shown *are* the picker (ambiguous from the screenshot alone,
  re-check with a click during the actual Cook-home checklist item).

### DOM structure & computed-style extraction — confirmed working

Renders to plain DOM, no shadow root/iframe to worry about: `#dc-root > .sc-host > ...`, using the
same real `.md-*` classes as the source CSS (`md-card`, `md-button--filled/outlined/text`,
`md-chip--red/green`, `md-tabs`, `md-tab--active`, `md-top-app-bar`,
`md-typescale-headline-medium` etc.). `document.querySelector('.md-chip--red')` +
`getBoundingClientRect()`/`getComputedStyle()` works directly via the `javascript_tool`, e.g.:

```js
const el = document.querySelector('.md-chip--red');
const r = el.getBoundingClientRect(), s = getComputedStyle(el);
({w: r.width, h: r.height, padding: s.padding, radius: s.borderRadius, bg: s.backgroundColor})
```
confirmed output: `{w: 67.2, h: 32, padding: "0px 16px", radius: "8px", bg: "rgb(251, 224, 220)"}`.

**One gotcha:** dumping large raw `innerHTML` via the JS tool can trip the extension's
"Cookie/query string data" privacy heuristic and get blocked outright — query specific elements/
properties instead of serializing whole subtrees.

## Auth / test data needed for the live app

- **Organizer:** log in at `http://localhost:4200/login` with the existing test organizer account
  (`claude@claude.com` — password was supplied directly by the user during Part F's live audit,
  not recorded in this doc; ask the user if a fresh session is needed and it's not already
  active). Seed data from Part F still exists: a "Test Plate"/"Color Test Duel" challenge and a
  revealed "Schnitzel" challenge (Daniel Daniel vs. Michael Holzer).
- **Guest/cook links:** generate fresh access-link tokens from the organizer's Challenge Detail →
  "Send links" dialog (copy the link rather than actually emailing it), or read them from the dev
  Postgres DB directly (`docker exec backend-postgres-1 psql -U cookoff -d cookoff`, same approach
  Part F used). Needed for Participant Home (guest + cook), Blind Scoring, Challenge Results,
  Public Registration.

## Methodology (per screen)

1. **Reach the state** in both the mockup bundle (per Part 0's instructions) and the live app
   (navigate + auth as needed), at the same viewport size — start with desktop
   (~1280×900; the app's grids/breakpoints assume desktop-first per Part F's "not fully covered"
   note on responsive).
2. **Screenshot both** for a visual gut-check — obvious arrangement/size differences often jump
   out immediately without needing measurement.
3. **Measure both.** For each element that looks off (or as a matter of course for headings,
   buttons, card containers, and the main content grid/flex wrapper), pull from both DOMs via the
   browser tool's JS execution:
   - `getBoundingClientRect()` — width, height, position
   - `getComputedStyle()` — `font-size`, `font-weight`, `line-height`, `padding`, `margin`, `gap`,
     `border-radius`, `flex-direction`, `grid-template-columns`, `color`, `background-color`
   A small helper to run in both pages via the JS tool:
   ```js
   [...document.querySelectorAll('SELECTOR')].map(el => {
     const r = el.getBoundingClientRect(), s = getComputedStyle(el);
     return { text: el.textContent.trim().slice(0, 40), w: r.width, h: r.height,
       font: s.fontSize, weight: s.fontWeight, lh: s.lineHeight, pad: s.padding,
       gap: s.gap, radius: s.borderRadius, flexDir: s.flexDirection, color: s.color };
   })
   ```
   Diff the two result sets by eye or by pasting both into a scratch file and comparing.
4. **Fix** in the app's own `.html`/`.scss` for that component — arrangement, our own explicit
   sizes, or which Material variant/type-scale class is bound to which element. Do not edit
   Material's own internal styles (see "Scope" above).
5. **Re-screenshot and re-measure** to confirm the fix actually closed the gap.
6. **Run verification**: `cd frontend && npx ng build && npx ng test --watch=false && npm run
   lint`. Note the pre-existing unrelated `error-interceptor.spec.ts` failure if it's still there
   (documented in every prior Part of the fidelity-plan doc) — don't treat it as a regression you
   caused.
7. **Log it** — see "Logging completion" below.

## Ground rules (read before starting any item)

- **Don't revert documented intentional deviations.** Check
  `frontend-design-fidelity-plan.md` before "fixing" something — several mismatches are
  deliberate: plate-color indicators always pair color with text (WCAG AA — the mockup uses bare
  color), trophy icon instead of the mockup's 👑 crown emoji, a `<table>` instead of a flex-row
  list for the guest roster (accessibility), organizer name omitted from the top bar (no backend
  data source yet), cook-home/guest-home merged into one shared component, New/Edit Account's
  extra Password field (mockup has no auth story). Full list is in that doc's "Confirmed
  intentional" sections.
- **Don't touch Angular Material's internal component CSS** — see "Scope" above.
- **Don't invent design decisions the mockup doesn't make.** If something is genuinely ambiguous
  (not a case of the app being wrong), flag it for the user rather than guessing.
- **Prefer live checks over code-only comparison** — this plan exists specifically because
  code-only comparison already proved insufficient. If dev servers aren't running, say so and ask
  before falling back to source-reading.

## Logging completion

Append findings/fixes to `frontend-design-fidelity-plan.md` as a new dated "Part G" section
(create it once, then sub-number G1, G2, ... as items complete), following the existing format
used by Parts C–F: a table or list of (mockup vs. live vs. fix), a verification subsection
(build/test/lint + live check), and files touched. Don't create a separate log file per item.

## Screen checklist

Each item below is one unit of work. For each: reach the state in both mockup and live app,
measure, fix, verify, log. Canonical screen/dialog inventory reused from
`frontend-design-fidelity-plan.md` Part F's own live-audit route list (already validated as
complete once before).

### Organizer-facing

1. **Organizer Login** (`/login`) — card shell, form layout, field order, button placement.
2. **Challenge History** (`/challenges`) — grid layout, card internals (photo, cook names,
   status chip, winner styling).
3. **New Challenge dialog** — field layout/order, photo drop-zone.
4. **Challenge Detail — Open state** (`/challenges/:id`) — header (kicker, title, chip, photo
   split), guest list section, actions section.
5. **Challenge Detail — Revealed state** — reveal banner/animation, results table, head-to-head
   crown row.
6. **Challenge Detail dialogs**: Send/resend links, Registration QR code, Edit cooks & guests,
   Reveal confirm, Unreveal confirm — one sub-item each if arrangement differs.
7. **Accounts Admin** (`/accounts`) — table/list layout, role chips, Edit control.
8. **New/Edit Account dialog** — field layout, dynamic title/submit label states.
9. **Rivalries List** (`/rivalries`) — card grid, kicker line.
10. **Rivalry Detail** (`/rivalries/:cookA/:cookB`) — challenge card list, outcome labels.
11. **Organizer shell chrome** — top app bar + tabs row, spacing, active-tab treatment.

### Participant-facing

12. **Guest Home — open section** (`/home`) — card layout (content/photo split), status
    treatment, "Submitted" chip placement.
13. **Guest Home — past section** — compact row layout, "Revealed" chip.
14. **Cook Home — needsColor state** — outlined/no-photo card, swatch size/gap, picker layout.
15. **Cook Home — colorChosen state** — "You're plating {color}" card layout.
16. **Color-pick confirm dialog** — copy layout, button placement.
17. **Blind Scoring — not submitted** (`/challenges/:id/score`) — back link, instructions,
    divider, plate-column grid, submit button.
18. **Blind Scoring — submitted (success state)** — celebratory state layout.
19. **Challenge Results** (`/challenges/:id/results`) — header, chip, photo, back link.
20. **Public Registration — form state** (`/register`) — centered card shell, field layout.
21. **Public Registration — success state** — celebratory card layout.
22. **Link Expired — link kind** (`/link-expired`) — icon/copy/button layout.
23. **Link Expired — qr kind** — same, different copy branch.
24. **Participant shell chrome** — header/nav treatment.

### Cross-cutting

25. **404 Not Found** — layout vs. mockup if one exists for it (check Part 0 recon; the mockup
    project may not model a 404 at all — if so, log as "no mockup source, skip" rather than
    guessing).
26. **Responsive/mobile pass** — flagged as "not fully covered" in Part F. Re-run the full
    checklist above (or at least items 2, 9, 12–15) at a mobile viewport (~390×844) against
    whatever mobile treatment the mockup defines. Confirm arrangement actually reflows (stacks,
    single-column) rather than just shrinking the desktop grid.

## Verification (whole sweep, once all items are done)

1. `cd frontend && npx ng build` clean.
2. `npx ng test --watch=false` — no new failures beyond the known pre-existing
   `error-interceptor.spec.ts` one.
3. `npm run lint` clean.
4. Full live click-through of every route/dialog in the checklist above, organizer + guest + cook.
5. `frontend-design-fidelity-plan.md`'s new "Part G" section fully reflects what changed.
