# 0002 — Repository ports live in the application layer, not domain

## Status

Accepted (2026-08-03).

## Context

Every repository interface in this codebase (`AccountRepository`, `ChallengeRepository`,
`ScoreSubmissionRepository`, `PlateColorRepository`, `CookRivalryRepository`) originally lived
under `<module>/domain/repository`, carrying jMolecules' `@Repository` DDD stereotype. This
follows Eric Evans' original DDD tactical pattern: a Repository "provides the illusion of an
in-memory collection of aggregates," so the interface (though not its JPA-backed
implementation) is treated as part of the domain's ubiquitous language.

This project's own docs already apply a second, competing convention alongside that one:
`docs/backend/02-ddd-modulith.md`'s package structure puts `NotificationPort` — an outgoing
port that isn't itself a DDD tactical pattern — under `application/port`, not `domain`. Under
the stricter Clean-Architecture/hexagonal reading (where "domain" shrinks to just
entities/value objects/domain services, and "application" owns every port — driving *and*
driven — because a port is defined as "what a use case needs from the outside world," not a
domain concept per se), `Repository` belongs there too. The project was mixing both
conventions: `Repository` in domain, everything else in application.

This surfaced concretely while extending Phase 4 of
`docs/cookingChallenge/plans/openapi-first-api-plan.md` (new `RivalriesListService`/
`RivalryDetailService`): a question about whether repository ports could use Spring Data's
`Pageable`/`Page` led to the question of which layer actually owns these interfaces, since
that determines what "the domain must be framework-free" rule even applies to.

## Decision

Repository interfaces move from `<module>/domain/repository` to `<module>/application/port`,
alongside every other outgoing port. Concretely, in this migration:

- `auth.domain.repository.AccountRepository` → `auth.application.port.AccountRepository`
- `cookoff.domain.repository.{ChallengeRepository, ScoreSubmissionRepository,
  PlateColorRepository, CookRivalryRepository}` →
  `cookoff.application.port.{same names}`

Domain aggregates/value objects (`Account`, `Challenge`, `CookRivalry`, ...) referenced by
these interfaces are unaffected and stay in `domain.model`. The jMolecules `@Repository`
stereotype annotation is kept on the relocated interfaces — jMolecules validates the
annotation's structural semantics (e.g. methods return/accept aggregate types), not package
location, so `JMoleculesDddRules.all()` continues to pass unchanged.

The project's custom ArchUnit layering rule
(`at.fraihs.cookoff.JMoleculesArchitectureTests#dependenciesPointInward`) already allows
`Application` to depend on `Domain`, so application services injecting these now
application-layer ports needed no rule change. Nothing in any `domain` package referenced
these interfaces before the move (verified by grepping for the import inside `domain/`
before relocating), so the move doesn't introduce an upward dependency from domain into
application anywhere.

`docs/backend/02-ddd-modulith.md`'s package-structure diagram and "Repository Pattern" section
are updated to show the new location and link back to this ADR.

## Consequences

- One consistent rule for every outgoing port (`Repository`, `NotificationPort`,
  `ImageStoragePort`, `AccessLinkRepository`, `RegistrationInviteRepository`, ...): all live
  under `application/port`. No more "Repository is domain, everything else is application"
  special case to remember or explain to a new contributor.
- The domain layer is now unambiguously just entities/value objects/domain
  services/domain events — genuinely framework- and port-free, not "framework-free except
  for the one interface type that happens to be a DDD tactical pattern."
- This is a deliberate departure from Evans' original placement of Repository as a domain
  concept. That reading remains legitimate and is what jMolecules' own documentation/examples
  generally assume — if a future contributor expects `domain.repository` per the jMolecules
  docs or a textbook DDD reference, point them here rather than treating the discrepancy as a
  bug.
- Enables ADR 0003 (Spring Data `Pageable`/`Page` in repository ports): since the application
  layer already depends on Spring (`@Transactional`, `ApplicationEventPublisher`, generated
  OpenAPI models), a repository port living there taking a Spring Data type is consistent with
  its layer's existing framework exposure — it would have been a harder sell with `Repository`
  still classified as a domain type.
- Pure mechanical rename: 55 files touched (import updates only), 5 interface files moved, 2
  `findAll()` signatures additionally changed per ADR 0003, no other behavior change.
  `./gradlew build` + full test suite (229 tests, including the jMolecules/ArchUnit layering
  suite) pass unchanged in shape, confirming no consumer needed logic changes beyond the
  import path.
