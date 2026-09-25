package at.fraihs.cookoff.shared.mail;

import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.dto.InvitationNotification;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.infrastructure.notification.EmailNotificationAdapter;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every other test runs with {@code app.mail.enabled} unset, so this is the only coverage of the
 * arrangement that actually ships: the real {@link EmailNotificationAdapter} wired to
 * {@link MailDispatcher} over an application event, on a real async executor. It catches the two
 * things unit tests structurally cannot — that the conditional bean swap picks the mail adapter,
 * and that the event published by the adapter is actually delivered to the listener.
 */
@SpringBootTest(properties = {
        "app.mail.enabled=true",
        "app.mail.from=CookOff <no-reply@cookoff.test>",
        "spring.mail.host=localhost",
})
class MailWiringIntegrationTest {

    @Autowired
    private NotificationPort notificationPort;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void should_useTheEmailAdapter_when_mailIsEnabled() {
        assertInstanceOf(EmailNotificationAdapter.class, notificationPort);
    }

    @Test
    void should_reachTheMailSender_when_anInvitationIsSentOutsideATransaction() {
        when(javaMailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        notificationPort.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=abc",
                true, false, Locale.ENGLISH));

        // No transaction here, so the listener's fallbackExecution path runs it inline — but
        // still on the mail executor, hence the timeout rather than a bare verify.
        verify(javaMailSender, timeout(5_000)).send(any(MimeMessage.class));
    }
}
