package at.fraihs.cookoff.auth.infrastructure.notification;

import at.fraihs.cookoff.auth.application.dto.PasswordResetNotification;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.shared.mail.MailMessages;
import at.fraihs.cookoff.shared.mail.MailRequest;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailPasswordResetNotificationAdapterTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private MailMessages mailMessages = new MailMessages();

    @InjectMocks
    private EmailPasswordResetNotificationAdapter adapter;

    private MailRequest send(Locale locale) {
        adapter.sendPasswordReset(new PasswordResetNotification(
                new Email("olga@example.com"), "Olga", "https://cookoff.test/reset-password?token=t", locale));
        ArgumentCaptor<MailRequest> captor = ArgumentCaptor.forClass(MailRequest.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_addressTheRecipientWithTheResetTemplate_when_sendingAPasswordReset() {
        MailRequest request = send(Locale.ENGLISH);

        assertEquals("olga@example.com", request.to());
        assertEquals("password-reset", request.template());
    }

    @Test
    void should_passNameAndLinkToTheTemplate_when_sendingAPasswordReset() {
        assertEquals(
                Map.of("firstName", "Olga", "link", "https://cookoff.test/reset-password?token=t"),
                send(Locale.ENGLISH).model());
    }

    @Test
    void should_writeTheSubjectInEnglish_when_recipientPrefersEnglish() {
        MailRequest request = send(Locale.ENGLISH);

        assertEquals("Reset your CookOff password", request.subject());
        assertEquals(Locale.ENGLISH, request.locale());
    }

    @Test
    void should_writeTheSubjectInGerman_when_recipientPrefersGerman() {
        MailRequest request = send(Locale.GERMAN);

        assertEquals("Setze dein CookOff-Passwort zurück", request.subject());
        assertEquals(Locale.GERMAN, request.locale());
    }
}
