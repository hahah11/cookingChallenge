package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;

/**
 * joined=false means the challenge closed between QR generation and scan — the account was
 * still created, just not added as a guest. The controller phase uses this to pick the
 * response copy ("registered and joined" vs. "registered, but this event has already
 * closed").
 */
public record PublicRegistrationResult(AccountId accountId, ChallengeId challengeId, boolean joined) {
}
