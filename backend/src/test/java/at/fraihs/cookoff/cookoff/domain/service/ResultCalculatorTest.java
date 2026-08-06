package at.fraihs.cookoff.cookoff.domain.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeResult;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultCalculatorTest {

    private final ResultCalculator calculator = new ResultCalculator();
    private final AccountId cookA = AccountId.generate();
    private final AccountId cookB = AccountId.generate();
    private final AccountId organizer = AccountId.generate();

    private Challenge newChallenge() {
        return Challenge.create(LocalDate.now(), "Test", new DishName("Schnitzel"), cookA, cookB, List.of(), organizer);
    }

    private ScoreSubmission submissionFavoring(ChallengeId challengeId, DishLabel favored, int categoriesWonByFavored) {
        DishLabel other = favored == DishLabel.A ? DishLabel.B : DishLabel.A;
        Category[] categories = Category.values();
        List<Score> scores = new java.util.ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            boolean favoredWinsThisCategory = i < categoriesWonByFavored;
            scores.add(new Score(favored, categories[i], favoredWinsThisCategory ? 5 : 2));
            scores.add(new Score(other, categories[i], favoredWinsThisCategory ? 2 : 5));
        }
        return ScoreSubmission.submit(challengeId, AccountId.generate(), scores, Instant.now());
    }

    @Test
    void should_declareCookAOverallWinner_when_cookAWinsMoreCategories() {
        Challenge challenge = newChallenge();
        ScoreSubmission submission = submissionFavoring(challenge.getId(), DishLabel.A, 2);

        ChallengeResult result = calculator.calculate(challenge, List.of(submission));

        assertEquals(cookA, result.overallWinnerAccountId());
        assertEquals(DishLabel.A, result.categoryWinners().get(Category.MUNDGEFUEHL));
        assertEquals(DishLabel.A, result.categoryWinners().get(Category.TELLERSPRACHE));
        assertEquals(DishLabel.B, result.categoryWinners().get(Category.GESCHMACK));
    }

    @Test
    void should_omitCategory_when_categoryIsTied() {
        Challenge challenge = newChallenge();
        ScoreSubmission tied = ScoreSubmission.submit(challenge.getId(), AccountId.generate(), List.of(
                new Score(DishLabel.A, Category.MUNDGEFUEHL, 3),
                new Score(DishLabel.B, Category.MUNDGEFUEHL, 3),
                new Score(DishLabel.A, Category.TELLERSPRACHE, 5),
                new Score(DishLabel.B, Category.TELLERSPRACHE, 1),
                new Score(DishLabel.A, Category.GESCHMACK, 1),
                new Score(DishLabel.B, Category.GESCHMACK, 5)
        ), Instant.now());

        ChallengeResult result = calculator.calculate(challenge, List.of(tied));

        assertFalse(result.categoryWinners().containsKey(Category.MUNDGEFUEHL));
        assertEquals(DishLabel.A, result.categoryWinners().get(Category.TELLERSPRACHE));
        assertEquals(DishLabel.B, result.categoryWinners().get(Category.GESCHMACK));
    }

    @Test
    void should_declareOverallDraw_when_categoriesSplitEvenly() {
        Challenge challenge = newChallenge();
        ScoreSubmission split = ScoreSubmission.submit(challenge.getId(), AccountId.generate(), List.of(
                new Score(DishLabel.A, Category.MUNDGEFUEHL, 5),
                new Score(DishLabel.B, Category.MUNDGEFUEHL, 1),
                new Score(DishLabel.A, Category.TELLERSPRACHE, 1),
                new Score(DishLabel.B, Category.TELLERSPRACHE, 5),
                new Score(DishLabel.A, Category.GESCHMACK, 3),
                new Score(DishLabel.B, Category.GESCHMACK, 3)
        ), Instant.now());

        ChallengeResult result = calculator.calculate(challenge, List.of(split));

        assertNull(result.overallWinnerAccountId());
    }

    @Test
    void should_sumAcrossMultipleSubmissions() {
        Challenge challenge = newChallenge();
        ScoreSubmission s1 = submissionFavoring(challenge.getId(), DishLabel.B, 3);
        ScoreSubmission s2 = submissionFavoring(challenge.getId(), DishLabel.B, 3);

        ChallengeResult result = calculator.calculate(challenge, List.of(s1, s2));

        assertEquals(cookB, result.overallWinnerAccountId());
        assertTrue(result.categoryWinners().values().stream().allMatch(label -> label == DishLabel.B));
    }
}
