package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ScoreEmbeddable;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ScoreSubmissionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ScoreSubmission is immutable with no public constructor (only submit/reconstitute
 * factories), so this mapping is fully hand-written rather than MapStruct-generated. It's a
 * plain constructor-injected {@code @Component}, not a MapStruct {@code @Mapper} abstract
 * class: MapStruct doesn't forward a hand-declared constructor to its generated {@code Impl}
 * subclass, so an abstract {@code @Mapper} class can't have hand-written methods reach a
 * constructor-injected sub-mapper field — only a plain class can, per
 * docs/backend/03-code-style.md#mapper-usage-mapstruct. Composes ScoreSubmissionIdMapper,
 * ChallengeIdMapper, AccountIdMapper, and ScoreMapper for its sub-objects, per that doc's
 * mapper-composition rule.
 */
@Component
@RequiredArgsConstructor
public class ScoreSubmissionMapper {

    private final ScoreSubmissionIdMapper scoreSubmissionIdMapper;
    private final ChallengeIdMapper challengeIdMapper;
    private final AccountIdMapper accountIdMapper;
    private final ScoreMapper scoreMapper;

    public ScoreSubmission toDomain(ScoreSubmissionJpaEntity entity) {
        List<Score> scores = entity.getScores().stream().map(scoreMapper::toDomain).toList();
        return ScoreSubmission.reconstitute(
                scoreSubmissionIdMapper.toDomain(entity.getId()),
                challengeIdMapper.toDomain(entity.getChallengeId()),
                accountIdMapper.toDomain(entity.getGuestAccountId()),
                scores,
                entity.getSubmittedAt());
    }

    public ScoreSubmissionJpaEntity toEntity(ScoreSubmission submission) {
        List<ScoreEmbeddable> scores = submission.getScores().stream()
                .map(scoreMapper::toEmbeddable)
                .toList();
        return new ScoreSubmissionJpaEntity(
                scoreSubmissionIdMapper.toRaw(submission.getId()),
                challengeIdMapper.toRaw(submission.getChallengeId()),
                accountIdMapper.toRaw(submission.getGuestAccountId()),
                submission.getSubmittedAt(),
                new ArrayList<>(scores));
    }
}
