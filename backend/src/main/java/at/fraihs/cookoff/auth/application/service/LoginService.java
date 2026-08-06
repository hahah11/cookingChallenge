package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidCredentialsException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.shared.web.openapi.model.AuthToken;
import at.fraihs.cookoff.shared.web.openapi.model.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Password login for ORGANIZER/ADMIN accounts, per
 * docs/cookingChallenge/first-plan.md's "Login issues a JWT... used by the Angular app for
 * ORGANIZER/ADMIN" — guests never call this, they use the access-link flow (AccessLinkService)
 * instead. Deliberately does not distinguish "unknown email" from "wrong password" in its
 * exception (InvalidCredentialsException for both) to avoid leaking which emails are registered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    @Value("${app.jwt.expiration:PT12H}")
    private Duration expiration = Duration.ofHours(12);

    @Transactional(readOnly = true)
    public AuthToken execute(LoginRequest request) {
        Account account = accountRepository.findByEmail(new Email(request.getEmail()))
                .orElseThrow(InvalidCredentialsException::new);
        if (account.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        AuthToken token = jwtIssuer.issueUntil(account, Instant.now().plus(expiration));
        log.info("Login succeeded for account {}", account.getId());
        return token;
    }
}
