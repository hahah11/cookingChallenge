package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.mapper.ChallengeModelMapper;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeResult;
import at.fraihs.cookoff.cookoff.domain.service.ResultCalculator;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultRestDto;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reachable by either the organizer (bearer token) or a participant (access-link token) -
 * only once REVEALED (409 before that, per docs/cookingChallenge/first-plan.md Step 3).
 * Category/overall winners aren't persisted separately from the ChallengeRevealed event's
 * one-time CookRivalry update, so this recomputes them from the (now-immutable, since
 * Challenge.reveal() closes scoring) stored submissions via ResultCalculator - deterministic,
 * no need for a results table.
 */
@Service
@RequiredArgsConstructor
public class GetChallengeResultsService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final CookRivalryRepository cookRivalryRepository;
    private final AccountLookup accountLookup;
    private final ResultCalculator resultCalculator = new ResultCalculator();

    @Transactional(readOnly = true)
    public ChallengeResultRestDto execute(
            String challengeIdString, AccountId requesterAccountId) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isParticipant(requesterAccountId) && !accountLookup.canOrganize(requesterAccountId)) {
            throw new NotAParticipantException(requesterAccountId.toString(), challengeIdString);
        }
        if (challenge.getStatus() != ChallengeStatus.REVEALED) {
            throw new ChallengeNotRevealedException(challengeIdString);
        }

        List<ScoreSubmission> submissions = scoreSubmissionRepository.findByChallengeId(challengeId);
        ChallengeResult result = resultCalculator.calculate(challenge, submissions);
        return ChallengeModelMapper.toGeneratedResult(challenge, result, cookRivalryRepository, accountLookup);
    }
}
