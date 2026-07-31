# 0001 — Use an explicit H2 in-memory database for `@DataJpaTest`, not Testcontainers

## Status

Accepted (2026-07-31). Supersedes the Testcontainers decision recorded in
`docs/cookingChallenge/plans/backend-persistence-api-security-plan.md`'s Phase 2 section.

## Context

Phase 2 of the backend persistence plan added `@DataJpaTest` repository integration tests for
`Account`, `Challenge`, `ScoreSubmission`, and `CookRivalry`, covering full aggregate round-trips
and the DB-level constraints Liquibase creates (`score_submissions`' unique constraint,
`cook_rivalries`' ordered-pair `CHECK`).

These tests were first built against a real Postgres via Testcontainers
(`org.testcontainers:testcontainers-postgresql` + `testcontainers-junit-jupiter`, wired through
`@ServiceConnection`), on the reasoning that a real Postgres instance validates the actual
production constraints, not an approximation. In practice this was unstable in this environment:

- A single full-suite run against Testcontainers took **4–5 minutes** (image pull + one
  `SpringApplicationContext`/Postgres startup per `@DataJpaTest` class), against ~10–15s for the
  rest of the unit test suite.
- Multiple concurrent test invocations (e.g. one from a terminal, one re-triggered before the
  first finished) ended up racing for the same Postgres container and Docker daemon resources;
  one run's container was OOM-killed mid-suite (`docker ps -a` showed it `Dead`), failing 8 of 69
  tests with `ConnectException`/`CannotCreateTransactionException` — not a code defect, but a
  flaky-infrastructure failure indistinguishable from one in CI output.
- Docker availability itself was inconsistent between contexts (a plain shell vs. an IDE-launched
  terminal) in this setup, adding a class of failure unrelated to the code under test.

None of this reflects on Testcontainers as a general practice — it's the right default when the
target dialect's behavior genuinely can't be approximated (window functions, specific extensions,
replica behavior, etc.). But this project's tests only depend on portable SQL: `UNIQUE`/`CHECK`
constraints, standard column types, and foreign keys — all of which H2 enforces equivalently to
Postgres — so the extra fidelity wasn't buying anything the flakiness was worth trading for.

## Decision

Repository `@DataJpaTest`s run against an **in-memory H2 database** (`com.h2database:h2`,
`testRuntimeOnly`), not Testcontainers/Postgres.

- `backend/src/test/resources/application.yaml` sets an **explicit**
  `spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, and every
  `@DataJpaTest` class carries `@AutoConfigureTestDatabase(replace = Replace.NONE)` so Spring Boot
  doesn't override it with its own auto-generated embedded datasource.
- `backend/src/main/resources/application.yaml` sets `spring.jpa.hibernate.ddl-auto: none`
  globally (prod and test): Liquibase (`db/changelog`) is the single source of schema truth,
  including the hand-written `CHECK` constraints added via raw `sql:` changesets. Without this,
  Hibernate's default `ddl-auto=create-drop` for embedded databases would silently generate its
  own schema from JPA entity annotations and drop Liquibase's — including those `CHECK`
  constraints, which have no JPA annotation equivalent — the moment H2 was added to the test
  classpath.
- `AbstractPostgresIntegrationTest` (the Testcontainers `@ServiceConnection` base class) and the
  `org.testcontainers:*` / `org.springframework.boot:spring-boot-testcontainers` test
  dependencies were removed.

### Non-obvious pitfall found along the way — don't use Spring Boot's auto-generated embedded datasource

`@DataJpaTest`'s **default** behavior (`@AutoConfigureTestDatabase` with `Replace.ANY` and no
explicit `spring.datasource.url`) replaces any configured datasource with one Spring Boot builds
itself, at a random name (`jdbc:h2:mem:<uuid>;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false`, via
`TestDatabaseAutoConfiguration.EmbeddedDataSourceFactory`).

On this project's stack (Spring Boot 4.1.0, H2 2.4.240), that auto-generated datasource
**deterministically and reproducibly** produced `H2 error 23514 "Check constraint invalid:
CHK_SCORES_DISH_LABEL"` on every insert into `scores` — even for a plainly valid `dish_label='A'`
that satisfies `CHECK (dish_label IN ('A', 'B'))`. This was chased at length:

- The JDBC bind trace confirmed the correct value (`'A'`) was actually sent — ruled out a mapping
  bug (a red herring first suspected here was Hibernate's dialect-dependent native-enum-type
  inference for `@Enumerated(EnumType.STRING)`; adding `@JdbcTypeCode(SqlTypes.VARCHAR)` changed
  the bound JDBC type from `ENUM` to `VARCHAR` but did **not** fix the failure, so it was reverted).
- Raw JDBC (`DriverManager`, and separately `HikariDataSource`) against the exact same
  `jdbc:h2:mem:<uuid>;DB_CLOSE_DELAY=-1` URL scheme, running the exact same DDL and a
  `PreparedStatement`-bound insert, worked without error — ruling out H2/HikariCP/the URL format
  itself.
- Switching only the datasource acquisition path — same Spring context, same Liquibase changelog,
  same Hibernate entities — from `@AutoConfigureTestDatabase`'s default auto-embedded datasource
  to an **explicit** `spring.datasource.url` (tried both file-based and a fixed in-memory name)
  made the identical test pass immediately, every time.

The isolated conclusion: the failure is specific to Spring Boot 4.1.0's
`TestDatabaseAutoConfiguration.EmbeddedDataSourceFactory` bean-creation path for H2 — not H2, not
Hibernate's enum mapping, not this project's schema. Pinning an explicit `spring.datasource.url`
sidesteps it entirely. **If upgrading Spring Boot in the future, it is worth re-testing whether
`@AutoConfigureTestDatabase(replace = Replace.NONE)` + the explicit URL is still necessary**, since
this may be a version-specific regression rather than a permanent constraint.

## Consequences

- Full backend test suite (69 tests, including all four `@DataJpaTest` classes) runs in
  **~11–13 seconds**, down from 4–5 minutes, and needs no Docker daemon at all.
- `CookoffApplicationTests.contextLoads()` (a `@SpringBootTest`) now passes too, as a side effect
  of the same test-scoped `spring.datasource.url` being visible to every test in the module — it
  no longer needs Phase 6's `docker compose up` to get a real datasource.
- All four `@DataJpaTest` classes share the same `@DataJpaTest` + datasource configuration
  signature, so Spring's test context cache reuses one `ApplicationContext` (and Liquibase run)
  across all of them within a single test run — part of why the suite is fast, not just because
  H2 itself is fast.
- Trade-off accepted: H2 is not byte-for-byte Postgres. If a future migration relies on a
  genuinely Postgres-specific feature (an extension, a Postgres-only function, `SERIAL`/sequence
  edge cases, etc.), that specific test should use Testcontainers deliberately rather than
  reverting this decision wholesale — most of this project's schema (standard types, `UNIQUE`,
  `CHECK`, `FOREIGN KEY`) has no such dependency.
