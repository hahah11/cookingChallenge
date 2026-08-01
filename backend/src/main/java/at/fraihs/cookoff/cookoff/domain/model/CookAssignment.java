package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.jmolecules.ddd.annotation.ValueObject;

/** Which Account cooks under which blind label, within one Challenge. Not a global identity — see docs/cookingChallenge/first-plan.md. */
@ValueObject
public record CookAssignment(AccountId accountId, DishLabel label) {

    public CookAssignment {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId must not be null");
        }
        if (label == null) {
            throw new IllegalArgumentException("label must not be null");
        }
    }
}
