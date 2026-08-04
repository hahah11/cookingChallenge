package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountModelMapper accountModelMapper;

    @Transactional
    public at.fraihs.cookoff.shared.web.openapi.model.Account execute(CreateAccountRequest request) {
        Email email = new Email(request.getEmail());
        if (accountRepository.existsByEmail(email)) {
            log.warn("Account creation rejected, email already exists: {}", request.getEmail());
            throw new AccountAlreadyExistsException(request.getEmail());
        }
        SystemRole[] initialRoles = toDomainRoles(request);
        at.fraihs.cookoff.auth.domain.model.Account account =
                at.fraihs.cookoff.auth.domain.model.Account.create(email, request.getName(), initialRoles);
        applyPassword(request.getPassword(), account);
        accountRepository.save(account);
        log.info("Account created: {}", account.getId());
        return accountModelMapper.toGenerated(account);
    }

    private SystemRole[] toDomainRoles(CreateAccountRequest request) {
        if (request.getRoles() == null) {
            return new SystemRole[0];
        }
        return request.getRoles().stream()
                .map(role -> SystemRole.valueOf(role.name()))
                .toArray(SystemRole[]::new);
    }

    private void applyPassword(JsonNullable<String> password, at.fraihs.cookoff.auth.domain.model.Account account) {
        if (password == null || !password.isPresent()) {
            return;
        }
        String value = password.get();
        if (value != null && !value.isBlank()) {
            account.changePasswordHash(passwordEncoder.encode(value));
        }
    }
}
