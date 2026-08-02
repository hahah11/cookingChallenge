# OpenAPI-First API Plan — Spec → Codegen → Services

## Status

New plan, not started. Confirmed approach with the user 2026-08-02.

**Supersedes** the `interfaces/rest` and `application/{service,dto}` portions of Phases 1
and 4 in `backend-persistence-api-security-plan.md`. Those were built before the UI/UX
mockup existed and are being treated as exploratory scaffolding, per the user: "the
controllers I already have were created with the blue field project as ideas... we don't
need to stay with them." Everything else that plan built — the domain model, JPA
infrastructure, the access-link mechanism, and the security config — stays untouched.

## Approach

1. Design an OpenAPI spec shaped around what the UI ([`design-reference.md`](../design-reference.md),
   the `CookingChallenge Frontend.dc.html` mockup) actually needs, screen by screen and
   use case by use case — not a 1:1 dump of the domain model. The goal, per the user: the
   frontend should contain almost no logic beyond UI state (dialog open/closed, form
   bindings) — every computed value (winners, progress counts, rivalry text) comes
   pre-shaped from the API.
2. Generate Spring server interfaces + models from the spec. Controllers become thin
   implementations of the generated interface: bind request → build a command → call one
   service → map the result to the generated response model.
3. Generate a TypeScript Angular client from the same spec once `cookingChallenge-angular`
   is scaffolded (separate, later task).
4. Write new application services — one per use case, same shape as the services that
   already exist (`CreateChallengeService`, `RevealChallengeService`, etc.) — as the bridge
   between the generated server contracts and the existing DDD domain model. These are the
   "link between the OpenAPI spec and the DDD models" the user asked for.

## Conflict to resolve before Phase 3: are submitted scores editable until reveal?

The mockup's guest home screen tags an already-scored challenge **"Submitted — editable
until reveal"**, and the scoring screen pre-fills the guest's prior answers
(`c.guestScores[GUEST_ID]`) for resubmission. The current backend disagrees:
`ScoreSubmission` is enforced one-shot (`score_submissions` unique constraint on
`(challenge_id, guest_account_id)`, `SubmitScoreService` throws `DuplicateSubmissionException`
on a second attempt), and `first-plan.md`'s non-goals section says outright: *"No editing
of submitted scores once sent (simplest, matches 'one shot' nature of the event)."*

This has to be decided before Phase 3 touches `SubmitScoreService`/`ScoreSubmission`,
because it changes an aggregate invariant, not just an endpoint:

- **Keep one-shot** (simplest, matches current tests/constraint) → the mockup's "editable
  until reveal" copy and pre-fill behavior are wrong and should be cut from the UI.
- **Allow edit-until-reveal** → `ScoreSubmission` needs an `update(...)` domain method (or
  becomes delete-and-recreate), the unique constraint becomes an upsert target instead of
  a hard reject, and `SubmitScoreService` needs to distinguish "first submission" (201)
  from "resubmission" (200) — no `DuplicateSubmissionException` case survives at all for
  participants correcting themselves.

Flag this to the user explicitly before starting Phase 3's scoring work.

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
| Edit cooks & guests | `PATCH /api/v1/challenges/{id}/participants` | **New** — `Challenge` has no cook-reassignment or guest-removal method, only additive `addGuest` |
| Reveal results | `POST /api/v1/challenges/{id}/reveal` | Existing (`RevealChallengeService`) |
| Revealed results + rivalry text | `GET /api/v1/challenges/{id}/results` | Existing for results; rivalry summary needs joining in `CookRivalryRepository.findByPair` (repo method already exists — just needs wiring into the response) |
| Unreveal challenge | `POST /api/v1/challenges/{id}/unreveal` | **New** — `Challenge` has no reverse transition; see the rivalry double-count question below |
| Accounts list | `GET /api/v1/accounts` | Existing (`ListAccountsService`) |
| New account | `POST /api/v1/accounts` | Existing (`CreateAccountService`) |
| Edit account (name/roles) | `PATCH /api/v1/accounts/{id}` | **New** — no `Account` update method or repo update path yet |
| Rivalries list | `GET /api/v1/rivalries` | **New** — `CookRivalryRepository` only has `findByPair`, needs `findAll()` |
| Rivalry detail (pair + their challenges) | `GET /api/v1/rivalries/{cookAId}/{cookBId}` | **New** — needs a `ChallengeRepository` query by cook pair; the `CookRivalry` aggregate itself has no challenge references |
| Guest home (open + past challenges) | `GET /api/v1/me/home` | Existing but scoped to *open, not-yet-submitted* only (`HomeService`) — mockup also needs a "past" bucket (submitted/revealed). Application-layer extension, no domain gap |
| Guest results view | `GET /api/v1/challenges/{id}/results` | Existing, same endpoint as organizer results, already link-token gated |
| Blind scoring submit | `POST /api/v1/challenges/{id}/scores` | Existing (`SubmitScoreService`) — **contingent on the edit-until-reveal decision above** |

## Domain gaps to fill (Phase 3)

1. `Challenge.unreveal()` — reverse `REVEALED → OPEN`. Open question: does it reverse the
   `CookRivalry.recordResult(...)` call from the original reveal, and does a later re-reveal
   recompute the same result without double-counting the rivalry? Needs a decision, not an
   assumption — results aren't stored separately from `ScoreSubmission`s today (recomputed
   via `ResultCalculator` on every `/results` call), so re-reveal *should* be deterministic
   as long as submissions aren't mutated in between — but the rivalry counter update is a
   one-time side effect tied to the `ChallengeRevealed` event, and firing it twice for the
   same reveal would corrupt the running record.
2. `Challenge` cook reassignment + guest removal — only `addGuest` (additive, OPEN-only)
   exists. New domain methods, same `requireOpen()` guard style.
3. `CookRivalryRepository.findAll()` for the rivalries list screen.
4. A `ChallengeRepository` query for "challenges between this cook pair" for the rivalry
   detail screen (not a `CookRivalry` responsibility — that aggregate has no challenge
   references, only running counters).
5. `Account` update — check whether `Account` has any mutation method today (name/roles);
   if not, add one plus a corresponding `AccountRepository` update path.
6. Conditional on the conflict above: `ScoreSubmission` update path if edit-until-reveal is
   confirmed.

## Phased execution

**Phase 1 — Author the spec.** No code. Write `openapi/cookingchallenge-api.yaml` covering
every row in the use-case inventory, using the domain's existing vocabulary
(`ChallengeStatus`, `Category`, `DishLabel`, `SystemRole`) for enums so generated models
line up with domain enum names. Resolve the edit-until-reveal conflict with the user before
finalizing the scoring paths.

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
