package at.fraihs.cookoff.shared.application.service;

import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.cookoff.PlateColors;
import at.fraihs.cookoff.shared.web.openapi.model.ConfigRestDto;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs GET /api/v1/config - bootstrap data fetched once at app start. Roles and the
 * active plate-color palette are never hardcoded or derived client-side; this is their
 * single source of truth.
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final PlateColors plateColors;
    private final ConfigModelMapper configModelMapper;

    @Transactional(readOnly = true)
    public ConfigRestDto execute() {
        return new ConfigRestDto(
                configModelMapper.mapRoles(SystemRole.values()),
                plateColors.listActive().stream()
                        .map(configModelMapper::toGenerated)
                        .toList(),
                Map.of());
    }
}
