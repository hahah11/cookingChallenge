package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.repository.CookRivalryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeRevealedRivalryUpdaterTest {

    @Mock
    private CookRivalryRepository cookRivalryRepository;

    @InjectMocks
    private ChallengeRevealedRivalryUpdater updater;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();

    @Test
    void should_startNewRivalry_when_noneExistsYetForThisPair() {
        ChallengeRevealed event = new ChallengeRevealed(ChallengeId.generate(), cookAId, cookBId, cookAId);
        when(cookRivalryRepository.findByPair(cookAId, cookBId)).thenReturn(Optional.empty());

        updater.on(event);

        verify(cookRivalryRepository).save(argThatHasOneWinFor(cookAId));
    }

    @Test
    void should_updateExistingRivalry_when_pairAlreadyHasARecord() {
        ChallengeRevealed event = new ChallengeRevealed(ChallengeId.generate(), cookAId, cookBId, null);
        CookRivalry existing = CookRivalry.start(cookAId, cookBId);
        existing.recordResult(cookAId);
        when(cookRivalryRepository.findByPair(cookAId, cookBId)).thenReturn(Optional.of(existing));

        updater.on(event);

        verify(cookRivalryRepository).save(existing);
        assertEquals(1, existing.getDraws());
        assertEquals(2, existing.getTotalChallenges());
    }

    private CookRivalry argThatHasOneWinFor(AccountId winner) {
        return org.mockito.ArgumentMatchers.argThat(rivalry ->
                rivalry.getTotalChallenges() == 1
                        && (winner.equals(rivalry.getCookAAccountId()) ? rivalry.getCookAWins() == 1 : rivalry.getCookBWins() == 1));
    }
}
