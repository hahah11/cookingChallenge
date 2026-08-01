package at.fraihs.cookoff.cookoff.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public enum ChallengeStatus {
    OPEN,
    REVEALED
}
