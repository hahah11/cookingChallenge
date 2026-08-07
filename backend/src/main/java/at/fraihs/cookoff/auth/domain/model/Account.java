package at.fraihs.cookoff.auth.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.EnumSet;
import java.util.Set;

@AggregateRoot
public class Account {

    @Identity
    private final AccountId id;
    private Email email;
    private String firstName;
    private String lastName;
    private String passwordHash;
    private final Set<SystemRole> roles;

    private Account(AccountId id, Email email, String firstName, String lastName, String passwordHash,
                     Set<SystemRole> roles) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(SystemRole.class) : EnumSet.copyOf(roles);
    }

    /**
     * Most accounts are created by an organizer/admin up front; the one exception is
     * self-registration via a QR registration invite (auth.RegistrationInvites), which also
     * calls this factory with no explicit roles, defaulting to USER.
     */
    public static Account create(Email email, String firstName, String lastName, SystemRole... initialRoles) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Account first name must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Account last name must not be blank");
        }
        Set<SystemRole> roles = initialRoles.length == 0
                ? EnumSet.of(SystemRole.USER)
                : EnumSet.copyOf(Set.of(initialRoles));
        return new Account(AccountId.generate(), email, firstName, lastName, null, roles);
    }

    public static Account reconstitute(AccountId id, Email email, String firstName, String lastName,
                                        String passwordHash, Set<SystemRole> roles) {
        return new Account(id, email, firstName, lastName, passwordHash, roles);
    }

    public void grantRole(SystemRole role) {
        roles.add(role);
    }

    public void revokeRole(SystemRole role) {
        if (roles.size() == 1 && roles.contains(role)) {
            throw new IllegalStateException("Account must retain at least one role");
        }
        roles.remove(role);
    }

    public boolean hasRole(SystemRole role) {
        return roles.contains(role);
    }

    /** ORGANIZER and ADMIN can create/manage challenges (see docs/cookingChallenge/first-plan.md Step 2). */
    public boolean canOrganize() {
        return hasRole(SystemRole.ORGANIZER) || hasRole(SystemRole.ADMIN);
    }

    public void renameFirst(String newFirstName) {
        if (newFirstName == null || newFirstName.isBlank()) {
            throw new IllegalArgumentException("Account first name must not be blank");
        }
        this.firstName = newFirstName;
    }

    public void renameLast(String newLastName) {
        if (newLastName == null || newLastName.isBlank()) {
            throw new IllegalArgumentException("Account last name must not be blank");
        }
        this.lastName = newLastName;
    }

    /** Email uniqueness is a repository-level concern (see AccountRepository#existsByEmail), not checked here. */
    public void changeEmail(Email newEmail) {
        if (newEmail == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        this.email = newEmail;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public AccountId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<SystemRole> getRoles() {
        return Set.copyOf(roles);
    }
}
