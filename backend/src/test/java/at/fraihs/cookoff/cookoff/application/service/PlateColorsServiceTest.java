package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.cookoff.PlateColorSummary;
import at.fraihs.cookoff.cookoff.application.port.PlateColorRepository;
import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateColorsServiceTest {

    @Mock
    private PlateColorRepository plateColorRepository;

    private PlateColorsService service;

    @BeforeEach
    void setUp() {
        service = new PlateColorsService(plateColorRepository);
    }

    @Test
    void should_mapActivePlateColors_toSummaries() {
        PlateColor red = PlateColor.create("Red", "#c0392b", 0, true);
        when(plateColorRepository.findAllActiveOrderedBySortOrder()).thenReturn(List.of(red));

        List<PlateColorSummary> result = service.listActive();

        assertEquals(1, result.size());
        assertEquals(red.getId().toString(), result.get(0).id());
        assertEquals("Red", result.get(0).name());
        assertEquals("#c0392b", result.get(0).hexCode());
        assertEquals(0, result.get(0).sortOrder());
    }
}
