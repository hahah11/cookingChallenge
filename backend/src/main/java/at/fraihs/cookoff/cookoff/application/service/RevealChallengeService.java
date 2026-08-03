package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.cookoff.application.dto.ChallengeResultView;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevealChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ResultCalculator resultCalculator = new ResultCalculator();

    @Transactional
    public ChallengeResultView execute(String challengeIdString) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        List<ScoreSubmission> submissions = scoreSubmissionRepository.findByChallengeId(challengeId);
        ChallengeResult result = resultCalculator.calculate(challenge, submissions);

        ChallengeRevealed event = challenge.reveal(result.overallWinnerAccountId());
        challengeRepository.save(challenge);
        eventPublisher.publishEvent(event);
        log.info("Challenge revealed: {}, overall winner: {}", challengeId, result.overallWinnerAccountId());

        return toView(challenge, result);
    }

    private static ChallengeResultView toView(Challenge challenge, ChallengeResult result) {
        Map<String, String> categoryWinners = result.categoryWinners().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), entry -> entry.getValue().name()));
        ChallengeView challengeView = ChallengeView.from(challenge);
        String overallWinnerAccountId = result.overallWinnerAccountId() == null
                ? null
                : result.overallWinnerAccountId().toString();
        return new ChallengeResultView(
                challenge.getId().toString(),
                categoryWinners,
                overallWinnerAccountId,
                challengeView.cookAssignments());
    }
}
