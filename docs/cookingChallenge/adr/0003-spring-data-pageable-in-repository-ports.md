# 0003 — Use Spring Data's `Pageable`/`Page<T>` directly in repository ports

## Status

Accepted (2026-08-03). Builds on
[0002](0002-repository-ports-in-application-layer.md).

## Context

Every list-returning repository port method (`AccountRepository.findAll()`,
`CookRivalryRepository.findAll()`) originally returned a plain `List<T>` with no pagination
support, and the two current list-screen use cases that need it
(`RivalriesListService`/Phase 4 of `docs/cookingChallenge/plans/openapi-first-api-plan.md`,
and the still-open `ListAccountsService` rewrite) had to invent their own pagination on top —
first attempt was an in-memory `PagedResult.of(List<T> all, int page, int size)` that loaded
the entire table and sliced it in the application layer.

Two paths were considered for doing this "properly" (real `LIMIT`/`OFFSET` at the DB, not
"load everything"):

1. **A homegrown, framework-free pagination type** (e.g. a domain-owned `PageQuery`/`Page<T>`
   mirroring how `AccountId`/`Email`/`Tsid` are homegrown rather than reusing a library type),
   with the JPA adapter translating to/from Spring Data's `Pageable` internally. This keeps the
   port's *contract* independent of Spring Data specifically, at the cost of a translation
   layer and a parallel vocabulary (our `Page<T>` vs. Spring's) that every future adapter would
   need to convert between.
2. **Spring Data's `Pageable`/`Page<T>` directly in the port signature.** Zero translation
   layer — `JpaRepository<Entity, Long>` already implements `findAll(Pageable): Page<Entity>`
   for free, and `Page<T>.map(Function)` converts content while preserving pagination
   metadata.

This project is not migrating away from Spring Boot (confirmed with the user) — there is no
realistic future adapter that swaps out Spring Data for something else while keeping the rest
of the stack. Option 1's isolation buys portability this project will never use, in exchange
for a real, permanent translation layer.

This decision follows directly from [ADR 0002](0002-repository-ports-in-application-layer.md):
with `Repository` reclassified as an application-layer port, this is consistent with every
other framework dependency already accepted at that layer (`@Transactional`,
`ApplicationEventPublisher`, the generated OpenAPI models) — it would have been a harder,
more inconsistent case to make while `Repository` was still classified as a domain type
expected to be framework-free.

## Decision

Repository ports that need pagination take `org.springframework.data.domain.Pageable` and
return `org.springframework.data.domain.Page<T>` directly — no homegrown pagination
abstraction.

- `AccountRepository.findAll(): List<Account>` → `findAll(Pageable): Page<Account>`
- `CookRivalryRepository.findAll(): List<CookRivalry>` → `findAll(Pageable): Page<CookRivalry>`
- `AccountRepositoryImpl`/`CookRivalryRepositoryImpl` delegate straight to the generated
  `JpaRepository.findAll(Pageable)`, mapping content via `Page.map(mapper::toDomain)` — no new
  query methods needed.
- `RivalriesListService` builds a `PageRequest.of(page, size)`, calls the port, and maps the
  resulting `Page<CookRivalry>` to `Page<Rivalry>` (the generated OpenAPI model) via
  `Page.map(...)`.
- `shared.web.PagedResult<T>` — a small record wrapping content + the generated `Pagination`
  schema, per `docs/shared/04-api-design.md`'s pagination envelope convention — is repurposed
  from "slice a `List` in memory" to `PagedResult.of(Page<T>)`, reading the real pagination
  metadata (`getNumber()`, `getTotalElements()`, `getTotalPages()`, `isFirst()`, `isLast()`)
  off the Spring `Page` instead of computing it by hand.
- `ListAccountsService` (not yet rewritten to the generated `AccountListResponse` — see
  `openapi-first-api-plan.md`'s Phase 4 status) calls the new port method with
  `Pageable.unpaged()`, preserving its current "return everything" behavior unchanged until
  its own rewrite lands.
- `ChallengeRepository.findAll()` and other list methods not currently backing a paginated
  screen are left as plain `List<T>` — this ADR establishes the pattern for pagination going
  forward, it does not retrofit every existing repository method in one pass.

## Consequences

- Real DB-level pagination for the two converted methods — a page request now issues one
  `LIMIT`/`OFFSET` query plus one `COUNT`, not "load every row, then slice in Java." Matters
  once these tables outgrow small-event scale.
- No parallel pagination vocabulary to maintain — `Pageable`/`Page<T>` is used consistently
  from controller (Phase 5, not yet built) through application service through repository
  port through JPA adapter.
- Explicit trade-off accepted: every repository port that uses this now has a hard compile-time
  dependency on `spring-data-commons`. This is judged acceptable because the project has no
  intention of ever swapping Spring Data out; if that assumption changes, revisit this
  decision rather than work around it piecemeal.
- No stable sort by cook/account **name** is available at the query level for
  `RivalriesListService` — `cook_rivalries` stores account IDs, not names (names live in the
  `auth` module's `Account` aggregate), so sorting by display name would require a
  cross-module join. Rows currently come back in the DB's natural order for a plain
  `findAll(Pageable)` (effectively insertion order); revisit if the rivalries list needs a
  guaranteed display order once it has real usage.
- `./gradlew build` + full test suite (229 tests, including the jMolecules/ArchUnit layering
  suite) pass with this change in place.
