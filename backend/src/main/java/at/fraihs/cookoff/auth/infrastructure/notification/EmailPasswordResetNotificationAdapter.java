package at.fraihs.cookoff.auth.infrastructure.notification;

import at.fraihs.cookoff.auth.application.dto.PasswordResetNotification;
import at.fraihs.cookoff.auth.application.port.PasswordResetNotificationPort;
import at.fraihs.cookoff.shared.mail.MailMessages;
import at.fraihs.cookoff.shared.mail.MailRequest;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Real mail delivery for password resets, active when {@code app.mail.enabled=true}. Same shape
 * as cookoff's {@code EmailNotificationAdapter}: it decides what to say and hands the when to
 * {@code MailDispatcher}, which waits for the token's transaction to commit.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailPasswordResetNotificationAdapter implements PasswordResetNotificationPort {

    private final ApplicationEventPublisher eventPublisher;
    private final MailMessages mailMessages;

    @Override
    public void sendPasswordReset(PasswordResetNotification notification) {
        eventPublisher.publishEvent(new MailRequest(
                notification.recipient().value(),
                mailMessages.get("mail.passwordReset.subject", notification.locale()),
                "password-reset",
                Map.of(
                        "firstName", notification.firstName(),
                        "link", notification.link()),
                notification.locale()));
    }
}
