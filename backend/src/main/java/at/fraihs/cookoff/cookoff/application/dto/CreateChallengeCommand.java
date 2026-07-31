package at.fraihs.cookoff.cookoff.application.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateChallengeCommand(
        LocalDate date,
        String title,
        String dishName,
        String cookAAccountId,
        String cookBAccountId,
        List<String> guestAccountIds,
        String organizerAccountId) {
}
