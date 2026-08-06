package at.fraihs.cookoff.cookoff.infrastructure.image;

import at.fraihs.cookoff.cookoff.infrastructure.image.entity.ChallengeImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChallengeImageJpaRepository extends JpaRepository<ChallengeImageJpaEntity, Long> {
}
