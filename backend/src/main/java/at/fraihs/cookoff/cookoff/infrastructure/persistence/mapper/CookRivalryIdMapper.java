package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.CookRivalryId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CookRivalryIdMapper {

    default CookRivalryId toDomain(Long raw) {
        return raw == null ? null : new CookRivalryId(raw);
    }

    default Long toRaw(CookRivalryId id) {
        return id == null ? null : id.value();
    }
}
