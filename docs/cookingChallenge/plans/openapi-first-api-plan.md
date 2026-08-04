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

**Phase 1 re-done (2026-08-03).** Correction to the note above: the file wasn't actually
gone — a separate commit (`b7bf0a2`, "revert openapi specs") had reverted `e05bd5e`'s
deletion and restored the **pre-PRD** spec content (the original Phase 1 from 2026-08-02,
already covering edit-until-reveal but none of the PRD deltas). This session's work started
from that restored file, not a blank one, and layered the PRD deltas on top of it rather
than re-authoring from scratch — same end state either way, less churn. Changes made:
- **New `GET /api/v1/config`** (`Config` tag) — `{availableRoles, plateColors,
  featureFlags}`, per `docs/shared/04-api-design.md`'s Configuration Endpoint convention.
  `availableRoles` **removed** from `AccountListResponse`/`AccountDetailResponse` — this
  spec's own Phase-1-revised note above is now superseded on that one point; the frontend
  fetches roles from `/config` at bootstrap instead. `featureFlags` is an empty
  `additionalProperties: boolean` map for now — no actual flags exist yet, kept only
  because the shared doc's convention names it as part of this endpoint's shape.
- **`Score`/`ScoreEntry.points` minimum**: `0` → `1`, per Phase 7.1.
- **`CookAssignment` gains nullable `colorId`** (organizer-facing; always resolvable once
  picked, never hidden — the organizer sees identity + color unconditionally).
- **`Challenge` gains `hasImage: boolean` and nullable `overallWinnerAccountId`** — the
  history grid needs to tint a revealed winner's name in one list call, without an N+1
  fetch to `/results` per card; `overallWinnerAccountId` is backed directly by the existing
  `Challenge.lastRevealResult` from Phase 7.6, no new computation needed.
- **`ParticipantChallenge` restructured** for the cook/color-pick flow: the old nullable
  `cookAssignments: CookAssignment[] | null` (all-or-nothing pre-reveal hiding) is replaced
  by an always-present `participantCookAssignments: ParticipantCookAssignment[]` (new
  schema) where only **`accountId`** is null pre-reveal — `colorId` is visible from the
  moment it's picked, because blind scoring is done by plate color ("columns colored solid
  per cook's color"), which requires dish-to-color without dish-to-cook. Also gains
  `hasImage`, `myCookLabel` (the requester's own label if they're a cook, else null),
  `canScore` (backed by `Challenge.canScore`, Phase 7.7), and `canPickColor` (true iff
  `myCookLabel` is set and neither cook has picked yet) — so the frontend picks
  score-vs-pick-color-vs-view-only UI from flags instead of inferring a role.
- **`GET /me/home`'s description reframed** as generically "personalized home for a guest
  or cook," not guest-only — `open`/`past` now key off `canScore`/`canPickColor` rather
  than literally "submitted," since a cook's pending action is picking a color, not
  scoring. The actual bucketing logic is Phase 4's job (`HomeService` rewrite); this is a
  spec-language change only.
- **`ChallengeResult` gains `categoryTotals`** (`CategoryScoreTotal[]`/`DishScoreTotal[]`,
  new schemas) — the mockup's results screen wants a raw per-category, per-dish score
  table with a Total row, not just `categoryWinners`. `ResultCalculator` already computes
  these sums internally today and discards them (see
  `cookoff.domain.service.ResultCalculator`) — Phase 4 either exposes them from there or
  re-sums from `ScoreSubmission`s at the application layer; deliberately left as a Phase 4
  implementation choice, not decided here. The frontend sums the 3 category totals itself
  for the Total row — a plain arithmetic sum, not business logic, so it's fine to leave
  off the server.
- **New endpoints, per `frontend-prd.md` §7**: `POST
  /api/v1/challenges/{id}/color-pick` (link-token gated, cook-only, returns the updated
  `ParticipantChallengeResponse`), `PATCH`/`GET /api/v1/challenges/{id}/image`
  (multipart upload / raw-bytes stream), `POST
  /api/v1/challenges/{id}/registration-invites` (organizer/admin, returns `{token,
  registrationUrl}` — the controller builds the full URL the same way
  `SendChallengeInvitationsService` already does for access links), and `POST
  /api/v1/public/registrations` (new `Public` tag, unauthenticated, gated only by the QR
  token in the body).
- **Deferred to Phase 4/controller time, not decided here**: whether the create-challenge
  dialog's photo upload happens as part of `POST /api/v1/challenges` or as a same-dialog
  follow-up `PATCH .../image` call — `Challenge.create(...)` doesn't accept an image (Phase
  7.5 hard-codes `imageRef = null` at creation, only `changeImage(...)` sets it), so
  `CreateChallengeRequest` was **not** given an image field; the frontend does two calls,
  UX-sequenced as one dialog.
- Validated clean with `redocly lint` (0 errors; the 2 pre-existing warnings —
  missing `info.license`, `/config`'s GET having no 4xx response — are both expected: no
  license has been chosen for this project, and `/config` genuinely has no failure mode).
  Re-ran the Phase 2 Gradle codegen smoke test against the updated spec: 7 interfaces now
  (`AuthApi`/`AccountsApi`/`ChallengesApi`/`RivalriesApi`/`HomeApi` +
  new `ConfigApi`/`PublicApi`) and 60 model classes (up from 47), `compileJava` succeeds,
  all 198 backend tests still pass untouched. Phase 3 (domain gap-filling) not started.

**Phase 3 done (2026-08-03).** Gaps 7–12 in the "Domain gaps to fill" list below were
already built under `backend-persistence-api-security-plan.md`'s Phase 7 (commits `23e5ed1`
through `cc014e1`) before this session started, so the only work actually left in this
plan's own Phase 3 was gaps 3–6 — confirmed by re-reading both plans side by side rather
than assumed. Built, each with unit/integration test coverage in the same style as the
existing suite (Mockito-free domain tests + `@DataJpaTest` repository round-trips, no
application-service or controller work — that's still Phase 4/5):
- **Gap 3** — `CookRivalryRepository.findAll()`, backed by `CookRivalryJpaRepository`'s
  inherited `findAll()` (already present via `JpaRepository`, no new query needed).
- **Gap 4** — `ChallengeRepository.findByCookPair(AccountId, AccountId)`, matching either
  cook-A/B ordering via a new `@Query` on `ChallengeJpaRepository` (the same
  order-independence `CookRivalry.orderPair` already established for gap 3's aggregate,
  applied here as a query predicate instead of a canonicalized storage order, since
  `Challenge` has no equivalent pair-normalization step to reuse).
- **Gap 5** — `Account.changeEmail(Email)`. Turned out to be the only missing piece per the
  gap's own note: `grantRole`/`revokeRole` already existed, and `AccountRepository.save()`
  already upserts by TSID, so no new repository method was needed here — just the domain
  mutator (`email` field changed from `final` to mutable) plus a null guard, uniqueness
  staying a repository/application-layer concern per the existing `existsByEmail` pattern.
- **Gap 6** — `ScoreSubmission.update(List<Score>, Instant)` (scores/`submittedAt` fields
  changed from `final` to mutable) plus
  `ScoreSubmissionRepository.findByChallengeIdAndGuestAccountId(...)`, so a future
  `SubmitScoreService` rewrite (Phase 4) can look up an existing submission and call
  `update(...)` on it before `save()`, instead of hitting the `score_submissions` unique
  constraint `ScoreSubmissionRepositoryImpl.save()` currently turns into
  `DuplicateSubmissionException`. Verified with a repository-level round-trip test
  (`should_updateInPlace_when_savingAResubmittedScoreSubmission`) that saving the same
  aggregate twice after `update()` updates the row in place rather than violating the
  constraint — confirms the upsert path gap 6 called for actually works end-to-end at the
  persistence layer, not just at the domain-model level.
- Three existing test doubles (`InMemoryChallengeRepository`/`InMemoryScoreSubmissionRepository`/
  `InMemoryCookRivalryRepository` in `ChallengeRevealUnrevealRivalryIntegrationTest`) needed
  the three new interface methods stubbed to keep compiling — trivial no-op/pass-through
  implementations, no behavior change to that test.
- `./gradlew build` passes end to end: 207 tests (up from 198; 9 new), including the
  ArchUnit/jMolecules layering suite, all green. No controller or application-service work
  done here — that's still Phase 4/5, now unblocked for gaps 3–6 the same way Phase 7 already
  unblocked it for gaps 7–12.

**Phase 4 in progress (2026-08-03).** Started with the two genuinely-new use cases called
out in Phase 4's own description (gaps 3–5 from the list above) rather than the broader
rewrite of already-existing services, since these don't touch any currently-live controller
and give a self-contained, buildable increment:
- **`GetAccountDetailService`** (`auth.application.service`) — `execute(AccountId)`, returns
  the generated `Account` model directly (404 via the existing `AccountNotFoundException`).
- **`UpdateAccountService`** — `execute(AccountId, UpdateAccountRequest)`. `name`/`email` are
  applied only if non-null (partial update); `email` reuses `AccountAlreadyExistsException`
  for the 409-on-collision case, skipping the uniqueness check entirely when the email is
  unchanged. `roles` is spec'd as "replaces the account's full role set" but is a plain
  (non-`JsonNullable`) list, so an omitted field and an explicitly empty array deserialize
  identically — treated as "leave roles alone" rather than "clear all roles", since the
  latter is unreachable anyway (`Account.revokeRole` refuses to drop the last role). When
  roles *are* given, every target role is granted before any dropped role is revoked, so the
  account never transiently holds zero roles mid-update.
- **`RivalriesListService`** (`cookoff.application.service`, gap 3) — maps every
  `CookRivalryRepository.findAll()` row to the generated `Rivalry` model, resolving cook
  names via the existing `auth.AccountLookup` port (not `AccountRepository` directly — cross-
  module access stays behind the published interface, same pattern
  `EditChallengeParticipantsService` etc. already use).
- **`RivalryDetailService`** (gap 4) — joins `ChallengeRepository.findByCookPair(...)` (every
  shared challenge, revealed or not) with `CookRivalryRepository.findByPair(...)` (the
  win/loss record, present only once the pair has been revealed at least once). New
  `RivalryNotFoundException` (404, wired into `GlobalExceptionHandler`) fires only when
  *neither* exists for the pair — i.e. the two accounts have never even been scheduled
  together; a shared-but-unrevealed challenge still returns 200 with zeroed
  wins/draws/totals. The path params are canonicalized via `CookRivalry.orderPair(...)`
  before either repository call, so `{a}/{b}` and `{b}/{a}` return identically-shaped output.
- New shared **`RivalryHeadline`** helper builds the spec's server-rendered display text
  ("Alice leads Bob 3-1 (1 draw)"); reusable later by `GetChallengeResultsService`'s
  `ChallengeResult.rivalry` field (`RivalrySummary` schema, same shape), which is a separate,
  not-yet-done rewrite item below.
- New shared **`PagedResult<T>`** (`shared.web`) wraps a Spring Data `Page<T>` into the
  generated `Pagination` block, per `04-api-design.md`'s convention — used by
  `RivalriesListService` now, intended for reuse by the accounts/challenges list rewrites
  below. See the architecture follow-up entry immediately below for how this and the
  repository ports it wraps evolved after this increment first shipped.
- 24 new unit tests (Mockito-mocked repositories/ports, same style as the existing suite,
  plus direct tests for the two new pure-logic helpers). `./gradlew build` passes end to
  end: 230 tests (up from 207), including the jMolecules architecture suite.

**Architecture follow-up (2026-08-03, same day).** Reviewing this increment surfaced two
inconsistencies with the rest of the codebase, resolved with the user and recorded as ADRs
rather than left as implicit choices:
- **Repository ports relocated from `domain.repository` to `application.port`** — all five
  (`AccountRepository`, `ChallengeRepository`, `ScoreSubmissionRepository`,
  `PlateColorRepository`, `CookRivalryRepository`), across both modules. Rationale, full
  consequences, and the alternative considered:
  [`docs/cookingChallenge/adr/0002-repository-ports-in-application-layer.md`](../../adr/0002-repository-ports-in-application-layer.md).
  `docs/backend/02-ddd-modulith.md` updated to match. Pure mechanical rename (55 files,
  import-only changes) — no behavior change.
- **`AccountRepository`/`CookRivalryRepository`'s `findAll()` now take
  `org.springframework.data.domain.Pageable` and return `Page<T>`**, replacing the in-memory
  `PagedResult.of(List, page, size)` slicing this increment first shipped with. Real DB-level
  `LIMIT`/`OFFSET` now, not "load the whole table." `AccountRepositoryImpl`/
  `CookRivalryRepositoryImpl` delegate straight to `JpaRepository.findAll(Pageable)` (already
  free, no new query methods); `ListAccountsService` passes `Pageable.unpaged()` to keep its
  pre-existing "return everything" behavior until its own Phase 4 rewrite lands. Rationale:
  [`docs/cookingChallenge/adr/0003-spring-data-pageable-in-repository-ports.md`](../../adr/0003-spring-data-pageable-in-repository-ports.md).
  `ChallengeRepository.findAll()` and other not-yet-paginated list methods are untouched —
  this establishes the pattern, it doesn't retrofit every method in one pass.
- **`AccountModelMapper`** (domain `Account` → generated `Account` model, used by
  `GetAccountDetailService`/`UpdateAccountService`) converted from a hand-written static
  utility to a MapStruct `@Mapper(componentModel = "spring")` interface with a `default`
  method — matching `infrastructure/persistence`'s existing MapStruct mappers' own pattern
  for typed-VO conversions (`AccountMapper`'s hand-written `default toDomain`/`toEntity`,
  since `Account` has no public constructor MapStruct could target), extended here to the
  domain→generated-model direction per `docs/backend/03-code-style.md`'s Mapper Usage
  section (not previously scoped to that direction).
- `./gradlew build` passes end to end: 229 tests (one net fewer than the prior entry — a
  `PagedResult` test scenario specific to manual list-slicing no longer applies once it wraps
  a real `Page`), including the jMolecules/ArchUnit layering suite, confirming the relocation
  introduced no upward (domain → application) dependency.

**Still open in Phase 4** — the broader item the plan's own Phase 4 section calls for:
rewriting `CreateAccountService`/`ListAccountsService`/`LoginService` and every `cookoff`
use-case service (`CreateChallengeService`, `ListChallengesService`,
`GetChallengeStatusService`, `SendChallengeInvitationsService`, `RevealChallengeService`,
`GetChallengeResultsService`, `HomeService`, `GetChallengeForParticipantService`,
`SubmitScoreService`) to take/return generated types instead of the hand-written
`AccountView`/`ChallengeView`/etc. DTOs. Deliberately deferred past this increment: those
services' current callers are the **live** `AccountController`/`AuthController`/
`ChallengeController`/`HomeController` — rewriting a service's signature without also
swapping its controller in the same step would break a working endpoint, so that work is
sequenced together with Phase 5 (new controllers), one use case at a time, rather than done
as a separate Phase 4 pass first.

**Phase 5 started (2026-08-04) — `GET /api/v1/config`.** First endpoint wired end-to-end
against a generated interface; every other live endpoint still runs through the pre-OpenAPI
controllers listed under "Superseded" above. Chosen as the starting point because, unlike
every other row in the use-case inventory, it had no existing service to rewrite and no live
controller to swap out in the same step - a self-contained slice to establish the pattern.
- **New `shared.application.service.ConfigService`** + **`ConfigModelMapper`** (MapStruct,
  same pattern as `auth.application.service.AccountModelMapper`) + **new
  `shared.interfaces.rest.ConfigController`** implementing the generated `ConfigApi`.
  `SecurityConfig` gained a `permitAll` matcher for `GET /api/v1/config`, matching the
  spec's `security: []`.
- **New public contract `cookoff.PlateColors`/`PlateColorSummary`**, implemented by
  `cookoff.application.service.PlateColorsService` - discovered mid-implementation, not
  planned up front: `ModularityTests` failed the first pass because `shared` (an `OPEN`
  Spring Modulith module) can reach into `auth.application.service`/`auth.domain.model`
  freely only because those packages carry explicit `@NamedInterface` annotations: `OPEN`
  relaxes what other modules may reach *into `shared`*, not what `shared` itself may reach
  into a normal (closed) module like `cookoff`. `cookoff.domain.model` and
  `cookoff.application.port` (where `PlateColor`/`PlateColorRepository` live) have no such
  exposure, so `ConfigService` calling `PlateColorRepository` directly was a real boundary
  violation, not a false positive. Fixed the same way `auth.AccountLookup`/`AccountSummary`
  already solve this for `Account` - a root-package interface + plain summary record,
  keeping `PlateColor`/`PlateColorId`/`PlateColorRepository` internal to `cookoff`. This is
  a reusable precedent for every other cross-module field Phase 5's remaining controllers
  will need (e.g. resolving cook names for `Challenges`/`Rivalries` already goes through
  `AccountLookup` the same way).
- 5 new tests (`ConfigServiceTest`, `ConfigControllerTest`, `PlateColorsServiceTest`, plus a
  `should_return200_when_unauthenticatedRequestHitsConfigEndpoint` case added to the existing
  `SecurityIntegrationTest`). `./gradlew build` passes end to end: 234 tests (up from 229),
  including `ModularityTests`/`JMoleculesArchitectureTests`, all green.
- **Next**: Accounts group (`GetAccountDetailService`/`UpdateAccountService` already
  generated-type-ready; `CreateAccountService`/`ListAccountsService` still need the
  generated-type rewrite), one `AccountsController` implementing `AccountsApi`, then delete
  the old `AccountController` + its request records.

**Phase 5 continued (2026-08-04) — Accounts group.** `CreateAccountService` and
`ListAccountsService` rewritten to generated types, joining the already-generated-ready
`GetAccountDetailService`/`UpdateAccountService`; new `AccountsController` implements the
generated `AccountsApi` (all four operations); old `AccountController`,
`interfaces/rest.CreateAccountRequest`, `application/dto.AccountView`, and
`application/dto.CreateAccountCommand` deleted per the Superseded list.
`ListAccountsService` follows the same `PagedResult<T>` + `Pageable`/`PageRequest` shape
`RivalriesListService` established. `SecurityConfig` gained matchers for
`GET`/`PATCH /api/v1/accounts/*` (organizer+, same level as the existing list endpoint).
`SecurityIntegrationTest`'s account-creation test helpers moved off the deleted
`CreateAccountCommand`/`AccountView` onto the generated `CreateAccountRequest`/`Account`.
- **Bug found and fixed, not scoped to Accounts alone**: wiring `createAccount` surfaced
  that the project's Jackson 3 (`tools.jackson.databind`) `ObjectMapper` has no
  (de)serializer for `org.openapitools.jackson.nullable.JsonNullable` -
  `org.openapitools:jackson-databind-nullable` 0.2.6 only ships a Jackson 2
  (`com.fasterxml.jackson.databind`) module, and openapi-generator's `spring` templates
  wrap every `nullable: true` + optional schema property in `JsonNullable<T>` regardless of
  request vs. response. `CreateAccountRequest.password` was the first *request* field to hit
  this (deserialization threw `InvalidDefinitionException`, surfacing as a 500 through
  `GlobalExceptionHandler`); the same gap would have broken serialization the first time any
  already-generated *response* field using it (`CookAssignment.colorId`,
  `Challenge.overallWinnerAccountId`, `ParticipantChallenge.mySubmission`, etc. - 9 more
  occurrences in the spec today) got exercised by Phase 4/5 work on the Challenges group.
  Fixed generally, not by working around this one field: new
  `shared.config.jackson.{JsonNullableSerializer,JsonNullableDeserializer,JsonNullableModule}`
  (a small Jackson 3 `SimpleModule` - serialization just delegates to the wrapped value's
  runtime-type serializer via `SerializationContext#writeValue`; deserialization resolves the
  wrapped type `T` contextually per-property via `BeanProperty#getType()` +
  `JavaType#containedType(0)`, since `JsonNullable` itself carries no static type info),
  registered as a `JacksonModule` bean in new `shared.config.JacksonConfig` so Spring Boot
  4's Jackson autoconfiguration picks it up automatically for the real app. `@WebMvcTest`
  slices don't scan plain `@Configuration` classes, so `AccountsControllerTest`
  `@Import`s it explicitly (same pattern as `GlobalExceptionHandler`); a new
  `SecurityIntegrationTest` case
  (`should_return201_when_adminCreatesAccountWithPasswordOverHttp`) proves the fix through
  the real, non-mocked full app context, not just the slice test's manual import.
- New tests: `ListAccountsServiceTest` (didn't exist before, mirrors
  `RivalriesListServiceTest`), `AccountsControllerTest` (replaces the deleted
  `AccountControllerTest`, covers all four operations + 400/404/409), plus the
  `SecurityIntegrationTest` case above. `./gradlew build` passes end to end: 240 tests (up
  from 234), including `ModularityTests`/`JMoleculesArchitectureTests`, all green.
- **Not done in this increment**: `LoginService` and the `cookoff`-module use-case services
  listed under "Still open in Phase 4" above are unchanged - this pass was scoped to the
  Accounts group only, per the user's request.

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
3. ~~`CookRivalryRepository.findAll()`~~ — **resolved**, backed by the JPA repo's inherited
   `findAll()`.
4. ~~A `ChallengeRepository` query for "challenges between this cook pair"~~ — **resolved**,
   `findByCookPair(AccountId, AccountId)`, order-independent via an `OR`-matched `@Query`.
5. ~~`Account` update~~ — **resolved**: `grantRole`/`revokeRole` already existed;
   `changeEmail(Email)` was the only new mutator needed, `AccountRepository.save()` already
   upserts by id.
6. ~~`ScoreSubmission` update path~~ — **resolved**: `ScoreSubmission.update(List<Score>,
   Instant)` plus `ScoreSubmissionRepository.findByChallengeIdAndGuestAccountId(...)` for a
   future `SubmitScoreService` to look up-then-update instead of insert-and-catch-conflict.
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
mocks of repository ports), before any controller depends on them. For gaps 7–12 (the
PRD-driven ones), this is now `backend-persistence-api-security-plan.md`'s Phase 7 — that
doc owns the actual domain/application-service implementation and stops at that layer;
Phase 4 below picks up from its finished application services rather than duplicating them.

**Phase 4 — New application services.** One per use case, in
`{module}.application.service`, taking generated request models (or a thin command mapped
from one) and returning generated response models. `unreveal`, `edit participants`,
`color-pick`, `challenge image`, and `registration-invites`/self-registration are **not**
net-new here — `backend-persistence-api-security-plan.md` Phase 7 already builds those
application services against plain domain-shaped inputs/outputs; this phase only maps
generated request/response models onto calls to them, it doesn't duplicate the
orchestration logic. Still genuinely net-new in this phase: `edit account`,
`rivalries list/detail` (gaps 3–5 in the list above, unchanged by the PRD).

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
