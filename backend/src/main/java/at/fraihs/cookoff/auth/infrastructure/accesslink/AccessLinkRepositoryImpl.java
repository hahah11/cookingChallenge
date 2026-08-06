package at.fraihs.cookoff.auth.infrastructure.accesslink;

import at.fraihs.cookoff.auth.application.dto.AccessLink;
import at.fraihs.cookoff.auth.application.port.AccessLinkRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.accesslink.entity.AccessLinkJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AccessLinkRepositoryImpl implements AccessLinkRepository {

    private final AccessLinkJpaRepository jpaRepository;

    @Override
    public AccessLink save(AccessLink accessLink) {
        AccessLinkJpaEntity entity = new AccessLinkJpaEntity(
                accessLink.id(),
                accessLink.accountId().value(),
                accessLink.challengeId(),
                accessLink.token(),
                accessLink.expiresAt(),
                accessLink.usedAt(),
                accessLink.createdAt());
        AccessLinkJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AccessLink> findByToken(String token) {
        return jpaRepository.findByToken(token).map(AccessLinkRepositoryImpl::toDomain);
    }

    private static AccessLink toDomain(AccessLinkJpaEntity entity) {
        return new AccessLink(
                entity.getId(),
                new AccountId(entity.getAccountId()),
                entity.getChallengeId(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt());
    }
}
