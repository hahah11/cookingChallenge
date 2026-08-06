package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChallengeIdMapper {

    default ChallengeId toDomain(Long raw) {
        return raw == null ? null : new ChallengeId(raw);
    }

    default Long toRaw(ChallengeId id) {
        return id == null ? null : id.value();
    }
}
