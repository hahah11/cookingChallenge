package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backs GET /api/v1/me/home: the link-token-authenticated account's open scoring
 * requests across challenges, per docs/cookingChallenge/first-plan.md's "home" flow —
 * open challenges where the account participates and hasn't submitted yet.
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional(readOnly = true)
    public List<ChallengeParticipantView> execute(AccountId accountId) {
        return challengeRepository.findOpenByParticipant(accountId).stream()
                .filter(challenge -> !scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(
                        challenge.getId(), accountId))
                .map(ChallengeParticipantView::from)
                .toList();
    }
}
