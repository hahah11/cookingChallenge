package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeTest {

    private final AccountId cookA = AccountId.generate();
    private final AccountId cookB = AccountId.generate();
    private final AccountId organizer = AccountId.generate();

    private Challenge newChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
    }

    @Test
    void should_throw_when_bothCooksAreTheSameAccount() {
        assertThrows(IllegalArgumentException.class, () ->
                Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"), cookA, cookA, List.of(), organizer));
    }

    @Test
    void should_assignDistinctLabels_when_created() {
        Challenge challenge = newChallenge();

        assertEquals(cookA, challenge.cookAssignmentFor(DishLabel.A).accountId());
        assertEquals(cookB, challenge.cookAssignmentFor(DishLabel.B).accountId());
        assertEquals(ChallengeStatus.OPEN, challenge.getStatus());
    }

    @Test
    void should_addGuest_when_open() {
        Challenge challenge = newChallenge();
        AccountId guest = AccountId.generate();

        challenge.addGuest(guest);

        assertTrue(challenge.isGuest(guest));
    }

    @Test
    void should_throw_when_addingTheSameGuestTwice() {
        Challenge challenge = newChallenge();
        AccountId guest = AccountId.generate();
        challenge.addGuest(guest);

        assertThrows(IllegalStateException.class, () -> challenge.addGuest(guest));
    }

    @Test
    void should_transitionToRevealed_when_revealed() {
        Challenge challenge = newChallenge();

        ChallengeRevealed event = challenge.reveal(cookA);

        assertEquals(ChallengeStatus.REVEALED, challenge.getStatus());
        assertEquals(challenge.getId(), event.challengeId());
        assertEquals(cookA, event.cookAAccountId());
        assertEquals(cookB, event.cookBAccountId());
        assertEquals(cookA, event.overallWinnerAccountId());
    }

    @Test
    void should_throw_when_revealingTwice() {
        Challenge challenge = newChallenge();
        challenge.reveal(cookA);

        assertThrows(IllegalStateException.class, () -> challenge.reveal(cookA));
    }

    @Test
    void should_throw_when_addingGuestAfterReveal() {
        Challenge challenge = newChallenge();
        challenge.reveal(null);

        assertThrows(IllegalStateException.class, () -> challenge.addGuest(AccountId.generate()));
    }
}
