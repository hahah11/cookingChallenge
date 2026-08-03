package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeResultView;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.service.ChallengeResult;
import at.fraihs.cookoff.cookoff.domain.service.ResultCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Link-token-facing GET .../results — only once REVEALED (404 before that, per
 * docs/cookingChallenge/first-plan.md Step 3). Category/overall winners aren't persisted
 * separately from the ChallengeRevealed event's one-time CookRivalry update, so this
 * recomputes them from the (now-immutable, since Challenge.reveal() closes scoring)
 * stored submissions via ResultCalculator — deterministic, no need for a results table.
 */
@Service
@RequiredArgsConstructor
public class GetChallengeResultsService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final ResultCalculator resultCalculator = new ResultCalculator();

    @Transactional(readOnly = true)
    public ChallengeResultView execute(String challengeIdString, AccountId requesterAccountId) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isParticipant(requesterAccountId)) {
            throw new NotAParticipantException(requesterAccountId.toString(), challengeIdString);
        }
        if (challenge.getStatus() != ChallengeStatus.REVEALED) {
            throw new ChallengeNotRevealedException(challengeIdString);
        }

        List<ScoreSubmission> submissions = scoreSubmissionRepository.findByChallengeId(challengeId);
        ChallengeResult result = resultCalculator.calculate(challenge, submissions);

        Map<String, String> categoryWinners = result.categoryWinners().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), entry -> entry.getValue().name()));
        String overallWinnerAccountId = result.overallWinnerAccountId() == null
                ? null
                : result.overallWinnerAccountId().toString();
        ChallengeView challengeView = ChallengeView.from(challenge);
        return new ChallengeResultView(challengeIdString, categoryWinners, overallWinnerAccountId,
                challengeView.cookAssignments());
    }
}
