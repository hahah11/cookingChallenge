package at.fraihs.cookoff.cookoff.domain.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;

import java.util.Map;

/**
 * A tied category is omitted from categoryWinners (no winner for that category).
 * overallWinnerAccountId is null when categories split evenly (a draw).
 */
public record ChallengeResult(Map<Category, DishLabel> categoryWinners, AccountId overallWinnerAccountId) {

    public ChallengeResult {
        categoryWinners = Map.copyOf(categoryWinners);
    }
}
