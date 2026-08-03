package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.repository.CookRivalryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CookRivalryRepositoryImpl implements CookRivalryRepository {

    private final CookRivalryJpaRepository jpaRepository;
    private final CookRivalryMapper mapper;

    /**
     * Normalizes the pair the same way CookRivalry.orderPair(...) does before querying, so
     * this hits the DB's chk_cook_rivalries_ordered_pair-backed row regardless of argument
     * order.
     */
    @Override
    public Optional<CookRivalry> findByPair(AccountId firstAccountId, AccountId secondAccountId) {
        AccountId[] ordered = CookRivalry.orderPair(firstAccountId, secondAccountId);
        return jpaRepository.findByCookAAccountIdAndCookBAccountId(ordered[0].value(), ordered[1].value())
                .map(mapper::toDomain);
    }

    @Override
    public List<CookRivalry> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public CookRivalry save(CookRivalry rivalry) {
        CookRivalryJpaEntity saved = jpaRepository.save(mapper.toEntity(rivalry));
        return mapper.toDomain(saved);
    }
}
