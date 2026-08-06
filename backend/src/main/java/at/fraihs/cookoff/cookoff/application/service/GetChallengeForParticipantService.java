package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.mapper.ChallengeModelMapper;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Link-token-facing GET .../challenges/{id} — no cook mapping before reveal. */
@Service
@RequiredArgsConstructor
public class GetChallengeForParticipantService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional(readOnly = true)
    public ParticipantChallengeRestDto execute(
            String challengeIdString, AccountId requesterAccountId) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isParticipant(requesterAccountId)) {
            throw new NotAParticipantException(requesterAccountId.toString(), challengeIdString);
        }
        ScoreSubmission mySubmission = scoreSubmissionRepository
                .findByChallengeIdAndGuestAccountId(challengeId, requesterAccountId)
                .orElse(null);
        return ChallengeModelMapper.toParticipantChallenge(challenge, mySubmission, requesterAccountId);
    }
}
