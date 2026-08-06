package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetChallengeForParticipantServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private GetChallengeForParticipantService service;

    private final AccountId cookA = AccountId.generate();
    private final AccountId cookB = AccountId.generate();
    private final AccountId guest = AccountId.generate();
    private final AccountId organizer = AccountId.generate();

    private Challenge challenge() {
        return Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                cookA, cookB, List.of(guest), organizer);
    }

    @Test
    void should_hideCookMapping_when_notYetRevealed() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(challenge.getId(), guest))
                .thenReturn(Optional.empty());

        ParticipantChallengeRestDto view = service.execute(challenge.getId().toString(), guest);

        assertTrue(view.getParticipantCookAssignments().stream().allMatch(a -> a.getAccountId().get() == null));
    }

    @Test
    void should_throw_when_requesterNotAParticipant() {
        Challenge challenge = challenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(NotAParticipantException.class,
                () -> service.execute(challenge.getId().toString(), AccountId.generate()));
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        when(challengeRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class,
                () -> service.execute(AccountId.generate().toString(), guest));
    }

    @Test
    void should_includeCookMapping_when_revealed() {
        Challenge challenge = challenge();
        challenge.reveal(cookA);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(challenge.getId(), guest))
                .thenReturn(Optional.empty());

        ParticipantChallengeRestDto view = service.execute(challenge.getId().toString(), guest);

        assertEquals(2, view.getParticipantCookAssignments().size());
        assertTrue(view.getParticipantCookAssignments().stream().allMatch(a -> a.getAccountId().get() != null));
    }
}
