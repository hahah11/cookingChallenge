package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreSubmissionRepository {

    Optional<ScoreSubmission> findById(ScoreSubmissionId id);

    List<ScoreSubmission> findByChallengeId(ChallengeId challengeId);

    /** Looked up before an edit-until-reveal resubmission, to call ScoreSubmission#update on the existing aggregate. */
    Optional<ScoreSubmission> findByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId);

    boolean existsByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId);

    ScoreSubmission save(ScoreSubmission submission);
}
