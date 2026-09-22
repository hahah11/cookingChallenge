package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeRevealedNotifierTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private AccessLinkService accessLinkService;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private ChallengeRevealedNotifier notifier;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId guestId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();

    private Challenge challenge() {
        return Challenge.create(LocalDate.now(), "Schnitzel-Off", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(guestId), organizerId);
    }

    private AccountSummary accountFor(AccountId id) {
        return new AccountSummary(id, new Email(id + "@example.com"), "Name " + id, "Ada");
    }

    private ChallengeRevealed eventFor(Challenge challenge) {
        return new ChallengeRevealed(challenge.getId(), cookAId, cookBId, cookAId);
    }

    @Test
    void should_notifyEveryGuestAndBothCooks_when_challengeRevealed() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountLookup.getById(any(AccountId.class))).thenAnswer(i -> accountFor(i.getArgument(0)));
        when(accessLinkService.issue(any(AccountId.class), anyLong(), any(Duration.class))).thenReturn("tok");

        notifier.on(eventFor(challenge));

        verify(notificationPort, times(3)).sendResultsAvailable(any(ResultsAvailableNotification.class));
    }

    @Test
    void should_sendAFreshAccessLinkPerRecipient_when_challengeRevealed() {
        Challenge challenge = challenge();
        ReflectionTestUtils.setField(notifier, "frontendBaseUrl", "https://cookoff.test");
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountLookup.getById(eq(guestId))).thenReturn(accountFor(guestId));
        when(accountLookup.getById(eq(cookAId))).thenReturn(accountFor(cookAId));
        when(accountLookup.getById(eq(cookBId))).thenReturn(accountFor(cookBId));
        when(accessLinkService.issue(eq(guestId), anyLong(), any(Duration.class))).thenReturn("guest-token");
        when(accessLinkService.issue(eq(cookAId), anyLong(), any(Duration.class))).thenReturn("cook-a-token");
        when(accessLinkService.issue(eq(cookBId), anyLong(), any(Duration.class))).thenReturn("cook-b-token");

        notifier.on(eventFor(challenge));

        ArgumentCaptor<ResultsAvailableNotification> captor =
                ArgumentCaptor.forClass(ResultsAvailableNotification.class);
        verify(notificationPort, times(3)).sendResultsAvailable(captor.capture());
        assertEquals("https://cookoff.test/home?token=guest-token", captor.getAllValues().getFirst().link());
    }

    @Test
    void should_nameTheChallengeInEveryNotification_when_challengeRevealed() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountLookup.getById(any(AccountId.class))).thenAnswer(i -> accountFor(i.getArgument(0)));
        when(accessLinkService.issue(any(AccountId.class), anyLong(), any(Duration.class))).thenReturn("tok");

        notifier.on(eventFor(challenge));

        ArgumentCaptor<ResultsAvailableNotification> captor =
                ArgumentCaptor.forClass(ResultsAvailableNotification.class);
        verify(notificationPort, times(3)).sendResultsAvailable(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(n -> "Schnitzel-Off".equals(n.challengeTitle())));
    }

    @Test
    void should_notifyNobody_when_challengeNoLongerExists() {
        ChallengeId challengeId = ChallengeId.generate();
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

        notifier.on(new ChallengeRevealed(challengeId, cookAId, cookBId, null));

        verify(notificationPort, never()).sendResultsAvailable(any(ResultsAvailableNotification.class));
    }
}
