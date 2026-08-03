package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeUnrevealed;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeUnrevealedRivalryUpdaterTest {

    @Mock
    private CookRivalryRepository cookRivalryRepository;

    @InjectMocks
    private ChallengeUnrevealedRivalryUpdater updater;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();

    @Test
    void should_reverseTheRecordedWin_when_pairHasARecord() {
        ChallengeUnrevealed event = new ChallengeUnrevealed(ChallengeId.generate(), cookAId, cookBId, cookAId);
        CookRivalry existing = CookRivalry.start(cookAId, cookBId);
        existing.recordResult(cookAId);
        when(cookRivalryRepository.findByPair(cookAId, cookBId)).thenReturn(Optional.of(existing));

        updater.on(event);

        verify(cookRivalryRepository).save(existing);
        assertEquals(0, existing.getCookAWins());
        assertEquals(0, existing.getTotalChallenges());
    }

    @Test
    void should_throw_when_noRivalryExistsForThisPair() {
        ChallengeUnrevealed event = new ChallengeUnrevealed(ChallengeId.generate(), cookAId, cookBId, cookAId);
        when(cookRivalryRepository.findByPair(cookAId, cookBId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> updater.on(event));
    }
}
