package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ScoreSubmissionJpaRepository extends JpaRepository<ScoreSubmissionJpaEntity, Long> {

    List<ScoreSubmissionJpaEntity> findByChallengeId(Long challengeId);

    Optional<ScoreSubmissionJpaEntity> findByChallengeIdAndGuestAccountId(Long challengeId, Long guestAccountId);

    boolean existsByChallengeIdAndGuestAccountId(Long challengeId, Long guestAccountId);
}
