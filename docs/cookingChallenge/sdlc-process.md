# SDLC Process — CookingChallenge

How this project moves from idea to shipped feature, and which artifact in this repo
covers each step. Scaled for a solo, multi-year effort: the team-alignment ceremony is
dropped, the memory-preserving artifacts are kept (see rationale below the list).

## Steps

1. **Discovery** — capture the user journeys and decide what's in scope for which
   release.
   Artifact: [`story-mapping.yaml`](./story-mapping.yaml) (storymaps.io).

2. **Domain modeling** — name the aggregates, value objects, invariants, and module
   boundaries (`auth`, `cookoff`).
   Artifact: [`domain-model.puml`](./domain-model.puml).

3. **Architecture decisions** — record the handful of decisions worth remembering the
   reasoning for; a light Context/Container view of the system, revisited occasionally
   rather than exhaustively diagrammed.
   Artifact: [`adr/`](./adr/) (ADR per decision); no standing C4 diagram yet.

4. **API / contract design** — request/response shapes for the slice being built.
   Artifact: [`plans/openapi-first-api-plan.md`](./plans/openapi-first-api-plan.md),
   `openapi/`.

5. **Planning** — turn the stories in the active release slice into tickets. See
   "Linking the map to tickets" below.

6. **Implementation** — build the thinnest vertical slice first (Walking Skeleton),
   then each release slice in order.

7. **Testing** — unit/integration/e2e alongside implementation, not a separate late
   phase. See [`docs/shared/03-testing.md`](../shared/03-testing.md).

8. **CI/CD & deployment** — pipeline, environments, release.

9. **Feedback loop** — usage informs the next pass at discovery; update the story map
   and domain model rather than letting them go stale.

## Why keep this, solo

Steps 1–4 exist here to survive gaps of weeks or months between working sessions and to
give an AI coding agent working context without re-explaining the project each time —
not to align multiple people. Kept lightweight: no ticket velocity tracking, no
consensus-building diagrams, ADRs only for decisions worth not re-litigating.

## Linking the map to tickets

The standard mechanism (Jira/Linear/Asana, all of which storymaps.io can import from
and export to) is:

- **One ticket per story card.** Each card in a release slice becomes one issue.
- **The card links to the ticket.** storymaps.io cards support an external URL — paste
  the issue link onto the card via its menu.
- **The card's status mirrors the ticket's status.** storymaps.io cards support a
  status (planned / in-progress / done / blocked); update it as the linked ticket
  moves. A glance at the map then shows what's actually built vs. still planned,
  per-slice, without opening the tracker.
- **The ticket references the map.** Put the step + card name in the ticket
  description (e.g. "Story map: Reveal Cookoff → Ask organizer to confirm before
  revealing results") so the ticket is traceable back to the map even outside
  storymaps.io.

This repo uses GitHub, not Jira/Linear/Asana, so storymaps.io's tracker integrations
don't apply directly — the same pattern still works manually: paste the GitHub Issue
URL into the card's link field, and update the card's status by hand as the issue
moves through open → in progress → closed.
