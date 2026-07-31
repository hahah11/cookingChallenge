package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ScoreSubmissionJpaRepository extends JpaRepository<ScoreSubmissionJpaEntity, Long> {

    List<ScoreSubmissionJpaEntity> findByChallengeId(Long challengeId);

    boolean existsByChallengeIdAndGuestAccountId(Long challengeId, Long guestAccountId);
}
