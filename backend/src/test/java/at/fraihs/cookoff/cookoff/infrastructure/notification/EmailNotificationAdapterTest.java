package at.fraihs.cookoff.cookoff.infrastructure.notification;

import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.dto.InvitationNotification;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;
import at.fraihs.cookoff.shared.mail.MailMessages;
import at.fraihs.cookoff.shared.mail.MailRequest;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationAdapterTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private MailMessages mailMessages = new MailMessages();

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
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.ENGLISH));

        assertEquals("ada@example.com", publishedRequest().to());
    }

    @Test
    void should_useTheAccessLinkTemplate_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.ENGLISH));

        assertEquals("access-link", publishedRequest().template());
    }

    @Test
    void should_nameTheChallengeInTheSubject_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.ENGLISH));

        assertEquals("You're invited: Schnitzel-Off", publishedRequest().subject());
    }

    @Test
    void should_passLinkAndNameToTheTemplate_when_sendingAnInvitation() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.ENGLISH));

        assertEquals(
                Map.of("firstName", "Ada", "challengeTitle", "Schnitzel-Off",
                        "link", "https://cookoff.test/home?token=t",
                        "canRate", true, "picksPlateColor", false),
                publishedRequest().model());
    }

    @Test
    void should_useTheResultsTemplate_when_sendingAResultsNotification() {
        adapter.sendResultsAvailable(new ResultsAvailableNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.ENGLISH));

        assertEquals("results-available", publishedRequest().template());
    }

    @Test
    void should_nameTheChallengeInTheSubject_when_sendingAResultsNotification() {
        adapter.sendResultsAvailable(new ResultsAvailableNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.ENGLISH));

        assertEquals("The results are in: Schnitzel-Off", publishedRequest().subject());
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void should_passWordingFlagsToTheTemplate_when_sendingAnInvitation(boolean canRate, boolean picksPlateColor) {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                canRate, picksPlateColor, Locale.ENGLISH));

        Map<String, Object> model = publishedRequest().model();
        assertEquals(canRate, model.get("canRate"));
        assertEquals(picksPlateColor, model.get("picksPlateColor"));
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void should_passWordingFlagsToTheTemplate_when_sendingAResultsNotification(
            boolean canRate, boolean picksPlateColor) {
        adapter.sendResultsAvailable(new ResultsAvailableNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                canRate, picksPlateColor, Locale.ENGLISH));

        Map<String, Object> model = publishedRequest().model();
        assertEquals(canRate, model.get("canRate"));
        assertEquals(picksPlateColor, model.get("picksPlateColor"));
    }

    @Test
    void should_writeTheInvitationSubjectInTheRecipientsLanguage_when_recipientPrefersGerman() {
        adapter.sendAccessLink(new InvitationNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.GERMAN));

        MailRequest request = publishedRequest();
        assertEquals("Du bist eingeladen: Schnitzel-Off", request.subject());
        assertEquals(Locale.GERMAN, request.locale());
    }

    @Test
    void should_writeTheResultsSubjectInTheRecipientsLanguage_when_recipientPrefersGerman() {
        adapter.sendResultsAvailable(new ResultsAvailableNotification(
                new Email("ada@example.com"), "Ada", "Schnitzel-Off", "https://cookoff.test/home?token=t",
                true, false, Locale.GERMAN));

        MailRequest request = publishedRequest();
        assertEquals("Die Ergebnisse sind da: Schnitzel-Off", request.subject());
        assertEquals(Locale.GERMAN, request.locale());
    }
}
