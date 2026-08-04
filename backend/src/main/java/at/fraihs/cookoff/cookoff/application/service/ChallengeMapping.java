package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.CookAssignment;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.RevealResult;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain {@link Challenge} -> generated-OpenAPI-model mapping shared by every use case that
 * returns the organizer-facing {@code Challenge} model or one of its nested pieces. Kept as
 * plain static helpers rather than a MapStruct interface (like auth's AccountModelMapper)
 * because the generated model bundles cross-aggregate computed fields (submittedGuestCount,
 * hasImage) that MapStruct can't derive from the Challenge aggregate alone.
 */
final class ChallengeMapping {

    private ChallengeMapping() {
    }

    static at.fraihs.cookoff.shared.web.openapi.model.Challenge toGenerated(Challenge challenge, int submittedGuestCount) {
        return new at.fraihs.cookoff.shared.web.openapi.model.Challenge(
                challenge.getId().toString(),
                challenge.getDate(),
                challenge.getTitle(),
                challenge.getDishName().toString(),
                at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatus.valueOf(challenge.getStatus().name()),
                cookAssignments(challenge),
                challenge.getGuestAccountIds().stream().map(AccountId::toString).toList(),
                challenge.getCreatedBy().toString(),
                submittedGuestCount,
                challenge.getGuestAccountIds().size(),
                challenge.getImageRef() != null,
                overallWinnerAccountId(challenge));
    }

    static int submittedGuestCount(Challenge challenge, ScoreSubmissionRepository scoreSubmissionRepository) {
        Set<AccountId> guestAccountIds = Set.copyOf(challenge.getGuestAccountIds());
        return (int) scoreSubmissionRepository.findByChallengeId(challenge.getId()).stream()
                .map(ScoreSubmission::getGuestAccountId)
                .filter(guestAccountIds::contains)
                .count();
    }

    static List<at.fraihs.cookoff.shared.web.openapi.model.CookAssignment> cookAssignments(Challenge challenge) {
        return challenge.getCookAssignments().stream()
                .map(assignment -> new at.fraihs.cookoff.shared.web.openapi.model.CookAssignment(
                        assignment.accountId().toString(),
                        at.fraihs.cookoff.shared.web.openapi.model.DishLabel.valueOf(assignment.label().name()),
                        assignment.colorId() == null ? null : assignment.colorId().toString()))
                .toList();
    }

    static String overallWinnerAccountId(Challenge challenge) {
        RevealResult result = challenge.getLastRevealResult();
        return result == null || result.winnerAccountId() == null ? null : result.winnerAccountId().toString();
    }

    static at.fraihs.cookoff.shared.web.openapi.model.Score toGeneratedScore(Score score) {
        return new at.fraihs.cookoff.shared.web.openapi.model.Score(
                at.fraihs.cookoff.shared.web.openapi.model.DishLabel.valueOf(score.dishLabel().name()),
                at.fraihs.cookoff.shared.web.openapi.model.Category.valueOf(score.category().name()),
                score.points());
    }

    static at.fraihs.cookoff.shared.web.openapi.model.MyScoreSubmission toGeneratedMySubmission(ScoreSubmission submission) {
        if (submission == null) {
            return null;
        }
        List<at.fraihs.cookoff.shared.web.openapi.model.Score> scores = submission.getScores().stream()
                .map(ChallengeMapping::toGeneratedScore)
                .toList();
        return new at.fraihs.cookoff.shared.web.openapi.model.MyScoreSubmission(
                scores, submission.getSubmittedAt().atOffset(ZoneOffset.UTC));
    }

    /**
     * Guest/cook-facing view via a link token. {@code accountId} in each cook assignment is
     * hidden until REVEALED (blind scoring); {@code colorId} is always visible once picked,
     * since blind scoring is done by plate color, not by cook identity - see
     * openapi-first-api-plan.md's ParticipantChallenge restructuring note.
     */
    static at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge toParticipantChallenge(
            Challenge challenge, ScoreSubmission mySubmission, AccountId requesterAccountId) {
        boolean revealed = challenge.getStatus() == ChallengeStatus.REVEALED;
        List<at.fraihs.cookoff.shared.web.openapi.model.ParticipantCookAssignment> assignments =
                challenge.getCookAssignments().stream()
                        .map(assignment -> new at.fraihs.cookoff.shared.web.openapi.model.ParticipantCookAssignment(
                                at.fraihs.cookoff.shared.web.openapi.model.DishLabel.valueOf(assignment.label().name()),
                                revealed ? assignment.accountId().toString() : null,
                                assignment.colorId() == null ? null : assignment.colorId().toString()))
                        .toList();

        DishLabel myLabel = challenge.getCookAssignments().stream()
                .filter(assignment -> assignment.accountId().equals(requesterAccountId))
                .map(CookAssignment::label)
                .findFirst()
                .orElse(null);
        at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge.MyCookLabelEnum myCookLabel = myLabel == null
                ? null
                : at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge.MyCookLabelEnum.fromValue(myLabel.name());
        boolean anyColorPicked = challenge.getCookAssignments().stream().anyMatch(CookAssignment::hasColor);
        boolean canPickColor = myCookLabel != null && !anyColorPicked;

        return new at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge(
                challenge.getId().toString(),
                challenge.getDate(),
                challenge.getTitle(),
                challenge.getDishName().toString(),
                at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatus.valueOf(challenge.getStatus().name()),
                Arrays.asList(at.fraihs.cookoff.shared.web.openapi.model.DishLabel.values()),
                Arrays.asList(at.fraihs.cookoff.shared.web.openapi.model.Category.values()),
                assignments,
                challenge.getImageRef() != null,
                mySubmission != null,
                toGeneratedMySubmission(mySubmission),
                myCookLabel,
                challenge.canScore(requesterAccountId),
                canPickColor);
    }

    /**
     * Reads the pair's persisted {@link CookRivalry} fresh from the repository - correct for
     * {@code getChallengeResults}, called strictly after a reveal (and its
     * {@code ChallengeRevealedRivalryUpdater} AFTER_COMMIT listener) has already persisted.
     * {@code revealChallenge} itself must not use this overload - see
     * {@link RevealChallengeService} for why.
     */
    static at.fraihs.cookoff.shared.web.openapi.model.ChallengeResult toGeneratedResult(
            Challenge challenge, at.fraihs.cookoff.cookoff.domain.service.ChallengeResult result,
            CookRivalryRepository cookRivalryRepository, AccountLookup accountLookup) {
        AccountId challengeCookA = challenge.cookAssignmentFor(DishLabel.A).accountId();
        AccountId challengeCookB = challenge.cookAssignmentFor(DishLabel.B).accountId();
        AccountId[] canonical = CookRivalry.orderPair(challengeCookA, challengeCookB);
        CookRivalry rivalry = cookRivalryRepository.findByPair(canonical[0], canonical[1]).orElse(null);
        return toGeneratedResult(challenge, result, rivalry, accountLookup);
    }

    /**
     * Builds the result using an already-resolved {@link CookRivalry} instance instead of
     * looking one up - lets {@link RevealChallengeService} pass an in-memory, not-yet-saved
     * copy that already reflects the reveal just performed (the authoritative persisted
     * update only lands after commit, via {@code ChallengeRevealedRivalryUpdater}), so its own
     * response isn't off-by-one-game.
     */
    static at.fraihs.cookoff.shared.web.openapi.model.ChallengeResult toGeneratedResult(
            Challenge challenge, at.fraihs.cookoff.cookoff.domain.service.ChallengeResult result,
            CookRivalry rivalry, AccountLookup accountLookup) {
        Map<String, String> categoryWinners = Arrays.stream(Category.values())
                .filter(category -> result.categoryWinners().containsKey(category))
                .collect(java.util.stream.Collectors.toMap(Enum::name,
                        category -> result.categoryWinners().get(category).name()));
        List<at.fraihs.cookoff.shared.web.openapi.model.CategoryScoreTotal> categoryTotals =
                Arrays.stream(Category.values())
                        .map(category -> {
                            Map<DishLabel, Integer> byLabel = result.categoryTotals().getOrDefault(category, Map.of());
                            List<at.fraihs.cookoff.shared.web.openapi.model.DishScoreTotal> dishTotals =
                                    Arrays.stream(DishLabel.values())
                                            .map(label -> new at.fraihs.cookoff.shared.web.openapi.model.DishScoreTotal(
                                                    at.fraihs.cookoff.shared.web.openapi.model.DishLabel.valueOf(label.name()),
                                                    byLabel.getOrDefault(label, 0)))
                                            .toList();
                            return new at.fraihs.cookoff.shared.web.openapi.model.CategoryScoreTotal(
                                    at.fraihs.cookoff.shared.web.openapi.model.Category.valueOf(category.name()), dishTotals);
                        })
                        .toList();
        String overallWinnerAccountId = result.overallWinnerAccountId() == null
                ? null : result.overallWinnerAccountId().toString();
        return new at.fraihs.cookoff.shared.web.openapi.model.ChallengeResult(
                challenge.getId().toString(),
                categoryWinners,
                categoryTotals,
                overallWinnerAccountId,
                cookAssignments(challenge),
                rivalrySummary(challenge, rivalry, accountLookup));
    }

    /**
     * {@code CookRivalry}'s own cookA/cookB designation is a canonical id-sort order (see
     * {@link CookRivalry#orderPair}), independent of this specific challenge's DishLabel A/B
     * assignment - the two must be reconciled so the returned summary's cookA/cookB matches
     * the challenge's own labeling, swapping win counts if the orders disagree. {@code rivalry}
     * is null when the pair has never been revealed together before (zeroed record).
     */
    private static at.fraihs.cookoff.shared.web.openapi.model.RivalrySummary rivalrySummary(
            Challenge challenge, CookRivalry rivalry, AccountLookup accountLookup) {
        AccountId challengeCookA = challenge.cookAssignmentFor(DishLabel.A).accountId();
        AccountId challengeCookB = challenge.cookAssignmentFor(DishLabel.B).accountId();
        boolean challengeAIsRivalryA = rivalry == null || challengeCookA.equals(rivalry.getCookAAccountId());
        int rivalryAWins = rivalry == null ? 0 : rivalry.getCookAWins();
        int rivalryBWins = rivalry == null ? 0 : rivalry.getCookBWins();
        int cookAWins = challengeAIsRivalryA ? rivalryAWins : rivalryBWins;
        int cookBWins = challengeAIsRivalryA ? rivalryBWins : rivalryAWins;
        int draws = rivalry == null ? 0 : rivalry.getDraws();
        int totalChallenges = rivalry == null ? 0 : rivalry.getTotalChallenges();

        AccountSummary cookA = accountLookup.getById(challengeCookA);
        AccountSummary cookB = accountLookup.getById(challengeCookB);
        String headline = RivalryHeadline.build(cookA.name(), cookB.name(), cookAWins, cookBWins, draws);
        return new at.fraihs.cookoff.shared.web.openapi.model.RivalrySummary(
                challengeCookA.toString(), challengeCookB.toString(), cookAWins, cookBWins, draws, totalChallenges, headline);
    }
}
