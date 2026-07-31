package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.domain.repository.AccountRepository;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendChallengeInvitationsServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private AccountRepository accountRepository;

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

    private Account accountFor(AccountId id) {
        return Account.create(new Email(id + "@example.com"), "Name " + id, SystemRole.USER);
    }

    @Test
    void should_issueLinkAndNotify_forEachDistinctParticipant_when_challengeExists() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountRepository.findById(any(AccountId.class)))
                .thenAnswer(invocation -> Optional.of(accountFor(invocation.getArgument(0))));
        when(accessLinkService.issue(any(AccountId.class), anyLong(), any(Duration.class))).thenReturn("token");

        int sent = service.execute(challenge.getId().toString());

        assertEquals(3, sent);
        verify(accessLinkService, times(3)).issue(any(AccountId.class), anyLong(), any(Duration.class));
        verify(notificationPort, times(3)).sendAccessLink(any(Email.class), anyString());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        when(challengeRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(cookAId.toString()));
    }

    @Test
    void should_throw_when_participantAccountMissing() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(accountRepository.findById(any(AccountId.class))).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.execute(challenge.getId().toString()));
    }
}
