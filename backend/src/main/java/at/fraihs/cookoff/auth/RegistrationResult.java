package at.fraihs.cookoff.auth;

import at.fraihs.cookoff.auth.domain.model.AccountId;

/** The account created by {@link RegistrationInvites#register} and the challenge its invite was issued for. */
public record RegistrationResult(AccountId accountId, long challengeId) {
}
