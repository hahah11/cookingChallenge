# CookingChallenge App — Planning

## Context

A friend runs a monthly blind cook-off: two cooks make the same dish, guests taste both
without knowing who made which, and score each dish 0–5 (integers) in three categories —
**Mundgefühl**, **Tellersprache**, **Geschmack**. Whoever wins more categories wins the
challenge (a tied category counts for neither cook). We're building a small web app for
this: a mobile-friendly Angular frontend + Spring Boot backend, with host-managed guest
lists, self-serve guest score entry, on-demand reveal, and a running history across events.

The working directory already contains a `CLAUDE.md` and `docs/` tree copied from an
unrelated project ("IMPEPI", Spring Boot 4 + DB2 + Angular 21). The user wants to **keep
that stack and its architectural conventions** (Clean Architecture + DDD + Spring
Modulith on the backend, standalone-component Angular on the frontend) but rebrand the
docs to CookingChallenge, and swap DB2 → PostgreSQL. **No project source files are to be
created yet** — the user will scaffold the actual backend/frontend projects themselves.
This plan only covers: (1) the doc rename, and (2) the domain/API/architecture design to
follow once they scaffold.

## Step 0 — Commit this plan into the repo as documentation

- Copy this plan's content into `docs/PLAN.md` in the repo (new file — the `docs/`
  tree currently only has the generic dev-guideline docs, no project-specific plan).
- Commit it, along with pushing the earlier uncommitted local commit
  (`cb1c1c4`, `CLAUDE.md` + `docs/` rebrand-pending files) to `origin/main`, so the
  GitHub remote actually has content for future cloud sessions (this was the root
  cause of the Ultraplan cloud session not finding the repo).
- Leave the `.idea/` and `cookingChallenge.iml` untracked files alone (IDE-local,
  not project docs) — do not add them.

## Step 1 — Rebrand existing docs (only file change in this task)

- In `CLAUDE.md` and every file under `docs/`, replace `IMPEPI` → `CookingChallenge`
  (project name, package example `com.impepi` → `com.cookingchallenge`, and the example
  domain `customer`/`order` references can stay as illustrative examples since they're
  generic architecture samples — only the project identity/naming needs to change, not
  the teaching examples).
- Swap the one DB2 mention (`docs/README` intro: "Spring Boot 4 with DB2") to PostgreSQL.
- Do not add any new files or folders — this is a find/replace pass over existing docs.

## Step 2 — Domain design (for when scaffolding happens)

Two bounded contexts / Spring Modulith modules, per `docs/backend/02-ddd-modulith.md`:

### `auth` module
- `HostAccount` aggregate: id, username, hashed password, roles. Multiple host accounts
  are supported (rotating organizers); any authenticated host can see/manage all
  challenges — history is shared across the friend group, not siloed per host.
- Login issues a JWT (or Spring Session) used by the Angular app for host-only actions.
- Guests never authenticate — they reach a challenge via its link and self-identify by
  picking their name from the host-entered guest list.

### `cookoff` module (the core domain)
- **`Challenge`** aggregate root: id, date, optional title, two `Cook` value objects
  (name), two `Dish` entities labeled internally `A`/`B` (never exposed to guests before
  reveal), pre-added `Guest` list (name only), status (`OPEN` / `REVEALED`), createdBy
  (HostAccount id).
- **`ScoreSubmission`** aggregate root (own transaction boundary since guests submit
  independently/concurrently): id, challengeId, guestName, and 6 `Score` entries — one
  per (dish A/B × category). A repository invariant enforces exactly one submission per
  (challengeId, guestName) — resubmission is rejected with 409, matching the casual/trust
  model (no guest auth).
- **`Category`** enum: `MUNDGEFUEHL`, `TELLERSPRACHE`, `GESCHMACK`.
- **`Score`** value object: category + points, validated `0 <= points <= 5` (integer).
- **Domain service** `ResultCalculator`: given a challenge + all its submissions, sums
  points per dish per category, determines a per-category winner (dish with higher sum;
  equal sums = no winner for that category), then an overall winner (cook with most
  category wins; tie possible if categories split evenly or all tie).
- **Reveal**: a host action (`revealChallenge`) that flips status to `REVEALED`, after
  which the API exposes the cook↔dish mapping and computed results. Before reveal, the
  host can query submission progress (`submittedGuestCount / totalGuestCount`) but not
  the scores/results themselves; guests only see "submitted" confirmation.

## Step 3 — REST API (`/api/v1`, per `docs/shared/04-api-design.md` conventions)

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/login` | host | authenticate, returns JWT |
| POST | `/api/v1/challenges` | host | create challenge: date, title, 2 cook names, 2 dish names, guest name list |
| GET | `/api/v1/challenges` | host | history list (all challenges, shared across hosts) |
| GET | `/api/v1/challenges/{id}` | public (link) | challenge info for guests: guest list to pick from, dish labels A/B, categories — no cook mapping |
| GET | `/api/v1/challenges/{id}/status` | host | submission progress: which guests have/haven't submitted |
| POST | `/api/v1/challenges/{id}/scores` | public (link) | guest submits: `{ guestName, scores: [{dish: "A"|"B", category, points}, ...] }` (6 entries); 409 if guestName already submitted or isn't in the pre-added list |
| POST | `/api/v1/challenges/{id}/reveal` | host | closes scoring, computes + returns results, exposes cook↔dish mapping |
| GET | `/api/v1/challenges/{id}/results` | public (link) | results + reveal, only returns data once status is `REVEALED` (404/403 before that) |

All responses follow the existing envelope (`data`/`meta`, error envelope with
`code`/`message`/`details`) already documented in `docs/shared/04-api-design.md`.

## Step 4 — Frontend structure (Angular, per `docs/frontend/01-architecture.md`)

- `features/auth/` — host login form, auth guard, JWT interceptor (in `core/`).
- `features/challenges/` — host-only: create-challenge form (cooks, dishes, guest list),
  history list, challenge detail (submission progress + reveal button + results view).
- `features/scoring/` — public guest flow: open challenge link → pick name from list →
  score form (2 dishes × 3 categories, 0–5 selectors) → submit → confirmation.
- `shared/components/` — results display (category winners, overall winner), score input
  control (reusable 0–5 picker), guest-picker dropdown.

## Step 5 — Data model (PostgreSQL)

- `host_accounts(id, username, password_hash, created_at)`
- `challenges(id, title, challenge_date, cook_a_name, cook_b_name, dish_a_name, dish_b_name, status, created_by_host_id, created_at)`
- `challenge_guests(id, challenge_id, guest_name)` — pre-added guest list
- `score_submissions(id, challenge_id, guest_name, submitted_at)` — unique (challenge_id, guest_name)
- `scores(id, submission_id, dish_label, category, points)` — check constraint `points between 0 and 5`

## Key flows

1. **Create**: host logs in → creates challenge with cooks A/B (kept server-side only),
   dish name, date, guest list → shares the challenge link with guests.
2. **Score**: each guest opens the link, picks their name (once — enforced by the unique
   submission constraint), scores both dishes across 3 categories, submits.
3. **Track**: host can check submission progress anytime without seeing scores.
4. **Reveal**: host triggers reveal when ready (not gated on 100% submission) → backend
   computes category + overall winners and exposes which cook made which dish.

## Non-goals / open assumptions (flag if wrong)

- No guest authentication — trust-based, friend-group setting.
- Ties: a tied category counts toward neither cook; an overall tie is possible and simply
  reported as a draw (not forced to resolve).
- Shared history across all hosts, not per-host silos.
- No editing of submitted scores once sent (simplest, matches "one shot" nature of the event).

## Verification (once the user scaffolds the projects)

- Backend: unit tests for `ResultCalculator` (category/overall winner logic incl. ties),
  repository/integration tests for the unique-submission constraint and 0–5 validation,
  controller tests for status codes per `docs/shared/03-testing.md`.
- Frontend: component/service tests for the score picker (rejects out-of-range/non-integer
  input) and the guest-picker flow.
- Manual end-to-end: create a challenge, submit as 2+ guests, confirm progress view, reveal,
  confirm category/overall winner matches hand-calculated totals.
