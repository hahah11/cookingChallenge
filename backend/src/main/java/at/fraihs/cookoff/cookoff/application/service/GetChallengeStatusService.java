package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.GuestSubmissionStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SubmissionStatusRestDto;

import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Which guests have/haven't submitted" — organizer-only progress view per
 * docs/cookingChallenge/first-plan.md Step 3. Tracks the pre-added guest list only, not
 * the two cooks, matching that row's literal wording even though SubmitScoreService also
 * accepts the challenge creator's own submission.
 */
@Service
@RequiredArgsConstructor
public class GetChallengeStatusService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final AccountLookup accountLookup;

    @Transactional(readOnly = true)
    public SubmissionStatusRestDto execute(String challengeIdString) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        Map<AccountId, ScoreSubmission> submissionsByGuest = scoreSubmissionRepository.findByChallengeId(challengeId)
                .stream()
                .collect(Collectors.toMap(ScoreSubmission::getGuestAccountId, submission -> submission));

        var guests = challenge.getGuestAccountIds().stream()
                .map(guestAccountId -> toGuestSubmissionStatus(guestAccountId, submissionsByGuest.get(guestAccountId)))
                .toList();
        long submittedCount = guests.stream().filter(GuestSubmissionStatusRestDto::getSubmitted).count();

        return new SubmissionStatusRestDto(challengeIdString, guests.size(), (int) submittedCount, guests);
    }

    private GuestSubmissionStatusRestDto toGuestSubmissionStatus(AccountId guestAccountId, ScoreSubmission submission) {
        AccountSummary account = accountLookup.getById(guestAccountId);
        GuestSubmissionStatusRestDto status = new GuestSubmissionStatusRestDto(
                guestAccountId.toString(), account.name(), account.email().toString(), submission != null);
        if (submission != null) {
            status.submittedAt(submission.getSubmittedAt().atOffset(ZoneOffset.UTC));
        }
        return status;
    }
}
