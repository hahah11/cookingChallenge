# 0004 — Mapper and DTO package convention

## Status

Accepted (2026-08-06).

## Context

Mappers and data types were placed inconsistently across the backend:

- `cookoff/infrastructure/persistence/mapper/` already held its mappers in a dedicated
  package, but `auth/infrastructure/persistence/AccountMapper` sat next to the repository
  implementation it served.
- Application-layer mappers lived inside `service` packages next to the use cases that called
  them: `auth.application.service.AccountModelMapper`, `shared.application.service.
  ConfigModelMapper`, `cookoff.application.service.ChallengeMapping`.
- Data types sat in packages meant for something else: `AccessLink` / `RegistrationInvite` /
  `StoredImage` were records in `application/port` (a package intended for interfaces only),
  `ChallengeResult` was a record in `domain/service`, and the API envelope records sat in
  `shared/web` next to `GlobalExceptionHandler`.
- Both `auth/application/dto` and `cookoff/application/dto` existed but were empty.

The docs codified the *old* placement: `docs/backend/03-code-style.md` explicitly said
"mappers stay next to the entity they convert", and `docs/backend/01-architecture.md`'s
package tree was a generic `controller/service/repository/model` layout that didn't match
anything in this codebase. Following the docs as written would have reproduced the
inconsistency rather than resolved it.

## Decision

One rule, applied uniformly across every module and every layer: mappers live in a `mapper/`
package **of the layer they map for**, and data types live in `dto/` (application layer),
`model/` (domain layer), or `entity/` (JPA/infrastructure layer) — never in `service`, `port`,
or next to the type they convert.

Concretely:

- `infrastructure/<adapter>/mapper/` — domain ↔ entity mappers (e.g. `AccountMapper`,
  `ChallengeMapper`).
- `application/mapper/` — domain → generated-OpenAPI-model mappers (e.g. `AccountModelMapper`,
  `ConfigModelMapper`, and `ChallengeMapping` renamed to `ChallengeModelMapper` for
  consistency with the other two).
- `application/dto/` — commands, query results, and port payloads (`AccessLink`,
  `RegistrationInvite`, `StoredImage`).
- `domain/model/` — aggregates, value objects, enums, and domain records (`ChallengeResult`
  moves here from `domain/service`, which now holds only domain services).
- `infrastructure/<adapter>/entity/` — `@Entity`/`@Embeddable` JPA types.

The one exemption: a use-case-private nested record (e.g. `SubmitScoreService.Result`) stays
nested inside its service, because it is part of that use case's signature and has no
independent identity outside it.

This is enforced going forward by ArchUnit rules in
`PackageConventionArchitectureTests` (mapper-named classes and `@Mapper`-annotated classes
must live in `..mapper..`, `@Entity`/`@Embeddable` classes must live in `..entity..`, and
top-level records must not live in `..application.service..`, `..application.port..`, or
`..domain.service..`), so the split this ADR fixes cannot silently reappear.

## Consequences

- One consistent rule to explain to a new contributor: "mappers in `mapper/`, data in
  `dto/model/entity`" — no more case-by-case judgment calls about whether a given mapper or
  record belongs in `service`, `port`, or next to its aggregate.
- `ChallengeMapping` is renamed to `ChallengeModelMapper` and widened from package-private to
  `public`, since its 11 callers in `cookoff.application.service` now reach it from a sibling
  package (`cookoff.application.mapper`) instead of the same one. Its javadoc explaining why
  it's static helpers rather than a `@Mapper` interface is preserved.
- `RivalryHeadline` (`cookoff.application.service`, explicitly not moved by this ADR — it's a
  display-text helper, neither mapper nor model) needed the same package-private-to-`public`
  widening on its class and `build` method: `ChallengeModelMapper` calls it, and it now lives
  in a different package. Not anticipated when this migration was scoped, but a direct
  consequence of moving `ChallengeModelMapper` out of `application.service` — any helper it
  called that stayed behind needed the same visibility bump.
- An extra package hop for every mapper and relocated data type — callers import from
  `mapper`/`dto`/`entity` instead of the package they used to share with the type. Purely
  mechanical: package declarations and imports only, no behavior change.
- Rejected alternative: keep mappers next to the type they convert (the status quo). This is
  what produced today's inconsistent split in the first place — "next to the entity" and
  "next to the aggregate" pull in different directions once both a persistence mapper and an
  application-layer mapper exist for the same domain type, and it gives no clear answer for
  where a mapper with no single "next to" home (like `ChallengeModelMapper`, which composes
  data from multiple aggregates) should live.
