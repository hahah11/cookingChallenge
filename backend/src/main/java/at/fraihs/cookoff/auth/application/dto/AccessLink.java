package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.AccountId;

import java.time.Instant;

/**
 * A personalized access-link token. Deliberately not a domain aggregate (see
 * docs/cookingChallenge/first-plan.md's auth section) — challengeId is a raw TSID
 * long, not the cookoff module's typed ChallengeId, so the auth module never depends
 * on the cookoff module (which already depends on auth for AccountId).
 */
public record AccessLink(
        long id,
        AccountId accountId,
        long challengeId,
        String token,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt) {

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public AccessLink markUsed(Instant usedAt) {
        return new AccessLink(id, accountId, challengeId, token, expiresAt, usedAt, createdAt);
    }
}
