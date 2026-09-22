package at.fraihs.cookoff.auth.infrastructure.passwordreset;

import at.fraihs.cookoff.auth.application.dto.PasswordResetToken;
import at.fraihs.cookoff.auth.application.port.PasswordResetTokenRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.passwordreset.entity.PasswordResetTokenJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity(
                token.id(),
                token.accountId().value(),
                token.token(),
                token.expiresAt(),
                token.usedAt(),
                token.createdAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(PasswordResetTokenRepositoryImpl::toDomain);
    }

    @Override
    public Optional<PasswordResetToken> claim(String token, Instant now) {
        if (jpaRepository.claim(token, now) == 0) {
            return Optional.empty();
        }
        return findByToken(token);
    }

    @Override
    public void deleteAllByAccountId(AccountId accountId) {
        jpaRepository.deleteAllByAccountId(accountId.value());
    }

    private static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                new AccountId(entity.getAccountId()),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt());
    }
}
