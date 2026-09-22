package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.Email;

/**
 * Payload for {@link at.fraihs.cookoff.auth.application.port.PasswordResetNotificationPort}.
 * {@code link} is the full {@code .../reset-password?token=...} URL.
 */
public record PasswordResetNotification(Email recipient, String firstName, String link) {
}
