# OpenAPI-First API Plan — Spec → Codegen → Services

## Status

**Phase 1 done (2026-08-02).** `openapi/cookingchallenge-api.yaml` authored, covering every
row in the use-case inventory below. Validated clean with `redocly lint` and smoke-tested
end to end with the actual `openapi-generator` `spring` generator (`interfaceOnly=true,
skipDefaultInterface=true, useTags=true`) — compiles to one interface per tag
(`AuthApi`, `AccountsApi`, `ChallengesApi`, `RivalriesApi`, `HomeApi`) plus models, no errors.
The edit-until-reveal conflict (below) was resolved with the user before finalizing the
scoring paths: **allow edit-until-reveal**.

**Phase 1 revised (2026-08-02).** Account editing was under-specified: `UpdateAccountRequest`
was missing `email` (organizer needs to edit name, email, *and* roles), and there was no way
for the frontend to know which roles are selectable without hardcoding the `SystemRole` enum
client-side. Fixed:

- `UpdateAccountRequest` gained an optional `email` field; `PATCH /api/v1/accounts/{id}`
  gained a `409` response (email is now editable, so it can collide with another account).
- New `GET /api/v1/accounts/{accountId}` (→ `AccountDetailResponse`), so the edit dialog
  loads that one account's current name/email/roles fresh when it opens, instead of trusting
  a possibly-stale row from the list already held in frontend state.
- `AccountListResponse` and `AccountDetailResponse` both carry `availableRoles: SystemRole[]`
  — the server is the single source of truth for which roles exist, so the frontend never
  hardcodes `[ADMIN, ORGANIZER, USER]` itself.

This is also the concrete instance of a general frontend-loading principle, per the user:
the frontend should fetch everything it needs to render a page **on that page's initial
load** (accounts list + `availableRoles` for the "new account" dialog, both in one
`GET /api/v1/accounts` call); opening a popup or navigating to another page/dialog triggers
its *own* fresh fetch (the edit dialog's `GET /api/v1/accounts/{id}` call) rather than reusing
whatever the parent page already had in memory. Apply this same pattern to future
screens/dialogs, not just accounts.

**Phase 2 done (2026-08-02).** `backend/build.gradle.kts` now applies
`org.openapi.generator` 7.24.0, wired to the `openApiGenerate` task: `inputSpec` points at
`../openapi/cookingchallenge-api.yaml`, output goes to `build/generated/openapi` (already
covered by the existing blanket `build/` gitignore — nothing generated is ever committed),
`apiPackage`/`modelPackage` are `at.fraihs.cookoff.shared.web.openapi.{api,model}` (sits
outside the `interfaces/application/domain/infrastructure` layer patterns the ArchUnit
layering rule matches on, same precedent as the existing `shared.web` package — confirmed
by running the suite, not just by inspection). Added `swagger-annotations-jakarta:2.2.31`
and `jackson-databind-nullable:0.2.6` as the two extra compile deps the generated code
needs (`useSpringBoot3=true` config option, so it's jakarta.* throughout, matching Spring
Boot 4). `compileJava` now depends on `openApiGenerate`, and the generated `src/main/java`
is added to the main source set. `./gradlew build` passes end to end: generates
`AuthApi`/`AccountsApi`/`ChallengesApi`/`RivalriesApi`/`HomeApi` + 47 model classes, compiles
alongside all existing hand-written code, and all 124 existing tests (including the
ArchUnit/jMolecules architecture tests) still pass untouched - nothing implements the new
interfaces yet, as expected. Phase 3 (domain gap-filling) not started.

**Supersedes** the `interfaces/rest` and `application/{service,dto}` portions of Phases 1
and 4 in `backend-persistence-api-security-plan.md`. Those were built before the UI/UX
mockup existed and are being treated as exploratory scaffolding, per the user: "the
controllers I already have were created with the blue field project as ideas... we don't
need to stay with them." Everything else that plan built — the domain model, JPA
infrastructure, the access-link mechanism, and the security config — stays untouched.

**PRD reconciliation (2026-08-03).** `docs/cookingChallenge/frontend-prd.md` walked the
actual mockup chat against this repo's DDD modulith and resolved every collision found
(self-registration scope, cook plate-color self-pick, who may score, challenge photos,
score scale) — see that document's §3 and §10 for the full resolution log, and
`backend-persistence-api-security-plan.md`'s new Phase 7 for the concrete backend sequencing.
It supersedes the "Use-case inventory" and "Domain gaps to fill" sections below wherever
they conflict; superseded rows/items are left in place with a note rather than deleted, for
history. Separately: `openapi/cookingchallenge-api.yaml` itself was **deleted** in commit
`e05bd5e` ("update architectural docs") pending a rewrite against both the PRD and the newer
page-scoped-query/config-endpoint conventions now documented in
`docs/shared/04-api-design.md` — **Phase 1 (spec authoring) restarts from scratch**, not
from the old file's content.

## Approach

1. Design an OpenAPI spec shaped around what the UI ([`design-reference.md`](../design-reference.md),
   the `CookingChallenge Frontend.dc.html` mockup) actually needs, screen by screen and
   use case by use case — not a 1:1 dump of the domain model. The goal, per the user: the
   frontend should contain almost no logic beyond UI state (dialog open/closed, form
   bindings) — every computed value (winners, progress counts, rivalry text) comes
   pre-shaped from the API.
2. Generate Spring server interfaces + models from the spec using a openAPI generator. Controllers become thin
   implementations of the generated interface: bind request → build a command → call one
   service → map the result to the generated response model.
3. Generate a TypeScript Angular client from the same spec once `cookingChallenge-angular`
   is scaffolded (separate, later task).
4. Write new application services — one per use case, same shape as the services that
   already exist (`CreateChallengeService`, `RevealChallengeService`, etc.) — as the bridge
   between the generated server contracts and the existing DDD domain model. These are the
   "link between the OpenAPI spec and the DDD models" the user asked for.

## Resolved: submitted scores are editable until reveal

The mockup's guest home screen tags an already-scored challenge **"Submitted — editable
until reveal"**, and the scoring screen pre-fills the guest's prior answers
(`c.guestScores[GUEST_ID]`) for resubmission. The current backend disagreed:
`ScoreSubmission` was enforced one-shot (`score_submissions` unique constraint on
`(challenge_id, guest_account_id)`, `SubmitScoreService` throws `DuplicateSubmissionException`
on a second attempt), and `first-plan.md`'s non-goals section said outright: *"No editing
of submitted scores once sent (simplest, matches 'one shot' nature of the event)."*

**Decided with the user (2026-08-02): allow edit-until-reveal.** The mockup's copy and
pre-fill behavior are correct; the backend invariant changes, not the UI. Consequences,
locked into the Phase 1 spec:

- `POST /api/v1/challenges/{challengeId}/scores` is now upsert-shaped: **201** on a
  participant's first submission, **200** when they resubmit before reveal. No
  `DuplicateSubmissionException` case survives for participants correcting themselves —
  409 is now reserved for "challenge already REVEALED."
- The participant-facing challenge schema (`ParticipantChallenge`, used by both
  `GET /api/v1/challenges/{challengeId}` and `GET /api/v1/me/home`) carries a `submitted`
  boolean and a nullable `mySubmission` (the requester's own prior scores) so the guest
  scoring screen can pre-fill without any client-side logic.
- `GET /api/v1/me/home`'s `past` bucket is guests' already-submitted-OR-revealed
  challenges (still editable while OPEN); `open` is not-yet-submitted OPEN challenges only.

Phase 3 still has to build the domain side of this: `ScoreSubmission` needs an
`update(...)` domain method (or delete-and-recreate), and the unique constraint becomes an
upsert target instead of a hard reject. See gap 6 below.

## Decisions locked in for Phase 1 (defaults — redirect if you want different)

- **Spec file location**: `openapi/cookingchallenge-api.yaml` at the repo root, not under
  `backend/`. It's a neutral contract both `backend` and the future
  `cookingChallenge-angular` generate from — neither project should have to reach into the
  other's tree.
- **OpenAPI version**: 3.0.3 — broader, more stable support in openapi-generator's Spring
  templates than 3.1.
- **Response envelope**: `docs/shared/04-api-design.md`'s `{data, meta}` success /
  `{error: {code, message, details, requestId, timestamp}}` error envelope is mandatory
  project-wide, so it has to be part of the generated schema, not bolted on after
  serialization by a `ResponseBodyAdvice` — that would desync the generated FE client's
  types from the actual wire format. Concretely: every success response is a named wrapper
  schema (`ChallengeResponse { data: Challenge, meta: ApiMeta }`,
  `ChallengeListResponse { data: Challenge[], meta: ApiMeta }`, etc.), not a shared generic
  — OpenAPI 3.0 has no real generics. Verbose to author, but it's the only way the
  generated client sees a type that matches reality.
- **Backend codegen**: `org.openapitools.openapi-generator-gradle-plugin`, generator
  `spring`, `interfaceOnly=true`, `skipDefaultInterface=true`, `useTags=true` — produces a
  Java interface per tag (e.g. `ChallengesApi`) plus model classes; `@RestController`
  classes implement the interface.
- **Frontend codegen** (once Angular exists — not blocking now): `@openapitools/openapi-generator-cli`,
  generator `typescript-angular`. Same tool family as the backend so there's one codegen
  toolchain to learn, not two. Revisit if the generated client feels too heavy once there's
  an actual frontend to try it against.

## What stays, what's superseded

**Stays as-is** (this plan does not touch it):
- `auth.domain.*`, `cookoff.domain.*` — every aggregate/VO/enum, `ResultCalculator`,
  `ChallengeRevealed` (may still need *new methods* in Phase 3 — see gaps below — but the
  existing ones aren't rewritten)
- `auth.infrastructure.*`, `cookoff.infrastructure.persistence.*` — JPA entities, MapStruct
  mappers, repository impls
- `auth.application.service.AccessLinkService` + `auth.infrastructure.accesslink.*` — the
  link issue/verify mechanism is infrastructure, not API-shaped, so it's untouched
- `shared.config.SecurityConfig`, `shared.config.JwtConfig`, `shared.security.*` — the auth
  *mechanism* stays; only the `authorizeHttpRequests` path matchers get updated in Phase 6
  once paths are final
- `shared.tsid.TsidSupport`

**Superseded** (deleted once the new controllers/services replace them, Phase 5):
- `auth.interfaces.rest.*`, `cookoff.interfaces.rest.*` — every existing controller +
  request record
- `auth.application.{service,dto}.*` except `AccessLinkService` — `CreateAccountService`,
  `ListAccountsService`, `LoginService` get rewritten against generated types;
  `AccountView`/`CreateAccountCommand`/`LoginCommand`/`AuthTokenView` are replaced by
  generated models
- `cookoff.application.{service,dto}.*` similarly — `ChallengeView`,
  `ChallengeParticipantView`, `ChallengeResultView`, `SubmissionStatusView`, `ScoreInput`,
  `CreateChallengeCommand`, `SubmitScoreCommand` all replaced by generated request/response
  models. The *domain orchestration logic* inside `CreateChallengeService`,
  `RevealChallengeService`, `SubmitScoreService`, etc. is what gets carried forward — only
  the DTO shapes around it change.
- `shared.web.ApiResponse`/`ApiMeta`/`ApiErrorResponse`/`ApiErrorBody`/`ApiErrorDetail` —
  replaced by generated envelope models; `GlobalExceptionHandler` keeps its
  exception-to-status-code mapping table, just builds the generated error model instead of
  the hand-written one.

## Use-case inventory (mockup screen → endpoint)

| Screen / action | Endpoint (proposed) | Backend support today |
|---|---|---|
| Organizer login | `POST /api/v1/auth/login` | Existing (`AuthController`/`LoginService`) |
| History list | `GET /api/v1/challenges` | Existing, but needs per-challenge submission progress embedded (currently a separate `/status` call) — application-layer extension, no domain gap |
| New challenge dialog | `POST /api/v1/challenges` | Existing (`CreateChallengeService`) |
| Challenge detail: guest list + status | `GET /api/v1/challenges/{id}/status` | Existing (`SubmissionStatusView`) |
| Send/resend links | `POST /api/v1/challenges/{id}/invitations` | Existing (`SendChallengeInvitationsService`) |
| Edit cooks & guests | `PATCH /api/v1/challenges/{id}/participants` | **Superseded by PRD** — `frontend-prd.md` §5.2 / Phase 7.4 specifies `Challenge.editParticipants(...)` precisely, including the color-reset rule on cook reassignment |
| Cook plate-color pick | `POST /api/v1/challenges/{id}/color-pick` | **New, from PRD** — Phase 7.3, needs `PlateColor` (7.2) first |
| Challenge photo upload/replace/view | `PATCH` + `GET /api/v1/challenges/{id}/image` | **New, from PRD** — Phase 7.5, `ImageStoragePort` + DB-blob adapter |
| Generate registration QR | `POST /api/v1/challenges/{id}/registration-invites` | **New, from PRD** — Phase 7.8 |
| Self-registration (walk-in) | `POST /api/v1/public/registrations` | **New, from PRD** — Phase 7.8; public but token-gated, also adds the new account as a guest of the QR's challenge |
| Reveal results | `POST /api/v1/challenges/{id}/reveal` | Existing (`RevealChallengeService`); Phase 7.6 adds populating `lastRevealResult` for unreveal |
| Revealed results + rivalry text | `GET /api/v1/challenges/{id}/results` | Existing for results; rivalry summary needs joining in `CookRivalryRepository.findByPair` (repo method already exists — just needs wiring into the response) |
| Unreveal challenge | `POST /api/v1/challenges/{id}/unreveal` | **Superseded by PRD** — the rivalry double-count question below is resolved concretely in `frontend-prd.md` §5.2 / Phase 7.6 (`lastRevealResult` + `CookRivalry.reverseResult` via a `ChallengeUnrevealed` event, mirroring `ChallengeRevealed`) |
| Accounts list (+ `availableRoles` for the new-account dialog) | `GET /api/v1/accounts` | Existing (`ListAccountsService`), needs `availableRoles` added to the response |
| New account | `POST /api/v1/accounts` | Existing (`CreateAccountService`) |
| Edit account: load fresh detail on dialog open | `GET /api/v1/accounts/{id}` | **New** — no single-account fetch exists today |
| Edit account (name/email/roles) | `PATCH /api/v1/accounts/{id}` | **New** — no `Account` update method or repo update path yet |
| Rivalries list | `GET /api/v1/rivalries` | **New** — `CookRivalryRepository` only has `findByPair`, needs `findAll()` |
| Rivalry detail (pair + their challenges) | `GET /api/v1/rivalries/{cookAId}/{cookBId}` | **New** — needs a `ChallengeRepository` query by cook pair; the `CookRivalry` aggregate itself has no challenge references |
| Guest home (open + past challenges) | `GET /api/v1/me/home` | Existing but scoped to *open, not-yet-submitted* only (`HomeService`) — mockup also needs a "past" bucket (submitted/revealed). Application-layer extension, no domain gap |
| Guest results view | `GET /api/v1/challenges/{id}/results` | Existing, same endpoint as organizer results, already link-token gated |
| Blind scoring submit | `POST /api/v1/challenges/{id}/scores` | Existing (`SubmitScoreService`) — **contingent on the edit-until-reveal decision above**; Phase 7.1 changes the score range to 1–5 (was 0–5) and Phase 7.7 narrows who may submit to guests + the challenge's `createdBy` account, excluding cooks |

## Domain gaps to fill (Phase 3)

1. ~~`Challenge.unreveal()`~~ — **resolved**, see `frontend-prd.md` §5.2 and
   `backend-persistence-api-security-plan.md` Phase 7.6.
2. ~~`Challenge` cook reassignment + guest removal~~ — **resolved**, see `frontend-prd.md`
   §5.2 and Phase 7.4 (`editParticipants`, including the color-reset rule).
3. `CookRivalryRepository.findAll()` for the rivalries list screen. *(still open, unchanged
   by the PRD)*
4. A `ChallengeRepository` query for "challenges between this cook pair" for the rivalry
   detail screen (not a `CookRivalry` responsibility — that aggregate has no challenge
   references, only running counters). *(still open, unchanged by the PRD)*
5. `Account` update — check whether `Account` has any mutation method today (name/email/roles);
   if not, add one plus a corresponding `AccountRepository` update path. Email changes need
   the same uniqueness check `CreateAccountService` already does, surfaced as the new `409`.
   *(still open, unchanged by the PRD — `Account.rename()`/`changePasswordHash()` already
   exist per the current domain source; a role add/remove + email-change path is what's
   still missing)*
6. `ScoreSubmission` update path — confirmed needed now that edit-until-reveal is decided
   (see "Resolved" section above): an `update(...)` domain method (or delete-and-recreate),
   and the repo's unique-constraint path becomes an upsert instead of a hard reject.
   *(still open, unchanged by the PRD)*
7. `PlateColor` aggregate + repository + reference data — new, see `frontend-prd.md` §5.2
   and Phase 7.2.
8. `Challenge.pickColor(...)` — new, Phase 7.3.
9. `ImageStoragePort` + `Challenge.imageRef`/`changeImage(...)` — new, Phase 7.5.
10. `Score`'s invariant narrows from `0 <= points <= 5` to `1 <= points <= 5` — Phase 7.1.
11. `Challenge.canScore(...)` narrowing scoring eligibility to guests + `createdBy`,
    excluding cooks — new, Phase 7.7.
12. `RegistrationInvite` mechanism (`auth` module, infrastructure-layer like `AccessLink`)
    + `auth.RegistrationInvites` public contract + `cookoff`'s
    `CreateRegistrationInviteService`/`PublicRegistrationService` orchestration — new,
    Phase 7.8.

## Phased execution

**Phase 1 — Author the spec.** No code — restarts from scratch since the prior spec file
was deleted (see the PRD-reconciliation note above). Write `openapi/cookingchallenge-api.yaml`
covering every row in the use-case inventory, now including the PRD-driven rows (color
pick, image upload/view, registration invites/self-registration), using the domain's
existing vocabulary (`ChallengeStatus`, `Category`, `DishLabel`, `SystemRole`, `PlateColor`)
for enums/schemas so generated models line up with domain names.

**Phase 2 — Backend codegen wiring.** Add the Gradle plugin, configure input spec path +
output package, verify `./gradlew build` generates interfaces/models that compile (nothing
implements them yet — that's expected).

**Phase 3 — Domain gap-filling.** Build the new domain methods + repository queries listed
above, unit-tested in isolation the same way the original plan's Phase 1 did (Mockito
mocks of repository ports), before any controller depends on them.

**Phase 4 — New application services.** One per use case, in
`{module}.application.service`, taking generated request models (or a thin command mapped
from one) and returning generated response models. Reuses existing domain orchestration
logic wherever a service already exists; net-new for `unreveal`, `edit participants`,
`edit account`, `rivalries list/detail`.

**Phase 5 — New controllers + deletion.** `@RestController`s implementing the generated
interfaces, delegating to Phase 4 services. Delete every file listed under "Superseded"
above once its replacement is wired and tested — don't leave both versions around.

**Phase 6 — Security config path updates.** Update `SecurityConfig#authorizeHttpRequests`
matchers for any new/changed paths (`unreveal`, `participants`, account edit, rivalries).

**Phase 7 — Frontend client codegen.** Deferred until `cookingChallenge-angular` is
scaffolded — separate task, not blocking this plan.

**Phase 8 — End-to-end verification.** Full regression pass. Expect some of the existing
121 tests to be deleted alongside their DTOs/controllers rather than ported; write new
tests for the new services and domain methods per `docs/backend/03-code-style.md`.

## Explicitly out of scope

- Scaffolding the Angular project itself.
- Real email delivery.
- Any endpoint not reachable from a screen in the mockup.
