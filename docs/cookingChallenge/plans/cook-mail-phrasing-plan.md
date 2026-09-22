# Cook Mail Phrasing Plan

## Status

**Implemented (2026-09-22).** Plan 2 of 3 from the 2026-09-22 feature request, built on the
`_layout.html` from [`mail-brand-palette-plan.md`](mail-brand-palette-plan.md).

- `Challenge.isCook(AccountId)` added. `isParticipant` and `PickColorService` now call it instead of
  repeating the stream themselves.
- Both notification records carry `canRate` / `picksPlateColor`. Both send loops set them from
  `canScore` / `isCook`, and `EmailNotificationAdapter` puts them in the template model.
- All four templates (`.html` and `.txt`) show each sentence under its own `th:if`. The shared lead-in
  ("You're taking part in …" / "… has been revealed.") is the same for everyone.
- Deviations: `CapturingNotificationPort` and `LoggingNotificationAdapter` are unchanged. No test needs
  the captured flags, and the logging adapter never destructures the record.
- Tests: `ChallengeTest` (`isCook`, including cook-and-guest); `EmailNotificationAdapterTest` (all four
  flag combinations, for both mails); `ChallengeRevealedNotifierTest` and
  `SendChallengeInvitationsServiceTest` (flags per recipient); `MailTemplateRenderingTest` (cook-only,
  guest-only, and both, rater sentence first, across all four templates). Full `./gradlew test` passes
  (356 tests). The Mailpit check under Verification has not been done yet.

## Context

Cooks receive the same two mails as guests, and both tell them to do something they cannot do:

> "Scoring is open — taste each plate and rate it on all three categories."

`Challenge.canScore(accountId)` is `isGuest(accountId) || accountId.equals(createdBy)` — cooks are
deliberately excluded. What a cook actually does is pick their plate color (`PickColorService`,
`POST /challenges/{id}/color-pick`). The results mail is likewise written for a spectator
("see who cooked which plate") rather than a competitor.

### The non-obvious part

`ChallengeRevealedNotifier.recipientsOf` carries this comment:

> *Guests and both cooks, de-duplicated — **a cook can also be a guest on the same cook-off**.*

So "cook" and "rater" are **not** mutually exclusive, and a single `isCook` flag would tell a
cook-who-is-also-a-guest not to rate when they genuinely can. The two capabilities are
independent, so model them as two booleans rather than one role:

- `canRate` ← `challenge.canScore(accountId)`
- `picksPlateColor` ← the recipient holds a `CookAssignment`

In the common case exactly one is true, and the template reads as a clean either/or.

## Approach

Both call sites already know the answer and throw it away. `SendChallengeInvitationsService`
merges guests and cooks with `Stream.concat(guests, cooks).distinct()` before the send loop;
`ChallengeRevealedNotifier.recipientsOf` does the same with a `LinkedHashSet`. **Do not unpick
those merges** — instead ask the aggregate inside the existing loop, which keeps the predicate in
the domain where it belongs:

```java
// Challenge.java — fills a real gap; canScore() already documents that cooks can't score,
// but nothing exposes the positive case.
public boolean isCook(AccountId accountId) {
    return cookAssignments.stream().anyMatch(a -> a.accountId().equals(accountId));
}
```

Then in each loop body:

```java
notificationPort.sendAccessLink(new InvitationNotification(
        account.email(), account.firstName(), challenge.getTitle(), link,
        challenge.canScore(accountId), challenge.isCook(accountId)));
```

## Copy

| Mail | Rater (unchanged) | Cook |
|---|---|---|
| Invitation | "Scoring is open — taste each plate and rate it on all three categories." | "You're cooking in this one. Open the cook-off to choose your plate color — the guests will score the dishes blind." |
| Results | "You can now see who cooked which plate, the scores per category, and the winner." | "The dishes are unblinded. See how yours scored in each category, and who took it." |

A recipient who is both gets both sentences, in that order.

## Files

| File | Change |
|---|---|
| `cookoff/domain/model/Challenge.java` | Add `isCook(AccountId)` |
| `cookoff/application/service/PickColorService.java` | Replace its inline `getCookAssignments().stream()...` cook check (line 42) with `challenge.isCook(accountId)` — the same predicate, now in the domain |
| `cookoff/application/dto/InvitationNotification.java` | Add `boolean canRate, boolean picksPlateColor` |
| `cookoff/application/dto/ResultsAvailableNotification.java` | Same two fields |
| `cookoff/application/service/SendChallengeInvitationsService.java` | One line in the send loop |
| `cookoff/application/event/ChallengeRevealedNotifier.java` | One line in the send loop |
| `cookoff/infrastructure/notification/EmailNotificationAdapter.java` | Put both flags in the model map — **note `Map.of` is fine, but the map now has 5 entries** |
| `cookoff/infrastructure/notification/LoggingNotificationAdapter.java` | Signature only, if it destructures |
| `templates/mail/access-link.{html,txt}` | `th:if`/`th:unless` on the body sentence |
| `templates/mail/results-available.{html,txt}` | Same |
| `shared/testsupport/CapturingNotificationPort.java` | Record the flags so tests can assert them |

## Tests

- `cookoff/domain/model/ChallengeTest` — `isCook` true for both assignments, false for a guest,
  and **true for an account that is both cook and guest** (the case that motivates the design).
- `EmailNotificationAdapterTest` — extend the existing model-map assertions to cover both flags in
  all four combinations.
- `ChallengeRevealedNotifierTest` — already asserts 2 cooks + 1 guest; assert each notification
  carries the right flags.
- `SendChallengeInvitationsServiceTest` — same, for the explicit-cook-ids request path.
- `MailTemplateRenderingTest` — render each HTML and TXT template with `canRate=true/false` and
  assert the right sentence appears and the wrong one does not. This is the only test that proves
  the `th:if` conditions are actually wired to the right variable names.

## Verification

Create a cook-off, use **Send links** with cooks selected, and read both mails in Mailpit
(<http://localhost:8025>) — the cook's copy should mention the plate color and never "rate".
Then reveal the cook-off and check the results mail the same way.
