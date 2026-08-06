package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalryId;
import org.mapstruct.Mapper;

/**
 * CookRivalry is immutable-shaped with no public constructor (only start/reconstitute
 * factories), so this mapper delegates to CookRivalry.reconstitute(...) rather than
 * MapStruct's generated field-by-field mapping, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct.
 */
@Mapper(componentModel = "spring")
public interface CookRivalryMapper {

    default CookRivalry toDomain(CookRivalryJpaEntity entity) {
        return CookRivalry.reconstitute(
                new CookRivalryId(entity.getId()),
                new AccountId(entity.getCookAAccountId()),
                new AccountId(entity.getCookBAccountId()),
                entity.getCookAWins(),
                entity.getCookBWins(),
                entity.getDraws(),
                entity.getTotalChallenges());
    }

    default CookRivalryJpaEntity toEntity(CookRivalry rivalry) {
        return new CookRivalryJpaEntity(
                rivalry.getId().value(),
                rivalry.getCookAAccountId().value(),
                rivalry.getCookBAccountId().value(),
                rivalry.getCookAWins(),
                rivalry.getCookBWins(),
                rivalry.getDraws(),
                rivalry.getTotalChallenges());
    }
}
