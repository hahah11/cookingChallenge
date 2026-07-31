package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.CookAssignment;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Challenge is immutable with no public constructor (only create/reconstitute factories) and
 * its List<CookAssignment> doesn't line up 1:1 with the entity's flat cookAAccountId/
 * cookBAccountId columns, so this mapper is hand-written rather than MapStruct-generated, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct.
 */
@Mapper(componentModel = "spring")
public interface ChallengeMapper {

    default Challenge toDomain(ChallengeJpaEntity entity) {
        List<CookAssignment> cookAssignments = List.of(
                new CookAssignment(new AccountId(entity.getCookAAccountId()), DishLabel.A),
                new CookAssignment(new AccountId(entity.getCookBAccountId()), DishLabel.B));
        List<AccountId> guestAccountIds = entity.getGuestAccountIds().stream()
                .map(AccountId::new)
                .toList();
        return Challenge.reconstitute(
                new ChallengeId(entity.getId()),
                entity.getChallengeDate(),
                entity.getTitle(),
                new DishName(entity.getDishName()),
                cookAssignments,
                guestAccountIds,
                entity.getStatus(),
                new AccountId(entity.getCreatedByAccountId()));
    }

    default ChallengeJpaEntity toEntity(Challenge challenge) {
        List<Long> guestAccountIds = challenge.getGuestAccountIds().stream()
                .map(AccountId::value)
                .toList();
        return new ChallengeJpaEntity(
                challenge.getId().value(),
                challenge.getTitle(),
                challenge.getDate(),
                challenge.getDishName().value(),
                challenge.cookAssignmentFor(DishLabel.A).accountId().value(),
                challenge.cookAssignmentFor(DishLabel.B).accountId().value(),
                challenge.getStatus(),
                challenge.getCreatedBy().value(),
                new ArrayList<>(guestAccountIds));
    }
}
