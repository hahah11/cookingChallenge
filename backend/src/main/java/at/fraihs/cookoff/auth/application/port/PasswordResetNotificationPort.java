package at.fraihs.cookoff.auth.application.port;

import at.fraihs.cookoff.auth.application.dto.PasswordResetNotification;

/**
 * Outgoing port for the password-reset mail. The auth module's own port rather than cookoff's
 * {@code NotificationPort}: auth must never depend on cookoff. Implementations must never let a
 * delivery failure escape.
 */
public interface PasswordResetNotificationPort {

    void sendPasswordReset(PasswordResetNotification notification);
}
