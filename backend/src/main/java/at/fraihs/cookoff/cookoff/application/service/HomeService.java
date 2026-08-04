package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.GuestHome;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Backs GET /api/v1/me/home: a personalized home for a guest or cook, per
 * openapi-first-api-plan.md's reframing. {@code open} holds challenges with a pending
 * action (an unsubmitted score for a guest/creator, or an unpicked color for a cook);
 * {@code past} holds every other challenge the requester participates in - already
 * actioned but still OPEN, or REVEALED.
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional(readOnly = true)
    public GuestHome execute(AccountId accountId) {
        List<ParticipantChallenge> open = new ArrayList<>();
        List<ParticipantChallenge> past = new ArrayList<>();

        for (Challenge challenge : challengeRepository.findByParticipant(accountId)) {
            ScoreSubmission mySubmission = scoreSubmissionRepository
                    .findByChallengeIdAndGuestAccountId(challenge.getId(), accountId)
                    .orElse(null);
            ParticipantChallenge view = ChallengeMapping.toParticipantChallenge(challenge, mySubmission, accountId);
            boolean pendingAction = (view.getCanScore() && !view.getSubmitted()) || view.getCanPickColor();
            if (challenge.getStatus() == ChallengeStatus.OPEN && pendingAction) {
                open.add(view);
            } else {
                past.add(view);
            }
        }

        return new GuestHome(open, past);
    }
}
