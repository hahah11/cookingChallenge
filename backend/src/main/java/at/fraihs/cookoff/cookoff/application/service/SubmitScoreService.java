package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ScoreInput;
import at.fraihs.cookoff.cookoff.application.dto.SubmitScoreCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.DuplicateSubmissionException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.repository.ScoreSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitScoreService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional
    public void execute(SubmitScoreCommand command) {
        ChallengeId challengeId = ChallengeId.fromString(command.challengeId());
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(command.challengeId()));
        if (challenge.getStatus() != ChallengeStatus.OPEN) {
            log.warn("Score submission rejected, challenge not open: {}", challengeId);
            throw new ChallengeNotOpenException(command.challengeId());
        }

        AccountId guestAccountId = AccountId.fromString(command.guestAccountId());
        boolean isCook = challenge.getCookAssignments().stream()
                .anyMatch(assignment -> assignment.accountId().equals(guestAccountId));
        if (!challenge.isGuest(guestAccountId) && !isCook) {
            log.warn("Score submission rejected, account {} is not a participant of challenge {}",
                    guestAccountId, challengeId);
            throw new NotAParticipantException(command.guestAccountId(), command.challengeId());
        }

        if (scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(challengeId, guestAccountId)) {
            log.warn("Score submission rejected, account {} already submitted for challenge {}",
                    guestAccountId, challengeId);
            throw new DuplicateSubmissionException(command.guestAccountId(), command.challengeId());
        }

        List<Score> scores = command.scores().stream()
                .map(SubmitScoreService::toScore)
                .toList();
        ScoreSubmission submission = ScoreSubmission.submit(challengeId, guestAccountId, scores, Instant.now());
        scoreSubmissionRepository.save(submission);
        log.info("Score submission recorded: challenge {}, account {}", challengeId, guestAccountId);
    }

    private static Score toScore(ScoreInput input) {
        return new Score(DishLabel.valueOf(input.dishLabel()), Category.valueOf(input.category()), input.points());
    }
}
