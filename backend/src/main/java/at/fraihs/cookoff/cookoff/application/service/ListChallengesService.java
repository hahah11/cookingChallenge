package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.shared.web.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organizer-facing history list — always includes the cook↔label mapping. */
@Service
@RequiredArgsConstructor
public class ListChallengesService {

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional(readOnly = true)
    public PagedResult<at.fraihs.cookoff.shared.web.openapi.model.Challenge> execute(int page, int size) {
        Page<at.fraihs.cookoff.shared.web.openapi.model.Challenge> mapped = challengeRepository
                .findAll(PageRequest.of(page, size))
                .map(challenge -> ChallengeMapping.toGenerated(
                        challenge, ChallengeMapping.submittedGuestCount(challenge, scoreSubmissionRepository)));
        return PagedResult.of(mapped);
    }
}
