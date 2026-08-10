package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.openapi.model.InvitationsSentRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SendInvitationsRequestRestDto;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendChallengeInvitationsServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private AccessLinkService accessLinkService;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private SendChallengeInvitationsService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId guestId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();

    private Challenge challenge() {
        return Challenge.create(LocalDate.now(), "Title", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(guestId), organizerId);
    }

    private AccountSummary accountFor(AccountId id) {
        return new AccountSummary(id, new Email(id + "@example.com"), "Name " + id, "Name");
    }

    @Test
    void should_issueLinkAndNotify_forEveryGuestWhoHasNotSubmitted_when_requestOmitted() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId())).thenReturn(List.of());
        when(accountLookup.getById(guestId)).thenReturn(accountFor(guestId));
        when(accessLinkService.issue(eq(guestId), anyLong(), any(Duration.class))).thenReturn("token");

        InvitationsSentRestDto sent = service.execute(challenge.getId().toString(), organizerId, null);

        assertEquals(1, sent.getCount());
        verify(accessLinkService, times(1)).issue(eq(guestId), anyLong(), any(Duration.class));
        verify(notificationPort, times(1)).sendAccessLink(any(Email.class), anyString());
    }

    @Test
    void should_targetExactGuests_when_requestProvidesIds() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountLookup.getById(guestId)).thenReturn(accountFor(guestId));
        when(accessLinkService.issue(eq(guestId), anyLong(), any(Duration.class))).thenReturn("token");

        InvitationsSentRestDto sent = service.execute(challenge.getId().toString(), organizerId,
                new SendInvitationsRequestRestDto().guestAccountIds(List.of(guestId.toString())));

        assertEquals(1, sent.getCount());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        when(challengeRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(cookAId.toString(), organizerId, null));
    }

    @Test
    void should_throw_when_participantAccountMissing() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId())).thenReturn(List.of());
        when(accountLookup.getById(any(AccountId.class)))
                .thenThrow(new AccountNotFoundException("missing"));

        assertThrows(AccountNotFoundException.class,
                () -> service.execute(challenge.getId().toString(), organizerId, null));
    }

    @Test
    void should_throw_when_requesterDidNotCreateTheChallenge() {
        Challenge challenge = challenge();
        AccountId otherOrganizerId = AccountId.generate();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountLookup.isAdmin(otherOrganizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(challenge.getId().toString(), otherOrganizerId, null));
        verify(notificationPort, org.mockito.Mockito.never()).sendAccessLink(any(Email.class), anyString());
    }
}
