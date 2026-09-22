package at.fraihs.cookoff.shared.application.service;

import at.fraihs.cookoff.cookoff.PlateColorSummary;
import at.fraihs.cookoff.cookoff.PlateColors;
import at.fraihs.cookoff.shared.application.mapper.ConfigModelMapperImpl;
import at.fraihs.cookoff.shared.web.openapi.model.ConfigRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private PlateColors plateColors;

    private ConfigService service;

    @BeforeEach
    void setUp() {
        service = new ConfigService(plateColors, new ConfigModelMapperImpl());
        // @Value is not applied outside a Spring context; the real value comes from the
        // APP_VERSION env var the container image sets.
        ReflectionTestUtils.setField(service, "version", "0.1.42");
    }

    @Test
    void should_returnAllSystemRoles_and_activePlateColorsInSortOrder() {
        PlateColorSummary red = new PlateColorSummary("color-1", "Red", "#c0392b", 0);
        PlateColorSummary yellow = new PlateColorSummary("color-2", "Yellow", "#e0b400", 1);
        when(plateColors.listActive()).thenReturn(List.of(red, yellow));

        ConfigRestDto result = service.execute();

        assertEquals(3, result.getAvailableRoles().size());
        assertTrue(result.getAvailableRoles().contains(SystemRoleRestDto.ADMIN));
        assertTrue(result.getAvailableRoles().contains(SystemRoleRestDto.ORGANIZER));
        assertTrue(result.getAvailableRoles().contains(SystemRoleRestDto.USER));

        assertEquals(2, result.getPlateColors().size());
        assertEquals("color-1", result.getPlateColors().get(0).getId());
        assertEquals("Red", result.getPlateColors().get(0).getName());
        assertEquals("#c0392b", result.getPlateColors().get(0).getHexCode());
        assertEquals(0, result.getPlateColors().get(0).getSortOrder());
        assertEquals("color-2", result.getPlateColors().get(1).getId());

        assertTrue(result.getFeatureFlags().isEmpty());
    }

    @Test
    void should_reportTheRunningBuildVersion_when_configRequested() {
        when(plateColors.listActive()).thenReturn(List.of());

        assertEquals("0.1.42", service.execute().getVersion());
    }

    @Test
    void should_returnEmptyPlateColors_when_noneActive() {
        when(plateColors.listActive()).thenReturn(List.of());

        ConfigRestDto result = service.execute();

        assertTrue(result.getPlateColors().isEmpty());
    }
}
