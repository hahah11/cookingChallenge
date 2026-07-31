package at.fraihs.cookoff.auth.infrastructure.accesslink;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AccessLinkJpaRepository extends JpaRepository<AccessLinkJpaEntity, Long> {

    Optional<AccessLinkJpaEntity> findByToken(String token);
}
