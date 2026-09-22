package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.AccountId;

import java.time.Instant;

/**
 * A single-use password-reset token, sibling of {@link AccessLink}. Unlike an access link it is
 * consumed on first use — but that is enforced by
 * {@link at.fraihs.cookoff.auth.application.port.PasswordResetTokenRepository#claim}, not by
 * reading {@code usedAt} here, which could not survive two concurrent redeems.
 */
public record PasswordResetToken(
        long id,
        AccountId accountId,
        String token,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt) {
}
