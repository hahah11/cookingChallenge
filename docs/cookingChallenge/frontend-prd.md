# CookingChallenge — Frontend PRD

## Status

Derived from the interactive prototype (`CookingChallenge Frontend.dc.html`, Claude Design
project `6749cf81…`, see [`design-reference.md`](design-reference.md)) and reconciled
against the current DDD modulith (`docs/cookingChallenge/domain-model.puml`,
[`first-plan.md`](first-plan.md), the actual backend source) on 2026-08-03. Five collisions
between the mockup and the existing domain model were found; all five are resolved in §3.
The five follow-up design questions those resolutions raised (§10) were discussed and
closed with the user the same day — nothing is open. This PRD is ready to drive the
`domain-model.puml` update and subsequent implementation planning.

## 1. Context

A cook-off app: two cooks blind-prepare the same dish under a plate color so tasters don't
know who made what. Guests (and the organizer) score 1–5 per category. Organizer reveals
results once everyone has scored. Mobile-friendly Angular 21 frontend + Spring Boot 4
backend (DDD + Spring Modulith, `auth` + `cookoff` modules).

## 2. Roles

- **Organizer/Admin** — logs in with email/password (JWT). Creates challenges, manages
  accounts, sends/resends personalized links, edits open challenges (cooks, guests, photo),
  reveals/unreveals results, may also score (see §3.3).
- **Cook** — reaches a personal dashboard via emailed link. Picks a plate color per open
  challenge they're assigned to; the other cook is auto-assigned the remaining color.
  Irreversible once picked. Does not score.
- **Guest** — reaches a personal dashboard via emailed link. Scores open challenges
  (editable until reveal), views results of revealed ones.
- **New participant** — self-registers via a QR code that an admin/organizer generates from
  within a specific open challenge. Scanning it opens a landing page (first name, last
  name, email); submitting both creates an `Account` (role `USER`, no password) **and**
  adds it as a guest of that challenge in the same step. The QR is reusable until it
  expires — one code can register many walk-ins, not just the first scanner.

## 3. Resolved Collisions With the Current Domain Model

| # | Topic | Mockup | Domain model said | Resolution |
|---|---|---|---|---|
| 1 | Self-registration | Public QR screen creates an `Account` (role `USER`) | `first-plan.md`: *"No self-registration — the host creates each Account up front… at least initially"* | **Adopted, but gated**: only an admin/organizer can generate the QR, and it's tied to one challenge — registering both creates the `Account` and adds it as a guest of that challenge. Not a fully public, unscoped flow. See §5.1/§5.2. |
| 2 | Cook color assignment | Cooks self-pick Red/Yellow from their dashboard post-creation; irreversible; other cook auto-assigned the remainder | Organizer assigns both cooks to fixed labels A/B at creation; no self-pick, no "unassigned" state | **Adopted, with colors as DB-managed data** (not a hardcoded 2-value enum) so the available palette can change without a code change. See §5.2. |
| 3 | Who can score | Only guests have a scoring UI; cooks don't | `SubmitScoreService` currently allows either cook to submit too | **Guests + the organizer who created the challenge score. Cooks never score.** See §5.2 and the open question in §10 about what "the organizer" means precisely. |
| 4 | Challenge photo | Card thumbnail + 4:3 upload at creation, editable later | No field, no storage, no DB column at all | **In scope now.** `Challenge` gains an image, settable at creation and editable while the challenge is still `OPEN` (via the same edit flow as cooks/guests). See §5.2. |
| 5 | Score scale | Star widget, 1–5 (a star rating can't represent 0) | `Score` invariant + DB check constraint: `0 <= points <= 5` | **Found during reconciliation, not previously discussed.** Resolved to match the mockup: **1–5**, no 0. Flagged in §10 — redirect if 0 (e.g. "inedible") should be a valid score. |

## 4. Screens & User Stories

### 4.1 Organizer / Admin

**Login**
- As an organizer, I want to log in with email/password so I get a JWT for host-side actions.

**History** (list + create)
- As an organizer, I want a grid of challenge cards (photo, date, dish name, title, cook
  names, status tag, submission progress) so I can see the state of every event at a glance.
- As an organizer, I want the winning cook's name highlighted in their plate color once a
  challenge is revealed, so the outcome is visible without opening the detail page.
- As an organizer, I want to create a challenge (title, date, dish name, pick 2 cook
  accounts, pick a guest list, upload a 4:3 rivalry photo) in one dialog, so setup is a
  single step.

**Challenge detail**
- *Open state*
  - As an organizer, I want to see the guest list with each guest's submission status, so I
    know who still needs to score.
  - As an organizer, I want to edit cooks and guests on an open challenge (reassign either
    cook, add/remove guests, replace the photo), so mistakes made at creation are fixable
    without recreating the event.
  - As an organizer, I want to send or resend personalized links to selected participants,
    so latecomers or people who lost their email can get back in.
  - As an organizer, I want to generate a registration QR code for this open challenge, so
    a walk-in guest who wasn't pre-added can register themselves on the spot and join as a
    guest of this event, without me creating their account first.
  - As an organizer, I want to reveal results with an explicit confirmation, so I don't
    trigger it by accident, and I want a celebratory full-screen animation when it happens.
- *Revealed state*
  - As an organizer, I want a score table (cook columns tinted to their plate color, winning
    score bold, a crown over the overall winner, a Total row, and a head-to-head-wins row
    with a crown per past win against this same opponent), so the outcome and history read
    at a glance.
  - As an organizer, I want to unreveal a challenge (with confirmation) to reopen scoring if
    I revealed too early or a score needs correcting.

**Accounts**
- As an admin, I want a table of all accounts (full names) with an edit dialog per row
  (name, email, roles — `USER` always on and locked), so I can manage who's in the system.

**Rivalries**
- As an organizer, I want a card per cook pair showing their open-vs-revealed challenge
  counts, so I can spot ongoing rivalries.
- As an organizer, I want to open a rivalry and see every challenge (open or revealed)
  between that pair with its outcome, so I can review the full history of that matchup.

### 4.2 Cook

- As a cook, I want to see my open challenges and pick Red or Yellow (with a confirmation
  dialog), so I claim my plate identity for that event.
- As a cook, once either cook has picked, I want the picker to disappear for both of us
  (the other color auto-assigned), so there's no race or duplicate pick.
- As a cook, I want a link to the read-only results of my past challenges, so I can see how
  I did after reveal.

### 4.3 Guest

- As a guest, I want to see my open challenges with a "Score now"/"Edit scores" action, so I
  know what's still pending.
- As a guest, I want to score 1–5 per category for each plate color (columns colored solid
  per cook's color, stars fill in that color on hover/select), so scoring is unambiguous
  even though I don't know who cooked which dish.
- As a guest, I want to edit my submission any time before reveal, so I can correct a
  mis-tap without asking the organizer.
- As a guest, once a challenge is revealed, I want the same results view the organizer sees
  (minus the unreveal control), so I can see who won and by how much.

### 4.4 Public

- As a walk-in guest, I want to scan a QR code an organizer generated for this specific
  challenge and register with just my name and email, so I'm both added to the system and
  joined as a guest of that event in one step, without the organizer creating my account
  first. If the code has expired or is invalid, I want a clear message rather than a
  half-created account.

## 5. Domain Model Deltas

Baseline: `docs/cookingChallenge/domain-model.puml`. Everything below is additive/changed
against that diagram; regenerate the `.puml` once these land.

### 5.1 `auth` module

- **New mechanism `RegistrationInvite`, infrastructure-layer like `AccessLink`** (see §Why
  isn't this in the `.puml?` discussion — same reasoning applies: a token with an expiry is
  authentication/invitation plumbing, not a domain concept with its own business
  invariants). Structurally a sibling of `AccessLink`, not a reuse of it — `AccessLink`
  always points at an *existing* `Account`; `RegistrationInvite` exists precisely because
  there isn't one yet.
  - `RegistrationInvite { id, issuedByAccountId, challengeId (raw long — see below), token,
    expiresAt }`. **Reusable until expiry**, same decision already made for `AccessLink`:
    one QR (a poster/screen at the event) is scanned by many different walk-ins, not just
    the first.
  - `challengeId` is a **raw TSID `long`, not the `cookoff` module's typed `ChallengeId`** —
    identical reasoning to `AccessLink.issue(AccountId, long challengeId, Duration)`
    already documented in `backend-persistence-api-security-plan.md` Phase 3: `cookoff`
    already depends on `auth` (one-way), so `auth` taking a compile dependency back on
    `cookoff.domain.model.ChallengeId` would create a module cycle.
  - `auth.application.service.RegistrationInviteService`:
    - `issue(issuedByAccountId, challengeId: long, validFor: Duration) : String (token)` —
      callable only by an `ORGANIZER`/`ADMIN` account (`account.canOrganize()`, the
      existing predicate).
    - `verify(token) : RegistrationInviteVerification { challengeId: long }` — rejects
      (throws `InvalidOrExpiredLinkException`, reused from the `AccessLink` mechanism) if
      missing/expired.
  - **New public contract** (mirrors `auth.AccountLookup`'s existing pattern — keeps
    `Account`/`AccountRepository` internal to `auth`): `auth.RegistrationInvites`, with two
    methods so `cookoff` (see §5.2) never needs to reach into `auth`'s internals:
    - `issue(issuedByAccountId, challengeId: long, validFor: Duration) : String (token)` —
      thin pass-through to `RegistrationInviteService.issue`, exposed publicly so
      `cookoff`'s challenge-scoped "generate QR" endpoint can call it.
    - `register(token, firstName, lastName, email) : RegistrationResult { accountId,
      challengeId: long }` — verifies the token, rejects on duplicate email (reuses
      `CreateAccountService`'s existing `AccountAlreadyExistsException` → 409 check), then
      `Account.create(email, name, SystemRole.USER)` and returns both the new `accountId`
      and the raw `challengeId` the token was tied to. This is the **only** new
      domain-adjacent step here — everything else is infra, same as `AccessLink`.

### 5.2 `cookoff` module

- **`Challenge`**
  - New field: `imageRef : String?` (nullable) — an **opaque reference**, not a URL. Only
    `ImageStoragePort` (below) knows how to resolve it to actual bytes; the domain layer
    just carries the reference so the same field works unchanged whether the backing
    adapter is a DB blob (today) or a real object-storage key (later).
  - New method `changeImage(newImageRef)` — `requireOpen()`-gated, same guard style as
    `addGuest`. The application-layer service stores the uploaded bytes via
    `ImageStoragePort` first, then calls this with the returned reference.
  - New method `editParticipants(newCookAAccountId?, newCookBAccountId?,
    guestIdsToAdd[], guestIdsToRemove[])` — `requireOpen()`-gated, replaces the
    additive-only `addGuest`. **Reassigning a cook clears that pairing's plate-color pick**
    (both, since the pairing changed) — a picked color is meaningless once the person behind
    label A/B changes.
  - New method `pickColor(cookAccountId, chosenColorId, otherColorId)` —
    `requireOpen()`-gated. Rejects if this challenge's colors are already fully assigned
    (irreversible-once-picked, matching the mockup). Assigns the picking cook's
    `chosenColorId` **and** the other cook's `otherColorId` in the same operation — both
    are set together, not independently, so "the other cook is auto-assigned" is atomic
    with the pick. The two candidate colors (first 2 active `PlateColor`s) are resolved by
    the calling application service, not looked up by `Challenge` itself, keeping the
    aggregate free of a repository dependency. **No new domain event** — unlike
    `reveal()`/`unreveal()`, nothing outside this aggregate needs to react to a color pick
    today, and `first-plan.md` is explicit that events shouldn't be added speculatively
    (`ChallengeRevealed` is called out as deliberately "currently the only domain event in
    the model"). A synchronous method, same as `editParticipants`/`changeImage`.
  - New method `unreveal()` — reverses `REVEALED → OPEN`, reopens scoring. Publishes a new
    `ChallengeUnrevealed` domain event, consumed by a new `@TransactionalEventListener`
    (mirroring the existing `ChallengeRevealedRivalryUpdater`) that calls
    `CookRivalry.reverseResult(...)` — **not** a direct `Challenge` → `CookRivalry` call,
    to keep the one cross-aggregate-update mechanism this module already uses for
    `reveal()` consistent rather than introducing a second pattern. This closes gap #1 from
    `openapi-first-api-plan.md`'s gap list.
  - Scoring eligibility narrows: previously any participant (`isParticipant` = guest or
    either cook). Now **guests ∪ `{createdBy}`** — the organizing account may score, cooks
    may not. `isParticipant`/`isGuest` stay as-is for their existing non-scoring uses (e.g.
    "who can view this challenge"); a new `canScore(accountId)` predicate backs the scoring
    check specifically.

- **`CookAssignment`** (value object)
  - Gains an optional `colorId : PlateColorId?` (nullable until picked).

- **New reference/lookup: `PlateColor`**
  - `PlateColor { id, name, hexCode, sortOrder, active }` — DB-backed, admin-manageable list
    (replaces the hardcoded Red/Yellow 2-value enum the mockup implies). Lives in the
    `cookoff` module since it's cookoff-specific presentation data, not a system-wide
    concern. Colors are identified by their `hexCode` so the palette can be restyled
    without a code change. **Every challenge always reserves the first 2 active colors**
    (by `sortOrder`) for its two cooks to choose between — no per-challenge organizer
    override; changing the default pair means editing the `plate_colors` table (via a
    future admin screen), not a per-event decision.

- **`Score`**
  - Invariant changes from `0 <= points <= 5` to **`1 <= points <= 5`** (see §3, item 5).

- **`CookRivalry`**
  - New method `reverseResult(previousOverallWinnerAccountId)` — the exact inverse of
    `recordResult(...)` (decrements the matching win/draw counter and `totalChallenges`).
  - `Challenge` needs to remember what it last contributed to the rivalry so `unreveal()`
    knows precisely what to reverse: add `lastRevealResult : AccountId?` (nullable,
    "draw" encoded as present-but-null-winner vs "never revealed" encoded as absent — needs
    a small design pass, not just a bare nullable field). Flow: `reveal()` stores the
    computed result on the challenge before publishing `ChallengeRevealed`; `unreveal()`
    reads it, calls `reverseResult(...)`, then clears it; a subsequent `reveal()` recomputes
    fresh (since scoring reopened) and stores/publishes again. This is a proposed design —
    confirm before Phase 3 work starts (§10).

- **New port: `ImageStoragePort`**
  - `store(bytes, contentType) : ImageRef` / `resolve(ImageRef) : (bytes, contentType)` /
    `delete(ImageRef)` — shaped so a real object-storage adapter (S3-compatible, `ImageRef`
    = bucket key) is a drop-in later. **Initial adapter persists the blob as a row in
    Postgres** (`challenge_images`, see §6) rather than local disk — chosen so this works
    identically in any deployment target from day one, with no filesystem/volume
    assumptions to unwind when a real object-storage adapter replaces it. Mirrors the
    existing `NotificationPort` → `LoggingNotificationAdapter` port/stub-adapter precedent.
  - A new `GET /api/v1/challenges/{id}/image` endpoint streams the resolved bytes
    regardless of which adapter is active — the frontend never needs to know how images are
    stored, only that this one URL renders them.

- **New use case, no new `Challenge` method**: self-registration's guest-add step reuses
  `editParticipants` (§5.2 above) as-is — `guestIdsToAdd = [newAccountId]`. Two new
  `cookoff.application.service` entries orchestrate across both modules (allowed direction:
  `cookoff` already depends on `auth`):
  - `CreateRegistrationInviteService.execute(organizerAccountId, challengeId)` — checks
    `challenge.getStatus() == OPEN`, then calls `auth.RegistrationInvites.issue(...)`
    (default `validFor`, e.g. the same 30-day constant `SendChallengeInvitationsService`
    already uses). Backs `POST /api/v1/challenges/{id}/registration-invites`
    (`ORGANIZER`/`ADMIN`-gated, same as every other challenge-management endpoint).
  - `PublicRegistrationService.execute(token, firstName, lastName, email)` — calls
    `auth.RegistrationInvites.register(...)`, then loads the returned `challengeId` via
    `ChallengeRepository`, calls `challenge.editParticipants(guestIdsToAdd = [accountId])`,
    saves. Backs the public `POST /api/v1/public/registrations` endpoint. If the challenge
    turns out not to be `OPEN` any more (e.g. revealed between QR generation and scan), this
    still creates the `Account` (the person is now in the system regardless) but skips the
    guest-add and surfaces that in the response — the frontend shows "you're registered, but
    this event has already closed" rather than a hard failure that would leave the new
    account half-created.

## 6. Data Model Deltas (PostgreSQL)

Additive to `first-plan.md` §Step 5's schema:

- `plate_colors(id, name, hex_code, sort_order, active)` — admin-managed lookup table;
  `sort_order` + `active` determine which 2 rows every challenge defaults to.
- `challenge_images(id, content_type, data BYTEA, created_at)` — the `ImageStoragePort`'s
  initial DB-blob adapter storage; `challenges.image_ref` holds this table's `id` (as text)
  until a real object-storage adapter replaces the backing store, at which point `image_ref`
  holds that store's key instead — the column's meaning is adapter-defined, not a FK.
- `challenges`: + `image_ref` (nullable, opaque text — see above), + `cook_a_color_id` /
  `cook_b_color_id` (nullable FK → `plate_colors`), + `last_reveal_overall_winner_account_id`
  (nullable, supports `unreveal()`'s rivalry reversal — see §5.2).
- `scores`: check constraint changes from `points between 0 and 5` to
  `points between 1 and 5`.
- `registration_invites(id, issued_by_account_id, challenge_id, token, expires_at)` — lives
  in the `auth` module's schema alongside `access_links`, with the same shape: `challenge_id`
  is `BIGINT` with a DB-level FK to `challenges(id)` (`ON DELETE CASCADE`), exactly matching
  `access_links.challenge_id`'s existing FK — the "raw `long`, not typed `ChallengeId`"
  rule in §5.1 is a **Java compile-dependency** rule only; the schema itself already links
  the two tables at the database level, same as it does for `access_links` today.
- `accounts`: no schema change — self-registered accounts simply never get a
  `password_hash`, same as any organizer-created account that hasn't had a password set.

## 7. API Surface Impact

The actual OpenAPI spec (`openapi/cookingchallenge-api.yaml`) was removed in the latest
commit (`e05bd5e`, "update architectural docs") pending a rewrite against the newer
page-scoped-query / config-endpoint conventions — this section is input for that rewrite,
not a replacement of it. Per screen (§4), roughly:

- `POST /api/v1/challenges/{id}/registration-invites` — new, `ORGANIZER`/`ADMIN`-gated;
  returns a token/URL the frontend renders as a QR code.
- `POST /api/v1/public/registrations` — new, unauthenticated but requires the token from
  the endpoint above (`{ token, firstName, lastName, email }`); creates the account and
  adds it as a guest of the token's challenge in one call.
- `PATCH /api/v1/challenges/{id}/participants` — new (edit cooks & guests).
- `PATCH /api/v1/challenges/{id}/image` (multipart upload) and
  `GET /api/v1/challenges/{id}/image` (streams resolved bytes) — new; kept separate from
  `participants` since one is a binary upload/download pair, not JSON.
- `POST /api/v1/challenges/{id}/color-pick` — new (cook self-pick).
- `POST /api/v1/challenges/{id}/unreveal` — new.
- `GET /api/v1/rivalries`, `GET /api/v1/rivalries/{cookAId}/{cookBId}` — new (both already
  flagged as gaps in `openapi-first-api-plan.md`).
- `GET /api/v1/accounts/{id}`, `PATCH /api/v1/accounts/{id}` — new (already flagged there
  too).
- `GET /api/v1/config` — per the just-updated `docs/shared/04-api-design.md`, serves
  available `SystemRole`s, the active `PlateColor` palette, and feature flags at app
  bootstrap. **Supersedes** the older `availableRoles` field embedded directly in
  `AccountListResponse`/`AccountDetailResponse` that `openapi-first-api-plan.md` describes —
  that field moves to the config endpoint instead of living on the accounts responses.
- Everything else in the mockup maps onto endpoints already listed in
  `openapi-first-api-plan.md`'s use-case inventory (login, history, create challenge, send
  invitations, reveal, results, guest home, scoring submit) — unchanged by this PRD except
  the score-range and scorer-eligibility deltas above.

## 8. Conventions Carried Forward

- `{data, meta}` success envelope / `{error: {code, message, details, requestId,
  timestamp}}` error envelope, OpenAPI-first spec → codegen on both sides (never hand-edit
  generated code).
- Page-scoped query endpoints: one primary GET per screen; popups/dialogs always fetch
  fresh on open, never reuse a parent page's already-loaded state (matches your standing
  [[project_frontend_data_loading_pattern]] preference).
- Roles/permissions/feature flags come only from `GET /api/v1/config`, fetched once at
  bootstrap — never hardcoded or inferred client-side.
- Frontend reactive-form validation is UX-only; the backend re-validates everything
  regardless.
- Every aggregate root id is a TSID, Base32-encoded at the API boundary.
- First names are used everywhere in guest/cook-facing UI except the Accounts table and the
  open-challenge guest list (organizer-facing), which use full names for unambiguous
  identification.
- All primary action buttons are black, not the design system's red accent, per your
  stated preference.

## 9. Out of Scope for This PRD

- Real email delivery (link-issuing stays a logging/no-op adapter).
- Swapping `ImageStoragePort`'s backing adapter from Postgres blobs to a real
  object-storage provider (S3-compatible) — the port is designed for that transition now
  (§5.2), but only the DB-blob adapter is built as part of this PRD's scope; the swap
  itself is a future, explicitly requested task.
- Any endpoint or screen not reachable from the mockup.

## 10. Decisions Finalized (2026-08-03)

The five design questions raised while drafting this PRD were discussed and closed with the
user the same day. No open questions remain against this document.

1. **Who may score as "the organizer" (§3, item 3)** — the challenge's own `createdBy`
   account only, not any `ORGANIZER`/`ADMIN` account. `Challenge` already holds this
   reference, so no new lookup is needed.
2. **Plate color reservation per challenge (§5.2)** — every challenge always uses the first
   2 active `plate_colors` rows by `sortOrder`; no per-challenge organizer override. Colors
   are identified by `hexCode`, stored in `plate_colors`, so the palette is restylable
   without a code change.
3. **Unreveal → `CookRivalry` reversal (§5.2)** — proceed with the proposed design:
   `Challenge.lastRevealResult` + `CookRivalry.reverseResult(...)`, called by `unreveal()`
   and re-populated by the next `reveal()`.
4. **Image storage backend (§5.2, §6)** — `ImageStoragePort` is designed now for a real
   object-storage adapter (S3-compatible) to slot in later, but the adapter actually built
   in this scope persists blobs in Postgres (`challenge_images`), not local disk and not a
   real object-storage provider yet.
5. **Score scale (§3, item 5)** — confirmed **1–5, no zero**. `Score`'s invariant and the
   `scores` table's check constraint both use `1 <= points <= 5`.
6. **Registration QR scope (§5.1, §5.2)** — tied to one challenge, not general: generated
   by an organizer/admin from within that challenge; scanning it creates the `Account` and
   adds it as a guest of that specific challenge in one step.
7. **Registration QR reuse** — reusable until expiry, same as `AccessLink`: one QR can be
   scanned and used to register by many different walk-ins, not just the first.

## References

- [`design-reference.md`](design-reference.md) — Claude Design source of truth for the UI.
- [`first-plan.md`](first-plan.md) — original domain/API/data-model design.
- `domain-model.puml` — class-level diagram this PRD's §5 extends.
- [`plans/openapi-first-api-plan.md`](plans/openapi-first-api-plan.md) — API/spec rollout,
  several of whose "domain gaps to fill" this PRD resolves or closes.
- [`plans/backend-persistence-api-security-plan.md`](plans/backend-persistence-api-security-plan.md)
  — current backend implementation status this PRD builds on top of.
