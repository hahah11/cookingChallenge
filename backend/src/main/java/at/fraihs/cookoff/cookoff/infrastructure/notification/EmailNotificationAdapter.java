package at.fraihs.cookoff.cookoff.infrastructure.notification;

import at.fraihs.cookoff.cookoff.application.dto.InvitationNotification;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.shared.mail.MailRequest;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Real mail delivery, active when {@code app.mail.enabled=true}; otherwise
 * {@link LoggingNotificationAdapter} takes over.
 *
 * <p>This class only decides <em>what to say</em>. Publishing a {@link MailRequest} rather than
 * sending inline hands the <em>when</em> to {@code MailDispatcher}, which waits for the
 * surrounding transaction to commit before anything reaches SMTP — the access link being mailed
 * is written by that very transaction.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailNotificationAdapter implements NotificationPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendAccessLink(InvitationNotification notification) {
        eventPublisher.publishEvent(new MailRequest(
                notification.recipient().value(),
                "You're invited: " + notification.challengeTitle(),
                "access-link",
                Map.of(
                        "firstName", notification.firstName(),
                        "challengeTitle", notification.challengeTitle(),
                        "link", notification.link())));
    }

    @Override
    public void sendResultsAvailable(ResultsAvailableNotification notification) {
        eventPublisher.publishEvent(new MailRequest(
                notification.recipient().value(),
                "The results are in: " + notification.challengeTitle(),
                "results-available",
                Map.of(
                        "firstName", notification.firstName(),
                        "challengeTitle", notification.challengeTitle(),
                        "link", notification.link())));
    }
}
