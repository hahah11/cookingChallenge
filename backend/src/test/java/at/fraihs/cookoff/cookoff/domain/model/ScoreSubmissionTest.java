package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreSubmissionTest {

    private static List<Score> completeScoreSet() {
        List<Score> scores = new ArrayList<>();
        for (DishLabel label : DishLabel.values()) {
            for (Category category : Category.values()) {
                scores.add(new Score(label, category, 3));
            }
        }
        return scores;
    }

    @Test
    void should_submit_when_scoresCoverEveryLabelAndCategoryOnce() {
        ScoreSubmission submission = ScoreSubmission.submit(
                ChallengeId.generate(), AccountId.generate(), completeScoreSet(), Instant.now());

        assertEquals(6, submission.getScores().size());
    }

    @Test
    void should_throw_when_notExactlySixScores() {
        List<Score> tooFew = completeScoreSet().subList(0, 5);

        assertThrows(IllegalArgumentException.class, () ->
                ScoreSubmission.submit(ChallengeId.generate(), AccountId.generate(), tooFew, Instant.now()));
    }

    @Test
    void should_throw_when_duplicateLabelCategoryCombination() {
        List<Score> scores = new ArrayList<>(completeScoreSet().subList(0, 5));
        scores.add(new Score(DishLabel.A, Category.GESCHMACK, 1)); // duplicate of an existing entry

        assertThrows(IllegalArgumentException.class, () ->
                ScoreSubmission.submit(ChallengeId.generate(), AccountId.generate(), scores, Instant.now()));
    }

    @Test
    void should_replaceScoresAndSubmittedAt_when_updating() {
        Instant firstSubmittedAt = Instant.now().minusSeconds(60);
        ScoreSubmission submission = ScoreSubmission.submit(
                ChallengeId.generate(), AccountId.generate(), completeScoreSet(), firstSubmittedAt);
        List<Score> revisedScores = new ArrayList<>();
        for (DishLabel label : DishLabel.values()) {
            for (Category category : Category.values()) {
                revisedScores.add(new Score(label, category, 5));
            }
        }
        Instant updatedAt = Instant.now();

        submission.update(revisedScores, updatedAt);

        assertEquals(revisedScores, submission.getScores());
        assertEquals(updatedAt, submission.getSubmittedAt());
    }

    @Test
    void should_throw_when_updatingWithNotExactlySixScores() {
        ScoreSubmission submission = ScoreSubmission.submit(
                ChallengeId.generate(), AccountId.generate(), completeScoreSet(), Instant.now());
        List<Score> tooFew = completeScoreSet().subList(0, 5);

        assertThrows(IllegalArgumentException.class, () -> submission.update(tooFew, Instant.now()));
    }
}
