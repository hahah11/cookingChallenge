package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScoreSubmissionIdMapper {

    default ScoreSubmissionId toDomain(Long raw) {
        return raw == null ? null : new ScoreSubmissionId(raw);
    }

    default Long toRaw(ScoreSubmissionId id) {
        return id == null ? null : id.value();
    }
}
