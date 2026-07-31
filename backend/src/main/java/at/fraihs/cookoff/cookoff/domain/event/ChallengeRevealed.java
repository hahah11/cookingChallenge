package at.fraihs.cookoff.cookoff.domain.event;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;

/** Published when a Challenge is revealed; consumed within the cookoff module to update CookRivalry. */
public record ChallengeRevealed(
        ChallengeId challengeId,
        AccountId cookAAccountId,
        AccountId cookBAccountId,
        AccountId overallWinnerAccountId // null = draw
) {
}
