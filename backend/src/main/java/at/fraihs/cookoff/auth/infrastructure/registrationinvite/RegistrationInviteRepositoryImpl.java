package at.fraihs.cookoff.auth.infrastructure.registrationinvite;

import at.fraihs.cookoff.auth.application.port.RegistrationInvite;
import at.fraihs.cookoff.auth.application.port.RegistrationInviteRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class RegistrationInviteRepositoryImpl implements RegistrationInviteRepository {

    private final RegistrationInviteJpaRepository jpaRepository;

    @Override
    public RegistrationInvite save(RegistrationInvite registrationInvite) {
        RegistrationInviteJpaEntity entity = new RegistrationInviteJpaEntity(
                registrationInvite.id(),
                registrationInvite.issuedByAccountId().value(),
                registrationInvite.challengeId(),
                registrationInvite.token(),
                registrationInvite.expiresAt());
        RegistrationInviteJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<RegistrationInvite> findByToken(String token) {
        return jpaRepository.findByToken(token).map(RegistrationInviteRepositoryImpl::toDomain);
    }

    private static RegistrationInvite toDomain(RegistrationInviteJpaEntity entity) {
        return new RegistrationInvite(
                entity.getId(),
                new AccountId(entity.getIssuedByAccountId()),
                entity.getChallengeId(),
                entity.getToken(),
                entity.getExpiresAt());
    }
}
