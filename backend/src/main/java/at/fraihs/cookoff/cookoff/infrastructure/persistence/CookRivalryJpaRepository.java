package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.CookRivalryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CookRivalryJpaRepository extends JpaRepository<CookRivalryJpaEntity, Long> {

    Page<CookRivalryJpaEntity> findAllByTotalChallengesGreaterThan(int total, Pageable pageable);

    Optional<CookRivalryJpaEntity> findByCookAAccountIdAndCookBAccountId(Long cookAAccountId, Long cookBAccountId);
}
