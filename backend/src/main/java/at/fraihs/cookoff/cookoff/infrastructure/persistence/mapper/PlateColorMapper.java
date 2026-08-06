package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import org.mapstruct.Mapper;

/**
 * PlateColor is immutable-shaped with no public constructor (only create/reconstitute
 * factories), so this mapper delegates to PlateColor.reconstitute(...) rather than
 * MapStruct's generated field-by-field mapping, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct.
 */
@Mapper(componentModel = "spring")
public interface PlateColorMapper {

    default PlateColor toDomain(PlateColorJpaEntity entity) {
        return PlateColor.reconstitute(
                new PlateColorId(entity.getId()),
                entity.getName(),
                entity.getHexCode(),
                entity.getSortOrder(),
                entity.isActive());
    }

    default PlateColorJpaEntity toEntity(PlateColor plateColor) {
        return new PlateColorJpaEntity(
                plateColor.getId().value(),
                plateColor.getName(),
                plateColor.getHexCode(),
                plateColor.getSortOrder(),
                plateColor.isActive());
    }
}
