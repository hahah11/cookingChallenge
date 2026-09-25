package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft-deletes a challenge. Deleting a REVEALED challenge publishes ChallengeUnrevealed so the
 * CookRivalry counters it contributed to are reversed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteChallengeService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(String challengeIdString, AccountId organizerAccountId) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Delete rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isOwnedBy(organizerAccountId) && !accountLookup.isAdmin(organizerAccountId)) {
            log.warn("Delete rejected, account {} does not own challenge {}", organizerAccountId, challengeId);
            throw new ForbiddenException("Account is not allowed to manage this challenge: " + organizerAccountId);
        }

        var unrevealed = challenge.delete();
        challengeRepository.save(challenge);
        unrevealed.ifPresent(eventPublisher::publishEvent);
        log.info("Challenge deleted: {}", challengeId);
    }
}
