package at.fraihs.cookoff.shared.application.service;

import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.cookoff.PlateColorSummary;
import at.fraihs.cookoff.shared.web.openapi.model.PlateColorRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Generated-OpenAPI-model mapping for GET /api/v1/config, per docs/backend/03-code-style.md's
 * Mapper Usage section (same pattern as auth.application.service.AccountModelMapper). Maps
 * from cookoff.PlateColorSummary, not the domain PlateColor aggregate - that stays internal
 * to the cookoff module, see cookoff.PlateColors.
 */
@Mapper(componentModel = "spring")
public interface ConfigModelMapper {

    PlateColorRestDto toGenerated(PlateColorSummary plateColor);

    default List<SystemRoleRestDto> mapRoles(SystemRole[] roles) {
        return Arrays.stream(roles)
                .map(role -> SystemRoleRestDto.valueOf(role.name()))
                .sorted()
                .toList();
    }
}
