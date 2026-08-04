package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.cookoff.PlateColorSummary;
import at.fraihs.cookoff.cookoff.PlateColors;
import at.fraihs.cookoff.cookoff.application.port.PlateColorRepository;
import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlateColorsService implements PlateColors {

    private final PlateColorRepository plateColorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlateColorSummary> listActive() {
        return plateColorRepository.findAllActiveOrderedBySortOrder().stream()
                .map(PlateColorsService::toSummary)
                .toList();
    }

    private static PlateColorSummary toSummary(PlateColor plateColor) {
        return new PlateColorSummary(
                plateColor.getId().toString(), plateColor.getName(), plateColor.getHexCode(), plateColor.getSortOrder());
    }
}
