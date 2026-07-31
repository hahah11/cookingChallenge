package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;

import java.time.LocalDate;
import java.util.List;

public record ChallengeView(
        String id,
        LocalDate date,
        String title,
        String dishName,
        String status,
        List<CookAssignmentView> cookAssignments,
        List<String> guestAccountIds,
        String createdByAccountId) {

    public record CookAssignmentView(String accountId, String label) {
    }

    public static ChallengeView from(Challenge challenge) {
        List<CookAssignmentView> cookAssignments = challenge.getCookAssignments().stream()
                .map(assignment -> new CookAssignmentView(assignment.accountId().toString(), assignment.label().name()))
                .toList();
        List<String> guestAccountIds = challenge.getGuestAccountIds().stream()
                .map(AccountId::toString)
                .toList();
        return new ChallengeView(
                challenge.getId().toString(),
                challenge.getDate(),
                challenge.getTitle(),
                challenge.getDishName().toString(),
                challenge.getStatus().name(),
                cookAssignments,
                guestAccountIds,
                challenge.getCreatedBy().toString());
    }
}
