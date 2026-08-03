package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import at.fraihs.cookoff.cookoff.domain.repository.PlateColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlateColorRepositoryImpl implements PlateColorRepository {

    private final PlateColorJpaRepository jpaRepository;
    private final PlateColorMapper mapper;

    @Override
    public List<PlateColor> findAllActiveOrderedBySortOrder() {
        return jpaRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PlateColor> findById(PlateColorId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }
}
