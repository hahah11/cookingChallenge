package at.fraihs.cookoff.auth.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.EnumSet;
import java.util.Set;

@AggregateRoot
public class Account {

    @Identity
    private final AccountId id;
    private final Email email;
    private String name;
    private String passwordHash;
    private final Set<SystemRole> roles;

    private Account(AccountId id, Email email, String name, String passwordHash, Set<SystemRole> roles) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(SystemRole.class) : EnumSet.copyOf(roles);
    }

    /**
     * Most accounts are created by an organizer/admin up front; the one exception is
     * self-registration via a QR registration invite (auth.RegistrationInvites), which also
     * calls this factory with no explicit roles, defaulting to USER.
     */
    public static Account create(Email email, String name, SystemRole... initialRoles) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name must not be blank");
        }
        Set<SystemRole> roles = initialRoles.length == 0
                ? EnumSet.of(SystemRole.USER)
                : EnumSet.copyOf(Set.of(initialRoles));
        return new Account(AccountId.generate(), email, name, null, roles);
    }

    public static Account reconstitute(AccountId id, Email email, String name, String passwordHash,
                                        Set<SystemRole> roles) {
        return new Account(id, email, name, passwordHash, roles);
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

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Account name must not be blank");
        }
        this.name = newName;
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

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<SystemRole> getRoles() {
        return Set.copyOf(roles);
    }
}
