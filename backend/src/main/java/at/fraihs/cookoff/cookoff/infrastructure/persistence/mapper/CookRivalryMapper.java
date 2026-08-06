package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.CookRivalryJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CookRivalry is immutable-shaped with no public constructor (only start/reconstitute
 * factories), so this mapping is fully hand-written rather than MapStruct-generated. It's a
 * plain constructor-injected {@code @Component}, not a MapStruct {@code @Mapper} abstract
 * class: MapStruct doesn't forward a hand-declared constructor to its generated {@code Impl}
 * subclass, so an abstract {@code @Mapper} class can't have hand-written methods reach a
 * constructor-injected sub-mapper field — only a plain class can, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct. Composes CookRivalryIdMapper and
 * AccountIdMapper for its sub-objects, per that doc's mapper-composition rule.
 */
@Component
@RequiredArgsConstructor
public class CookRivalryMapper {

    private final CookRivalryIdMapper cookRivalryIdMapper;
    private final AccountIdMapper accountIdMapper;

    public CookRivalry toDomain(CookRivalryJpaEntity entity) {
        return CookRivalry.reconstitute(
                cookRivalryIdMapper.toDomain(entity.getId()),
                accountIdMapper.toDomain(entity.getCookAAccountId()),
                accountIdMapper.toDomain(entity.getCookBAccountId()),
                entity.getCookAWins(),
                entity.getCookBWins(),
                entity.getDraws(),
                entity.getTotalChallenges());
    }

    public CookRivalryJpaEntity toEntity(CookRivalry rivalry) {
        return new CookRivalryJpaEntity(
                cookRivalryIdMapper.toRaw(rivalry.getId()),
                accountIdMapper.toRaw(rivalry.getCookAAccountId()),
                accountIdMapper.toRaw(rivalry.getCookBAccountId()),
                rivalry.getCookAWins(),
                rivalry.getCookBWins(),
                rivalry.getDraws(),
                rivalry.getTotalChallenges());
    }
}
