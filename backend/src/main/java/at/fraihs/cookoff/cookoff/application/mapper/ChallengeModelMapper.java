package at.fraihs.cookoff.cookoff.application.mapper;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.application.service.RevealChallengeService;
import at.fraihs.cookoff.cookoff.application.service.RivalryHeadline;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeResult;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.CookAssignment;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.RevealResult;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.CategoryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CategoryScoreTotalRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CookAssignmentRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.DishLabelRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.DishScoreTotalRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.MyScoreSubmissionRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantCookAssignmentRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalrySummaryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreRestDto;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain {@link Challenge} -> generated-OpenAPI-model mapping shared by every use case that
 * returns the organizer-facing {@code ChallengeRestDto} model or one of its nested pieces. Kept
 * as plain static helpers rather than a MapStruct interface (like
 * {@code auth.application.mapper.AccountModelMapper}) because the generated model bundles
 * cross-aggregate computed fields (submittedGuestCount, hasImage) that MapStruct can't derive
 * from the Challenge aggregate alone. See docs/backend/03-code-style.md's Mapper Usage section.
 */
public final class ChallengeModelMapper {

    private ChallengeModelMapper() {
    }

    public static ChallengeRestDto toGenerated(
            Challenge challenge, int submittedGuestCount, AccountLookup accountLookup) {
        return new ChallengeRestDto(
                challenge.getId().toString(),
                challenge.getDate(),
                challenge.getTitle(),
                challenge.getDishName().toString(),
                ChallengeStatusRestDto.valueOf(challenge.getStatus().name()),
                cookAssignments(challenge, accountLookup),
                challenge.getGuestAccountIds().stream().map(AccountId::toString).toList(),
                challenge.getCreatedBy().toString(),
                submittedGuestCount,
                challenge.getGuestAccountIds().size(),
                challenge.getImageRef() != null,
                overallWinnerAccountId(challenge));
    }

    public static int submittedGuestCount(Challenge challenge, ScoreSubmissionRepository scoreSubmissionRepository) {
        Set<AccountId> guestAccountIds = Set.copyOf(challenge.getGuestAccountIds());
        return (int) scoreSubmissionRepository.findByChallengeId(challenge.getId()).stream()
                .map(ScoreSubmission::getGuestAccountId)
                .filter(guestAccountIds::contains)
                .count();
    }

    public static List<CookAssignmentRestDto> cookAssignments(Challenge challenge, AccountLookup accountLookup) {
        return challenge.getCookAssignments().stream()
                .map(assignment -> new CookAssignmentRestDto(
                        assignment.accountId().toString(),
                        accountLookup.getById(assignment.accountId()).name(),
                        DishLabelRestDto.valueOf(assignment.label().name()),
                        assignment.colorId() == null ? null : assignment.colorId().toString()))
                .toList();
    }

    public static String overallWinnerAccountId(Challenge challenge) {
        RevealResult result = challenge.getLastRevealResult();
        return result == null || result.winnerAccountId() == null ? null : result.winnerAccountId().toString();
    }

    public static ScoreRestDto toGeneratedScore(Score score) {
        return new ScoreRestDto(
                DishLabelRestDto.valueOf(score.dishLabel().name()),
                CategoryRestDto.valueOf(score.category().name()),
                score.points());
    }

    public static MyScoreSubmissionRestDto toGeneratedMySubmission(ScoreSubmission submission) {
        if (submission == null) {
            return null;
        }
        List<ScoreRestDto> scores = submission.getScores().stream()
                .map(ChallengeModelMapper::toGeneratedScore)
                .toList();
        return new MyScoreSubmissionRestDto(
                scores, submission.getSubmittedAt().atOffset(ZoneOffset.UTC));
    }

    /**
     * Guest/cook-facing view via a link token. {@code accountId} in each cook assignment is
     * hidden until REVEALED (blind scoring); {@code colorId} is always visible once picked,
     * since blind scoring is done by plate color, not by cook identity - see
     * openapi-first-api-plan.md's ParticipantChallenge restructuring note.
     */
    public static ParticipantChallengeRestDto toParticipantChallenge(
            Challenge challenge, ScoreSubmission mySubmission, AccountId requesterAccountId,
            AccountLookup accountLookup) {
        boolean revealed = challenge.getStatus() == ChallengeStatus.REVEALED;
        List<ParticipantCookAssignmentRestDto> assignments =
                challenge.getCookAssignments().stream()
                        .map(assignment -> new ParticipantCookAssignmentRestDto(
                                DishLabelRestDto.valueOf(assignment.label().name()),
                                revealed ? assignment.accountId().toString() : null,
                                revealed ? accountLookup.getById(assignment.accountId()).name() : null,
                                assignment.colorId() == null ? null : assignment.colorId().toString()))
                        .toList();

        DishLabel myLabel = challenge.getCookAssignments().stream()
                .filter(assignment -> assignment.accountId().equals(requesterAccountId))
                .map(CookAssignment::label)
                .findFirst()
                .orElse(null);
        ParticipantChallengeRestDto.MyCookLabelEnum myCookLabel = myLabel == null
                ? null
                : ParticipantChallengeRestDto.MyCookLabelEnum.fromValue(myLabel.name());
        boolean anyColorPicked = challenge.getCookAssignments().stream().anyMatch(CookAssignment::hasColor);
        boolean canPickColor = myCookLabel != null && !anyColorPicked;

        return new ParticipantChallengeRestDto(
                challenge.getId().toString(),
                challenge.getDate(),
                challenge.getTitle(),
                challenge.getDishName().toString(),
                ChallengeStatusRestDto.valueOf(challenge.getStatus().name()),
                Arrays.asList(DishLabelRestDto.values()),
                Arrays.asList(CategoryRestDto.values()),
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
    public static ChallengeResultRestDto toGeneratedResult(
            Challenge challenge, ChallengeResult result,
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
    public static ChallengeResultRestDto toGeneratedResult(
            Challenge challenge, ChallengeResult result,
            CookRivalry rivalry, AccountLookup accountLookup) {
        Map<String, String> categoryWinners = Arrays.stream(Category.values())
                .filter(category -> result.categoryWinners().containsKey(category))
                .collect(java.util.stream.Collectors.toMap(Enum::name,
                        category -> result.categoryWinners().get(category).name()));
        List<CategoryScoreTotalRestDto> categoryTotals =
                Arrays.stream(Category.values())
                        .map(category -> {
                            Map<DishLabel, Integer> byLabel = result.categoryTotals().getOrDefault(category, Map.of());
                            List<DishScoreTotalRestDto> dishTotals =
                                    Arrays.stream(DishLabel.values())
                                            .map(label -> new DishScoreTotalRestDto(
                                                    DishLabelRestDto.valueOf(label.name()),
                                                    byLabel.getOrDefault(label, 0)))
                                            .toList();
                            return new CategoryScoreTotalRestDto(
                                    CategoryRestDto.valueOf(category.name()), dishTotals);
                        })
                        .toList();
        String overallWinnerAccountId = result.overallWinnerAccountId() == null
                ? null : result.overallWinnerAccountId().toString();
        return new ChallengeResultRestDto(
                challenge.getId().toString(),
                categoryWinners,
                categoryTotals,
                overallWinnerAccountId,
                cookAssignments(challenge, accountLookup),
                rivalrySummary(challenge, rivalry, accountLookup));
    }

    /**
     * {@code CookRivalry}'s own cookA/cookB designation is a canonical id-sort order (see
     * {@link CookRivalry#orderPair}), independent of this specific challenge's DishLabel A/B
     * assignment - the two must be reconciled so the returned summary's cookA/cookB matches
     * the challenge's own labeling, swapping win counts if the orders disagree. {@code rivalry}
     * is null when the pair has never been revealed together before (zeroed record).
     */
    private static RivalrySummaryRestDto rivalrySummary(
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
        return new RivalrySummaryRestDto(
                challengeCookA.toString(), challengeCookB.toString(), cookAWins, cookBWins, draws, totalChallenges, headline);
    }
}
