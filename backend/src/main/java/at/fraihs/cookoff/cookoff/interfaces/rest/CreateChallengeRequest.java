package at.fraihs.cookoff.cookoff.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateChallengeRequest(
        @NotNull(message = "date is required") LocalDate date,
        String title,
        @NotBlank(message = "dishName is required") String dishName,
        @NotBlank(message = "cookAAccountId is required") String cookAAccountId,
        @NotBlank(message = "cookBAccountId is required") String cookBAccountId,
        List<String> guestAccountIds,
        @NotBlank(message = "organizerAccountId is required") String organizerAccountId) {
}
