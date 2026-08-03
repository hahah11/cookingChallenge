package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CookRivalryTest {

    private final AccountId cookX = AccountId.generate();
    private final AccountId cookY = AccountId.generate();

    @Test
    void should_orderPairTheSameWay_regardlessOfInputOrder() {
        CookRivalry byXY = CookRivalry.start(cookX, cookY);
        CookRivalry byYX = CookRivalry.start(cookY, cookX);

        assertEquals(byXY.getCookAAccountId(), byYX.getCookAAccountId());
        assertEquals(byXY.getCookBAccountId(), byYX.getCookBAccountId());
    }

    @Test
    void should_incrementWinsForWinner_when_recordingResult() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);

        rivalry.recordResult(rivalry.getCookAAccountId());

        assertEquals(1, rivalry.getCookAWins());
        assertEquals(0, rivalry.getCookBWins());
        assertEquals(0, rivalry.getDraws());
        assertEquals(1, rivalry.getTotalChallenges());
    }

    @Test
    void should_incrementDraws_when_winnerIsNull() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);

        rivalry.recordResult(null);

        assertEquals(1, rivalry.getDraws());
        assertEquals(1, rivalry.getTotalChallenges());
    }

    @Test
    void should_throw_when_winnerIsNotPartOfTheRivalry() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);

        assertThrows(IllegalArgumentException.class, () -> rivalry.recordResult(AccountId.generate()));
    }

    @Test
    void should_decrementWinsForWinner_when_reversingAWin() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);
        rivalry.recordResult(rivalry.getCookAAccountId());

        rivalry.reverseResult(rivalry.getCookAAccountId());

        assertEquals(0, rivalry.getCookAWins());
        assertEquals(0, rivalry.getCookBWins());
        assertEquals(0, rivalry.getDraws());
        assertEquals(0, rivalry.getTotalChallenges());
    }

    @Test
    void should_decrementDraws_when_reversingADraw() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);
        rivalry.recordResult(null);

        rivalry.reverseResult(null);

        assertEquals(0, rivalry.getDraws());
        assertEquals(0, rivalry.getTotalChallenges());
    }

    @Test
    void should_throw_when_reversingOnAnEmptyRivalry() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);

        assertThrows(IllegalStateException.class, () -> rivalry.reverseResult(rivalry.getCookAAccountId()));
    }

    @Test
    void should_throw_when_reversingResultForAWinnerNotPartOfTheRivalry() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);
        rivalry.recordResult(rivalry.getCookAAccountId());

        assertThrows(IllegalArgumentException.class, () -> rivalry.reverseResult(AccountId.generate()));
    }

    @Test
    void should_endUpWithCorrectCounters_when_recordingReversingAndReRecordingADifferentWinner() {
        CookRivalry rivalry = CookRivalry.start(cookX, cookY);
        rivalry.recordResult(rivalry.getCookAAccountId());

        rivalry.reverseResult(rivalry.getCookAAccountId());
        rivalry.recordResult(rivalry.getCookBAccountId());

        assertEquals(0, rivalry.getCookAWins());
        assertEquals(1, rivalry.getCookBWins());
        assertEquals(0, rivalry.getDraws());
        assertEquals(1, rivalry.getTotalChallenges());
    }
}
