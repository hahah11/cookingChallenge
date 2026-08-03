# CookingChallenge App — Planning

## Context

A friend runs a monthly blind cook-off: two cooks make the same dish, guests taste both
without knowing who made which, and score each dish 0–5 (integers) in three categories —
**Mundgefühl**, **Tellersprache**, **Geschmack**. Whoever wins more categories wins the
challenge (a tied category counts for neither cook). We're building a small web app for
this: a mobile-friendly Angular frontend + Spring Boot backend, with host-managed guest
lists, self-serve guest score entry, on-demand reveal, and a running history across events.

The working directory already contains a `CLAUDE.md` and `docs/` tree copied from an
unrelated project ("CookingChallenge", Spring Boot 4 + DB2 + Angular 21). The user wants to **keep
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

- (Done) Rebranded `CLAUDE.md` and every file under `docs/` from the original `IMPEPI`
  identity to `CookingChallenge` (project name, package example `com.impepi` →
  `com.cookingchallenge`). The example domain `customer`/`order` references were left as
  illustrative examples since they're generic architecture samples — only the project
  identity/naming needed to change, not the teaching examples.
- Swap the one DB2 mention (`docs/README` intro: "Spring Boot 4 with DB2") to PostgreSQL.
- Do not add any new files or folders — this is a find/replace pass over existing docs.

## Step 2 — Domain design (for when scaffolding happens)

Two bounded contexts / Spring Modulith modules, per `docs/backend/02-ddd-modulith.md`.
Every aggregate root ID (`AccountId`, `ChallengeId`, `ScoreSubmissionId`,
`CookRivalryId`) is a **TSID**, stored as `BIGINT`, Base32-encoded at the API/URL
boundary — see `docs/backend/03-code-style.md#id-generation-tsid`.

### `auth` module
- **`Account`** aggregate root: id, email, name, hashed password, `roles: Set<SystemRole>`
  (`ADMIN` / `ORGANIZER` / `USER`) — a *system permission* set, not tied to any specific
  challenge: `ADMIN` can manage anything, `ORGANIZER` can create challenges and assign
  cooks/guests (and score), `USER` can only score. An account can hold several of these
  at once.
- **No self-registration** — the host creates each `Account` up front (email + name);
  there's no public sign-up flow, at least initially.
- Login issues a JWT (or Spring Session) used by the Angular app for `ORGANIZER`/`ADMIN`
  actions (host-side).
- **Casual access via personalized link, not password login**: for participating in a
  challenge (scoring, or just checking status), an `Account` doesn't log in with a
  password. The host triggers "send links" on a challenge; each participant gets an
  email with a personalized link (a signed/opaque token identifying their `Account`,
  not a shared secret) that opens their own "home" — the open scoring requests across
  challenges they're part of. This keeps the casual/trust model from the original plan
  while still tying every action to a real `Account` for history purposes. Mechanism is
  an interface/infrastructure-layer concern (token issuance + verification), not a new
  domain aggregate.
- `SystemRole` is a completely separate concern from "being a cook" or "being a guest"
  in a given challenge — see below.

### Cook/Guest are per-challenge roles, not entities
- There is no `Cook` or `Guest` aggregate. A guest or cook only exists as such because
  they're a linked `Account` participating in one specific `Challenge` — so `Challenge`
  (and `ScoreSubmission`, `CookRivalry`) reference `AccountId` directly.
- The same `Account` can be a cook in one challenge and a guest in another, or even
  create (organize) a challenge it also cooks in — "cook"/"guest" is just which slot an
  `AccountId` fills on a given `Challenge`, not a property of the account itself.
- This also means per-person history ("which cookoffs did X attend", "how has X scored")
  is just a query over `Challenge`/`ScoreSubmission` by `accountId` — no separate
  aggregate needed to support it.

### `cookoff` module (the core domain)
- **`Challenge`** aggregate root: id, date, optional title, a single `DishName` value
  object (the same dish is cooked by both cooks, e.g. "Schnitzel" — there's one dish
  name, not two), two `CookAssignment` value objects (`accountId` + label `A`/`B`; the
  assignment is never exposed to guests before reveal), pre-added guest list
  (`AccountId` references), status (`OPEN` / `REVEALED`), createdBy (`AccountId`, must
  hold `ORGANIZER` or `ADMIN`).
- **`ScoreSubmission`** aggregate root (own transaction boundary since guests submit
  independently/concurrently): id, challengeId, guestAccountId, and 6 `Score` entries —
  one per (dish label A/B × category). A repository invariant enforces exactly one
  submission per (challengeId, guestAccountId) — resubmission is rejected with 409,
  matching the casual/trust model.
- **`CookRivalry`** aggregate root: a running head-to-head record for one specific pair
  of cooks — cookAAccountId, cookBAccountId, cookAWins, cookBWins, draws,
  totalChallenges. Updated via `recordResult(overallWinnerAccountId)` whenever a
  `ChallengeRevealed` event fires, so "who's won more cookoffs against whom" is a stored
  counter, not recomputed per view.
- **`Category`** enum: `MUNDGEFUEHL`, `TELLERSPRACHE`, `GESCHMACK`.
- **`DishLabel`** enum: `A`, `B`.
- **`Score`** value object: dishLabel + category + points, validated `0 <= points <= 5`
  (integer).
- **Domain service** `ResultCalculator`: given a challenge + all its submissions, sums
  points per dish label per category, determines a per-category winner (label with
  higher sum; equal sums = no winner for that category), then an overall winner (the cook
  whose label won more categories; tie possible if categories split evenly or all tie).
- **Domain event** `ChallengeRevealed`: challengeId, cookAAccountId, cookBAccountId,
  overallWinnerAccountId (null = draw). Published when a host reveals a challenge;
  consumed within the `cookoff` module to update `CookRivalry`. This is currently the
  only domain event in the model — everything else stays synchronous/direct-call.
- **Reveal**: a host action (`revealChallenge`) that flips status to `REVEALED` and
  publishes `ChallengeRevealed`, after which the API exposes the cook↔label mapping and
  computed results. Before reveal, the host can query submission progress
  (`submittedGuestCount / totalGuestCount`) but not the scores/results themselves; guests
  only see "submitted" confirmation.

See `docs/cookingChallenge/domain-model.puml` for the full class-level PlantUML diagram
of this model (aggregates, value objects, domain service, and the reveal → rivalry
event flow).

## Step 3 — REST API (`/api/v1`, per `docs/shared/04-api-design.md` conventions)

> **Superseded.** This table is the original, pre-OpenAPI-first design and predates
> `docs/cookingChallenge/frontend-prd.md`'s PRD deltas (self-registration, cook color
> pick, challenge photo, unreveal, score scale, scoring eligibility). The current,
> maintained API surface is `docs/cookingChallenge/plans/openapi-first-api-plan.md`'s
> "Use-case inventory" table — read that instead; this one is kept only for historical
> context on the original domain/API shape.

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/login` | organizer/admin | password login, returns JWT (host-side app use) |
| POST | `/api/v1/accounts` | admin | create an `Account` (email + name), assign `SystemRole`(s) |
| GET | `/api/v1/accounts` | organizer+ | list existing accounts (to pick cooks/guests from when creating a challenge) |
| POST | `/api/v1/challenges` | organizer+ | create challenge: date, title, dishName, `{accountId, label}` × 2, guest accountId list |
| POST | `/api/v1/challenges/{id}/invitations` | organizer+ | "send links" — emails each participant (cooks + guests) their personalized access link |
| GET | `/api/v1/challenges` | organizer+ | history list (all challenges, shared across the friend group) |
| GET | `/api/v1/me/home` | link token | the authenticated-by-link account's open scoring requests across challenges |
| GET | `/api/v1/challenges/{id}` | link token | challenge info: guest list, dish name, labels A/B, categories — no cook mapping |
| GET | `/api/v1/challenges/{id}/status` | organizer+ | submission progress: which guests have/haven't submitted |
| POST | `/api/v1/challenges/{id}/scores` | link token | submit: `{ scores: [{dish: "A"|"B", category, points}, ...] }` (6 entries); guest identified by the link's account; 409 if already submitted or not in the pre-added list |
| POST | `/api/v1/challenges/{id}/reveal` | organizer+ | closes scoring, computes + returns results, exposes cook↔label mapping, updates `CookRivalry` |
| GET | `/api/v1/challenges/{id}/results` | link token | results + reveal, only returns data once status is `REVEALED` (404/403 before that) |
| GET | `/api/v1/accounts/{id}/rivalries/{otherAccountId}` | organizer+ | (optional, can defer) running head-to-head record between two cooks |

All responses follow the existing envelope (`data`/`meta`, error envelope with
`code`/`message`/`details`) already documented in `docs/shared/04-api-design.md`.

## Step 4 — Frontend structure (Angular, per `docs/frontend/01-architecture.md`)

- `features/auth/` — organizer/admin login form, auth guard, JWT interceptor (in
  `core/`).
- `features/accounts/` — admin-only: create/manage `Account`s (email, name, roles) and
  pick-existing UI used inside the create-challenge flow (cooks + guests).
- `features/challenges/` — organizer-only: create-challenge form (pick 2 cook accounts +
  assign labels, dish name, pick guest accounts), "send links" action, history list,
  challenge detail (submission progress + reveal button + results view + head-to-head
  record between the two cooks).
- `features/home/` — link-token flow: personalized link opens a participant's "home"
  (their open scoring requests across challenges), no password.
- `features/scoring/` — score form (2 dishes × 3 categories, 0–5 selectors), reached from
  `home`, identified via the link token → submit → confirmation.
- `shared/components/` — results display (category winners, overall winner), score input
  control (reusable 0–5 picker).

## Step 5 — Data model (PostgreSQL)

All `id` columns below are `BIGINT` TSIDs (see Step 2). `challenge_guests`/`scores` are
child rows of an aggregate, not aggregates themselves, so they use plain DB-sequence
`BIGSERIAL` ids — no need for TSID there.

- `accounts(id, email, name, password_hash NULL, created_at)` — `password_hash` only set
  for organizer/admin accounts that actually log in
- `account_roles(account_id, role)` — an account can hold multiple `SystemRole`s (ADMIN/ORGANIZER/USER)
- `access_links(id, account_id, challenge_id, token, expires_at, used_at NULL)` —
  personalized link tokens issued via "send links"; `token` is a separate high-entropy
  random secret (not the TSID — TSID is sortable/not a secret), looked up on every
  link-based request instead of a JWT
- `challenges(id, title, challenge_date, dish_name, cook_a_account_id, cook_b_account_id, status, created_by_account_id, created_at)` — `cook_a_account_id`/`cook_b_account_id`/`created_by_account_id` FK into `accounts`, cook columns always stored as label A/B assignment
- `challenge_guests(id, challenge_id, guest_account_id)` — pre-added guest list, FK into `accounts`
- `score_submissions(id, challenge_id, guest_account_id, submitted_at)` — unique (challenge_id, guest_account_id)
- `scores(id, submission_id, dish_label, category, points)` — check constraint `points between 0 and 5`
- `cook_rivalries(id, cook_a_account_id, cook_b_account_id, cook_a_wins, cook_b_wins, draws, total_challenges)` — unique constraint on the (normalized) `(cook_a_account_id, cook_b_account_id)` pair, updated on each `ChallengeRevealed`

## Key flows

1. **Create**: organizer logs in (password/JWT) → picks or creates the `Account`s for
   the two cooks and the guests (reused across events), assigns cooks to labels A/B
   (kept server-side only), sets dish name + date.
2. **Invite**: organizer clicks "send links" → each participant (2 cooks + guests) gets
   an email with their personalized access link.
3. **Score**: each participant opens their link → lands on their "home" (open scoring
   requests across challenges) → picks the open one → scores both dish labels across 3
   categories → submits (once — enforced by the unique submission constraint).
4. **Track**: organizer can check submission progress anytime without seeing scores.
5. **Reveal**: organizer triggers reveal when ready (not gated on 100% submission) →
   backend computes category + overall winners, exposes which cook made which dish
   (label), and updates the `CookRivalry` running record for that cook pair.

## Non-goals / open assumptions (flag if wrong)

- No password-based guest authentication — personalized link tokens instead, matching
  the original casual/trust-based, friend-group setting while still tying every action
  to a real `Account`.
- Ties: a tied category counts toward neither cook; an overall tie is possible and simply
  reported as a draw (not forced to resolve).
- Shared history across the whole friend group, not per-organizer silos.
- No editing of submitted scores once sent (simplest, matches "one shot" nature of the event).

## Verification (once the user scaffolds the projects)

- Backend: unit tests for `ResultCalculator` (category/overall winner logic incl. ties),
  repository/integration tests for the unique-submission constraint and 0–5 validation,
  controller tests for status codes per `docs/shared/03-testing.md`.
- Frontend: component/service tests for the score picker (rejects out-of-range/non-integer
  input) and the guest-picker flow.
- Manual end-to-end: create a challenge, submit as 2+ guests, confirm progress view, reveal,
  confirm category/overall winner matches hand-calculated totals.
