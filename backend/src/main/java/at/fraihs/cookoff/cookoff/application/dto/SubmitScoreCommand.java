package at.fraihs.cookoff.cookoff.application.dto;

import java.util.List;

public record SubmitScoreCommand(String challengeId, String guestAccountId, List<ScoreInput> scores) {
}
