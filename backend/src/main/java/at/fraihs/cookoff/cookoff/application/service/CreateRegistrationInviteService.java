package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.RegistrationInvites;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.shared.web.openapi.model.RegistrationInvite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;

/**
 * Organizer action generating a QR registration invite for walk-in self-registration
 * (frontend-prd.md's self-registration flow). Reuses the same 30-day link validity as
 * SendChallengeInvitationsService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateRegistrationInviteService {

    private static final Duration INVITE_VALIDITY = Duration.ofDays(30);

    private final ChallengeRepository challengeRepository;
    private final AccountLookup accountLookup;
    private final RegistrationInvites registrationInvites;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public RegistrationInvite execute(String challengeIdString, AccountId organizerAccountId) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Registration invite rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (challenge.getStatus() != ChallengeStatus.OPEN) {
            throw new ChallengeNotOpenException(challengeIdString);
        }

        String token = registrationInvites.issue(organizerAccountId, challengeId.value(), INVITE_VALIDITY);
        log.info("Registration invite issued for challenge {} by {}", challengeId, organizerAccountId);
        return new RegistrationInvite(token, URI.create(frontendBaseUrl + "/register?token=" + token));
    }
}
