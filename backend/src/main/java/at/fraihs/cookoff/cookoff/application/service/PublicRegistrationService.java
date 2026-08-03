package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.RegistrationInvites;
import at.fraihs.cookoff.auth.RegistrationResult;
import at.fraihs.cookoff.cookoff.application.dto.PublicRegistrationResult;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Public, unauthenticated self-registration via a QR-scanned invite token
 * (frontend-prd.md's self-registration flow). If the challenge is still OPEN, the new
 * account joins it as a guest; if it closed between QR generation and scan, the account is
 * still created — it shouldn't be left half-registered — just not added as a guest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicRegistrationService {

    private final RegistrationInvites registrationInvites;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public PublicRegistrationResult execute(String token, String firstName, String lastName, String email) {
        RegistrationResult result = registrationInvites.register(token, firstName, lastName, email);

        ChallengeId challengeId = new ChallengeId(result.challengeId());
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeId.toString()));

        boolean joined = challenge.getStatus() == ChallengeStatus.OPEN;
        if (joined) {
            challenge.editParticipants(null, null, List.of(result.accountId()), List.of());
            challengeRepository.save(challenge);
        }

        log.info("Account {} self-registered for challenge {} (joined={})", result.accountId(), challengeId, joined);
        return new PublicRegistrationResult(result.accountId(), challengeId, joined);
    }
}
