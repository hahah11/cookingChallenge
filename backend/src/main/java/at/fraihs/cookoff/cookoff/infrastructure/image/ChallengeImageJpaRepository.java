package at.fraihs.cookoff.cookoff.infrastructure.image;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChallengeImageJpaRepository extends JpaRepository<ChallengeImageJpaEntity, Long> {
}
