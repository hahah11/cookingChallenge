package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.CookAssignment;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CookAssignmentMapper {

    default CookAssignment toDomain(AccountId accountId, DishLabel label, PlateColorId colorId) {
        return new CookAssignment(accountId, label, colorId);
    }
}
