package at.fraihs.cookoff.cookoff.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public enum ChallengeStatus {
    OPEN,
    CLOSED,
    REVEALED,
    /** Soft-deleted: kept in the database, but never exposed or counted anywhere. */
    DELETED
}
