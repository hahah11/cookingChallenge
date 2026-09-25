# Soft-delete a challenge

Status: Implemented 2026-09-25 (backend + frontend + tests; manual run against dev stack not done).

## Context
Organizers need a "Delete challenge" button. It must not remove the DB row: it sets the challenge status to `DELETED`. A deleted challenge is invisible everywhere in the frontend and no longer counts toward rivalries.

Key finding: rivalries are **materialized counters** (`cook_rivalries`: wins/draws/total), updated only by AFTER_COMMIT listeners on `ChallengeRevealed` / `ChallengeUnrevealed`. Filtering queries is not enough; deleting a REVEALED challenge must reverse its counter contribution, exactly like unreveal.

Decisions (confirmed with user):
- Endpoint: `DELETE /api/v1/challenges/{challengeId}` → 204 (public API change, approved).
- Deletable from every state incl. REVEALED (rivalry is reversed).
- Rivalries with 0 total challenges are hidden from the rivalry list.

Design choices (mine, low risk):
- `DELETED` is **not** added to the OpenAPI `ChallengeStatus` enum; the API never returns a deleted challenge, so frontend/generated client stay unchanged apart from the new operation.
- No DB migration: `challenges.status` is `VARCHAR(16)` without CHECK; `DELETED` fits.
- Deleting a revealed challenge reuses the existing `ChallengeUnrevealed` event, so `ChallengeUnrevealedRivalryUpdater` does the reversal; no new listener.

## Backend (`backend/src/main/java/at/fraihs/cookoff/`)
1. **Domain** – `cookoff/domain/model/ChallengeStatus.java`: add `DELETED`.
   `Challenge.java`: add `Optional<ChallengeUnrevealed> delete()`:
   - throws `IllegalStateException` if already DELETED (→ 409);
   - if REVEALED: build the `ChallengeUnrevealed` event (previous winner, cooks) and clear `lastRevealResult` as `unreveal()` does (reuse its private helper; extract if needed);
   - set status `DELETED`.
   Make `pickColor`/`changeImage` (currently `requireNotRevealed`) also reject DELETED.
2. **Repository** – `ChallengeJpaRepository` / `ChallengeRepositoryImpl`: exclude `DELETED` in `findById` (`findByIdAndStatusNot`), `findAll(Pageable)`, `findAllByCreatedBy`, and the JPQL of `findByParticipant` and `findByCookPair` (`AND c.status <> DELETED`). Deleted ⇒ 404 for every existing flow (status, participant view, results, image, scores, color-pick, invitations, registration, QR, home, rivalry detail) without touching each service. `ChallengeModelMapper` `valueOf` calls stay safe since DELETED never reaches them.
3. **Service** – new `application/service/DeleteChallengeService` mirroring `UnrevealChallengeService`: `canOrganize` check → `findById` (404) → owner-or-admin check → `challenge.delete()` → `save` → publish returned event if present. `@Transactional`.
4. **Rivalries** – `CookRivalryRepository` (+ Jpa/Impl): list query `totalChallenges > 0`; `RivalriesListService` uses it. `RivalryDetailService`: throw `RivalryNotFoundException` when there are no (non-deleted) challenges and rivalry is absent **or has total 0**.
5. **API** – `openapi/cookingchallenge-api.yaml`: add `delete` on `/api/v1/challenges/{challengeId}`, `operationId: deleteChallenge`, tag Challenges, responses 204/401/403/404. `ChallengesController`: `@Override deleteChallenge` → service, return 204. `SecurityConfig`: `DELETE /api/v1/challenges/{id}` → `hasAnyRole("ORGANIZER","ADMIN")`.

## Frontend (`frontend/src/app/`, generated client not edited; run `npm run generate:api`)
- `features/challenges/challenge-detail/challenge-detail.{ts,html,scss}`: add a "Delete challenge" section at the bottom, visible in all states (incl. REVEALED, which currently has its own block), separated by `mat-divider`, `mat-stroked-button` with `delete` icon and error color. `confirmDelete()` opens the existing `ConfirmDialog` with `danger: true` (first user of that flag), then `deleteChallenge()` with `deleteBusy` signal → `ChallengesApi.deleteChallenge` → navigate to `/challenges`; errors via `notification.error`. Follow `confirmUnreveal()` pattern.
- i18n: `challengeDetail.delete`, `challengeDetail.deleteDialog.{title,message,confirm}` in **both** `frontend/public/i18n/en.json` and `de.json` (`translations.spec.ts` enforces parity).
- No changes needed to history/home/rivalry lists: backend no longer returns deleted challenges.

## Tests
Backend (Mockito/JUnit, `should_X_when_Y`):
- `ChallengeTest`: delete from OPEN/CLOSED (no event), REVEALED (event with previous winner, result cleared), already DELETED (throws); mutators reject DELETED.
- `DeleteChallengeServiceTest` (template: `CloseChallengeScoringServiceTest` / `UnrevealChallengeServiceTest`): forbidden, not found, owner, admin, event publish only when revealed.
- `ChallengeRepositoryImplTest` (`@DataJpaTest`): deleted excluded from all finders.
- `ChallengeRevealUnrevealRivalryIntegrationTest`: delete of a revealed challenge reverses counters once.
- `RivalriesListServiceTest`/`RivalryDetailServiceTest`: 0-total hidden / 404.
- `ChallengesControllerTest`: 204, 403, 404. `SecurityIntegrationTest`: DELETE role rules.

Frontend (Vitest): `challenge-detail.spec.ts` – button shown in OPEN/CLOSED/REVEALED, confirm-cancel does nothing, confirm calls API and navigates, error shows notification; AXE check.

## Housekeeping
- On execution, also save this plan to `docs/cookingChallenge/plans/delete-challenge.md` (project rule; plan-mode path is only a scratch location).
- Update the descriptions in the OpenAPI spec that enumerate statuses only if they become misleading (they don't, since DELETED is never exposed).

## Verification
1. `./gradlew test` in `backend/` (IntelliJ MCP is currently disconnected, so use bash) – incl. architecture tests.
2. `npm run generate:api && npm test` in `frontend/`.
3. Manual, against the running dev stack: create challenge, reveal it (rivalry shows 1 total), delete it → gone from organizer list, participant home, and rivalry list/detail (404); rivalry counters reversed; second DELETE returns 404. Delete an OPEN challenge and confirm participant links now 404.
