package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreEmbeddable {

    /**
     * @JdbcTypeCode(SqlTypes.VARCHAR) pins this to a plain VARCHAR bind: without it,
     * Hibernate's dialect-dependent native-enum-type inference (H2 supports a native ENUM
     * column type) binds a value the chk_scores_dish_label/chk_scores_points_range CHECK
     * constraints (VARCHAR(1) columns, literal 'A'/'B' comparison) don't recognize, even
     * though the column itself is plain VARCHAR(1), not a native enum.
     */
    @Column(name = "dish_label")
    @Enumerated(EnumType.STRING)
    private DishLabel dishLabel;

    @Enumerated(EnumType.STRING)
    private Category category;

    private int points;
}
