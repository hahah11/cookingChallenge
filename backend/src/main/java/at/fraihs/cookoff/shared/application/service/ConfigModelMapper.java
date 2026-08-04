package at.fraihs.cookoff.shared.application.service;

import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.cookoff.PlateColorSummary;
import org.mapstruct.Mapper;

import java.util.Arrays;
import java.util.List;

/**
 * Generated-OpenAPI-model mapping for GET /api/v1/config, per docs/backend/03-code-style.md's
 * Mapper Usage section (same pattern as auth.application.service.AccountModelMapper). Maps
 * from cookoff.PlateColorSummary, not the domain PlateColor aggregate - that stays internal
 * to the cookoff module, see cookoff.PlateColors.
 */
@Mapper(componentModel = "spring")
public interface ConfigModelMapper {

    at.fraihs.cookoff.shared.web.openapi.model.PlateColor toGenerated(PlateColorSummary plateColor);

    default List<at.fraihs.cookoff.shared.web.openapi.model.SystemRole> mapRoles(SystemRole[] roles) {
        return Arrays.stream(roles)
                .map(role -> at.fraihs.cookoff.shared.web.openapi.model.SystemRole.valueOf(role.name()))
                .sorted()
                .toList();
    }
}
