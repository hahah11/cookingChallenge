package at.fraihs.cookoff.cookoff.infrastructure.notification;

import at.fraihs.cookoff.cookoff.application.dto.InvitationNotification;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The default adapter: logs the link instead of mailing it, so a fresh checkout and the whole
 * test suite run without an SMTP server. Set {@code app.mail.enabled=true} to swap in
 * {@link EmailNotificationAdapter}.
 *
 * <p>Note the two conditions are exhaustive only for the literal values {@code true} and
 * {@code false} — any other value leaves no {@code NotificationPort} bean and fails startup,
 * which beats silently logging invitations in production.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingNotificationAdapter implements NotificationPort {

    @Override
    public void sendAccessLink(InvitationNotification notification) {
        log.info("Access link for {} ({}): {}",
                notification.recipient(), notification.challengeTitle(), notification.link());
    }

    @Override
    public void sendResultsAvailable(ResultsAvailableNotification notification) {
        log.info("Results available for {} ({}): {}",
                notification.recipient(), notification.challengeTitle(), notification.link());
    }
}
