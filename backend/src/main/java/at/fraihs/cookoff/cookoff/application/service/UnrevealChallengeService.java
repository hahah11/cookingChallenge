package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.UnrevealChallengeCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeUnrevealed;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnrevealChallengeService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(UnrevealChallengeCommand command) {
        AccountId organizerId = AccountId.fromString(command.organizerAccountId());
        if (!accountLookup.canOrganize(organizerId)) {
            log.warn("Unreveal rejected, account cannot organize: {}", organizerId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + command.organizerAccountId());
        }

        ChallengeId challengeId = ChallengeId.fromString(command.challengeId());
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(command.challengeId()));

        ChallengeUnrevealed event = challenge.unreveal();
        challengeRepository.save(challenge);
        eventPublisher.publishEvent(event);
        log.info("Challenge unrevealed: {}", challengeId);
    }
}
