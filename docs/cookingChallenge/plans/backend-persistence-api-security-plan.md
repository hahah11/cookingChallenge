# Backend Implementation Plan — Persistence, Application, API, Security

## Status

- **Done** (see `at.fraihs.cookoff.auth.domain.*` and `at.fraihs.cookoff.cookoff.domain.*`
  under `backend/src/main/java/`): pure-Java domain model — `Account`, `Challenge`,
  `ScoreSubmission`, `CookRivalry`, all value objects/enums, `ResultCalculator`,
  `ChallengeRevealed` event, and the three repository **ports** (interfaces only, no
  implementation yet). 42 domain unit tests pass. TSID (`shared/tsid/TsidSupport`) is the
  ID strategy for every aggregate root. `tsid-creator` + `postgresql` driver are already
  in `build.gradle.kts`; a Postgres service is in `compose.yaml`; the full Step 5 schema
  is in `backend/src/main/resources/db/changelog/`.
- **Done** (Phase 1 — see `at.fraihs.cookoff.{auth,cookoff}.application.*`): all application
  services, DTOs, and exceptions listed below, including `ChallengeRevealedRivalryUpdater`.
  `SendChallengeInvitationsService`/`NotificationPort` are still deferred to after Phase 3, as
  planned. `./gradlew test` passes (the only failure is `CookoffApplicationTests.contextLoads()`,
  expected until Phase 6 starts a real datasource).
- **Done** (Phase 2 — see `at.fraihs.cookoff.{auth,cookoff}.infrastructure.persistence`): all
  JPA entities, MapStruct mappers, Spring Data repositories, and `*RepositoryImpl` adapters for
  `Account`, `Challenge`, `ScoreSubmission`, and `CookRivalry`. `org.mapstruct:mapstruct:1.6.3` /
  `mapstruct-processor:1.6.3` added to `build.gradle.kts`. Every mapper is hand-written
  (`default` methods delegating to each aggregate's `reconstitute(...)` factory) rather than
  MapStruct-generated field mapping, since every domain aggregate here is immutable with no
  public constructor. `ScoreSubmissionRepositoryImpl.save()` uses `saveAndFlush` (not `save`) so
  the `score_submissions` unique-constraint violation surfaces synchronously and gets caught as
  `DuplicateSubmissionException`, instead of at some later, unrelated flush point.
  **Test database decision — H2 in-memory, not Testcontainers** (superseding the earlier
  Testcontainers decision this section used to document — see
  `docs/cookingChallenge/adr/0001-use-h2-in-memory-db-for-datajpatest.md` for the full record,
  including a non-obvious Spring Boot 4.1.0 pitfall found along the way): `@DataJpaTest`
  repository tests run against `com.h2database:h2` (`testRuntimeOnly`), configured via an
  **explicit** `spring.datasource.url` in `backend/src/test/resources/application.yaml` plus
  `@AutoConfigureTestDatabase(replace = Replace.NONE)` on every `@DataJpaTest` class — not Spring
  Boot's default auto-generated embedded datasource, which was found to deterministically corrupt
  a `CHECK` constraint's evaluation on this stack. `spring.jpa.hibernate.ddl-auto: none` is set
  globally in `backend/src/main/resources/application.yaml` so Hibernate never generates/drops
  schema — Liquibase (`db/changelog`) stays the single source of truth, in prod and in tests.
  Full suite (69 tests) now runs in ~11–13s with no Docker dependency; `CookoffApplicationTests`
  passes as a side effect too. Also note Spring Boot 4 moved
  `@DataJpaTest`/`@AutoConfigureTestDatabase`/`TestEntityManager` to new packages
  (`org.springframework.boot.data.jpa.test.autoconfigure`,
  `org.springframework.boot.jdbc.test.autoconfigure`, `org.springframework.boot.jpa.test.autoconfigure`
  respectively) — the old `org.springframework.boot.test.autoconfigure.orm.jpa`/`...jdbc` paths
  from Spring Boot 3.x no longer exist.
- **Done** (Phase 3 — see `at.fraihs.cookoff.auth.application.{port,service,exception}` and
  `at.fraihs.cookoff.auth.infrastructure.accesslink`): `AccessLinkService.issue(AccountId,
  long challengeId, Duration validFor)` / `.verify(String token)`, backed by an
  `AccessLinkRepository` port (`application/port/AccessLinkRepository.java` +
  `application/port/AccessLink.java`) and its `AccessLinkRepositoryImpl` JPA adapter —
  mirrors the Phase 2 port/adapter split so `AccessLinkService` stays testable with Mockito
  instead of needing Spring context. **Deviation from the plan's literal `issue(AccountId,
  ChallengeId, Duration)` signature**: `challengeId` is a raw `long`, not the cookoff
  module's typed `ChallengeId` — `cookoff` already depends on `auth` for `AccountId`
  (one-way), so giving `auth` a compile dependency back on `cookoff.domain.model.ChallengeId`
  would create a module cycle; a raw TSID long avoids it without losing anything (callers in
  `cookoff` just pass `challengeId.value()`). Token: `SecureRandom`-generated 256-bit,
  Base64url-encoded, **not** TSID (matches `docs/backend/03-code-style.md`'s ID-generation
  note that TSID is sortable/predictable and unfit as a secret). **Single-use vs reusable
  decision, as flagged in the plan**: implemented **reusable-until-expiry** — `verify()`
  never rejects on `usedAt`, matching the "home" flow across multiple open challenges. The
  `access_links.used_at` column is not currently populated by `verify()` (left `null`
  always) — no audit-stamping was requested; wire it up later if per-link "first opened"
  tracking is actually needed. `AccessLinkJpaEntity`/`AccessLinkJpaRepository` in
  `auth.infrastructure.accesslink`, per the plan. Unit tests
  (`AccessLinkServiceTest`, Mockito) cover issue→verify happy path, reuse of the same
  token, unknown-token rejection, and expired-token rejection; `AccessLinkRepositoryImplTest`
  (`@DataJpaTest`) round-trips a real row against H2, satisfying the `access_links` FK
  constraints by persisting `AccountJpaEntity`/`ChallengeJpaEntity` rows directly via
  `TestEntityManager` — the same cross-module JPA-entity-in-test pattern
  `ChallengeRepositoryImplTest` already uses for `AccountJpaEntity`. All 75 backend tests
  pass (69 prior + 6 new). Spring Security integration is still Phase 5, not built here, per
  the plan.
- **Done** (Phase 4 — see `at.fraihs.cookoff.{auth,cookoff}.interfaces.rest` and
  `at.fraihs.cookoff.shared.web`): all REST controllers from `first-plan.md` Step 3's API
  table except `AuthController` (still deferred — see below) and the optional
  `CookRivalryController`. Several deviations/decisions made along the way, recorded here
  per this plan's own instruction to flag them:
  - **Package name deviation: `interfaces/rest`, not `interface/rest`** — `interface` is a
    reserved Java keyword and cannot be a package segment, so
    `docs/backend/02-ddd-modulith.md`'s literal `interface/rest` structure doesn't compile.
    Used the common real-world workaround `interfaces/rest` in both modules instead.
  - **Finished the Phase 1 leftover first**: `cookoff.application.port.NotificationPort`
    (`sendAccessLink(Email, String link)`) and `SendChallengeInvitationsService` (issues
    one `AccessLinkService` token per distinct participant — both cooks + every guest,
    deduped — via a 30-day `Duration` constant, then calls the port) needed to exist
    before `POST /challenges/{id}/invitations` could be built, matching the original
    Phase 1 note ("implement after Phase 3"). `cookoff.infrastructure.notification.
    LoggingNotificationAdapter` is the stub adapter (`log.info`, no real email); the link
    URL is built from `app.frontend.base-url` (`application.yaml`, defaults to
    `http://localhost:4200`) — not yet the real frontend origin since it isn't scaffolded.
  - **Response envelope**: `shared.web.ApiResponse<T>`/`ApiMeta` (success) and
    `ApiErrorResponse`/`ApiErrorBody`/`ApiErrorDetail` (error), matching
    `docs/shared/04-api-design.md` exactly (no `pagination` block added — nothing in the
    Step 3 table needs it, so it was left out rather than built speculatively).
    `shared.web.GlobalExceptionHandler` maps every existing application exception; status
    code choices not explicitly in the docs: `ChallengeNotOpenException`/
    `DuplicateSubmissionException` → 409 (state conflict, not malformed request),
    `NotAParticipantException`/`ForbiddenException` → 403, `InvalidOrExpiredLinkException`
    → 401, not-found exceptions (including the new `ChallengeNotRevealedException` for
    `GET .../results` before reveal) → 404, bean-validation failures → 400 with a
    `details[]` field/message list, a bare `IllegalStateException` (e.g. double-reveal) →
    409, `IllegalArgumentException` (domain constructor guards) → 400.
  - **Guest identification, done now rather than deferred**: rather than trust a
    client-supplied `accountId` on link-token endpoints (`GET /challenges/{id}`,
    `GET .../results`, `POST .../scores`, `GET /me/home`), each of those controller
    methods takes a `token` query parameter and calls `AccessLinkService.verify(token)`
    directly to resolve the `AccountId` — the same lookup Phase 5's filter will eventually
    do, just at the controller instead of a shared `OncePerRequestFilter`. This satisfies
    the plan's "never let a client claim someone else's identity" rule immediately instead
    of leaving it unenforced until Phase 5; Phase 5 should replace the per-controller
    `verify(token)` calls with the filter setting the request principal (ideally via
    `@AuthenticationPrincipal`), not re-derive the identity logic from scratch.
  - **New guest-facing `ChallengeParticipantView` DTO** (`cookoff.application.dto`),
    distinct from the organizer-facing `ChallengeView`: omits the cook↔label mapping
    (`cookAssignments` is `null`) until `status == REVEALED`, matching the domain rule
    "the assignment is never exposed to guests before reveal" — `ChallengeView` always
    includes it and stays organizer-only. Used by both `GET /challenges/{id}` and
    `GET /me/home` so neither leaks the mapping early.
  - **New domain method `Challenge.isParticipant(AccountId)`** (guest OR either cook) —
    added for the new participant-gated services below rather than reusing
    `SubmitScoreService`'s existing inline `isGuest`-or-cook check, to avoid touching
    already-tested code per this repo's "only refactor code you touch" rule; the two
    checks are logically identical but literally duplicated. Fine as-is; worth
    consolidating if a third call site needs it.
  - **New read-model application services** (`cookoff.application.service`):
    `ListChallengesService` (organizer history list), `GetChallengeStatusService`
    (submission progress — counts only the pre-added *guest* list against
    `first-plan.md`'s literal "which guests have/haven't submitted" wording, not the two
    cooks, even though `SubmitScoreService` also accepts a cook's own submission),
    `GetChallengeForParticipantService` and `GetChallengeResultsService` (both
    `AccountId`-gated via `isParticipant`, throwing `NotAParticipantException` otherwise;
    `GetChallengeResultsService` additionally 404s via `ChallengeNotRevealedException`
    before `REVEALED` — results aren't persisted separately from the one-time
    `ChallengeRevealed` → `CookRivalry` update, so this recomputes them on every call via
    the existing `ResultCalculator` against the now-immutable stored submissions), and
    `HomeService` (open challenges via the new repository query below, filtered to ones
    the account hasn't submitted for yet).
  - **New repository methods, added when their controller needed them** (per the plan's
    own "don't retrofit speculatively" guidance): `AccountRepository.findAll()` (backs
    `GET /accounts`) and `ChallengeRepository.findOpenByParticipant(AccountId)` (backs
    `GET /me/home`; JPA adapter is a JPQL `@Query` left-joining the `guestAccountIds`
    element collection since Spring Data derived-query syntax can't express "cook A OR
    cook B OR in this collection").
  - **Interim `shared.config.SecurityConfig`**: `spring-boot-starter-security` was already
    on the classpath (added ahead of Phase 5), so its default auto-config locked every
    endpoint behind generated-password HTTP Basic the moment any controller existed —
    Phase 4 was otherwise unreachable/untestable. Added a minimal `SecurityFilterChain`
    bean (`csrf().disable()`, `anyRequest().permitAll()`) explicitly documented as a
    placeholder Phase 5 replaces wholesale with real JWT + link-token filters and
    per-endpoint role rules; `@WebMvcTest`s `@Import` it alongside `GlobalExceptionHandler`
    so the slice context sees it too.
  - **Organizer/admin role enforcement is still not wired up** for `POST /accounts`,
    `GET /accounts`, `POST /challenges`, `GET /challenges`, `GET .../status`,
    `POST .../reveal`, `POST .../invitations` — consistent with the pre-existing gap in
    `RevealChallengeService` (which never checked who was calling it, unlike
    `CreateChallengeService`, which already validates `canOrganize()` against an explicit
    `organizerAccountId` request field). This is explicitly Phase 5's job
    (`@PreAuthorize`/JWT-role matchers), not addressed here.
  - **`AuthController` / `POST /auth/login` deliberately not built** — the plan's own text
    says to stub it "after Phase 5's security beans exist"; building it now would mean
    wiring a fake `AuthenticationManager` just to remove it again in Phase 5, so it's left
    for that phase entirely.
  - **Spring Boot 4 / Jackson 3 note** (same spirit as Phase 2's `@DataJpaTest` package
    move): the JSON `ObjectMapper` used by `spring-boot-starter-webmvc-test` is
    `tools.jackson.databind.ObjectMapper` (Jackson 3), not `com.fasterxml.jackson.databind
    .ObjectMapper` (Jackson 2) — the old import compiles-fails with "package does not
    exist" since only `jackson-annotations` still lives under `com.fasterxml`.
  - `spring-boot-starter-validation` added to `build.gradle.kts` (`@Valid`/Bean Validation
    wasn't on the classpath before Phase 4's request DTOs needed it).
  - All 112 backend tests pass (75 prior + 37 new: `@WebMvcTest` for `AccountController`/
    `ChallengeController`/`HomeController`, Mockito unit tests for every new application
    service, plus new `ChallengeTest`/`ChallengeRepositoryImplTest` coverage for
    `isParticipant`/`findOpenByParticipant`).
- **Done** (Phase 5 — see `at.fraihs.cookoff.shared.{config,security}` and
  `at.fraihs.cookoff.auth.{application,interfaces.rest}`): real JWT + link-token security,
  replacing the Phase 4 permit-all placeholder. Deviations/decisions, flagged per this
  plan's own instruction:
  - **JWT signing key: in-memory RSA keypair, generated fresh on every startup**
    (`shared.config.JwtConfig`, `NimbusJwtEncoder`/`NimbusJwtDecoder` beans) — no external
    JWKS/rotation infra for this app's scale; every previously issued token is invalidated
    on restart. Revisit if multi-instance deployment or cross-restart token validity is
    ever needed.
  - **Roles claim**: JWT carries a `roles` claim (`List<String>` of `SystemRole` names,
    set at login time from the account's current roles — not re-checked per request).
    `shared.config.SecurityConfig#jwtAuthenticationConverter` maps it to
    `ROLE_*`-prefixed `GrantedAuthority`s via `JwtGrantedAuthoritiesConverter`.
  - **`POST /api/v1/auth/login`** (`auth.application.service.LoginService` +
    `auth.interfaces.rest.AuthController`): looks up the `Account` by email, verifies the
    password via `PasswordEncoder.matches` (`BCryptPasswordEncoder`,
    `shared.config.SecurityConfig#passwordEncoder`), then signs a JWT (`app.jwt.expiration`,
    default `PT12H`, `application.yaml`). Deliberately does not distinguish "unknown
    email" from "wrong password" from "no password ever set" — all three throw the same
    `InvalidCredentialsException` (→ 401 `INVALID_CREDENTIALS`) to avoid leaking which
    emails are registered.
  - **Password now settable at account creation**: `CreateAccountCommand`/
    `CreateAccountRequest`/`CreateAccountService` gained an optional `password` field
    (Phase 1 didn't have one; the plan's own Phase 5 section calls this out as needed
    "if not already covered by Phase 1"). Blank/absent leaves `passwordHash` unset, same
    as before — that account just can't pass `/auth/login` until a password is set later
    (no separate "set/reset password" endpoint was built; not asked for).
  - **Endpoint roles follow `first-plan.md`'s API table exactly, not just this plan's own
    coarser Phase 5 grouping**: `POST /api/v1/accounts` is `ADMIN`-only there (not
    "organizer/admin" as this plan's bullet list said) — `SecurityConfig` enforces the
    table's version since it's the more specific source.
  - **Link-token endpoints no longer call `AccessLinkService.verify(token)` per-controller**
    (Phase 4's stopgap) — `shared.security.AccessLinkAuthenticationFilter` (a
    `OncePerRequestFilter`, `addFilterBefore(..., BearerTokenAuthenticationFilter.class)`)
    now does this for exactly the four "link token" rows in the API table (`GET
    /me/home`, `GET /challenges/{id}`, `POST /challenges/{id}/scores`, `GET
    /challenges/{id}/results`, matched via `PathPatternRequestMatcher` — Spring Security
    7's replacement for the removed `AntPathRequestMatcher`), setting a
    `UsernamePasswordAuthenticationToken(AccountId, null, ROLE_LINK)` as principal.
    `ChallengeController`/`HomeController` now take `@AuthenticationPrincipal AccountId`
    instead of a `token` request param, per this plan's own instruction. On a
    missing/invalid/expired token the filter short-circuits and writes the
    `INVALID_OR_EXPIRED_LINK` 401 envelope itself (matching Phase 4's exact error
    contract) rather than deferring to `GlobalExceptionHandler`, since exceptions thrown
    inside the filter chain never reach `@RestControllerAdvice`.
  - **`shared.security.RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`**: write
    the shared `ApiErrorResponse` envelope (`UNAUTHENTICATED` 401 / `FORBIDDEN` 403)
    instead of Spring Security's default responses, for every other protected endpoint
    (JWT-gated organizer/admin routes with no/insufficient role).
  - **Sessions are stateless** (`SessionCreationPolicy.STATELESS`) — both auth mechanisms
    are per-request (bearer JWT, link token), no `HttpSession` involved.
  - **CORS deferred**, per the plan's own instruction — Angular still isn't scaffolded.
  - **Testing split**: per-controller `@WebMvcTest`s (`ChallengeControllerTest`,
    `HomeControllerTest`, `AccountControllerTest`) now disable the security filter chain
    entirely (`@AutoConfigureMockMvc(addFilters = false)`) and simulate the
    `AccountId`/JWT principal by setting `SecurityContextHolder` directly — the usual
    `.with(authentication(...))` `RequestPostProcessor` turned out to be a no-op with
    `addFilters = false` (it only stashes a context for `SecurityContextHolderFilter` to
    load, and that filter is exactly what's disabled), so slice tests set
    `SecurityContextHolder` directly instead, relying on MockMvc's single-threaded,
    synchronous dispatch. All real security enforcement (JWT roles, link-token
    validation/expiry, 401/403 envelopes) is instead covered by one new
    `@SpringBootTest`+`@AutoConfigureMockMvc` class, `shared.security.SecurityIntegrationTest`,
    matching this plan's own "Verify Phase 5" checklist line for line. New
    `LoginServiceTest` (Mockito) covers the login happy path and all three credential
    failure modes.
  - All 121 backend tests pass (112 prior + 11 new − 2 removed: the old per-controller
    `should_return401_when_tokenInvalid` tests in `ChallengeControllerTest`/
    `HomeControllerTest` no longer apply now that token verification moved to the filter,
    and are superseded by `SecurityIntegrationTest`'s equivalent).

- **Not started** (Phase 7 — see `docs/cookingChallenge/frontend-prd.md`): the domain/API
  deltas that PRD introduced on top of everything above — self-registration via
  organizer-generated QR, cook plate-color self-pick, editable cooks/guests/photo on open
  challenges, unreveal, a 1–5 (not 0–5) score scale, and narrowing who may score to guests
  + the challenge's creator. `docs/cookingChallenge/domain-model.puml` already reflects the
  target state; this plan's new Phase 7 section below sequences the implementation.

Read `docs/cookingChallenge/first-plan.md` (the domain/API/data-model design) and
`docs/cookingChallenge/domain-model.puml` before starting — this plan assumes that
design and does not re-derive it. Follow `docs/backend/01-architecture.md`,
`docs/backend/02-ddd-modulith.md`, and `docs/backend/03-code-style.md` for structure and
style; this plan only adds sequencing and cookoff-specific decisions on top.

**Order matters less within a phase than across phases** — application services (Phase
1) only need the domain repository *interfaces*, which already exist, so they compile
before the JPA adapters (Phase 2) exist. Implement and test each phase before moving to
the next; don't jump ahead to REST controllers before the service they call exists.

---

## Phase 1 — Application services

Package: `at.fraihs.cookoff.{module}.application.{service,dto}`, per
`docs/backend/02-ddd-modulith.md`'s module structure.

### 1.1 `auth` module

- `application/dto/CreateAccountCommand.java` — record: `email`, `name`,
  `Set<SystemRole> initialRoles`.
- `application/dto/AccountView.java` — record: `id` (as `String`, Base32 — never leak
  the raw `long` in a DTO), `email`, `name`, `roles`.
- `application/service/CreateAccountService.java` — `execute(CreateAccountCommand)`:
  reject if `accountRepository.existsByEmail(email)` (→ throw a new
  `AccountAlreadyExistsException`, mapped to 409 in Phase 4); otherwise
  `Account.create(...)`, save, return `AccountView`.
- No `UpdateAccountService`/role-management service yet unless you need it for Phase 4's
  `GET /api/v1/accounts` — keep to what's needed to unblock the API table in
  `first-plan.md` Step 3.

### 1.2 `cookoff` module

- `application/dto/CreateChallengeCommand.java` — record: `date`, `title` (nullable),
  `dishName`, `cookAAccountId`, `cookBAccountId`, `guestAccountIds`, `organizerAccountId`.
- `application/dto/SubmitScoreCommand.java` — record: `challengeId`, `guestAccountId`,
  `List<ScoreInput>` where `ScoreInput` is `{dishLabel, category, points}` (a DTO record,
  distinct from the domain `Score` VO — map one to the other inside the service).
- `application/dto/ChallengeView.java`, `SubmissionStatusView.java`,
  `ChallengeResultView.java` — read models for the controllers in Phase 4. Design these
  against the response shapes in `first-plan.md` Step 3's API table, not before it.
- `application/service/CreateChallengeService.java`:
  1. Load the organizer `Account` via `AccountRepository`; reject with
     `ForbiddenException` if `!account.canOrganize()`.
  2. `Challenge.create(...)`, save via `ChallengeRepository`.
  3. No email dispatch here — that's the separate "send links" action (1.2, next bullet).
- `application/service/SendChallengeInvitationsService.java`:
  1. Load the `Challenge`; collect the two cook `AccountId`s + guest `AccountId`s.
  2. For each, issue an access-link token — this depends on Phase 3 (`AccessLinkService`);
     implement this service *after* Phase 3, not before.
  3. Delegate actual email sending to a `NotificationPort` interface
     (`application/port/NotificationPort.java`, one method: `sendAccessLink(Email,
     String link)`); the Phase 2 infra step provides a stub/logging adapter — do not wire
     a real email provider unless asked.
- `application/service/SubmitScoreService.java`:
  1. Load `Challenge`; reject if not `OPEN`.
  2. Reject if `!challenge.isGuest(guestAccountId)` **and** the account isn't one of the
     two cooks either (re-check against `first-plan.md`: cooks and guests can both score
     — confirm this against the challenge's cook assignments too, not just
     `isGuest`). If neither, throw `NotAParticipantException`.
  3. Reject with `DuplicateSubmissionException` (→ 409 in Phase 4) if
     `scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(...)` is true.
  4. Map `ScoreInput` DTOs → domain `Score` VOs, `ScoreSubmission.submit(...)`, save.
- `application/service/RevealChallengeService.java`:
  1. Load `Challenge`; load all its submissions via
     `scoreSubmissionRepository.findByChallengeId(...)`.
  2. `new ResultCalculator().calculate(challenge, submissions)` → `ChallengeResult`.
  3. `challenge.reveal(result.overallWinnerAccountId())` → `ChallengeRevealed` event;
     save the challenge.
  4. Publish the event via `ApplicationEventPublisher` (`eventPublisher.publishEvent(...)`)
     — **do not** update `CookRivalry` directly in this service. Instead:
- `application/event/ChallengeRevealedRivalryUpdater.java` — `@Component`,
  `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` on
  `ChallengeRevealed`: `cookRivalryRepository.findByPair(cookA, cookB)` →
  `.orElseGet(() -> CookRivalry.start(cookA, cookB))` → `.recordResult(overallWinnerAccountId)`
  → save. This is the "only domain event in the model" from `first-plan.md` — keep it
  that way; don't add more events speculatively.

**Verify Phase 1**: `./gradlew compileJava` succeeds (services compile against the
existing domain repository interfaces even with no implementation registered yet — Spring
context won't start, but that's expected until Phase 2). Write unit tests per service
with Mockito mocks of the repository ports (`@ExtendWith(MockitoExtension.class)`,
matching `docs/backend/03-code-style.md`'s Unit Tests section) — cover the reject paths
(not organizer, already submitted, not a participant, challenge not open) as well as the
happy path.

---

## Phase 2 — JPA infrastructure adapters

Package: `at.fraihs.cookoff.{module}.infrastructure.persistence`. All JPA entity IDs are
the raw TSID `long` (matching the `BIGINT` columns from the Phase-0 changelog); mappers
convert to/from the domain's typed ID records (`new ChallengeId(entity.getId())` /
`.value()`). Never expose the JPA entity outside `infrastructure/persistence`.

Add `org.mapstruct:mapstruct` (`implementation`) + `org.mapstruct:mapstruct-processor`
(`annotationProcessor`) to `build.gradle.kts` at the start of this phase — not present yet.
Every `*Mapper` below (`AccountMapper`, `ChallengeMapper`, `ScoreSubmissionMapper`,
`CookRivalryMapper`) is a MapStruct `@Mapper(componentModel = "spring")` interface, per
`docs/backend/03-code-style.md#mapper-usage-mapstruct` — not a hand-written class.

### 2.1 `auth` module

- `AccountJpaEntity` — `@Entity @Table(name = "accounts")`: `id` (`Long`, no
  `@GeneratedValue` — the domain assigns the TSID before the entity is ever built),
  `email`, `name`, `passwordHash`, and `roles` as
  `@ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "account_roles",
  joinColumns = @JoinColumn(name = "account_id")) @Column(name = "role") @Enumerated(EnumType.STRING)
  Set<SystemRole>` (reuse the domain enum directly in the JPA entity here — it's a plain
  enum with no domain behavior, so this is fine per
  `docs/backend/03-code-style.md`, unlike entities/VOs with behavior).
- `AccountMapper` — `toDomain(AccountJpaEntity)` / `toEntity(Account)`. Domain `Account`
  has no public all-args constructor deliberately (`create`/`reconstitute` factories) —
  use `Account.reconstitute(...)` in `toDomain`.
- `AccountRepositoryImpl implements AccountRepository` — wraps a
  `AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long>` (add
  `findByEmail(String)`, `existsByEmail(String)` query methods on the Spring Data
  interface; the domain repo's `Email` VO unwraps to `.value()` before calling through).

### 2.2 `cookoff` module

- `ChallengeJpaEntity` — `id`, `title`, `challengeDate`, `dishName`, `cookAAccountId`,
  `cookBAccountId` (both `Long`), `status` (`@Enumerated(STRING)`),
  `createdByAccountId`, `createdAt`, and `guestAccountIds` as
  `@ElementCollection @CollectionTable(name = "challenge_guests", joinColumns =
  @JoinColumn(name = "challenge_id")) @Column(name = "guest_account_id") List<Long>` —
  the `challenge_guests.id` surrogate column from the changelog is intentionally left
  unmapped (Hibernate's `@ElementCollection` doesn't need it; it exists in the schema
  purely so the table has a natural PK for tooling, not for JPA).
- `ChallengeMapper` — note `CookAssignment` isn't stored as its own column pair in the
  domain reconstruction the way the DB stores it (`cook_a_account_id`/`cook_b_account_id`
  columns imply the label): reconstruct
  `List.of(new CookAssignment(new AccountId(entity.getCookAAccountId()), DishLabel.A),
  new CookAssignment(new AccountId(entity.getCookBAccountId()), DishLabel.B))` in
  `toDomain`, and read `challenge.cookAssignmentFor(DishLabel.A).accountId()` /
  `...B...` back out in `toEntity`.
- `ChallengeRepositoryImpl implements ChallengeRepository`.
- `ScoreSubmissionJpaEntity` — `id`, `challengeId`, `guestAccountId`, `submittedAt`, and
  `scores` as `@ElementCollection @CollectionTable(name = "scores", joinColumns =
  @JoinColumn(name = "submission_id")) List<ScoreEmbeddable>` where `ScoreEmbeddable` is
  an `@Embeddable` with `dishLabel` (`@Enumerated(STRING)`), `category`
  (`@Enumerated(STRING)`), `points` (`int`) — a JPA-only mirror of the domain `Score`
  record, mapped 1:1 in `ScoreSubmissionMapper`.
- `ScoreSubmissionRepositoryImpl implements ScoreSubmissionRepository` — the
  `existsByChallengeIdAndGuestAccountId` port method maps directly to a Spring Data
  derived query; this is also where the `score_submissions` unique constraint from the
  changelog is your real safety net — catch `DataIntegrityViolationException` on save
  and rethrow as the application layer's `DuplicateSubmissionException` too, in case of a
  race between the `exists` check and the `save` (the DB constraint is authoritative, the
  application-layer check is just a friendlier fast path).
- `CookRivalryJpaEntity` + `CookRivalryMapper` + `CookRivalryRepositoryImpl` — the
  `findByPair` port method must normalize the pair the same way
  `CookRivalry.orderPair(...)` does before querying, so it hits the DB's
  `chk_cook_rivalries_ordered_pair`-backed row instead of missing it due to argument
  order.

**Verify Phase 2**: `@DataJpaTest` repository integration tests per
`docs/backend/03-code-style.md`'s Integration Tests section, one per aggregate — at
minimum: save-then-find-by-id round-trips the full aggregate (including collections), the
`score_submissions` unique constraint actually rejects a duplicate
`(challenge_id, guest_account_id)` insert, and `cook_rivalries`' ordered-pair `CHECK`
rejects an unordered insert. These need a real Postgres — either start `compose.yaml`
(`docker compose up -d` from `backend/`) and point the test at it, or introduce
Testcontainers (`org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter`) —
pick Testcontainers if you want tests to run without a manually-started DB; flag this
choice to the user if not already decided.

---

## Phase 3 — Access-link mechanism

This implements the "personalized link, not password login" flow from
`first-plan.md`'s auth section. It's infrastructure/interface-layer, not a new domain
aggregate — don't add it to `domain-model.puml`.

- Schema: `access_links` table already exists (Phase 0 changelog) —
  `(id, account_id, challenge_id, token, expires_at, used_at)`.
- `at.fraihs.cookoff.auth.infrastructure.accesslink.AccessLinkJpaEntity` +
  `AccessLinkJpaRepository extends JpaRepository<AccessLinkJpaEntity, Long>` with a
  `findByToken(String)` derived query.
- `at.fraihs.cookoff.auth.application.service.AccessLinkService`:
  - `issue(AccountId, ChallengeId, Duration validFor)` — generate a high-entropy token
    (`java.security.SecureRandom`, ≥256 bits, Base64url-encoded — **not** TSID; TSID is
    sortable/predictable and must never be used as a secret, per
    `docs/backend/03-code-style.md`'s ID-generation note), persist, return the token
    string for `SendChallengeInvitationsService` (Phase 1) to embed in the emailed URL.
  - `verify(String token)` — look up by token; reject (throw
    `InvalidOrExpiredLinkException`) if missing or `expiresAt` has passed. Decide now
    (and record the decision in this file once made) whether a link is single-use
    (`usedAt` set on first verification) or reusable until expiry — `first-plan.md`
    describes a "home" the participant returns to across multiple open challenges, which
    reads as **reusable until expiry**, not single-use; single-use would break the "home"
    flow after the first click. Implement reusable-until-expiry unless the user says
    otherwise.
- Spring Security integration happens in Phase 5, not here — this phase only builds the
  issue/verify service and its persistence; keep it framework-agnostic where possible
  (no `SecurityContext` references in this class).

**Verify Phase 3**: unit test `AccessLinkService` with a fake/in-memory repository impl
or `@DataJpaTest`; cover issue→verify happy path, expired-token rejection, and
unknown-token rejection.

---

## Phase 4 — REST controllers

Package: `at.fraihs.cookoff.{module}.interface.rest`. Build the endpoint table from
`docs/cookingChallenge/first-plan.md` Step 3 exactly — don't invent new endpoints or
change paths without checking that table first (update it there if a real
implementation need forces a change, rather than letting docs and code drift).

- Response envelope: `data`/`meta` success shape and `code`/`message`/`details` error
  shape from `docs/shared/04-api-design.md` — implement once as generic
  wrapper types (e.g. `ApiResponse<T>`, `ApiError`) shared across both modules'
  `interface/rest` packages (this is the one place a tiny shared-kernel-style utility
  class is justified, since duplicating the envelope per controller would violate DRY
  for no benefit).
- `at.fraihs.cookoff.shared.web.GlobalExceptionHandler` (`@RestControllerAdvice`):
  map `AccountAlreadyExistsException` → 409, `DuplicateSubmissionException` → 409,
  `NotAParticipantException` → 403, `ForbiddenException` → 403, a not-found case (add a
  `ChallengeNotFoundException`/`AccountNotFoundException` if you don't have one yet) →
  404, `IllegalArgumentException` (bubbling up from domain constructors) → 400. Follow
  `docs/backend/03-code-style.md`'s Exception Handling section — domain-specific
  exceptions, not generic ones.
- `auth/interface/rest/AuthController` — `POST /api/v1/auth/login` (delegates to Spring
  Security's `AuthenticationManager`, built in Phase 5 — stub this controller last within
  this phase, after Phase 5's security beans exist).
- `auth/interface/rest/AccountController` — `POST`/`GET /api/v1/accounts`.
- `cookoff/interface/rest/ChallengeController` — `POST /api/v1/challenges`,
  `GET /api/v1/challenges`, `GET /api/v1/challenges/{id}`,
  `GET /api/v1/challenges/{id}/status`, `POST /api/v1/challenges/{id}/reveal`,
  `GET /api/v1/challenges/{id}/results`, `POST /api/v1/challenges/{id}/invitations`.
- `cookoff/interface/rest/ScoreController` (or a method on `ChallengeController` — pick
  one, don't split arbitrarily) — `POST /api/v1/challenges/{id}/scores`.
- `cookoff/interface/rest/HomeController` — `GET /api/v1/me/home`. This is a read model
  aggregating "open challenges where I'm a cook or guest and haven't submitted yet" —
  needs a new query method on `ChallengeRepository` (e.g.
  `findOpenByParticipant(AccountId)`) plus a per-challenge check against
  `ScoreSubmissionRepository`; add this query method to the Phase 1/2 repository
  port+adapter when you reach this controller, don't retrofit Phase 1/2 speculatively
  before it's needed.
- `cookoff/interface/rest/CookRivalryController` — optional per `first-plan.md`
  ("can defer") — build last, or skip until asked.
- **Guest identification**: link-token endpoints identify the guest via the token
  (Phase 3 + Phase 5's authentication filter sets the `AccountId` principal), never via a
  request-body `guestId`/`accountId` field — don't let a client claim someone else's
  identity by passing an arbitrary ID.

**Verify Phase 4**: `@WebMvcTest` per controller with mocked application services —
assert status codes for each row's happy path and at least one documented error case
(409 on duplicate submission, 403/404 as applicable) per
`docs/backend/03-code-style.md`'s Integration Tests section.

---

## Phase 5 — Security config

- Add `org.springframework.boot:spring-boot-starter-oauth2-resource-server` (brings
  `spring-security-oauth2-jose`/Nimbus) for JWT encode/decode — avoids hand-rolling JWT
  handling. This is a dependency addition the implementing agent should make explicitly
  in `build.gradle.kts`, not assume is already present.
- `PasswordEncoder` bean: `BCryptPasswordEncoder`. Only `ORGANIZER`/`ADMIN` accounts ever
  get a `passwordHash` set (per `first-plan.md`) — build the "set/reset password" flow as
  part of `AccountController`/`CreateAccountService` if not already covered by Phase 1.
- Two independent authentication mechanisms, both populating a `AccountId`-based
  principal + `SystemRole`-derived `GrantedAuthority`s (`ROLE_ADMIN`, `ROLE_ORGANIZER`,
  `ROLE_USER`):
  1. **JWT** (`Authorization: Bearer ...`) — for `/api/v1/auth/login` and
     organizer/admin-only endpoints (`POST /api/v1/challenges`, `GET /api/v1/challenges`,
     `POST .../invitations`, `POST .../reveal`, `POST/GET /api/v1/accounts`).
  2. **Link-token filter** (custom `OncePerRequestFilter`) — reads the token (query param
     or header, pick one and use it consistently across every link-based endpoint from
     Phase 4), calls `AccessLinkService.verify(token)` (Phase 3), sets the resulting
     `AccountId` as principal for `/api/v1/me/home`, `GET /api/v1/challenges/{id}`,
     `POST /api/v1/challenges/{id}/scores`, `GET /api/v1/challenges/{id}/results`,
     `GET /api/v1/challenges/{id}/status` (host-only per the API table — double check
     that one actually needs JWT/organizer, not a link token, since `status` is listed
     as host-only in `first-plan.md`).
  3. Use `@PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")` (or a
     `SecurityFilterChain` `authorizeHttpRequests` matcher — pick one style consistently)
     to enforce role checks; don't duplicate the `canOrganize()` domain-layer check *and*
     a security-layer check with different logic — keep one source of truth for "who can
     organize" (the security layer should just check the role claim baked into the JWT
     at login time; `Account.canOrganize()` stays the domain-layer invariant used at
     account-creation/role-grant time).
- **CORS**: needed once the Angular frontend exists — add a permissive-for-localhost
  `CorsConfigurationSource` now if frontend scaffolding is imminent, otherwise defer;
  don't guess the frontend's origin before it's scaffolded.

**Verify Phase 5**: `@SpringBootTest` + `spring-boot-starter-security-test`'s
`MockMvc` security helpers — assert an unauthenticated request to an organizer-only
endpoint gets 401/403, a `USER`-role JWT gets 403 on organizer-only endpoints, a valid
link token succeeds on guest endpoints, and an expired/invalid link token gets 401.

---

## Phase 6 — Wire it together, end-to-end check

Not a new module — a checkpoint. Once Phases 1–5 are done:

1. `docker compose up -d` (from `backend/`) to start Postgres from `compose.yaml`.
2. `./gradlew bootRun` (or the IDE run config) — Liquibase should apply the Phase 0
   schema on startup; check the logs for the 8 changesets running cleanly.
3. Manual smoke test matching `first-plan.md`'s "Key flows": create two organizer/cook/
   guest accounts → create a challenge → send invitations → submit as both guests →
   check status → reveal → confirm the results and `CookRivalry` counters match a
   hand-calculated expectation.
4. Re-enable `CookoffApplicationTests.contextLoads()` if it was left failing during
   earlier phases (it was failing at the end of the domain-scaffolding session purely
   because no datasource was running — this phase's `docker compose up` fixes that; if
   it still fails here, that's a real regression to fix, not an environment gap).

---

## Phase 7 — PRD domain deltas

Source of truth for everything in this phase: `docs/cookingChallenge/frontend-prd.md` §5–§7
and the corresponding sections of `docs/cookingChallenge/domain-model.puml`. Each
sub-section below is independently implementable/testable — order within the phase mostly
doesn't matter except 7.2 before 7.3 (color-pick needs `PlateColor` to exist) and 7.6
depending on nothing else added here. Follow the same package/testing conventions as
Phases 1–5 (Mockito unit tests for services, `@DataJpaTest` for repository adapters,
`@WebMvcTest` for controllers).

### 7.1 Score scale: 0–5 → 1–5

- `cookoff.domain.model.Score`'s constructor guard changes from `points < 0 || points > 5`
  to `points < 1 || points > 5`.
- New Liquibase changeset dropping the existing `scores` check constraint and re-adding it
  as `points between 1 and 5`.
- `ScoreEntryRequest`'s bean-validation annotation (`@Min`/`@Max` or equivalent) updates to
  match.
- Update every existing test that exercises `points = 0` as a valid case — it's now an
  invalid-input case (400), not a boundary-valid one.

**Verify 7.1**: `ScoreTest` covers `points = 0` now throwing `IllegalArgumentException`,
`points = 1`/`points = 5` as the new valid boundaries; `@DataJpaTest` confirms the DB
constraint rejects `0`.

### 7.2 `PlateColor` reference data

- `cookoff.domain.model.PlateColorId` — TSID value object, same pattern as `ChallengeId`.
- `cookoff.domain.model.PlateColor` — `AggregateRoot`: `id`, `name`, `hexCode`,
  `sortOrder`, `active`. Only a `create(...)`/`reconstitute(...)` factory pair and getters
  for now — no rename/deactivate mutation methods yet, since no admin screen consuming them
  is in scope (per this plan's own "don't retrofit speculatively" precedent); add those
  when an actual admin-facing color-management screen is requested.
- `cookoff.domain.repository.PlateColorRepository` port: `findAllActiveOrderedBySortOrder()
  : List<PlateColor>`, `findById(PlateColorId) : Optional<PlateColor>`.
- Liquibase: `plate_colors(id BIGINT PK, name VARCHAR(255), hex_code VARCHAR(7), sort_order
  INT, active BOOLEAN)`, plus a data-seed changeset inserting exactly 2 default rows
  (`sort_order` 1 and 2, `active = true`). **Placeholder hex values** — cross-check the
  actual "Modernist" design-system tokens (`design-reference.md`) for the real Red/Yellow
  hex before this ships; don't invent brand colors here.
- `cookoff.infrastructure.persistence.PlateColorJpaEntity` / `PlateColorMapper` /
  `PlateColorRepositoryImpl` — same hand-written-mapper-via-`reconstitute` pattern as every
  other aggregate in Phase 2.

**Verify 7.2**: `@DataJpaTest` round-trips a `PlateColor`; confirms
`findAllActiveOrderedBySortOrder()` returns only `active = true` rows, ordered.

### 7.3 Cook plate-color self-pick

- `cookoff.domain.model.CookAssignment` gains `colorId : PlateColorId?` (nullable).
- Liquibase: `challenges` + `cook_a_color_id` / `cook_b_color_id` (nullable `BIGINT`, FK →
  `plate_colors(id)`).
- `ChallengeMapper` extends its existing `CookAssignment` reconstruction (see Phase 2's
  note on `cook_a_account_id`/`cook_b_account_id` implying the label) to also carry the two
  nullable color columns into/out of each `CookAssignment`.
- `Challenge.pickColor(AccountId cookAccountId, PlateColorId chosenColorId, PlateColorId
  otherColorId)` — `requireOpen()`-gated; throws if `cookAccountId` isn't one of the two
  `CookAssignment`s, or if both are already color-assigned (irreversible-once-picked).
  Assigns `chosenColorId` to the picking cook and `otherColorId` to the other, atomically.
  Synchronous, no domain event (see `frontend-prd.md` §5.2 — deliberately not adding one;
  `first-plan.md` warns against speculative events and nothing outside `Challenge` needs to
  react to this today).
- New `cookoff.application.service.PickColorService`: loads the `Challenge`, calls
  `plateColorRepository.findAllActiveOrderedBySortOrder()`, takes the first 2, validates
  the requested `colorId` is one of them, resolves "the other" as whichever of the 2 it
  isn't, calls `challenge.pickColor(...)`, saves. Rejects with `NotAParticipantException`
  if the caller isn't one of this challenge's two cooks.
- New REST: `POST /api/v1/challenges/{id}/color-pick` (link-token gated, cook only).

**Verify 7.3**: unit tests for `Challenge.pickColor` (reject: not open, not a cook, already
assigned; happy path: both colors set atomically) and `PickColorService` (Mockito); a
`@WebMvcTest`/`@DataJpaTest` round-trip confirming the FK columns persist.

### 7.4 Edit cooks & guests (+ color reset)

- `Challenge.editParticipants(AccountId? newCookAAccountId, AccountId? newCookBAccountId,
  List<AccountId> guestIdsToAdd, List<AccountId> guestIdsToRemove)` —
  `requireOpen()`-gated, replaces the additive-only `addGuest`. **Reassigning either cook
  clears both `CookAssignment.colorId`s** (a pick is meaningless once the person behind the
  label changes) — implement this as part of the same method, not a separate call, so it
  can't be forgotten by a caller.
- Decide the removed `addGuest` fate now that `editParticipants` supersedes it: delete it
  and update `CreateChallengeService`/any other caller to the new method, per this repo's
  "don't leave both versions around" rule from the earlier OpenAPI plan.
- New `cookoff.application.service.EditChallengeParticipantsService`: organizer/admin-gated
  (`account.canOrganize()`), loads `Challenge`, calls `editParticipants(...)`, saves.
- New REST: `PATCH /api/v1/challenges/{id}/participants` (organizer/admin only).

**Verify 7.4**: unit tests for `Challenge.editParticipants` (cook reassignment clears both
colors; guest add/remove; reject when not open) and the new service's organizer-gate reject
path.

### 7.5 Challenge photo

- `cookoff.application.port.ImageStoragePort`: `store(byte[] bytes, String contentType) :
  String (imageRef)` / `resolve(String imageRef) : StoredImage {bytes, contentType}` /
  `delete(String imageRef)`.
- `Challenge` gains `imageRef : String?`; new method `changeImage(String newImageRef)` —
  `requireOpen()`-gated, same guard style as `editParticipants`.
- Liquibase: `challenges` + `image_ref` (nullable `VARCHAR`); new table
  `challenge_images(id BIGINT PK, content_type VARCHAR(255), data BYTEA, created_at
  TIMESTAMP)` — the initial adapter's own storage, not FK'd from `challenges` (the
  relationship is adapter-interpreted via `imageRef`, per `frontend-prd.md` §6).
- `cookoff.infrastructure.image.DatabaseImageStorageAdapter implements ImageStoragePort` —
  writes/reads `challenge_images` directly via a plain Spring Data repository; `imageRef`
  is that row's id as text. Mirrors the `NotificationPort` →
  `LoggingNotificationAdapter` port/stub-adapter precedent from Phase 1 — a real
  object-storage adapter (S3-compatible) is a later, explicitly-requested swap, not built
  here.
- New `cookoff.application.service.ChangeChallengeImageService`: organizer/admin-gated,
  calls `ImageStoragePort.store(...)` then `challenge.changeImage(ref)`; if replacing an
  existing image, calls `ImageStoragePort.delete(oldRef)` after the new one is persisted,
  not before (don't orphan the challenge with no image if the new upload fails partway).
- New REST: `PATCH /api/v1/challenges/{id}/image` (multipart upload, organizer/admin only),
  `GET /api/v1/challenges/{id}/image` (streams resolved bytes, same visibility as the
  challenge itself — organizer JWT or a valid link token).

**Verify 7.5**: unit test for `Challenge.changeImage`; `@DataJpaTest` round-trips a blob
through `DatabaseImageStorageAdapter`; `@WebMvcTest` for the upload/download endpoints
(content-type handling, 404 when no image set).

### 7.6 Unreveal + `CookRivalry` reversal

- `Challenge` gains `lastRevealResult : AccountId?` — needs a 3-state encoding (never
  revealed / revealed with a winner / revealed as a draw), not a bare nullable
  `AccountId?` collapsing the last two. Use a small wrapper, e.g.
  `Optional<RevealResult>` where `RevealResult` wraps a nullable winner id, or an explicit
  `hasBeenRevealed : boolean` flag alongside the nullable `AccountId` — pick one and be
  consistent with how `ChallengeResult.overallWinnerAccountId()` already encodes "draw" as
  `null`; the gap this closes is only "never revealed" vs. "revealed, drew" collapsing to
  the same `null`.
- `Challenge.reveal(AccountId overallWinnerAccountId)` — before publishing
  `ChallengeRevealed`, also sets `lastRevealResult` from the same value.
- New `Challenge.unreveal()` — requires `status == REVEALED`; flips back to `OPEN`,
  publishes a new `cookoff.domain.event.ChallengeUnrevealed { challengeId,
  previousOverallWinnerAccountId }` built from `lastRevealResult`, then clears
  `lastRevealResult`.
- `CookRivalry.reverseResult(AccountId? previousOverallWinnerAccountId)` — exact inverse of
  `recordResult`: decrements the matching win/draw counter and `totalChallenges`.
- New `cookoff.application.event.ChallengeUnrevealedRivalryUpdater` — `@Component`,
  `@TransactionalEventListener(phase = AFTER_COMMIT)` on `ChallengeUnrevealed`, mirrors
  `ChallengeRevealedRivalryUpdater` exactly: `cookRivalryRepository.findByPair(cookA,
  cookB)` → `.reverseResult(previousOverallWinnerAccountId)` → save.
- New `cookoff.application.service.UnrevealChallengeService`: organizer/admin-gated, loads
  `Challenge`, calls `unreveal()`, publishes the event via `ApplicationEventPublisher` (same
  pattern as `RevealChallengeService` — **not** a direct `CookRivalry` call), saves.
- Liquibase: `challenges` + whatever columns the `lastRevealResult` encoding above needs
  (at minimum a nullable `AccountId`-typed column; add a boolean if that encoding is
  chosen).
- New REST: `POST /api/v1/challenges/{id}/unreveal` (organizer/admin only, confirmation is
  a frontend-only concern — no special backend handling beyond the state-transition guard).

**Verify 7.6**: unit tests for `Challenge.unreveal` (reject if not `REVEALED`) and
`CookRivalry.reverseResult` (decrements correctly for a win and for a draw); an integration
test covering reveal → unreveal → re-reveal with a *different* computed result (scores
edited in between) confirming `CookRivalry`'s counters end up correct, not double- or
under-counted — this is the scenario the whole mechanism exists for.

### 7.7 Scoring eligibility: guests + creator only, not cooks

- `Challenge` gains `canScore(AccountId accountId) : boolean` = `isGuest(accountId) ||
  accountId.equals(createdBy)`. Deliberately not reusing `isParticipant` (which still means
  "guest or either cook" for its existing non-scoring call sites, e.g. viewing a challenge)
  — a new, narrower predicate for the one call site that needs it.
- `SubmitScoreService` — replace its existing "guest or either cook" check with
  `challenge.canScore(accountId)`; the `NotAParticipantException` reject path is unchanged,
  just the predicate behind it.
- `GetChallengeStatusService`'s submission-progress count already only counts the pre-added
  guest list per `first-plan.md`'s original wording — confirm it doesn't need a change now
  that the organizer can also score (does "submission progress" surface the organizer's own
  status? Check the mockup's guest-list UI — if the organizer isn't shown there, this is a
  cosmetic decision, not a domain one; flag to the user if it needs a call).

**Verify 7.7**: `SubmitScoreServiceTest` gains a case asserting a cook's submission attempt
now throws `NotAParticipantException` (previously accepted), and a case asserting the
challenge's `createdBy` account can submit even without being a pre-added guest.

### 7.8 Self-registration via organizer-generated QR

- `auth` module, infrastructure-layer like `AccessLink` (not added to `domain-model.puml`,
  same reasoning as `AccessLink` — see `frontend-prd.md`'s "why isn't this in the puml"
  discussion):
  - Liquibase: `registration_invites(id BIGINT PK, issued_by_account_id BIGINT NOT NULL FK →
    accounts, challenge_id BIGINT NOT NULL FK → challenges ON DELETE CASCADE, token
    VARCHAR(255) NOT NULL, expires_at TIMESTAMP NOT NULL)` — same shape as `access_links`.
  - `auth.infrastructure.registrationinvite.RegistrationInviteJpaEntity` /
    `RegistrationInviteJpaRepository` (`findByToken(String)`) / a repository impl, mirroring
    `auth.infrastructure.accesslink.*` exactly.
  - `auth.application.service.RegistrationInviteService`: `issue(AccountId
    issuedByAccountId, long challengeId, Duration validFor) : String (token)` — same
    `SecureRandom`/Base64url token generation as `AccessLinkService.issue`; `verify(String
    token) : long (challengeId)` — throws `InvalidOrExpiredLinkException` if
    missing/expired. `challengeId` is a raw `long`, **not** `cookoff`'s typed `ChallengeId`
    — identical module-cycle reasoning already documented for `AccessLinkService.issue` in
    this plan's Phase 3.
  - New public contract `auth.RegistrationInvites` (mirrors `auth.AccountLookup`'s existing
    pattern — keeps `Account`/`RegistrationInviteService` internal): `issue(AccountId,
    long, Duration) : String` (thin pass-through) and `register(String token, String
    firstName, String lastName, String email) : RegistrationResult { AccountId accountId,
    long challengeId }` — verifies the token, checks `accountRepository.existsByEmail(...)`
    (reuse `CreateAccountService`'s existing `AccountAlreadyExistsException` → 409), then
    `Account.create(email, name, SystemRole.USER)`, saves, returns both ids.
- `cookoff` module (allowed to depend on `auth`, not the reverse):
  - `cookoff.application.service.CreateRegistrationInviteService.execute(AccountId
    organizerAccountId, ChallengeId challengeId)`: loads the `Challenge`, checks `status ==
    OPEN`, calls `auth.RegistrationInvites.issue(organizerAccountId, challengeId.value(),
    validFor)` (reuse `SendChallengeInvitationsService`'s existing 30-day `Duration`
    constant). Backs `POST /api/v1/challenges/{id}/registration-invites`
    (organizer/admin-gated).
  - `cookoff.application.service.PublicRegistrationService.execute(String token, String
    firstName, String lastName, String email)`: calls
    `auth.RegistrationInvites.register(...)`, loads the returned `challengeId` via
    `ChallengeRepository`. If the challenge is still `OPEN`, calls
    `editParticipants(guestIdsToAdd = [accountId])` (from 7.4) and saves; if it's no longer
    `OPEN` (revealed between QR generation and scan), **skip the guest-add, don't fail the
    registration** — the account already exists at that point and shouldn't be left
    half-created. Return a result flag the controller uses to pick the response copy
    ("registered and joined" vs. "registered, but this event has already closed"). Backs
    the public, unauthenticated `POST /api/v1/public/registrations`.
- Security config: `POST /api/v1/public/registrations` added to the permit-all list (no
  JWT, no link token — it's genuinely public, gated only by the QR token in its body);
  `POST /api/v1/challenges/{id}/registration-invites` follows the same
  organizer/admin `authorizeHttpRequests` matcher as every other challenge-management
  endpoint.

**Verify 7.8**: `RegistrationInviteServiceTest` (issue→verify happy path, expiry, unknown
token — same shape as the existing `AccessLinkServiceTest`); `PublicRegistrationServiceTest`
covers happy path, duplicate-email 409, expired/invalid token, and the
challenge-no-longer-open degrade path; a `SecurityIntegrationTest` case confirming the
public registration endpoint needs no `Authorization` header/link token at all.

**Verify Phase 7 (whole-phase check)**: full regression (`./gradlew build`); manual smoke
test extending Phase 6's — create a challenge, generate a registration QR, register a new
walk-in through it and confirm they land on the guest list, have both cooks pick colors,
upload/replace the challenge photo, score with the 1–5 UI, reveal, unreveal, re-reveal with
a changed score and confirm `CookRivalry` still matches a hand-calculated expectation.

## Explicitly out of scope for this plan

- Angular frontend (`docs/cookingChallenge/first-plan.md` Step 4,
  `docs/cookingChallenge/frontend-prd.md`) — separate plan when that work starts.
- Real email delivery — Phase 1's `NotificationPort` gets a logging/no-op adapter here;
  swapping in a real provider (SES, Postgmark, etc.) is a future, explicitly-requested
  task.
- Swapping Phase 7.5's `DatabaseImageStorageAdapter` for a real object-storage provider —
  `ImageStoragePort` is shaped for that swap, but building the adapter itself is a future,
  explicitly-requested task (see `frontend-prd.md` §9).
- `CookRivalryController` and any other "optional, can defer" row from the Step 3 API
  table — build only if asked.
