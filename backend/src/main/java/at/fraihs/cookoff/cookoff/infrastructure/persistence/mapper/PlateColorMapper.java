package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.PlateColorJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PlateColor is immutable-shaped with no public constructor (only create/reconstitute
 * factories), so this mapping is fully hand-written rather than MapStruct-generated. It's a
 * plain constructor-injected {@code @Component}, not a MapStruct {@code @Mapper} abstract
 * class: MapStruct doesn't forward a hand-declared constructor to its generated {@code Impl}
 * subclass, so an abstract {@code @Mapper} class can't have hand-written methods reach a
 * constructor-injected sub-mapper field — only a plain class can, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct. Composes PlateColorIdMapper for its
 * id, per that doc's mapper-composition rule.
 */
@Component
@RequiredArgsConstructor
public class PlateColorMapper {

    private final PlateColorIdMapper plateColorIdMapper;

    public PlateColor toDomain(PlateColorJpaEntity entity) {
        return PlateColor.reconstitute(
                plateColorIdMapper.toDomain(entity.getId()),
                entity.getName(),
                entity.getHexCode(),
                entity.getSortOrder(),
                entity.isActive());
    }

    public PlateColorJpaEntity toEntity(PlateColor plateColor) {
        return new PlateColorJpaEntity(
                plateColorIdMapper.toRaw(plateColor.getId()),
                plateColor.getName(),
                plateColor.getHexCode(),
                plateColor.getSortOrder(),
                plateColor.isActive());
    }
}
