package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Map;

/**
 * A tied category is omitted from categoryWinners (no winner for that category).
 * overallWinnerAccountId is null when categories split evenly (a draw). categoryTotals
 * carries every category's raw per-dish point sum (including ties), for the results
 * screen's score table - see openapi-first-api-plan.md's ChallengeResult.categoryTotals note.
 */
@ValueObject
public record ChallengeResult(Map<Category, DishLabel> categoryWinners,
                               Map<Category, Map<DishLabel, Integer>> categoryTotals,
                               AccountId overallWinnerAccountId) {

    public ChallengeResult {
        categoryWinners = Map.copyOf(categoryWinners);
        categoryTotals = Map.copyOf(categoryTotals);
    }
}
