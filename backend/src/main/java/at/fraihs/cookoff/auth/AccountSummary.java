package at.fraihs.cookoff.auth;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;

import java.util.Locale;

/** {@code locale} is the language notifications to this account should be written in. */
public record AccountSummary(AccountId id, Email email, String name, String firstName, Locale locale) {

    /** English recipient - for callers that don't care about the language. */
    public AccountSummary(AccountId id, Email email, String name, String firstName) {
        this(id, email, name, firstName, Locale.ENGLISH);
    }
}
