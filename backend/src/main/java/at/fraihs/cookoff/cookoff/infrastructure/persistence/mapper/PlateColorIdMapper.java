package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlateColorIdMapper {

    default PlateColorId toDomain(Long raw) {
        return raw == null ? null : new PlateColorId(raw);
    }

    default Long toRaw(PlateColorId id) {
        return id == null ? null : id.value();
    }
}
