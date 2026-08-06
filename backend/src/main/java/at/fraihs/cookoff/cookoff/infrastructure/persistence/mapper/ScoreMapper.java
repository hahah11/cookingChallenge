package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ScoreEmbeddable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScoreMapper {

    default Score toDomain(ScoreEmbeddable embeddable) {
        return new Score(embeddable.getDishLabel(), embeddable.getCategory(), embeddable.getPoints());
    }

    default ScoreEmbeddable toEmbeddable(Score score) {
        return new ScoreEmbeddable(score.dishLabel(), score.category(), score.points());
    }
}
