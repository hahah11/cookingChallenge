package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Guest/cook-facing challenge view via a link token — deliberately omits the
 * cook-to-label mapping until the challenge is REVEALED (per
 * docs/cookingChallenge/first-plan.md: "the assignment is never exposed to guests
 * before reveal"). Use {@link ChallengeView} instead for the organizer-facing view,
 * which always includes the mapping.
 */
public record ChallengeParticipantView(
        String id,
        LocalDate date,
        String title,
        String dishName,
        String status,
        List<String> labels,
        List<String> categories,
        List<String> guestAccountIds,
        List<ChallengeView.CookAssignmentView> cookAssignments) {

    public static ChallengeParticipantView from(Challenge challenge) {
        boolean revealed = challenge.getStatus() == ChallengeStatus.REVEALED;
        return new ChallengeParticipantView(
                challenge.getId().toString(),
                challenge.getDate(),
                challenge.getTitle(),
                challenge.getDishName().toString(),
                challenge.getStatus().name(),
                Arrays.stream(DishLabel.values()).map(Enum::name).toList(),
                Arrays.stream(Category.values()).map(Enum::name).toList(),
                challenge.getGuestAccountIds().stream().map(Object::toString).toList(),
                revealed ? ChallengeView.from(challenge).cookAssignments() : null);
    }
}
