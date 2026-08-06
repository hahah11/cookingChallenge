package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.CookAssignment;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ChallengeJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Challenge is immutable with no public constructor (only create/reconstitute factories) and
 * its List&lt;CookAssignment&gt; doesn't line up 1:1 with the entity's flat cookAAccountId/
 * cookBAccountId columns, so this mapping is fully hand-written rather than MapStruct-generated.
 * It's a plain constructor-injected {@code @Component}, not a MapStruct {@code @Mapper}
 * abstract class: MapStruct doesn't forward a hand-declared constructor to its generated
 * {@code Impl} subclass, so an abstract {@code @Mapper} class can't have hand-written methods
 * reach a constructor-injected sub-mapper field — only a plain class can, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct. Composes the dedicated mappers for
 * each sub-object (typed ids, DishName, CookAssignment, RevealResult) instead of inlining
 * their conversions here.
 */
@Component
@RequiredArgsConstructor
public class ChallengeMapper {

    private final ChallengeIdMapper challengeIdMapper;
    private final AccountIdMapper accountIdMapper;
    private final PlateColorIdMapper plateColorIdMapper;
    private final DishNameMapper dishNameMapper;
    private final CookAssignmentMapper cookAssignmentMapper;
    private final RevealResultMapper revealResultMapper;

    public Challenge toDomain(ChallengeJpaEntity entity) {
        List<CookAssignment> cookAssignments = List.of(
                cookAssignmentMapper.toDomain(
                        accountIdMapper.toDomain(entity.getCookAAccountId()), DishLabel.A,
                        plateColorIdMapper.toDomain(entity.getCookAColorId())),
                cookAssignmentMapper.toDomain(
                        accountIdMapper.toDomain(entity.getCookBAccountId()), DishLabel.B,
                        plateColorIdMapper.toDomain(entity.getCookBColorId())));
        List<AccountId> guestAccountIds = entity.getGuestAccountIds().stream()
                .map(accountIdMapper::toDomain)
                .toList();
        return Challenge.reconstitute(
                challengeIdMapper.toDomain(entity.getId()),
                entity.getChallengeDate(),
                entity.getTitle(),
                dishNameMapper.toDomain(entity.getDishName()),
                cookAssignments,
                guestAccountIds,
                entity.getStatus(),
                accountIdMapper.toDomain(entity.getCreatedByAccountId()),
                entity.getImageRef(),
                revealResultMapper.toDomain(entity.isHasBeenRevealed(),
                        accountIdMapper.toDomain(entity.getLastRevealWinnerAccountId())));
    }

    public ChallengeJpaEntity toEntity(Challenge challenge) {
        List<Long> guestAccountIds = challenge.getGuestAccountIds().stream()
                .map(accountIdMapper::toRaw)
                .toList();
        CookAssignment cookA = challenge.cookAssignmentFor(DishLabel.A);
        CookAssignment cookB = challenge.cookAssignmentFor(DishLabel.B);
        return new ChallengeJpaEntity(
                challengeIdMapper.toRaw(challenge.getId()),
                challenge.getTitle(),
                challenge.getDate(),
                dishNameMapper.toRaw(challenge.getDishName()),
                accountIdMapper.toRaw(cookA.accountId()),
                accountIdMapper.toRaw(cookB.accountId()),
                plateColorIdMapper.toRaw(cookA.colorId()),
                plateColorIdMapper.toRaw(cookB.colorId()),
                challenge.getStatus(),
                accountIdMapper.toRaw(challenge.getCreatedBy()),
                new ArrayList<>(guestAccountIds),
                challenge.getImageRef(),
                revealResultMapper.hasBeenRevealed(challenge.getLastRevealResult()),
                accountIdMapper.toRaw(revealResultMapper.winnerAccountId(challenge.getLastRevealResult())));
    }
}
