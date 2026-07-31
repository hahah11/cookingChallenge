package at.fraihs.cookoff.cookoff.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void should_accept_when_pointsInRange(int points) {
        new Score(DishLabel.A, Category.GESCHMACK, points);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 6, 100})
    void should_throw_when_pointsOutOfRange(int points) {
        assertThrows(IllegalArgumentException.class, () -> new Score(DishLabel.A, Category.GESCHMACK, points));
    }

    @Test
    void should_throw_when_dishLabelIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Score(null, Category.GESCHMACK, 3));
    }

    @Test
    void should_throw_when_categoryIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Score(DishLabel.A, null, 3));
    }
}
