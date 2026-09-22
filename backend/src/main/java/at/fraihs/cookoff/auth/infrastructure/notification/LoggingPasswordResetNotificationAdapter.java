package at.fraihs.cookoff.auth.infrastructure.notification;

import at.fraihs.cookoff.auth.application.dto.PasswordResetNotification;
import at.fraihs.cookoff.auth.application.port.PasswordResetNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** The default: logs the reset link instead of mailing it, like cookoff's logging adapter. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetNotificationAdapter implements PasswordResetNotificationPort {

    @Override
    public void sendPasswordReset(PasswordResetNotification notification) {
        log.info("Password reset link for {}: {}", notification.recipient(), notification.link());
    }
}
