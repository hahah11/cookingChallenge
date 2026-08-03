package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.RegistrationInvites;
import at.fraihs.cookoff.auth.RegistrationResult;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationInvitesService implements RegistrationInvites {

    private final RegistrationInviteService registrationInviteService;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public String issue(AccountId issuedByAccountId, long challengeId, Duration validFor) {
        return registrationInviteService.issue(issuedByAccountId, challengeId, validFor);
    }

    @Override
    @Transactional
    public RegistrationResult register(String token, String firstName, String lastName, String email) {
        long challengeId = registrationInviteService.verify(token);

        Email accountEmail = new Email(email);
        if (accountRepository.existsByEmail(accountEmail)) {
            log.warn("Self-registration rejected, email already exists: {}", email);
            throw new AccountAlreadyExistsException(email);
        }

        Account account = Account.create(accountEmail, (firstName + " " + lastName).trim());
        accountRepository.save(account);
        log.info("Account self-registered via QR invite: {} for challenge {}", account.getId(), challengeId);
        return new RegistrationResult(account.getId(), challengeId);
    }
}
