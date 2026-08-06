package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.dto.RegistrationInvite;
import at.fraihs.cookoff.auth.application.port.RegistrationInviteRepository;
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
 * Issues and verifies self-registration QR tokens (frontend-prd.md's self-registration
 * flow). Token generation mirrors AccessLinkService exactly (SecureRandom, 256-bit,
 * Base64url — not TSID, per docs/backend/03-code-style.md's ID-generation note), but the
 * token here identifies "which challenge to join", not "who the caller is" — the account it
 * eventually creates doesn't exist yet at issue time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationInviteService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RegistrationInviteRepository registrationInviteRepository;

    @Transactional
    public String issue(AccountId issuedByAccountId, long challengeId, Duration validFor) {
        String token = generateToken();
        Instant now = Instant.now();
        registrationInviteRepository.save(new RegistrationInvite(
                TsidSupport.generate(), issuedByAccountId, challengeId, token, now.plus(validFor)));
        log.info("Registration invite issued by account {} for challenge {}", issuedByAccountId, challengeId);
        return token;
    }

    @Transactional(readOnly = true)
    public long verify(String token) {
        RegistrationInvite invite = registrationInviteRepository.findByToken(token)
                .orElseThrow(InvalidOrExpiredLinkException::new);
        if (invite.isExpired(Instant.now())) {
            throw new InvalidOrExpiredLinkException();
        }
        return invite.challengeId();
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
