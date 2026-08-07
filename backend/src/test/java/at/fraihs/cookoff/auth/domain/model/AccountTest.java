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
        Account account = Account.create(EMAIL, "Alex", "Cook");

        assertTrue(account.hasRole(SystemRole.USER));
        assertEquals(1, account.getRoles().size());
    }

    @Test
    void should_holdMultipleRoles_when_grantingAnother() {
        Account account = Account.create(EMAIL, "Alex", "Cook", SystemRole.ORGANIZER);

        account.grantRole(SystemRole.USER);

        assertTrue(account.hasRole(SystemRole.ORGANIZER));
        assertTrue(account.hasRole(SystemRole.USER));
        assertTrue(account.canOrganize());
    }

    @Test
    void should_notOrganize_when_onlyUserRole() {
        Account account = Account.create(EMAIL, "Alex", "Cook", SystemRole.USER);

        assertFalse(account.canOrganize());
    }

    @Test
    void should_throw_when_revokingTheOnlyRemainingRole() {
        Account account = Account.create(EMAIL, "Alex", "Cook", SystemRole.USER);

        assertThrows(IllegalStateException.class, () -> account.revokeRole(SystemRole.USER));
    }

    @Test
    void should_throw_when_firstNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Account.create(EMAIL, "  ", "Cook"));
    }

    @Test
    void should_throw_when_lastNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Account.create(EMAIL, "Alex", "  "));
    }

    @Test
    void should_deriveFullName_when_gettingName() {
        Account account = Account.create(EMAIL, "Alex", "Cook");

        assertEquals("Alex Cook", account.getName());
    }

    @Test
    void should_replaceEmail_when_changingEmail() {
        Account account = Account.create(EMAIL, "Alex", "Cook");

        account.changeEmail(new Email("newmail@example.com"));

        assertEquals(new Email("newmail@example.com"), account.getEmail());
    }

    @Test
    void should_throw_when_changingEmailToNull() {
        Account account = Account.create(EMAIL, "Alex", "Cook");

        assertThrows(IllegalArgumentException.class, () -> account.changeEmail(null));
    }
}
