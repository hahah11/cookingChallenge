package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
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
    public Challenge save(Challenge challenge) {
        ChallengeJpaEntity saved = jpaRepository.save(mapper.toEntity(challenge));
        return mapper.toDomain(saved);
    }
}
