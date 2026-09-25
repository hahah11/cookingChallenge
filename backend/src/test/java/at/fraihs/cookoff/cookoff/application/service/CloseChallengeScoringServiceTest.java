package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatusRestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseChallengeScoringServiceTest {

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private CloseChallengeScoringService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), new DishName("Schnitzel"),
                cookAId, cookBId, List.of(), organizerId);
    }

    private Challenge closedChallenge() {
        Challenge challenge = openChallenge();
        challenge.closeScoring();
        return challenge;
    }

    private void stubCookNames() {
        when(accountLookup.getById(any())).thenReturn(
                new AccountSummary(AccountId.generate(), new Email("cook@example.com"), "Cook", "Cook"));
    }

    @Test
    void should_closeScoring_withoutEventOrMail_when_ownerRequests() {
        Challenge challenge = openChallenge();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        stubCookNames();

        ChallengeRestDto result = service.execute(challenge.getId().toString(), organizerId);

        assertEquals(ChallengeStatus.CLOSED, challenge.getStatus());
        assertEquals(ChallengeStatusRestDto.CLOSED, result.getStatus());
        verify(challengeRepository).save(challenge);
        verifyNoInteractions(eventPublisher, notificationPort);
    }

    @Test
    void should_closeScoring_when_requesterIsAdminButNotTheCreator() {
        Challenge challenge = openChallenge();
        AccountId adminId = AccountId.generate();
        when(accountLookup.canOrganize(adminId)).thenReturn(true);
        when(accountLookup.isAdmin(adminId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        stubCookNames();

        service.execute(challenge.getId().toString(), adminId);

        assertEquals(ChallengeStatus.CLOSED, challenge.getStatus());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void should_throw_when_scoringIsAlreadyClosed() {
        Challenge challenge = closedChallenge();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(IllegalStateException.class, () -> service.execute(challenge.getId().toString(), organizerId));
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(missingId.toString(), organizerId));
    }

    @Test
    void should_throw_when_accountCannotOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(ChallengeId.generate().toString(), organizerId));
        verify(challengeRepository, never()).findById(any());
    }

    @Test
    void should_throw_when_requesterDidNotCreateTheChallenge() {
        Challenge challenge = openChallenge();
        AccountId otherOrganizerId = AccountId.generate();
        when(accountLookup.canOrganize(otherOrganizerId)).thenReturn(true);
        when(accountLookup.isAdmin(otherOrganizerId)).thenReturn(false);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ForbiddenException.class, () -> service.execute(challenge.getId().toString(), otherOrganizerId));
        verify(challengeRepository, never()).save(any());
    }
}
