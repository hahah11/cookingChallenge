package at.fraihs.cookoff.auth;

import at.fraihs.cookoff.auth.domain.model.AccountId;

import java.time.Duration;

/**
 * Public contract for other modules to issue/consume self-registration QR invites. Keeps
 * RegistrationInviteService and the Account aggregate internal to the auth module (see
 * docs/backend/02-ddd-modulith.md's Module Contracts pattern) — mirrors AccountLookup.
 */
public interface RegistrationInvites {

    /** Thin pass-through to RegistrationInviteService.issue. */
    String issue(AccountId issuedByAccountId, long challengeId, Duration validFor);

    /**
     * Verifies the token, then creates a brand-new USER account for the walk-in — rejecting
     * if the email is already registered — and returns both the new account's id and the
     * challenge id the invite was issued for.
     *
     * @throws at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException if the token is missing/expired
     * @throws at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException  if the email is already registered
     */
    RegistrationResult register(String token, String firstName, String lastName, String email);
}
