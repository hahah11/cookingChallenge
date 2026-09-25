package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.Email;

import java.util.Locale;

/**
 * Payload for {@link at.fraihs.cookoff.auth.application.port.PasswordResetNotificationPort}.
 * {@code link} is the full {@code .../reset-password?token=...} URL; {@code locale} is the
 * recipient's preferred language.
 */
public record PasswordResetNotification(Email recipient, String firstName, String link, Locale locale) {
}
