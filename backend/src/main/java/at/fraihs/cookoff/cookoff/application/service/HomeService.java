package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.mapper.ChallengeModelMapper;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.GuestHomeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs GET /api/v1/me/home: a personalized home for a guest or cook, per
 * openapi-first-api-plan.md's reframing. {@code open} holds every OPEN challenge the
 * requester participates in, whether or not there's still a pending action (an
 * already-submitted score or already-picked color stays visible and editable there
 * until reveal); {@code past} holds every REVEALED challenge.
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final AccountLookup accountLookup;

    @Transactional(readOnly = true)
    public GuestHomeRestDto execute(AccountId accountId) {
        List<ParticipantChallengeRestDto> open = new ArrayList<>();
        List<ParticipantChallengeRestDto> past = new ArrayList<>();

        for (Challenge challenge : challengeRepository.findByParticipant(accountId)) {
            ScoreSubmission mySubmission = scoreSubmissionRepository
                    .findByChallengeIdAndGuestAccountId(challenge.getId(), accountId)
                    .orElse(null);
            ParticipantChallengeRestDto view =
                    ChallengeModelMapper.toParticipantChallenge(challenge, mySubmission, accountId, accountLookup);
            if (challenge.getStatus() == ChallengeStatus.OPEN) {
                open.add(view);
            } else {
                past.add(view);
            }
        }

        String displayName = accountLookup.getById(accountId).firstName();
        return new GuestHomeRestDto(displayName, open, past);
    }
}
