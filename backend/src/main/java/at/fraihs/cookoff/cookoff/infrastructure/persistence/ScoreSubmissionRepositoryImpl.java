package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.DuplicateSubmissionException;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ScoreSubmissionJpaEntity;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ScoreSubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ScoreSubmissionRepositoryImpl implements ScoreSubmissionRepository {

    private final ScoreSubmissionJpaRepository jpaRepository;
    private final ScoreSubmissionMapper mapper;

    @Override
    public Optional<ScoreSubmission> findById(ScoreSubmissionId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<ScoreSubmission> findByChallengeId(ChallengeId challengeId) {
        return jpaRepository.findByChallengeId(challengeId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ScoreSubmission> findByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId) {
        return jpaRepository.findByChallengeIdAndGuestAccountId(challengeId.value(), guestAccountId.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId) {
        return jpaRepository.existsByChallengeIdAndGuestAccountId(challengeId.value(), guestAccountId.value());
    }

    /**
     * The application-layer exists-check (SubmitScoreService) is just a friendlier fast path;
     * the score_submissions unique constraint is the real safety net against a race between
     * that check and this save. saveAndFlush (not save) so the constraint violation surfaces
     * here, synchronously, instead of at the next unrelated flush point.
     */
    @Override
    public ScoreSubmission save(ScoreSubmission submission) {
        try {
            ScoreSubmissionJpaEntity saved = jpaRepository.saveAndFlush(mapper.toEntity(submission));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateSubmissionException(
                    submission.getGuestAccountId().toString(), submission.getChallengeId().toString());
        }
    }
}
