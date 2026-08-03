package at.fraihs.cookoff.cookoff.application.port;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository ports live in the application layer, not domain - see
 * docs/cookingChallenge/adr/0002-repository-ports-in-application-layer.md.
 */
@Repository
public interface ScoreSubmissionRepository {

    Optional<ScoreSubmission> findById(ScoreSubmissionId id);

    List<ScoreSubmission> findByChallengeId(ChallengeId challengeId);

    /** Looked up before an edit-until-reveal resubmission, to call ScoreSubmission#update on the existing aggregate. */
    Optional<ScoreSubmission> findByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId);

    boolean existsByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId);

    ScoreSubmission save(ScoreSubmission submission);
}
