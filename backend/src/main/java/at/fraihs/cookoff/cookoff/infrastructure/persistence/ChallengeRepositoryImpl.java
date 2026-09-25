package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ChallengeJpaEntity;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ChallengeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ChallengeRepositoryImpl implements ChallengeRepository {

    private static final ChallengeStatus HIDDEN = ChallengeStatus.DELETED;

    private final ChallengeJpaRepository jpaRepository;
    private final ChallengeMapper mapper;

    @Override
    public Optional<Challenge> findById(ChallengeId id) {
        return jpaRepository.findByIdAndStatusNot(id.value(), HIDDEN).map(mapper::toDomain);
    }

    @Override
    public Page<Challenge> findAll(Pageable pageable) {
        return jpaRepository.findAllByStatusNot(HIDDEN, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Challenge> findAllByCreatedBy(AccountId createdBy, Pageable pageable) {
        return jpaRepository.findAllByCreatedByAccountIdAndStatusNot(createdBy.value(), HIDDEN, pageable).map(mapper::toDomain);
    }

    @Override
    public List<Challenge> findByParticipant(AccountId accountId) {
        return jpaRepository.findByParticipant(accountId.value(), HIDDEN).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Challenge> findByCookPair(AccountId firstAccountId, AccountId secondAccountId) {
        return jpaRepository.findByCookPair(firstAccountId.value(), secondAccountId.value(), HIDDEN).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Challenge save(Challenge challenge) {
        ChallengeJpaEntity saved = jpaRepository.save(mapper.toEntity(challenge));
        return mapper.toDomain(saved);
    }
}
