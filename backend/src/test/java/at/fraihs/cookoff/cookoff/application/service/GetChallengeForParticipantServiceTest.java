package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetChallengeForParticipantServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

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

        ChallengeParticipantView view = service.execute(challenge.getId().toString(), guest);

        assertNull(view.cookAssignments());
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

        ChallengeParticipantView view = service.execute(challenge.getId().toString(), guest);

        assertEquals(2, view.cookAssignments().size());
    }
}
