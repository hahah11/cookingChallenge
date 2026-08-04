package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.service.ChallengeResult;
import at.fraihs.cookoff.cookoff.domain.service.ResultCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevealChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final CookRivalryRepository cookRivalryRepository;
    private final AccountLookup accountLookup;
    private final ApplicationEventPublisher eventPublisher;
    private final ResultCalculator resultCalculator = new ResultCalculator();

    @Transactional
    public at.fraihs.cookoff.shared.web.openapi.model.ChallengeResult execute(String challengeIdString) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        List<ScoreSubmission> submissions = scoreSubmissionRepository.findByChallengeId(challengeId);
        ChallengeResult result = resultCalculator.calculate(challenge, submissions);

        ChallengeRevealed event = challenge.reveal(result.overallWinnerAccountId());
        challengeRepository.save(challenge);
        eventPublisher.publishEvent(event);
        log.info("Challenge revealed: {}, overall winner: {}", challengeId, result.overallWinnerAccountId());

        // ChallengeRevealedRivalryUpdater persists CookRivalry only AFTER_COMMIT, so a plain
        // repository read here would return this reveal's PRE-update stats. Project the same
        // update in memory (without saving - the listener remains the sole writer) so this
        // response's rivalry summary already reflects the reveal just performed.
        CookRivalry rivalry = cookRivalryRepository.findByPair(event.cookAAccountId(), event.cookBAccountId())
                .orElseGet(() -> CookRivalry.start(event.cookAAccountId(), event.cookBAccountId()));
        rivalry.recordResult(result.overallWinnerAccountId());

        return ChallengeMapping.toGeneratedResult(challenge, result, rivalry, accountLookup);
    }
}
