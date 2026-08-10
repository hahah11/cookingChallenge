package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountLookupService implements AccountLookup {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountSummary getById(AccountId id) {
        Account account = findOrThrow(id);
        return new AccountSummary(account.getId(), account.getEmail(), account.getName(), account.getFirstName());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canOrganize(AccountId id) {
        return findOrThrow(id).canOrganize();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(AccountId id) {
        return findOrThrow(id).hasRole(SystemRole.ADMIN);
    }

    private Account findOrThrow(AccountId id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
    }
}
