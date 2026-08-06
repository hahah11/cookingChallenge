package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.AccountId;

import java.time.Instant;

/**
 * A self-registration QR invite token, scoped to one challenge and issued by an organizer.
 * Deliberately not a domain aggregate — same reasoning as AccessLink — challengeId is a raw
 * TSID long, not the cookoff module's typed ChallengeId, so the auth module never depends on
 * the cookoff module (which already depends on auth for AccountId).
 */
public record RegistrationInvite(
        long id,
        AccountId issuedByAccountId,
        long challengeId,
        String token,
        Instant expiresAt) {

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
