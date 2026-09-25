package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeUnrevealed;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishName;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class DeleteChallengeServiceTest {

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeleteChallengeService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), new DishName("Schnitzel"), cookAId, cookBId, List.of(), organizerId);
    }

    private Challenge revealedChallenge() {
        Challenge challenge = openChallenge();
        challenge.closeScoring();
        challenge.reveal(cookAId);
        return challenge;
    }

    @Test
    void should_markChallengeDeleted_andPublishNoEvent_when_challengeIsNotRevealed() {
        Challenge challenge = openChallenge();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        service.execute(challenge.getId().toString(), organizerId);

        assertEquals(ChallengeStatus.DELETED, challenge.getStatus());
        verify(challengeRepository).save(challenge);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_publishUnrevealedEvent_when_challengeIsRevealed() {
        Challenge challenge = revealedChallenge();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        service.execute(challenge.getId().toString(), organizerId);

        assertEquals(ChallengeStatus.DELETED, challenge.getStatus());
        ArgumentCaptor<ChallengeUnrevealed> captor = ArgumentCaptor.forClass(ChallengeUnrevealed.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(cookAId, captor.getValue().previousOverallWinnerAccountId());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void should_allowAdmin_when_adminDeletesChallengeOfAnotherOrganizer() {
        Challenge challenge = openChallenge();
        AccountId adminId = AccountId.generate();
        when(accountLookup.canOrganize(adminId)).thenReturn(true);
        when(accountLookup.isAdmin(adminId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        service.execute(challenge.getId().toString(), adminId);

        assertEquals(ChallengeStatus.DELETED, challenge.getStatus());
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_throw_when_requesterDidNotCreateTheChallenge() {
        Challenge challenge = revealedChallenge();
        AccountId otherOrganizerId = AccountId.generate();
        when(accountLookup.canOrganize(otherOrganizerId)).thenReturn(true);
        when(accountLookup.isAdmin(otherOrganizerId)).thenReturn(false);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ForbiddenException.class, () -> service.execute(challenge.getId().toString(), otherOrganizerId));
        verify(challengeRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
