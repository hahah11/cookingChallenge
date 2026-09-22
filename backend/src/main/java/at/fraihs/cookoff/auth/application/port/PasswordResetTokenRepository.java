package at.fraihs.cookoff.auth.application.port;

import at.fraihs.cookoff.auth.application.dto.PasswordResetToken;
import at.fraihs.cookoff.auth.domain.model.AccountId;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Atomically marks the token used if it is unused and unexpired at {@code now}, returning the
     * claimed token, or empty for an unknown, expired or already-claimed one. One conditional
     * UPDATE, so of two concurrent claims on the same token exactly one wins.
     */
    Optional<PasswordResetToken> claim(String token, Instant now);

    /** Drops every reset token for the account, so issuing a new one supersedes older links. */
    void deleteAllByAccountId(AccountId accountId);
}
