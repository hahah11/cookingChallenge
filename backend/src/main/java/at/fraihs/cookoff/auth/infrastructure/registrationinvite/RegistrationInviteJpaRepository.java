package at.fraihs.cookoff.auth.infrastructure.registrationinvite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface RegistrationInviteJpaRepository extends JpaRepository<RegistrationInviteJpaEntity, Long> {

    Optional<RegistrationInviteJpaEntity> findByToken(String token);
}
