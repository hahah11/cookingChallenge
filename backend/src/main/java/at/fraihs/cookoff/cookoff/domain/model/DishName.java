package at.fraihs.cookoff.cookoff.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/** The one dish both cooks make (e.g. "Schnitzel") — a Challenge has a single DishName, not one per label. */
@ValueObject
public record DishName(String value) {

    public DishName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Dish name must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
