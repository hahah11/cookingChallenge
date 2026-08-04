package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.shared.web.openapi.model.SubmitScoresRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Scores are editable until the challenge is revealed - see openapi-first-api-plan.md's
 * "submitted scores are editable until reveal" resolution. An existing submission is
 * updated in place (upsert) rather than rejected as a duplicate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitScoreService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional
    public Result execute(String challengeIdString, AccountId requesterAccountId, SubmitScoresRequest request) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (challenge.getStatus() != at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus.OPEN) {
            log.warn("Score submission rejected, challenge not open: {}", challengeId);
            throw new ChallengeNotOpenException(challengeIdString);
        }
        if (!challenge.canScore(requesterAccountId)) {
            log.warn("Score submission rejected, account {} may not score challenge {}",
                    requesterAccountId, challengeId);
            throw new ForbiddenException("Account may not submit scores for this challenge: " + requesterAccountId);
        }

        List<Score> scores = request.getScores().stream().map(SubmitScoreService::toScore).toList();
        Instant now = Instant.now();
        ScoreSubmission existing = scoreSubmissionRepository
                .findByChallengeIdAndGuestAccountId(challengeId, requesterAccountId)
                .orElse(null);

        boolean created = existing == null;
        ScoreSubmission submission;
        if (existing != null) {
            existing.update(scores, now);
            submission = existing;
        } else {
            submission = ScoreSubmission.submit(challengeId, requesterAccountId, scores, now);
        }
        scoreSubmissionRepository.save(submission);
        log.info("Score submission {}: challenge {}, account {}", created ? "recorded" : "updated",
                challengeId, requesterAccountId);

        at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission data =
                new at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission(
                        challengeIdString,
                        requesterAccountId.toString(),
                        submission.getScores().stream().map(ChallengeMapping::toGeneratedScore).toList(),
                        submission.getSubmittedAt().atOffset(ZoneOffset.UTC));
        return new Result(data, created);
    }

    private static Score toScore(at.fraihs.cookoff.shared.web.openapi.model.ScoreEntry entry) {
        return new Score(
                at.fraihs.cookoff.cookoff.domain.model.DishLabel.valueOf(entry.getDishLabel().name()),
                at.fraihs.cookoff.cookoff.domain.model.Category.valueOf(entry.getCategory().name()),
                entry.getPoints());
    }

    public record Result(at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission data, boolean created) {
    }
}
