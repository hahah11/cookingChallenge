package at.fraihs.cookoff.shared.mail;

import at.fraihs.cookoff.shared.config.MailConfig;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailDispatcherTest {

    @Mock
    private MailTransport mailTransport;

    private MailDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new MailDispatcher(
                new MailConfig().mailTemplateEngine(), mailTransport, "CookOff <no-reply@cookoff.test>", 2);
    }

    private MailRequest request() {
        return new MailRequest("ada@example.com", "You're invited: Schnitzel-Off", "access-link",
                Map.of("firstName", "Ada", "challengeTitle", "Schnitzel-Off",
                        "link", "https://cookoff.test/home?token=abc"));
    }

    private void transportAcceptsMessages() {
        when(mailTransport.createMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    private MimeMessage sentMessage() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailTransport).send(captor.capture());
        MimeMessage message = captor.getValue();
        // Jakarta Mail only writes the Content-Type headers on saveChanges(); without this the
        // parts are there but isMimeType(...) has nothing to read. A real send does this itself.
        message.saveChanges();
        return message;
    }

    @Test
    void should_addressTheRecipient_when_dispatching() throws Exception {
        transportAcceptsMessages();

        dispatcher.on(request());

        assertEquals("ada@example.com", sentMessage().getAllRecipients()[0].toString());
    }

    @Test
    void should_useTheConfiguredFromAddress_when_dispatching() throws Exception {
        transportAcceptsMessages();

        dispatcher.on(request());

        assertEquals("CookOff <no-reply@cookoff.test>", sentMessage().getFrom()[0].toString());
    }

    @Test
    void should_carryTheSubject_when_dispatching() throws Exception {
        transportAcceptsMessages();

        dispatcher.on(request());

        assertEquals("You're invited: Schnitzel-Off", sentMessage().getSubject());
    }

    @Test
    void should_carryAPlainTextAlternative_when_dispatching() throws Exception {
        transportAcceptsMessages();

        dispatcher.on(request());

        // Clients that refuse to render HTML must still get a readable body with the link in it.
        String plainText = bodyOfType(sentMessage(), "text/plain");
        assertTrue(plainText.contains("https://cookoff.test/home?token=abc"), plainText);
    }

    @Test
    void should_carryTheHtmlBody_when_dispatching() throws Exception {
        transportAcceptsMessages();

        dispatcher.on(request());

        String html = bodyOfType(sentMessage(), "text/html");
        assertTrue(html.contains("Schnitzel-Off"), html);
    }

    /** Walks the nested multiparts MimeMessageHelper builds and returns the first body of a type. */
    private static String bodyOfType(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return part.getContent().toString();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = bodyOfType(multipart.getBodyPart(i), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void should_swallowTheFailure_when_everySendAttemptFailed() {
        transportAcceptsMessages();
        doThrow(new MailSendException("smtp is down")).when(mailTransport).send(any());

        // A dead mailbox must never surface as an error: by this point the organizer's request
        // has already been answered, and the exception would only reach an async thread.
        assertDoesNotThrow(() -> dispatcher.on(request()));
    }

    @Test
    void should_swallowTheFailure_when_theTemplateIsMissing() {
        transportAcceptsMessages();

        assertDoesNotThrow(() -> dispatcher.on(new MailRequest(
                "ada@example.com", "Subject", "no-such-template", Map.of())));
    }
}
