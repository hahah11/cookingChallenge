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
}
