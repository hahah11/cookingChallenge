package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.RegistrationInvites;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRegistrationInviteServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private RegistrationInvites registrationInvites;

    @InjectMocks
    private CreateRegistrationInviteService service;

    private final AccountId organizerId = AccountId.generate();

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(), organizerId);
    }

    @Test
    void should_issueInvite_when_organizerAndChallengeIsOpen() {
        Challenge challenge = openChallenge();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(registrationInvites.issue(organizerId, challenge.getId().value(), Duration.ofDays(30)))
                .thenReturn("tok");

        String token = service.execute(organizerId, challenge.getId());

        assertEquals("tok", token);
    }

    @Test
    void should_throw_when_accountCannotOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(organizerId, ChallengeId.generate()));
        verify(challengeRepository, never()).findById(any());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(organizerId, missingId));
    }

    @Test
    void should_throw_when_challengeIsAlreadyRevealed() {
        Challenge challenge = openChallenge();
        challenge.reveal(null);
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ChallengeNotOpenException.class, () -> service.execute(organizerId, challenge.getId()));
        verify(registrationInvites, never()).issue(any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }
}
