package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChallengeJpaRepository extends JpaRepository<ChallengeJpaEntity, Long> {
}
