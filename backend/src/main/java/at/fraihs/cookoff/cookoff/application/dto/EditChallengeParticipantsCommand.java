package at.fraihs.cookoff.cookoff.application.dto;

import java.util.List;

public record EditChallengeParticipantsCommand(String challengeId, String organizerAccountId,
                                                String newCookAAccountId, String newCookBAccountId,
                                                List<String> guestIdsToAdd, List<String> guestIdsToRemove) {
}
