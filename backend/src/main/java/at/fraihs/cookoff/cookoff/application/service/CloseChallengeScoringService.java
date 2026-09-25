package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.mapper.ChallengeModelMapper;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Freezes scoring (OPEN to CLOSED) ahead of the reveal. No event and no mail: nothing reacts to this transition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloseChallengeScoringService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional
    public ChallengeRestDto execute(
            String challengeIdString, AccountId organizerAccountId) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Close scoring rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isOwnedBy(organizerAccountId) && !accountLookup.isAdmin(organizerAccountId)) {
            log.warn("Close scoring rejected, account {} does not own challenge {}", organizerAccountId, challengeId);
            throw new ForbiddenException("Account is not allowed to manage this challenge: " + organizerAccountId);
        }

        challenge.closeScoring();
        challengeRepository.save(challenge);
        log.info("Challenge scoring closed: {}", challengeId);
        return ChallengeModelMapper.toGenerated(
                challenge, ChallengeModelMapper.submittedGuestCount(challenge, scoreSubmissionRepository),
                accountLookup);
    }
}
