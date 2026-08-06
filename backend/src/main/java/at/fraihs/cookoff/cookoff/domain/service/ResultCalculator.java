package at.fraihs.cookoff.cookoff.domain.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeResult;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import org.jmolecules.ddd.annotation.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spans two aggregates (Challenge, ScoreSubmission) so it lives as a stateless domain
 * service rather than a method on either aggregate.
 */
@Service
public class ResultCalculator {

    public ChallengeResult calculate(Challenge challenge, List<ScoreSubmission> submissions) {
        Map<Category, Map<DishLabel, Integer>> totals = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            totals.put(category, new EnumMap<>(DishLabel.class));
        }

        for (ScoreSubmission submission : submissions) {
            for (Score score : submission.getScores()) {
                totals.get(score.category()).merge(score.dishLabel(), score.points(), Integer::sum);
            }
        }

        Map<Category, DishLabel> categoryWinners = new EnumMap<>(Category.class);
        int cookAWins = 0;
        int cookBWins = 0;

        for (Category category : Category.values()) {
            Map<DishLabel, Integer> byLabel = totals.get(category);
            int scoreA = byLabel.getOrDefault(DishLabel.A, 0);
            int scoreB = byLabel.getOrDefault(DishLabel.B, 0);
            if (scoreA > scoreB) {
                categoryWinners.put(category, DishLabel.A);
                cookAWins++;
            } else if (scoreB > scoreA) {
                categoryWinners.put(category, DishLabel.B);
                cookBWins++;
            }
            // equal sums -> no winner for this category, omitted from categoryWinners
        }

        AccountId overallWinner = null;
        if (cookAWins > cookBWins) {
            overallWinner = challenge.cookAssignmentFor(DishLabel.A).accountId();
        } else if (cookBWins > cookAWins) {
            overallWinner = challenge.cookAssignmentFor(DishLabel.B).accountId();
        }

        return new ChallengeResult(categoryWinners, totals, overallWinner);
    }
}
