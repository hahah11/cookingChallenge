# Close Scoring Plan

## Status: Implemented (2026-09-25)

Implementation notes:
- Kept this file as the single plan (no separate `close-scoring-plan.md`, per the "update the existing plan" rule).
- Color-pick and image-upload 409 descriptions in the OpenAPI spec now read "already REVEALED", matching `requireNotRevealed()`. The `/me/home` description was also stale (it described action-based bucketing); it now states the status-based rule the code uses.
- Reveal/unreveal confirm-dialog copy no longer claims reveal "closes scoring" or unreveal "reopens scoring".
- The close status chip uses the M3 tertiary-container role (the palette has no warning token); the read-only blind-scoring banner uses the same role.
- The guest card always offers a button while CLOSED ("View my scores", or "View" when the guest never scored) so a guest can always open the challenge; the score page then shows the read-only grid or just the banner.
- Backend build is Gradle: run tests with `./gradlew test` in `backend/`.
- Pre-existing, unrelated failure: `error-interceptor.spec.ts` "leaves non-UNAUTHENTICATED 401s alone" fails because `error-interceptor.ts` logs out on any 401.

## Context
Challenges have two states today: OPEN, where guests can score and edit, and REVEALED, where results are visible and mails go out. At a live event the organizer presents the results. Before that, scoring has to be frozen without showing any results. This plan adds a third state, **CLOSED**, shown to users as "Scoring closed". Guests can still log in with their link and see the challenge, but their scores are read-only. The organizer gets "Close scoring" and "Reopen scoring" buttons.

## Decisions (confirmed with user)
- **Lifecycle:** OPEN → CLOSED → REVEALED.
  - Reopen goes CLOSED → OPEN.
  - Unreveal now goes REVEALED → **CLOSED** (it used to go to OPEN).
  - Reveal works only from CLOSED.
- **While CLOSED:**
  - Blocked: score submission, QR/self-registration, registration invites and **editing cooks & guests**.
  - Still allowed: organizer image change, cook plate-color pick, sending links.
- **No mails on close or reopen:** closing and reopening scoring send no email to anyone. Mails still go out only on reveal, as today.
- **Guest view:** the challenge stays in the "open" section of home. The score page shows the guest's submitted scores read-only, with a banner saying "Scoring is closed — results coming soon". Cook identities stay hidden, as before reveal.
- **API change (approved):**
  - Add `CLOSED` to the `ChallengeStatus` enum.
  - Add `POST /api/v1/challenges/{id}/close` and `POST /api/v1/challenges/{id}/reopen`.
  - The status tag label is "Scoring closed".
- **Database:** no migration. `status` is `VARCHAR(16)` with no CHECK constraint, and existing rows are OPEN or REVEALED, both still valid.

## Backend (`backend/src/main/java/at/fraihs/cookoff/cookoff/`)
1. **`domain/model/ChallengeStatus.java`:** add `CLOSED` between OPEN and REVEALED.
2. **`domain/model/Challenge.java`:**
   - Add `closeScoring()` (requires OPEN, sets CLOSED) and `reopenScoring()` (requires CLOSED, sets OPEN). Neither publishes an event, because nothing listens for these changes.
   - `reveal()` now calls a new `requireClosed()`.
   - `unreveal()` sets `CLOSED` instead of OPEN; update its javadoc.
   - `pickColor` and `changeImage` accept OPEN or CLOSED through a new `requireNotRevealed()`.
   - `editParticipants` keeps `requireOpen()`, so it is blocked while CLOSED (409). `requireOpen()` is also used by the close transition.
3. **`openapi/cookingchallenge-api.yaml`:**
   - Add `CLOSED` to the enum.
   - Add the `/close` and `/reopen` operations, modelled on `/unreveal` (L579-603): same response schema, 403/404/409.
   - Update the `/reveal` 409 description to "not CLOSED" and the `/scores` 409 description to "not OPEN".
4. **New services in `application/service/`:**
   - `CloseChallengeScoringService` and `ReopenChallengeScoringService`, mirroring `UnrevealChallengeService`: the `canOrganize` check and owner-or-admin check, then load, transition, save and return the same DTO. They publish no event and call no `NotificationPort`, so no mail is sent.
5. **`interfaces/rest/ChallengesController.java`:** implement the two new generated operations next to reveal and unreveal (L147-156).
6. **Status-dependent code that needs updating:**
   - `HomeService.java:46`: treat OPEN **or CLOSED** as open.
   - `SubmitScoreService`: already rejects anything that is not OPEN with `ChallengeNotOpenException`, so CLOSED is covered. No change.
   - `PublicRegistrationService` and `CreateRegistrationInviteService`: already OPEN-only, so CLOSED is blocked. No change.
   - `ChallengeModelMapper.toParticipantChallenge`: cooks are hidden until REVEALED, which already covers CLOSED. No change.
   - Check that `ChallengeOutcomeLabel` and `RivalryDetailService` handle CLOSED like OPEN (no result yet).

## Frontend (`frontend/src/app/`)
1. Run `npm run generate:api` to get `ChallengeStatus.CLOSED` and the new `closeChallenge` / `reopenChallenge` API methods. Do not hand-edit generated code.
2. **`shared/components/status-tag/status-tag.ts`:** add a CLOSED label "Scoring closed" and a `status-tag--closed` class (a neutral or warning token from the existing palette).
3. **`features/challenges/challenge-detail/`** (organizer page):
   - **OPEN:** keep the guest list and all actions. Replace "Reveal results" with "Close scoring", which opens a `ConfirmDialog`: "Close scoring? Guests can no longer submit or edit scores."
   - **CLOSED:** show the guest list plus "Send links", "Reopen scoring" (stroked) and "Reveal results" (flat, with the existing confirm and animation). Hide the QR button and the "Edit cooks & guests" button, because both are blocked while CLOSED.
   - **REVEALED:** unchanged, except the unreveal copy becomes "Made a mistake? You can hide the results again (scoring stays closed)."
   - Add `closeScoring()` / `reopenScoring()` handlers that patch `challenge.status` locally, following the `unreveal()` pattern. Keep the results loading gated on REVEALED only.
4. **`features/challenges/blind-scoring/`:**
   - When the status is CLOSED, add a read-only branch: a banner "Scoring is closed — results coming soon", then the grid with the `disabled` input on `app-star-rating`, which already exists. Hide the submit button. If there is no submission, show only the banner.
   - On a 409 from submit, reload the challenge instead of assuming it was revealed, so either state renders correctly.
5. **`features/home/participant-challenge-card/`:** when CLOSED, replace "Score now" / "Edit scores" with a "Scoring closed" chip and a "View my scores" button that opens the read-only page when the guest has submitted.
6. **`shared/components/challenge-card/challenge-card.html:41-48`** (organizer list): when CLOSED, show "x/y submitted · Scoring closed".
7. **Blind-scoring success copy:** "Results reveal once the host closes scoring" is still accurate. Keep it.

## Tests
- **`ChallengeTest`:**
  - close from OPEN; close twice fails; reopen from CLOSED; reopen from OPEN fails.
  - reveal from OPEN fails; reveal from CLOSED succeeds; unreveal lands in CLOSED.
  - `pickColor` and `changeImage` work in CLOSED and fail in REVEALED; `editParticipants` fails in CLOSED.
  - The close and reopen service tests verify that no event is published and `NotificationPort` is never called.
  - Update the existing reveal and unreveal tests to go through `closeScoring()` first.
- **New service tests** `CloseChallengeScoringServiceTest` / `ReopenChallengeScoringServiceTest` (owner, admin, forbidden, wrong state). Update `RevealChallengeServiceTest`, `UnrevealChallengeServiceTest`, `SubmitScoreServiceTest` (CLOSED gives 409) and `HomeServiceTest` (CLOSED lands in open).
- **Other backend tests:**
  - `ChallengesControllerTest`: the close and reopen endpoints.
  - `ChallengeRepositoryImplTest`: a CLOSED round-trip.
  - `ChallengeRevealUnrevealRivalryIntegrationTest`: fix setup to close before reveal.
- **Frontend specs:**
  - `challenge-detail.spec.ts`: buttons per state; close and reopen call the API and update the status.
  - `blind-scoring.spec.ts`: CLOSED renders read-only with no submit button; a 409 triggers a reload.
  - `participant-challenge-card.spec.ts`, `status-tag.spec.ts`, `challenge-card.spec.ts`.

## Docs
This plan, with its Status section updated once implementation is done.

## Verification
1. Run the backend tests with `./gradlew test` in `backend/` (IntelliJ MCP is unavailable, so run them from bash). Run the frontend tests with `npm test` and check that `npm run build` passes.
2. Manual end-to-end check:
   - Create a challenge and score as a guest.
   - As the organizer, click Close scoring.
   - The guest link shows the challenge under open with read-only scores, and submitting through the API returns 409.
   - Reopen scoring; the guest can edit again.
   - Close again, then Reveal; the mails go out.
   - Unreveal; the challenge is back in CLOSED and the guest sees read-only scores.
