package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.port.AccessLink;
import at.fraihs.cookoff.auth.application.port.AccessLinkRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues and verifies personalized access-link tokens (docs/cookingChallenge/first-plan.md's
 * "casual access via personalized link" flow). Links are reusable until expiry, not
 * single-use — a participant returns to the same link across multiple open challenges, so
 * invalidating it after the first click would break that "home" flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLinkService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccessLinkRepository accessLinkRepository;

    @Transactional
    public String issue(AccountId accountId, long challengeId, Duration validFor) {
        String token = generateToken();
        Instant now = Instant.now();
        accessLinkRepository.save(new AccessLink(
                TsidSupport.generate(), accountId, challengeId, token, now.plus(validFor), null, now));
        log.info("Access link issued for account {} on challenge {}", accountId, challengeId);
        return token;
    }

    @Transactional(readOnly = true)
    public AccessLink verify(String token) {
        AccessLink accessLink = accessLinkRepository.findByToken(token)
                .orElseThrow(InvalidOrExpiredLinkException::new);
        if (accessLink.isExpired(Instant.now())) {
            throw new InvalidOrExpiredLinkException();
        }
        return accessLink;
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
