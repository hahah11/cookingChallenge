package at.fraihs.cookoff.cookoff.application.dto;

import java.util.List;

public record SubmissionStatusView(
        String challengeId,
        int totalGuestCount,
        int submittedGuestCount,
        List<String> submittedGuestAccountIds) {
}
