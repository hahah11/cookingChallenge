package at.fraihs.cookoff.cookoff.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Score(DishLabel dishLabel, Category category, int points) {

    public Score {
        if (dishLabel == null) {
            throw new IllegalArgumentException("dishLabel must not be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (points < 0 || points > 5) {
            throw new IllegalArgumentException("points must be between 0 and 5, was " + points);
        }
    }
}
