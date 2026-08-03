package at.fraihs.cookoff.cookoff.application.port;

import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository ports live in the application layer, not domain - see
 * docs/cookingChallenge/adr/0002-repository-ports-in-application-layer.md.
 */
@Repository
public interface PlateColorRepository {

    List<PlateColor> findAllActiveOrderedBySortOrder();

    Optional<PlateColor> findById(PlateColorId id);
}
