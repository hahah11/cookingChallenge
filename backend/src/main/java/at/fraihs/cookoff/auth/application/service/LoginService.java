package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.AuthTokenView;
import at.fraihs.cookoff.auth.application.dto.LoginCommand;
import at.fraihs.cookoff.auth.application.exception.InvalidCredentialsException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
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
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer:cookoff}")
    private String issuer = "cookoff";

    @Value("${app.jwt.expiration:PT12H}")
    private Duration expiration = Duration.ofHours(12);

    @Transactional(readOnly = true)
    public AuthTokenView execute(LoginCommand command) {
        Account account = accountRepository.findByEmail(new Email(command.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (account.getPasswordHash() == null
                || !passwordEncoder.matches(command.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(account.getId().toString())
                .claim("roles", account.getRoles().stream().map(Enum::name).toList())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.info("Login succeeded for account {}", account.getId());
        return new AuthTokenView(token, expiresAt);
    }
}
