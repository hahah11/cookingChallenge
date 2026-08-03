package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Link-token-facing GET .../challenges/{id} — no cook mapping before reveal. */
@Service
@RequiredArgsConstructor
public class GetChallengeForParticipantService {

    private final ChallengeRepository challengeRepository;

    @Transactional(readOnly = true)
    public ChallengeParticipantView execute(String challengeIdString, AccountId requesterAccountId) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isParticipant(requesterAccountId)) {
            throw new NotAParticipantException(requesterAccountId.toString(), challengeIdString);
        }
        return ChallengeParticipantView.from(challenge);
    }
}
