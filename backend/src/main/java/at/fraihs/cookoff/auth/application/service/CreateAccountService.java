package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.dto.CreateAccountCommand;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccountView execute(CreateAccountCommand command) {
        Email email = new Email(command.email());
        if (accountRepository.existsByEmail(email)) {
            log.warn("Account creation rejected, email already exists: {}", command.email());
            throw new AccountAlreadyExistsException(command.email());
        }
        SystemRole[] initialRoles = command.initialRoles() == null
                ? new SystemRole[0]
                : command.initialRoles().toArray(new SystemRole[0]);
        Account account = Account.create(email, command.name(), initialRoles);
        if (command.password() != null && !command.password().isBlank()) {
            account.changePasswordHash(passwordEncoder.encode(command.password()));
        }
        accountRepository.save(account);
        log.info("Account created: {}", account.getId());
        return AccountView.from(account);
    }
}
