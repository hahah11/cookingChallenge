package at.fraihs.cookoff.auth.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private static final Email EMAIL = new Email("host@example.com");

    @Test
    void should_defaultToUserRole_when_noRolesGiven() {
        Account account = Account.create(EMAIL, "Alex");

        assertTrue(account.hasRole(SystemRole.USER));
        assertEquals(1, account.getRoles().size());
    }

    @Test
    void should_holdMultipleRoles_when_grantingAnother() {
        Account account = Account.create(EMAIL, "Alex", SystemRole.ORGANIZER);

        account.grantRole(SystemRole.USER);

        assertTrue(account.hasRole(SystemRole.ORGANIZER));
        assertTrue(account.hasRole(SystemRole.USER));
        assertTrue(account.canOrganize());
    }

    @Test
    void should_notOrganize_when_onlyUserRole() {
        Account account = Account.create(EMAIL, "Alex", SystemRole.USER);

        assertFalse(account.canOrganize());
    }

    @Test
    void should_throw_when_revokingTheOnlyRemainingRole() {
        Account account = Account.create(EMAIL, "Alex", SystemRole.USER);

        assertThrows(IllegalStateException.class, () -> account.revokeRole(SystemRole.USER));
    }

    @Test
    void should_throw_when_nameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Account.create(EMAIL, "  "));
    }
}
