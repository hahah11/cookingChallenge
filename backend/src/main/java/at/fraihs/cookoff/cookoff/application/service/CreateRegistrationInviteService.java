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
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public String execute(AccountId organizerAccountId, ChallengeId challengeId) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Registration invite rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeId.toString()));
        if (challenge.getStatus() != ChallengeStatus.OPEN) {
            throw new ChallengeNotOpenException(challengeId.toString());
        }

        String token = registrationInvites.issue(organizerAccountId, challengeId.value(), INVITE_VALIDITY);
        log.info("Registration invite issued for challenge {} by {}", challengeId, organizerAccountId);
        return token;
    }
}
