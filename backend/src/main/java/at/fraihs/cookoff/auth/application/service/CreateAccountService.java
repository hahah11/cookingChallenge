package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.dto.CreateAccountCommand;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountView execute(CreateAccountCommand command) {
        Email email = new Email(command.email());
        if (accountRepository.existsByEmail(email)) {
            throw new AccountAlreadyExistsException(command.email());
        }
        SystemRole[] initialRoles = command.initialRoles() == null
                ? new SystemRole[0]
                : command.initialRoles().toArray(new SystemRole[0]);
        Account account = Account.create(email, command.name(), initialRoles);
        accountRepository.save(account);
        return AccountView.from(account);
    }
}
