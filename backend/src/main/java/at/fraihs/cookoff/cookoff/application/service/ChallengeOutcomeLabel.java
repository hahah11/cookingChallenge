package at.fraihs.cookoff.cookoff.application.service;

/**
 * Server-rendered display text for a single challenge's outcome within a rivalry, e.g.
 * "Alice won" / "Draw" / "Pending". Mirrors {@link RivalryHeadline}'s pattern for the
 * pair-level record.
 */
public final class ChallengeOutcomeLabel {

    private ChallengeOutcomeLabel() {
    }

    public static String build(boolean revealed, String overallWinnerAccountId,
                                String cookAAccountId, String cookAName,
                                String cookBAccountId, String cookBName) {
        if (!revealed) {
            return "Pending";
        }
        if (overallWinnerAccountId == null) {
            return "Draw";
        }
        if (overallWinnerAccountId.equals(cookAAccountId)) {
            return cookAName + " won";
        }
        if (overallWinnerAccountId.equals(cookBAccountId)) {
            return cookBName + " won";
        }
        return "Draw";
    }
}
