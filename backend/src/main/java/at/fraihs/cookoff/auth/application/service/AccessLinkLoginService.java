package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.port.AccessLink;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.shared.web.openapi.model.AccessLinkLoginRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenRestDto;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exchanges a personalized access-link token (AccessLinkService, "casual access via
 * personalized link") for a JWT, so guests authenticate through the same bearer-token
 * mechanism as organizers from that point on — see
 * docs/cookingChallenge/plans/access-link-jwt-unification-plan.md. The link token itself
 * isn't consumed; it stays valid until its own expiry and can be exchanged again.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLinkLoginService {

    private final AccessLinkService accessLinkService;
    private final AccountRepository accountRepository;
    private final JwtIssuer jwtIssuer;

    @Value("${app.jwt.guest-expiration-cap:P1D}")
    private Duration guestExpirationCap = Duration.ofDays(1);

    @Transactional(readOnly = true)
    public AuthTokenRestDto execute(AccessLinkLoginRequestRestDto request) {
        AccessLink accessLink = accessLinkService.verify(request.getToken());
        Account account = accountRepository.findById(accessLink.accountId())
                .orElseThrow(InvalidOrExpiredLinkException::new);

        Instant cap = Instant.now().plus(guestExpirationCap);
        Instant expiresAt = accessLink.expiresAt().isBefore(cap) ? accessLink.expiresAt() : cap;

        AuthTokenRestDto token = jwtIssuer.issueUntil(account, expiresAt);
        log.info("Access-link login succeeded for account {}", account.getId());
        return token;
    }
}
