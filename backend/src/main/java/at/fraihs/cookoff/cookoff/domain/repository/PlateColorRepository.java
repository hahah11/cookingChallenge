package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlateColorRepository {

    List<PlateColor> findAllActiveOrderedBySortOrder();

    Optional<PlateColor> findById(PlateColorId id);
}
