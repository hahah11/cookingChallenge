package at.fraihs.cookoff.auth.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AccountIdTest {

    @Test
    void should_roundTripThroughBase32String() {
        AccountId original = AccountId.generate();

        AccountId parsed = AccountId.fromString(original.toString());

        assertEquals(original, parsed);
    }

    @Test
    void should_generateDistinctIds() {
        assertNotEquals(AccountId.generate(), AccountId.generate());
    }
}
