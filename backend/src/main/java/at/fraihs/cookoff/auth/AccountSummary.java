package at.fraihs.cookoff.auth;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;

public record AccountSummary(AccountId id, Email email, String name) {
}
