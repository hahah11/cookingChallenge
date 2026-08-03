package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlateColorRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlateColorJpaRepository jpaRepository;

    private PlateColorRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PlateColorRepositoryImpl(jpaRepository, new PlateColorMapperImpl());
    }

    @Test
    void should_roundTripPlateColor_when_persistingThenFindingById() {
        long id = persistPlateColor("Green", "#00FF00", 5, true);

        Optional<PlateColor> found = repository.findById(new PlateColorId(id));

        assertTrue(found.isPresent());
        assertEquals("Green", found.get().getName());
        assertEquals("#00FF00", found.get().getHexCode());
        assertEquals(5, found.get().getSortOrder());
        assertTrue(found.get().isActive());
    }

    @Test
    void should_includeSeededDefaultColors_when_queryingActiveOrderedBySortOrder() {
        List<PlateColor> active = repository.findAllActiveOrderedBySortOrder();

        assertTrue(active.size() >= 2);
        assertEquals("Red", active.get(0).getName());
        assertEquals("Yellow", active.get(1).getName());
    }

    @Test
    void should_excludeInactiveColors_when_queryingActiveOrderedBySortOrder() {
        long activeExtraId = persistPlateColor("Green", "#00FF00", 5, true);
        long inactiveId = persistPlateColor("Blue", "#0000FF", 3, false);

        List<PlateColor> active = repository.findAllActiveOrderedBySortOrder();

        assertTrue(active.stream().anyMatch(c -> c.getId().value() == activeExtraId));
        assertTrue(active.stream().noneMatch(c -> c.getId().value() == inactiveId));
        List<Integer> sortOrders = active.stream().map(PlateColor::getSortOrder).toList();
        assertEquals(sortOrders.stream().sorted().toList(), sortOrders);
    }

    private long persistPlateColor(String name, String hexCode, int sortOrder, boolean active) {
        PlateColorId id = PlateColorId.generate();
        entityManager.persistAndFlush(new PlateColorJpaEntity(id.value(), name, hexCode, sortOrder, active));
        return id.value();
    }
}
