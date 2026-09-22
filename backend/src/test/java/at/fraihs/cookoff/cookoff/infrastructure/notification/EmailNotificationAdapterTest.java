package at.fraihs.cookoff.cookoff.infrastructure.notification;

import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.dto.InvitationNotification;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;
import at.fraihs.cookoff.shared.mail.MailRequest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationAdapterTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EmailNotificationAdapter adapter;

    private MailRequest publishedRequest() {
        ArgumentCaptor<MailRequest> captor = ArgumentCaptor.forClass(MailRequest.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_addressTheRecipient_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t"));

        assertEquals("ada@example.com", publishedRequest().to());
    }

    @Test
    void should_useTheAccessLinkTemplate_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t"));

        assertEquals("access-link", publishedRequest().template());
    }

    @Test
    void should_nameTheChallengeInTheSubject_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t"));

        assertEquals("You're invited: Schnitzel-Off", publishedRequest().subject());
    }

    @Test
    void should_passLinkAndNameToTheTemplate_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t"));

        assertEquals(
                Map.of("firstName", "Ada", "challengeTitle", "Schnitzel-Off",
                        "link", "https://cookoff.test/home?token=t"),
                publishedRequest().model());
    }

    @Test
    void should_useTheResultsTemplate_when_sendingAResultsNotification() {
        adapter.sendResultsAvailable(new ResultsAvailableNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t"));

        assertEquals("results-available", publishedRequest().template());
    }

    @Test
    void should_nameTheChallengeInTheSubject_when_sendingAResultsNotification() {
        adapter.sendResultsAvailable(new ResultsAvailableNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t"));

        assertEquals("The results are in: Schnitzel-Off", publishedRequest().subject());
    }
}
