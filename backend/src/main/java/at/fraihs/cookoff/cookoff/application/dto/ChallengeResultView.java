package at.fraihs.cookoff.cookoff.application.dto;

import java.util.List;
import java.util.Map;

public record ChallengeResultView(
        String challengeId,
        Map<String, String> categoryWinners,
        String overallWinnerAccountId,
        List<ChallengeView.CookAssignmentView> cookAssignments) {
}
