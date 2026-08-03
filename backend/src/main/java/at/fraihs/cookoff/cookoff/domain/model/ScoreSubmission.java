package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Own transaction boundary from Challenge since guests submit independently/concurrently.
 * Uniqueness of (challengeId, guestAccountId) is enforced by the repository/DB constraint,
 * not here — a single submission has no visibility into siblings.
 */
@AggregateRoot
public class ScoreSubmission {

    @Identity
    private final ScoreSubmissionId id;
    private final ChallengeId challengeId;
    private final AccountId guestAccountId;
    private List<Score> scores;
    private Instant submittedAt;

    private ScoreSubmission(ScoreSubmissionId id, ChallengeId challengeId, AccountId guestAccountId,
                             List<Score> scores, Instant submittedAt) {
        this.id = id;
        this.challengeId = challengeId;
        this.guestAccountId = guestAccountId;
        this.scores = List.copyOf(scores);
        this.submittedAt = submittedAt;
    }

    public static ScoreSubmission submit(ChallengeId challengeId, AccountId guestAccountId,
                                          List<Score> scores, Instant submittedAt) {
        validateScores(scores);
        return new ScoreSubmission(ScoreSubmissionId.generate(), challengeId, guestAccountId, scores, submittedAt);
    }

    public static ScoreSubmission reconstitute(ScoreSubmissionId id, ChallengeId challengeId,
                                                AccountId guestAccountId, List<Score> scores, Instant submittedAt) {
        return new ScoreSubmission(id, challengeId, guestAccountId, scores, submittedAt);
    }

    /** Resubmission before reveal (edit-until-reveal, see openapi-first-api-plan.md), replacing the prior scores. */
    public void update(List<Score> newScores, Instant updatedAt) {
        validateScores(newScores);
        this.scores = List.copyOf(newScores);
        this.submittedAt = updatedAt;
    }

    private static void validateScores(List<Score> scores) {
        if (scores.size() != 6) {
            throw new IllegalArgumentException(
                    "Expected exactly 6 scores (2 dish labels x 3 categories), got " + scores.size());
        }
        Set<String> seen = new HashSet<>();
        for (Score score : scores) {
            if (!seen.add(score.dishLabel() + ":" + score.category())) {
                throw new IllegalArgumentException(
                        "Duplicate score for dishLabel=" + score.dishLabel() + ", category=" + score.category());
            }
        }
    }

    public ScoreSubmissionId getId() {
        return id;
    }

    public ChallengeId getChallengeId() {
        return challengeId;
    }

    public AccountId getGuestAccountId() {
        return guestAccountId;
    }

    public List<Score> getScores() {
        return scores;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
