package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

/**
 * ScoreSubmission is immutable with no public constructor (only submit/reconstitute
 * factories), so this mapper delegates to ScoreSubmission.reconstitute(...) rather than
 * MapStruct's generated field-by-field mapping, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct. Score <-> ScoreEmbeddable map 1:1.
 */
@Mapper(componentModel = "spring")
public interface ScoreSubmissionMapper {

    default ScoreSubmission toDomain(ScoreSubmissionJpaEntity entity) {
        List<Score> scores = entity.getScores().stream().map(this::toScore).toList();
        return ScoreSubmission.reconstitute(
                new ScoreSubmissionId(entity.getId()),
                new ChallengeId(entity.getChallengeId()),
                new AccountId(entity.getGuestAccountId()),
                scores,
                entity.getSubmittedAt());
    }

    default ScoreSubmissionJpaEntity toEntity(ScoreSubmission submission) {
        List<ScoreEmbeddable> scores = submission.getScores().stream()
                .map(this::toEmbeddable)
                .toList();
        return new ScoreSubmissionJpaEntity(
                submission.getId().value(),
                submission.getChallengeId().value(),
                submission.getGuestAccountId().value(),
                submission.getSubmittedAt(),
                new ArrayList<>(scores));
    }

    default Score toScore(ScoreEmbeddable embeddable) {
        return new Score(embeddable.getDishLabel(), embeddable.getCategory(), embeddable.getPoints());
    }

    default ScoreEmbeddable toEmbeddable(Score score) {
        return new ScoreEmbeddable(score.dishLabel(), score.category(), score.points());
    }
}
