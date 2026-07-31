package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;

import java.util.List;
import java.util.Optional;

public interface ScoreSubmissionRepository {

    Optional<ScoreSubmission> findById(ScoreSubmissionId id);

    List<ScoreSubmission> findByChallengeId(ChallengeId challengeId);

    boolean existsByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId);

    ScoreSubmission save(ScoreSubmission submission);
}
