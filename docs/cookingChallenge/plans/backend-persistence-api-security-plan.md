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
- **Not done** (this plan): everything below. No application services, no JPA adapters,
  no REST controllers, no security config exist yet.

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

## Explicitly out of scope for this plan

- Angular frontend (`docs/cookingChallenge/first-plan.md` Step 4) — separate plan when
  that work starts.
- Real email delivery — Phase 1's `NotificationPort` gets a logging/no-op adapter here;
  swapping in a real provider (SES, Postgmark, etc.) is a future, explicitly-requested
  task.
- `CookRivalryController` and any other "optional, can defer" row from the Step 3 API
  table — build only if asked.
