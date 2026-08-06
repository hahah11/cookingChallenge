package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.RevealResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RevealResultMapper {

    default RevealResult toDomain(boolean hasBeenRevealed, AccountId winnerAccountId) {
        return hasBeenRevealed ? new RevealResult(winnerAccountId) : null;
    }

    default boolean hasBeenRevealed(RevealResult result) {
        return result != null;
    }

    default AccountId winnerAccountId(RevealResult result) {
        return result == null ? null : result.winnerAccountId();
    }
}
