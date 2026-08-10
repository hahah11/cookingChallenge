package at.fraihs.cookoff.auth;

import at.fraihs.cookoff.auth.domain.model.AccountId;

/**
 * Public contract for other modules that need account data. Keeps the {@code Account}
 * aggregate and {@code AccountRepository} internal to the auth module (see
 * docs/backend/02-ddd-modulith.md's Module Contracts pattern).
 */
public interface AccountLookup {

    /** @throws at.fraihs.cookoff.auth.application.exception.AccountNotFoundException if no account has this id */
    AccountSummary getById(AccountId id);

    /** @throws at.fraihs.cookoff.auth.application.exception.AccountNotFoundException if no account has this id */
    boolean canOrganize(AccountId id);

    /** @throws at.fraihs.cookoff.auth.application.exception.AccountNotFoundException if no account has this id */
    boolean isAdmin(AccountId id);
}
