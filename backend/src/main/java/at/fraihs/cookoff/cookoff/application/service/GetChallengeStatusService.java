package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.SubmissionStatusView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.repository.ScoreSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * "Which guests have/haven't submitted" — organizer-only progress view per
 * docs/cookingChallenge/first-plan.md Step 3. Tracks the pre-added guest list only, not
 * the two cooks, matching that row's literal wording even though SubmitScoreService also
 * accepts a cook's own submission.
 */
@Service
@RequiredArgsConstructor
public class GetChallengeStatusService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional(readOnly = true)
    public SubmissionStatusView execute(String challengeIdString) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        Set<AccountId> guestAccountIds = Set.copyOf(challenge.getGuestAccountIds());
        List<String> submittedGuestAccountIds = scoreSubmissionRepository.findByChallengeId(challengeId).stream()
                .map(ScoreSubmission::getGuestAccountId)
                .filter(guestAccountIds::contains)
                .map(AccountId::toString)
                .toList();

        return new SubmissionStatusView(
                challengeIdString,
                guestAccountIds.size(),
                submittedGuestAccountIds.size(),
                submittedGuestAccountIds);
    }
}
