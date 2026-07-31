package at.fraihs.cookoff.cookoff.infrastructure.notification;

import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub adapter — logs the access link instead of sending a real email. Swapping in a
 * real provider (SES, Postmark, etc.) is a future, explicitly-requested task, per
 * docs/cookingChallenge/plans/backend-persistence-api-security-plan.md.
 */
@Slf4j
@Component
public class LoggingNotificationAdapter implements NotificationPort {

    @Override
    public void sendAccessLink(Email email, String link) {
        log.info("Access link for {}: {}", email, link);
    }
}
