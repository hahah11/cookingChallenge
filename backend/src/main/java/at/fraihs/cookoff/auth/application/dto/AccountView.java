package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.SystemRole;

import java.util.Set;

public record AccountView(String id, String email, String name, Set<SystemRole> roles) {

    public static AccountView from(Account account) {
        return new AccountView(
                account.getId().toString(),
                account.getEmail().toString(),
                account.getName(),
                account.getRoles());
    }
}
