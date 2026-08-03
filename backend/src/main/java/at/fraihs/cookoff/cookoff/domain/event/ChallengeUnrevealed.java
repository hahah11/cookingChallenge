package at.fraihs.cookoff.cookoff.domain.event;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import org.jmolecules.event.annotation.DomainEvent;

/**
 * Published when a Challenge is unrevealed; consumed within the cookoff module to reverse
 * the CookRivalry update {@link ChallengeRevealed} made. Carries the same cook pair as
 * {@code ChallengeRevealed} (not just challengeId/previousOverallWinnerAccountId) because
 * the reversal listener needs it to look up the CookRivalry, mirroring
 * {@code ChallengeRevealed}'s shape exactly.
 */
@DomainEvent
public record ChallengeUnrevealed(
        ChallengeId challengeId,
        AccountId cookAAccountId,
        AccountId cookBAccountId,
        AccountId previousOverallWinnerAccountId // null = was a draw
) {
}
