package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.DishName;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DishNameMapper {

    default DishName toDomain(String raw) {
        return raw == null ? null : new DishName(raw);
    }

    default String toRaw(DishName dishName) {
        return dishName == null ? null : dishName.value();
    }
}
