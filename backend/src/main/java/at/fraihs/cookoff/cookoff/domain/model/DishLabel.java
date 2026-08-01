package at.fraihs.cookoff.cookoff.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

/** Blind label for scoring; the cook-to-label assignment is hidden from guests until reveal. */
@ValueObject
public enum DishLabel {
    A,
    B
}
