package at.fraihs.cookoff.auth.infrastructure.persistence;

import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

    Optional<AccountJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
