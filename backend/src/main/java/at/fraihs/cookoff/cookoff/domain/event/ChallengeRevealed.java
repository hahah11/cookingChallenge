package at.fraihs.cookoff.cookoff.domain.event;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import org.jmolecules.event.annotation.DomainEvent;

/** Published when a Challenge is revealed; consumed within the cookoff module to update CookRivalry. */
@DomainEvent
public record ChallengeRevealed(
        ChallengeId challengeId,
        AccountId cookAAccountId,
        AccountId cookBAccountId,
        AccountId overallWinnerAccountId // null = draw
) {
}
