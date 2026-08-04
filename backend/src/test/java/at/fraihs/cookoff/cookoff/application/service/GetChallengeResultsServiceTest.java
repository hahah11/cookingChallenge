package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetChallengeResultsServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private CookRivalryRepository cookRivalryRepository;

    @Mock
    private AccountLookup accountLookup;

    @InjectMocks
    private GetChallengeResultsService service;

    private final AccountId cookA = AccountId.generate();
    private final AccountId cookB = AccountId.generate();
    private final AccountId organizer = AccountId.generate();

    @Test
    void should_throw_when_challengeNotYetRevealed() {
        Challenge open = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
        when(challengeRepository.findById(open.getId())).thenReturn(Optional.of(open));

        assertThrows(ChallengeNotRevealedException.class, () -> service.execute(open.getId().toString(), cookA));
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        when(challengeRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class,
                () -> service.execute(AccountId.generate().toString(), AccountId.generate()));
    }

    @Test
    void should_throw_when_requesterIsNotAParticipant() {
        Challenge challenge = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
        challenge.reveal(cookA);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(NotAParticipantException.class,
                () -> service.execute(challenge.getId().toString(), AccountId.generate()));
    }

    @Test
    void should_returnResults_when_challengeRevealed() {
        Challenge challenge = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
        challenge.reveal(cookA);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId())).thenReturn(List.of());
        when(cookRivalryRepository.findByPair(any(), any())).thenReturn(Optional.empty());
        when(accountLookup.getById(cookA)).thenReturn(new AccountSummary(cookA, new Email("a@x.com"), "Cook A"));
        when(accountLookup.getById(cookB)).thenReturn(new AccountSummary(cookB, new Email("b@x.com"), "Cook B"));

        ChallengeResult result = service.execute(challenge.getId().toString(), cookA);

        assertEquals(challenge.getId().toString(), result.getChallengeId());
    }
}
