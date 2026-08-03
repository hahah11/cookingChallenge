package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ChallengeRepositoryImpl implements ChallengeRepository {

    private final ChallengeJpaRepository jpaRepository;
    private final ChallengeMapper mapper;

    @Override
    public Optional<Challenge> findById(ChallengeId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Challenge> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Challenge> findOpenByParticipant(AccountId accountId) {
        return jpaRepository.findByStatusAndParticipant(ChallengeStatus.OPEN, accountId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Challenge> findByCookPair(AccountId firstAccountId, AccountId secondAccountId) {
        return jpaRepository.findByCookPair(firstAccountId.value(), secondAccountId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Challenge save(Challenge challenge) {
        ChallengeJpaEntity saved = jpaRepository.save(mapper.toEntity(challenge));
        return mapper.toDomain(saved);
    }
}
