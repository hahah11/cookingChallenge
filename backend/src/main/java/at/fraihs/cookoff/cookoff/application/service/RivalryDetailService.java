package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.RivalryNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.RevealResult;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryChallengeSummaryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetailRestDto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Joins the CookRivalry win/loss record (if the pair has ever been revealed) with every
 * challenge between them, revealed or not - the CookRivalry aggregate itself carries no
 * challenge references (see openapi-first-api-plan.md gap 4).
 */
@Service
@RequiredArgsConstructor
public class RivalryDetailService {

    private final CookRivalryRepository cookRivalryRepository;
    private final ChallengeRepository challengeRepository;
    private final AccountLookup accountLookup;

    @Transactional(readOnly = true)
    public RivalryDetailRestDto execute(AccountId requestedCookA, AccountId requestedCookB) {
        AccountId[] ordered = CookRivalry.orderPair(requestedCookA, requestedCookB);
        AccountId cookAId = ordered[0];
        AccountId cookBId = ordered[1];

        List<Challenge> challenges = challengeRepository.findByCookPair(cookAId, cookBId);
        Optional<CookRivalry> rivalry = cookRivalryRepository.findByPair(cookAId, cookBId);
        if (challenges.isEmpty() && rivalry.isEmpty()) {
            throw new RivalryNotFoundException(requestedCookA.toString(), requestedCookB.toString());
        }

        AccountSummary cookA = accountLookup.getById(cookAId);
        AccountSummary cookB = accountLookup.getById(cookBId);
        int cookAWins = rivalry.map(CookRivalry::getCookAWins).orElse(0);
        int cookBWins = rivalry.map(CookRivalry::getCookBWins).orElse(0);
        int draws = rivalry.map(CookRivalry::getDraws).orElse(0);
        int totalChallenges = rivalry.map(CookRivalry::getTotalChallenges).orElse(0);
        String headline = RivalryHeadline.build(cookA.name(), cookB.name(), cookAWins, cookBWins, draws);

        List<RivalryChallengeSummaryRestDto> challengeSummaries = challenges.stream()
                .sorted(Comparator.comparing(Challenge::getDate).reversed())
                .map(this::toGenerated)
                .toList();

        return new RivalryDetailRestDto(cookAId.toString(), cookA.name(), cookBId.toString(), cookB.name(),
                cookAWins, cookBWins, draws, totalChallenges, headline, challengeSummaries);
    }

    private RivalryChallengeSummaryRestDto toGenerated(Challenge challenge) {
        RevealResult result = challenge.getLastRevealResult();
        String overallWinnerAccountId = result == null || result.winnerAccountId() == null
                ? null
                : result.winnerAccountId().toString();
        ChallengeStatusRestDto status = ChallengeStatusRestDto.valueOf(challenge.getStatus().name());
        return new RivalryChallengeSummaryRestDto(challenge.getId().toString(), challenge.getDate(),
                challenge.getTitle(), status, overallWinnerAccountId);
    }
}
